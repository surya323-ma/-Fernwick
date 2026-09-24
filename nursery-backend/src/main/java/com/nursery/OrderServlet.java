package com.nursery;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Places an order from the signed-in user's cart, and lists their past
 * orders. The whole "place order" flow (stock check, order row, order
 * line items, stock decrement, empty the cart) runs inside a single
 * transaction so a failure partway through leaves nothing half-done.
 */
@WebServlet("/order")
public class OrderServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        Integer userId = sessionUserId(request);
        if (userId == null) {
            response.setStatus(401);
            out.print("{\"error\":\"Please sign in to place an order.\"}");
            return;
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                List<String> productIds = new ArrayList<>();
                List<Integer> quantities = new ArrayList<>();
                List<BigDecimal> prices = new ArrayList<>();
                BigDecimal totalAmount = BigDecimal.ZERO;

                String cartSql = "SELECT cart.product_id, cart.quantity, products.price, products.stock "
                        + "FROM cart JOIN products ON cart.product_id = products.id WHERE cart.user_id = ?";

                try (PreparedStatement cartStatement = connection.prepareStatement(cartSql)) {
                    cartStatement.setInt(1, userId);

                    try (ResultSet cartResult = cartStatement.executeQuery()) {
                        while (cartResult.next()) {
                            int quantity = cartResult.getInt("quantity");
                            int stock = cartResult.getInt("stock");

                            if (quantity > stock) {
                                connection.rollback();
                                response.setStatus(409);
                                out.print("{\"error\":\"Insufficient stock for one or more products\"}");
                                return;
                            }

                            BigDecimal price = cartResult.getBigDecimal("price");
                            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(quantity)));

                            productIds.add(cartResult.getString("product_id"));
                            quantities.add(quantity);
                            prices.add(price);
                        }
                    }
                }

                if (productIds.isEmpty()) {
                    connection.rollback();
                    response.setStatus(400);
                    out.print("{\"error\":\"Cart is empty\"}");
                    return;
                }

                int orderId;
                String orderSql = "INSERT INTO orders (user_id, total_amount, status) VALUES (?, ?, 'Pending')";

                try (PreparedStatement orderStatement = connection.prepareStatement(
                        orderSql, Statement.RETURN_GENERATED_KEYS)) {
                    orderStatement.setInt(1, userId);
                    orderStatement.setBigDecimal(2, totalAmount);
                    orderStatement.executeUpdate();

                    try (ResultSet generatedKeys = orderStatement.getGeneratedKeys()) {
                        if (!generatedKeys.next()) {
                            connection.rollback();
                            response.setStatus(500);
                            out.print("{\"error\":\"Unable to create order\"}");
                            return;
                        }
                        orderId = generatedKeys.getInt(1);
                    }
                }

                String orderItemSql = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
                String stockSql = "UPDATE products SET stock = stock - ? WHERE id = ?";

                try (PreparedStatement orderItemStatement = connection.prepareStatement(orderItemSql);
                     PreparedStatement stockStatement = connection.prepareStatement(stockSql)) {

                    for (int i = 0; i < productIds.size(); i++) {
                        orderItemStatement.setInt(1, orderId);
                        orderItemStatement.setString(2, productIds.get(i));
                        orderItemStatement.setInt(3, quantities.get(i));
                        orderItemStatement.setBigDecimal(4, prices.get(i));
                        orderItemStatement.executeUpdate();

                        stockStatement.setInt(1, quantities.get(i));
                        stockStatement.setString(2, productIds.get(i));
                        stockStatement.executeUpdate();
                    }
                }

                try (PreparedStatement clearCartStatement = connection.prepareStatement(
                        "DELETE FROM cart WHERE user_id = ?")) {
                    clearCartStatement.setInt(1, userId);
                    clearCartStatement.executeUpdate();
                }

                connection.commit();

                out.print("{\"success\":true,\"order_id\":" + orderId
                        + ",\"total_amount\":" + totalAmount.toPlainString()
                        + ",\"message\":\"Order placed successfully\"}");

            } catch (Exception e) {
                try {
                    connection.rollback();
                } catch (Exception rollbackException) {
                    rollbackException.printStackTrace();
                }
                e.printStackTrace();
                response.setStatus(500);
                out.print("{\"error\":\"Unable to place order\"}");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"error\":\"Unable to place order\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        Integer userId = sessionUserId(request);
        if (userId == null) {
            out.print("[]");
            return;
        }

        String ordersSql = "SELECT id, total_amount, order_date, status FROM orders "
                + "WHERE user_id = ? ORDER BY order_date DESC";
        String itemsSql = "SELECT order_items.product_id, order_items.quantity, order_items.price, products.name, products.image "
                + "FROM order_items JOIN products ON order_items.product_id = products.id WHERE order_items.order_id = ?";

        try (Connection connection = DBConnection.getConnection()) {

            List<String> orders = new ArrayList<>();

            try (PreparedStatement statement = connection.prepareStatement(ordersSql)) {
                statement.setInt(1, userId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        int orderId = resultSet.getInt("id");

                        StringBuilder itemsJson = new StringBuilder("[");
                        try (PreparedStatement itemsStatement = connection.prepareStatement(itemsSql)) {
                            itemsStatement.setInt(1, orderId);
                            try (ResultSet itemsResult = itemsStatement.executeQuery()) {
                                boolean firstItem = true;
                                while (itemsResult.next()) {
                                    if (!firstItem) {
                                        itemsJson.append(",");
                                    }
                                    itemsJson.append("{");
                                    itemsJson.append("\"product_id\":").append(JsonUtil.quoteOrNull(itemsResult.getString("product_id"))).append(",");
                                    itemsJson.append("\"name\":").append(JsonUtil.quoteOrNull(itemsResult.getString("name"))).append(",");
                                    itemsJson.append("\"image\":").append(JsonUtil.quoteOrNull(itemsResult.getString("image"))).append(",");
                                    itemsJson.append("\"quantity\":").append(itemsResult.getInt("quantity")).append(",");
                                    itemsJson.append("\"price\":").append(itemsResult.getBigDecimal("price").toPlainString());
                                    itemsJson.append("}");
                                    firstItem = false;
                                }
                            }
                        }
                        itemsJson.append("]");

                        String orderJson = "{"
                                + "\"id\":" + orderId + ","
                                + "\"total_amount\":" + resultSet.getBigDecimal("total_amount").toPlainString() + ","
                                + "\"order_date\":" + JsonUtil.quoteOrNull(String.valueOf(resultSet.getTimestamp("order_date"))) + ","
                                + "\"status\":" + JsonUtil.quoteOrNull(resultSet.getString("status")) + ","
                                + "\"items\":" + itemsJson
                                + "}";
                        orders.add(orderJson);
                    }
                }
            }

            out.print("[" + String.join(",", orders) + "]");

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"error\":\"Unable to fetch orders\"}");
        }
    }

    private Integer sessionUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (Integer) session.getAttribute("userId");
    }
}
