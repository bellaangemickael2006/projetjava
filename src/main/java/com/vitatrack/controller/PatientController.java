package com.vitatrack.controller;

import com.sun.net.httpserver.*;
import com.vitatrack.util.*;
import java.io.IOException;
import java.util.*;

public class PatientController implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Map<String, String> session = AdminController.requireRole(ex, "patient");
        if (session == null) return;

        String method = ex.getRequestMethod();
        String sub    = Http.subPath(ex, "/patient");

        try {
            switch (sub) {
                case "", "dashboard" -> showDashboard(ex, session);
                case "traitements"   -> showTraitements(ex, session);
                case "rendezvous"    -> showRendezvous(ex, session);
                case "signalement"   -> {
                    if ("POST".equals(method)) saveSignalement(ex, session);
                    else Http.html(ex, 200, Html.signalementPage(session.get("nom"), null, null));
                }
                case "questionnaires" -> showQuestionnaires(ex, session);
                case "passeport"     -> showPasseport(ex, session);
                default              -> Http.html(ex, 404, Html.notFoundPage());
            }
        } catch (Exception e) {
            Http.html(ex, 500, "<h1>Erreur</h1><pre>" + e.getMessage() + "</pre>");
        }
    }

    private void showDashboard(HttpExchange ex, Map<String, String> session) throws IOException {
        Http.html(ex, 200, Html.patientDashboard(session.get("nom"), new HashMap<>()));
    }

    private void showTraitements(HttpExchange ex, Map<String, String> session) throws Exception {
        String json = SupabaseClient.select("traitements",
                "select=*&patient_id=eq." + session.get("userId") + "&actif=eq.true&order=created_at.desc");
        Http.html(ex, 200, Html.patientTraitements(session.get("nom"), Json.array(json)));
    }

    private void showRendezvous(HttpExchange ex, Map<String, String> session) throws Exception {
        String json = SupabaseClient.select("rendezvous",
                "select=*&patient_id=eq." + session.get("userId") + "&order=date_rdv.asc");
        Http.html(ex, 200, Html.patientRendezvous(session.get("nom"), Json.array(json)));
    }

    private void saveSignalement(HttpExchange ex, Map<String, String> session) throws Exception {
        Map<String, String> form = Http.form(ex);
        String type    = form.getOrDefault("type", "autre");
        String desc    = form.getOrDefault("description", "");
        String intensite = form.getOrDefault("intensite", "5");

        if (desc.isBlank()) {
            Http.html(ex, 400, Html.signalementPage(session.get("nom"), "La description est obligatoire.", null));
            return;
        }

        int intVal = 5;
        try { intVal = Integer.parseInt(intensite); } catch (Exception ignored) {}
        boolean urgent = intVal >= 8;

        // Trouver le médecin du patient
        String medecinId = getMedecinId(session.get("userId"));

        String json = Json.build(Json.map(
                "patient_id", session.get("userId"),
                "medecin_id", medecinId != null ? medecinId : "",
                "type", type,
                "description", desc,
                "intensite", intVal,
                "urgent", urgent,
                "traite", false));
        SupabaseClient.insert("signalements", json);

        Http.html(ex, 200, Html.signalementPage(session.get("nom"), null,
                "Votre signalement a été envoyé au médecin."));
    }

    private void showQuestionnaires(HttpExchange ex, Map<String, String> session) throws Exception {
        String json = SupabaseClient.select("questionnaires",
                "select=*&patient_id=eq." + session.get("userId") + "&order=created_at.desc");
        List<String> rows = Json.array(json);
        var sb = new StringBuilder("<h1>Mes questionnaires</h1>");
        if (rows.isEmpty()) {
            sb.append("<p class=\"empty\">Aucun questionnaire en attente.</p>");
        } else {
            for (String r : rows) {
                sb.append("<div class=\"journal-entry\">")
                  .append("<strong>").append(Tpl.escHtml(Json.get(r, "titre"))).append("</strong>")
                  .append(" — ").append(Tpl.escHtml(Json.get(r, "statut")))
                  .append("</div>");
            }
        }
        Http.html(ex, 200, Html.layout("Questionnaires", "patient", session.get("nom"), sb.toString()));
    }

    private void showPasseport(HttpExchange ex, Map<String, String> session) throws Exception {
        String json = SupabaseClient.select("passeport_sante",
                "select=*&patient_id=eq." + session.get("userId") + "&limit=1");
        List<String> rows = Json.array(json);
        String data = rows.isEmpty() ? "{}" : rows.get(0);
        Http.html(ex, 200, Html.passeportPage(session.get("nom"), data));
    }

    private String getMedecinId(String patientId) {
        try {
            String json = SupabaseClient.select("utilisateurs",
                    "select=medecin_id&id=eq." + patientId + "&limit=1");
            List<String> rows = Json.array(json);
            return rows.isEmpty() ? null : Json.get(rows.get(0), "medecin_id");
        } catch (Exception e) { return null; }
    }
}
