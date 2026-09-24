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
import java.sql.Statement;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String name = trim(request.getParameter("name"));
        String email = trim(request.getParameter("email")).toLowerCase();
        String password = request.getParameter("password");

        if (name.isEmpty() || email.isEmpty() || !email.contains("@")
                || password == null || password.length() < 6) {
            response.setStatus(400);
            out.print("{\"error\":\"Please enter your name, a valid email and a password of at least 6 characters.\"}");
            return;
        }

        try (Connection connection = DBConnection.getConnection()) {

            try (PreparedStatement check = connection.prepareStatement("SELECT id FROM users WHERE email = ?")) {
                check.setString(1, email);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        response.setStatus(409);
                        out.print("{\"error\":\"An account with that email already exists.\"}");
                        return;
                    }
                }
            }

            String passwordHash = PasswordUtil.hash(password);

            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {

                insert.setString(1, name);
                insert.setString(2, email);
                insert.setString(3, passwordHash);
                insert.executeUpdate();

                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (!keys.next()) {
                        response.setStatus(500);
                        out.print("{\"error\":\"Unable to create your account.\"}");
                        return;
                    }

                    int userId = keys.getInt(1);
                    HttpSession session = request.getSession(true);
                    session.setAttribute("userId", userId);
                    session.setAttribute("userName", name);
                    session.setAttribute("userEmail", email);

                    out.print("{\"success\":true,\"id\":" + userId
                            + ",\"name\":\"" + JsonUtil.escape(name) + "\""
                            + ",\"email\":\"" + JsonUtil.escape(email) + "\"}");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"error\":\"Unable to create your account right now.\"}");
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
