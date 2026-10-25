package com.vitatrack.controller;

import com.sun.net.httpserver.*;
import com.vitatrack.util.*;
import java.io.IOException;
import java.util.*;

public class AdminController implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Map<String, String> session = requireRole(ex, "admin");
        if (session == null) return;

        String method = ex.getRequestMethod();
        String sub    = Http.subPath(ex, "/admin");

        try {
            switch (sub) {
                case "", "dashboard" -> showDashboard(ex, session);
                case "nouveau-medecin" -> {
                    if ("POST".equals(method)) createMedecin(ex, session);
                    else Http.html(ex, 200, Html.adminNewMedecin(null));
                }
                default -> Http.html(ex, 404, Html.notFoundPage());
            }
        } catch (Exception e) {
            Http.html(ex, 500, "<h1>Erreur</h1><pre>" + e.getMessage() + "</pre>");
        }
    }

    private void showDashboard(HttpExchange ex, Map<String, String> session) throws Exception {
        String medecinsJson = SupabaseClient.select("utilisateurs",
                "select=*&role=eq.medecin&order=nom");
        String patientsJson = SupabaseClient.select("utilisateurs",
                "select=*&role=eq.patient&order=nom");
        String logsJson     = SupabaseClient.select("journal_activite",
                "select=*&order=created_at.desc&limit=50");

        Http.html(ex, 200, Html.adminDashboard(
                session.get("nom"),
                Json.array(medecinsJson),
                Json.array(patientsJson),
                Json.array(logsJson)));
    }

    private void createMedecin(HttpExchange ex, Map<String, String> session) throws Exception {
        Map<String, String> form = Http.form(ex);
        String email    = form.getOrDefault("email","").trim();
        String nom      = form.getOrDefault("nom","").trim();
        String password = form.getOrDefault("password","");
        String spec     = form.getOrDefault("specialite","").trim();

        if (email.isBlank() || nom.isBlank() || password.isBlank()) {
            Http.html(ex, 400, Html.adminNewMedecin("Tous les champs sont obligatoires."));
            return;
        }

        // Créer le compte Supabase Auth
        String authResp = SupabaseClient.signUp(email, password);
        String userId   = Json.get(authResp, "id");

        if (userId == null) {
            String msg = Json.get(authResp, "message");
            Http.html(ex, 400, Html.adminNewMedecin(msg != null ? msg : "Erreur création du compte."));
            return;
        }

        // Insérer dans la table utilisateurs
        String json = Json.build(Json.map(
                "id", userId, "email", email, "nom", nom,
                "role", "medecin", "specialite", spec, "statut", "actif",
                "cree_par", session.get("userId")));
        SupabaseClient.insert("utilisateurs", json);

        Http.redirect(ex, "/admin");
    }

    static Map<String, String> requireRole(HttpExchange ex, String role) throws IOException {
        String sid = Http.cookie(ex, "VTKSID");
        Map<String, String> session = Session.get(sid);
        if (session == null) { Http.redirect(ex, "/login"); return null; }
        if (!role.equals(session.get("role"))) {
            Http.html(ex, 403, "<h1>Accès refusé</h1><p>Rôle requis : " + role + "</p>");
            return null;
        }
        return session;
    }
}
