package com.nursery;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/cart")
public class CartServlet extends HttpServlet {

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

        String sql = "SELECT cart.id, cart.product_id, cart.quantity, products.name, products.price, "
                + "products.image, products.stock FROM cart JOIN products ON cart.product_id = products.id "
                + "WHERE cart.user_id = ? ORDER BY cart.id ASC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) {
                        json.append(",");
                    }
                    json.append("{");
                    json.append("\"id\":").append(rs.getInt("id")).append(",");
                    json.append("\"product_id\":").append(JsonUtil.quoteOrNull(rs.getString("product_id"))).append(",");
                    json.append("\"name\":").append(JsonUtil.quoteOrNull(rs.getString("name"))).append(",");
                    json.append("\"price\":").append(rs.getBigDecimal("price").toPlainString()).append(",");
                    json.append("\"image\":").append(JsonUtil.quoteOrNull(rs.getString("image"))).append(",");
                    json.append("\"quantity\":").append(rs.getInt("quantity")).append(",");
                    json.append("\"stock\":").append(rs.getInt("stock"));
                    json.append("}");
                    first = false;
                }
                json.append("]");
                out.print(json.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"error\":\"Unable to load your basket.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        Integer userId = sessionUserId(request);
        if (userId == null) {
            response.setStatus(401);
            out.print("{\"error\":\"Please sign in to add items to your basket.\"}");
            return;
        }

        String productId = request.getParameter("product_id");
        if (productId == null || productId.isBlank()) {
            response.setStatus(400);
            out.print("{\"error\":\"product_id is required.\"}");
            return;
        }

        int quantity;
        try {
            quantity = Math.max(1, Integer.parseInt(request.getParameter("quantity")));
        } catch (Exception e) {
            quantity = 1;
        }

        try (Connection connection = DBConnection.getConnection()) {

            try (PreparedStatement check = connection.prepareStatement("SELECT id FROM products WHERE id = ?")) {
                check.setString(1, productId);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        response.setStatus(404);
                        out.print("{\"error\":\"That plant no longer exists.\"}");
                        return;
                    }
                }
            }

            try (PreparedStatement find = connection.prepareStatement(
                    "SELECT id, quantity FROM cart WHERE user_id = ? AND product_id = ?")) {
                find.setInt(1, userId);
                find.setString(2, productId);

                try (ResultSet existing = find.executeQuery()) {
                    if (existing.next()) {
                        int newQuantity = existing.getInt("quantity") + quantity;
                        try (PreparedStatement update = connection.prepareStatement(
                                "UPDATE cart SET quantity = ? WHERE id = ?")) {
                            update.setInt(1, newQuantity);
                            update.setInt(2, existing.getInt("id"));
                            update.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement insert = connection.prepareStatement(
                                "INSERT INTO cart (user_id, product_id, quantity) VALUES (?, ?, ?)")) {
                            insert.setInt(1, userId);
                            insert.setString(2, productId);
                            insert.setInt(3, quantity);
                            insert.executeUpdate();
                        }
                    }
                }
            }

            out.print("{\"success\":true}");

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"error\":\"Unable to add that item to your basket.\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        Integer userId = sessionUserId(request);
        if (userId == null) {
            response.setStatus(401);
            out.print("{\"error\":\"Please sign in.\"}");
            return;
        }

        String idParameter = request.getParameter("id");
        if (idParameter == null || idParameter.isBlank()) {
            response.setStatus(400);
            out.print("{\"error\":\"id is required.\"}");
            return;
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM cart WHERE id = ? AND user_id = ?")) {

            statement.setInt(1, Integer.parseInt(idParameter));
            statement.setInt(2, userId);

            int affected = statement.executeUpdate();
            if (affected == 0) {
                response.setStatus(404);
                out.print("{\"error\":\"Basket item not found.\"}");
                return;
            }

            out.print("{\"success\":true}");

        } catch (NumberFormatException e) {
            response.setStatus(400);
            out.print("{\"error\":\"Invalid id.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"error\":\"Unable to remove that item.\"}");
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
