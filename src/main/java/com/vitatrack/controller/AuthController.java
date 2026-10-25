package com.vitatrack.controller;

import com.sun.net.httpserver.*;
import com.vitatrack.util.*;
import java.io.IOException;
import java.util.Map;

public class AuthController implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path   = ex.getRequestURI().getPath();

        try {
            if ("/logout".equals(path)) {
                String sid = Http.cookie(ex, "VTKSID");
                Session.destroy(sid);
                Http.setCookie(ex, "VTKSID", "", true);
                Http.redirect(ex, "/login");
                return;
            }

            // Si déjà connecté, rediriger
            String sid = Http.cookie(ex, "VTKSID");
            Map<String, String> session = Session.get(sid);
            if (session != null && ("/".equals(path) || "/login".equals(path))) {
                redirectByRole(ex, session.get("role"));
                return;
            }

            if ("POST".equals(method) && "/login".equals(path)) {
                handleLogin(ex);
            } else {
                showLogin(ex, null);
            }
        } catch (Exception e) {
            Http.html(ex, 500, "<h1>Erreur serveur</h1><pre>" + e.getMessage() + "</pre>");
        }
    }

    private void handleLogin(HttpExchange ex) throws Exception {
        Map<String, String> form = Http.form(ex);
        String email    = form.getOrDefault("email", "").trim().toLowerCase();
        String password = form.getOrDefault("password", "");

        if (email.isBlank() || password.isBlank()) {
            showLogin(ex, "Veuillez remplir tous les champs.");
            return;
        }

        // Authentification via Supabase Auth
        String authResp = SupabaseClient.signIn(email, password);
        String jwt      = Json.get(authResp, "access_token");

        if (jwt == null) {
            String msg = Json.get(authResp, "message");
            showLogin(ex, msg != null ? msg : "Email ou mot de passe incorrect.");
            return;
        }

        // Récupérer le profil depuis la table utilisateurs
        String userId = Json.get(authResp, "id");
        if (userId == null) {
            // Extraire depuis user object
            String userJson = extractUserBlock(authResp);
            userId = Json.get(userJson, "id");
        }

        String profil = SupabaseClient.select("utilisateurs",
                "select=*&email=eq." + email + "&limit=1");
        String role   = "patient";
        String nom    = email;
        String uid    = userId;

        java.util.List<String> rows = Json.array(profil);
        if (!rows.isEmpty()) {
            String row = rows.get(0);
            role = Json.get(row, "role");
            nom  = Json.get(row, "nom");
            uid  = Json.get(row, "id");
        }

        String newSid = Session.create(uid, role, email, nom, jwt);
        Http.setCookie(ex, "VTKSID", newSid, false);
        redirectByRole(ex, role);
    }

    private String extractUserBlock(String json) {
        int i = json.indexOf("\"user\"");
        if (i < 0) return json;
        int start = json.indexOf("{", i);
        if (start < 0) return json;
        int depth = 0; int end = start;
        for (; end < json.length(); end++) {
            if (json.charAt(end) == '{') depth++;
            else if (json.charAt(end) == '}') { depth--; if (depth == 0) break; }
        }
        return json.substring(start, end + 1);
    }

    private void redirectByRole(HttpExchange ex, String role) throws IOException {
        switch (role == null ? "" : role) {
            case "admin"   -> Http.redirect(ex, "/admin");
            case "medecin" -> Http.redirect(ex, "/medecin");
            default        -> Http.redirect(ex, "/patient");
        }
    }

    private void showLogin(HttpExchange ex, String error) throws IOException {
        String body = Html.loginPage(error);
        Http.html(ex, 200, body);
    }
}
