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

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            response.setStatus(400);
            out.print("{\"error\":\"Enter your email and password.\"}");
            return;
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, name, email, password_hash FROM users WHERE email = ?")) {

            statement.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next() || !PasswordUtil.verify(password, rs.getString("password_hash"))) {
                    response.setStatus(401);
                    out.print("{\"error\":\"Incorrect email or password.\"}");
                    return;
                }

                int userId = rs.getInt("id");
                String name = rs.getString("name");
                String userEmail = rs.getString("email");

                HttpSession session = request.getSession(true);
                session.setAttribute("userId", userId);
                session.setAttribute("userName", name);
                session.setAttribute("userEmail", userEmail);

                out.print("{\"success\":true,\"id\":" + userId
                        + ",\"name\":\"" + JsonUtil.escape(name) + "\""
                        + ",\"email\":\"" + JsonUtil.escape(userEmail) + "\"}");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"error\":\"Unable to sign in right now.\"}");
        }
    }
}
