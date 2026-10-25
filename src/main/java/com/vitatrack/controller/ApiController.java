package com.vitatrack.controller;

import com.sun.net.httpserver.*;
import com.vitatrack.util.*;
import java.io.IOException;
import java.util.*;

/** API JSON pour les appels AJAX (notifications, stats). */
public class ApiController implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
        String sid = Http.cookie(ex, "VTKSID");
        Map<String, String> session = Session.get(sid);
        if (session == null) { Http.json(ex, 401, "{\"error\":\"Non authentifié\"}"); return; }

        String sub = Http.subPath(ex, "/api");
        try {
            switch (sub) {
                case "notifications" -> getNotifications(ex, session);
                case "stats"         -> getStats(ex, session);
                default -> Http.json(ex, 404, "{\"error\":\"Route inconnue\"}");
            }
        } catch (Exception e) {
            Http.json(ex, 500, "{\"error\":\"" + SupabaseClient.esc(e.getMessage()) + "\"}");
        }
    }

    private void getNotifications(HttpExchange ex, Map<String, String> session) throws Exception {
        String userId = session.get("userId");
        String role   = session.get("role");
        String json;

        if ("medecin".equals(role)) {
            json = SupabaseClient.select("signalements",
                    "select=*&medecin_id=eq." + userId + "&traite=eq.false&urgent=eq.true&order=created_at.desc&limit=5");
        } else {
            json = SupabaseClient.select("rendezvous",
                    "select=*&patient_id=eq." + userId + "&statut=eq.planifie&order=date_rdv.asc&limit=5");
        }
        Http.json(ex, 200, json);
    }

    private void getStats(HttpExchange ex, Map<String, String> session) throws Exception {
        if (!"admin".equals(session.get("role"))) {
            Http.json(ex, 403, "{\"error\":\"Accès refusé\"}");
            return;
        }
        String medecins = SupabaseClient.select("utilisateurs", "select=id&role=eq.medecin");
        String patients = SupabaseClient.select("utilisateurs", "select=id&role=eq.patient");
        String signalements = SupabaseClient.select("signalements", "select=id&urgent=eq.true&traite=eq.false");

        Http.json(ex, 200, "{\"medecins\":" + Json.array(medecins).size()
                + ",\"patients\":" + Json.array(patients).size()
                + ",\"alertes\":" + Json.array(signalements).size() + "}");
    }
}
