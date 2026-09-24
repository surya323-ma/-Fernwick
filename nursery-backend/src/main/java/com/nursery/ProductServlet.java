package com.nursery;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/products")
public class ProductServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String id = request.getParameter("id");

        try (Connection connection = DBConnection.getConnection()) {

            if (id != null && !id.isBlank()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM products WHERE id = ?")) {
                    statement.setString(1, id);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            response.setStatus(404);
                            out.print("{\"error\":\"Product not found.\"}");
                            return;
                        }
                        out.print(toJson(rs));
                    }
                }
                return;
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM products ORDER BY sort_order ASC");
                 ResultSet rs = statement.executeQuery()) {

                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) {
                        json.append(",");
                    }
                    json.append(toJson(rs));
                    first = false;
                }
                json.append("]");
                out.print(json.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"error\":\"Unable to load products.\"}");
        }
    }

    private String toJson(ResultSet rs) throws SQLException {
        BigDecimal price = rs.getBigDecimal("price");
        BigDecimal wasPrice = rs.getBigDecimal("was_price");

        StringBuilder json = new StringBuilder("{");
        json.append("\"id\":").append(JsonUtil.quoteOrNull(rs.getString("id"))).append(",");
        json.append("\"name\":").append(JsonUtil.quoteOrNull(rs.getString("name"))).append(",");
        json.append("\"latinName\":").append(JsonUtil.quoteOrNull(rs.getString("latin_name"))).append(",");
        json.append("\"price\":").append(price.toPlainString()).append(",");
        json.append("\"wasPrice\":").append(wasPrice == null ? "null" : wasPrice.toPlainString()).append(",");
        json.append("\"stock\":").append(rs.getInt("stock")).append(",");
        json.append("\"image\":").append(JsonUtil.quoteOrNull(rs.getString("image"))).append(",");
        json.append("\"badge\":").append(JsonUtil.quoteOrNull(rs.getString("badge"))).append(",");
        json.append("\"light\":").append(JsonUtil.quoteOrNull(rs.getString("light"))).append(",");
        json.append("\"water\":").append(JsonUtil.quoteOrNull(rs.getString("water"))).append(",");
        json.append("\"category\":").append(JsonUtil.quoteOrNull(rs.getString("category")));
        json.append("}");
        return json.toString();
    }
}
