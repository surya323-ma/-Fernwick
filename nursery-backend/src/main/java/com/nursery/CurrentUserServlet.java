package com.nursery;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/current-user")
public class CurrentUserServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            out.print("{\"loggedIn\":false}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String name = (String) session.getAttribute("userName");
        String email = (String) session.getAttribute("userEmail");

        out.print("{\"loggedIn\":true,\"id\":" + userId
                + ",\"name\":\"" + JsonUtil.escape(name) + "\""
                + ",\"email\":\"" + JsonUtil.escape(email) + "\"}");
    }
}
