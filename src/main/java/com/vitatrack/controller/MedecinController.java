package com.vitatrack.controller;

import com.sun.net.httpserver.*;
import com.vitatrack.util.*;
import java.io.IOException;
import java.util.*;

public class MedecinController implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Map<String, String> session = AdminController.requireRole(ex, "medecin");
        if (session == null) return;

        String method = ex.getRequestMethod();
        String sub    = Http.subPath(ex, "/medecin");

        try {
            switch (sub) {
                case "", "dashboard" -> showDashboard(ex, session);
                case "nouveau-patient" -> {
                    if ("POST".equals(method)) createPatient(ex, session);
                    else Http.html(ex, 200, Html.medecinNewPatient(null, null));
                }
                case "dossier" -> showDossier(ex, session);
                case "dossier/hospitalisation" -> saveFiche(ex, session, "hospitalisation");
                case "dossier/nouveau-rdv"     -> nouveauRdv(ex, session, method);
                case "dossier/nouveau-journal" -> nouveauJournal(ex, session, method);
                case "dossier/nouveau-questionnaire" -> nouveauQuestionnaire(ex, session, method);
                default -> Http.html(ex, 404, Html.notFoundPage());
            }
        } catch (Exception e) {
            Http.html(ex, 500, "<h1>Erreur</h1><pre>" + e.getMessage() + "</pre>");
        }
    }

    // ─── Dashboard médecin ────────────────────────────────────────────────

    private void showDashboard(HttpExchange ex, Map<String, String> session) throws Exception {
        String medecinId = session.get("userId");
        String patientsJson = SupabaseClient.select("utilisateurs",
                "select=*&role=eq.patient&medecin_id=eq." + medecinId + "&order=nom");
        Http.html(ex, 200, Html.medecinDashboard(session.get("nom"), Json.array(patientsJson)));
    }

    // ─── Création patient ─────────────────────────────────────────────────

    private void createPatient(HttpExchange ex, Map<String, String> session) throws Exception {
        Map<String, String> form = Http.form(ex);
        String email      = form.getOrDefault("email","").trim();
        String nom        = form.getOrDefault("nom","").trim();
        String password   = form.getOrDefault("password","");
        String dob        = form.getOrDefault("date_naissance","");
        String diagnostic = form.getOrDefault("diagnostic","");

        if (email.isBlank() || nom.isBlank() || password.isBlank()) {
            Http.html(ex, 400, Html.medecinNewPatient("Champs obligatoires manquants.", null));
            return;
        }

        String authResp = SupabaseClient.signUp(email, password);
        String userId   = Json.get(authResp, "id");
        if (userId == null) {
            String msg = Json.get(authResp, "message");
            Http.html(ex, 400, Html.medecinNewPatient(msg != null ? msg : "Erreur création.", null));
            return;
        }

        // Insérer utilisateur
        String userJson = Json.build(Json.map(
                "id", userId, "email", email, "nom", nom,
                "role", "patient", "statut", "actif",
                "medecin_id", session.get("userId"),
                "date_naissance", dob,
                "cree_par", session.get("userId")));
        SupabaseClient.insert("utilisateurs", userJson);

        // Créer fiche d'hospitalisation initiale
        if (!diagnostic.isBlank()) {
            String ficheJson = Json.build(Json.map(
                    "patient_id", userId,
                    "medecin_id", session.get("userId"),
                    "diagnostic", diagnostic,
                    "statut", "en_cours"));
            SupabaseClient.insert("hospitalisations", ficheJson);
        }

        Http.html(ex, 200, Html.medecinNewPatient(null,
                "Patient " + nom + " créé avec succès. Il peut se connecter avec " + email));
    }

    // ─── Dossier patient (7 onglets) ──────────────────────────────────────

    private void showDossier(HttpExchange ex, Map<String, String> session) throws Exception {
        String patientId = Http.param(ex, "id");
        String onglet    = Http.param(ex, "onglet");
        if (onglet == null || onglet.isBlank()) onglet = "hospitalisation";
        if (patientId == null) { Http.redirect(ex, "/medecin"); return; }

        // Infos patient
        String patientJson = SupabaseClient.select("utilisateurs",
                "select=*&id=eq." + patientId + "&limit=1");
        List<String> pRows = Json.array(patientJson);
        String patientNom = pRows.isEmpty() ? "Inconnu" : Json.get(pRows.get(0), "nom");

        Map<String, String> data = new HashMap<>();
        data.put("patient_id", patientId);

        switch (onglet) {
            case "hospitalisation" -> loadHospitalisation(patientId, data);
            case "traitements"     -> loadTraitements(patientId, data);
            case "rendezvous"      -> loadRendezvous(patientId, data);
            case "signalements"    -> loadSignalements(patientId, data);
            case "questionnaires"  -> loadQuestionnaires(patientId, data);
            case "journal"         -> loadJournal(patientId, data);
            case "passeport"       -> loadPasseport(patientId, data);
        }

        Http.html(ex, 200, Html.dossierPage(patientNom, patientId, session.get("nom"), onglet, data));
    }

    private void loadHospitalisation(String pid, Map<String, String> d) throws Exception {
        String json = SupabaseClient.select("hospitalisations",
                "select=*&patient_id=eq." + pid + "&order=created_at.desc&limit=1");
        List<String> rows = Json.array(json);
        if (!rows.isEmpty()) {
            String r = rows.get(0);
            d.put("diagnostic",      Json.get(r, "diagnostic"));
            d.put("date_entree",     Json.get(r, "date_entree"));
            d.put("date_sortie",     Json.get(r, "date_sortie"));
            d.put("medecin_referent",Json.get(r, "medecin_referent"));
            d.put("notes",           Json.get(r, "notes"));
            d.put("hospit_id",       Json.get(r, "id"));
        }
    }

    private void loadTraitements(String pid, Map<String, String> d) throws Exception {
        String json = SupabaseClient.select("traitements",
                "select=*&patient_id=eq." + pid + "&actif=eq.true&order=created_at.desc");
        List<String> rows = Json.array(json);
        var sb = new StringBuilder();
        if (rows.isEmpty()) { sb.append("<p class=\"empty\">Aucun traitement actif.</p>"); }
        else {
            sb.append("<table class=\"table\"><thead><tr><th>Médicament</th><th>Dosage</th><th>Fréquence</th><th>Durée</th></tr></thead><tbody>");
            for (String r : rows) {
                sb.append("<tr><td>").append(esc(Json.get(r,"medicament"))).append("</td>")
                  .append("<td>").append(esc(Json.get(r,"dosage"))).append("</td>")
                  .append("<td>").append(esc(Json.get(r,"frequence"))).append("</td>")
                  .append("<td>").append(esc(Json.get(r,"duree"))).append("</td></tr>");
            }
            sb.append("</tbody></table>");
            // Bouton ajouter
            sb.append("<a href=\"/medecin/dossier/nouveau-traitement?id=").append(pid).append("\" class=\"btn btn-primary btn-sm\">+ Prescrire</a>");
        }
        d.put("traitements_html", sb.toString());
    }

    private void loadRendezvous(String pid, Map<String, String> d) throws Exception {
        String json = SupabaseClient.select("rendezvous",
                "select=*&patient_id=eq." + pid + "&order=date_rdv.asc");
        List<String> rows = Json.array(json);
        var sb = new StringBuilder();
        if (rows.isEmpty()) { sb.append("<p class=\"empty\">Aucun rendez-vous.</p>"); }
        else {
            sb.append("<table class=\"table\"><thead><tr><th>Date</th><th>Heure</th><th>Motif</th><th>Statut</th></tr></thead><tbody>");
            for (String r : rows) {
                sb.append("<tr><td>").append(esc(Json.get(r,"date_rdv"))).append("</td>")
                  .append("<td>").append(esc(Json.get(r,"heure"))).append("</td>")
                  .append("<td>").append(esc(Json.get(r,"motif"))).append("</td>")
                  .append("<td><span class=\"badge\">").append(esc(Json.get(r,"statut"))).append("</span></td></tr>");
            }
            sb.append("</tbody></table>");
        }
        d.put("rdv_html", sb.toString());
    }

    private void loadSignalements(String pid, Map<String, String> d) throws Exception {
        String json = SupabaseClient.select("signalements",
                "select=*&patient_id=eq." + pid + "&order=created_at.desc&limit=20");
        List<String> rows = Json.array(json);
        var sb = new StringBuilder();
        if (rows.isEmpty()) { sb.append("<p class=\"empty\">Aucun signalement.</p>"); }
        else {
            for (String r : rows) {
                String urgent = "true".equals(Json.get(r, "urgent")) ? " style=\"border-left:4px solid red\"" : "";
                sb.append("<div class=\"signalement-item\"").append(urgent).append(">")
                  .append("<strong>").append(esc(Json.get(r,"type"))).append("</strong>")
                  .append(" — intensité ").append(esc(Json.get(r,"intensite"))).append("/10")
                  .append("<p>").append(esc(Json.get(r,"description"))).append("</p>")
                  .append("<small>").append(esc(Json.get(r,"created_at"))).append("</small>")
                  .append("</div>");
            }
        }
        d.put("sign_html", sb.toString());
    }

    private void loadQuestionnaires(String pid, Map<String, String> d) throws Exception {
        String json = SupabaseClient.select("questionnaires",
                "select=*&patient_id=eq." + pid + "&order=created_at.desc&limit=10");
        List<String> rows = Json.array(json);
        var sb = new StringBuilder();
        if (rows.isEmpty()) { sb.append("<p class=\"empty\">Aucun questionnaire.</p>"); }
        else {
            for (String r : rows) {
                sb.append("<div class=\"journal-entry\">")
                  .append("<strong>").append(esc(Json.get(r,"titre"))).append("</strong>")
                  .append(" — ").append(esc(Json.get(r,"statut")))
                  .append("<small> le ").append(esc(Json.get(r,"created_at"))).append("</small>")
                  .append("</div>");
            }
        }
        d.put("quest_html", sb.toString());
    }

    private void loadJournal(String pid, Map<String, String> d) throws Exception {
        String json = SupabaseClient.select("journal_suivi",
                "select=*&patient_id=eq." + pid + "&order=created_at.desc&limit=20");
        List<String> rows = Json.array(json);
        var sb = new StringBuilder();
        if (rows.isEmpty()) { sb.append("<p class=\"empty\">Aucune entrée.</p>"); }
        else {
            for (String r : rows) {
                sb.append("<div class=\"journal-entry\">")
                  .append("<strong>").append(esc(Json.get(r,"date_entree"))).append("</strong>")
                  .append("<p>").append(esc(Json.get(r,"contenu"))).append("</p>")
                  .append("</div>");
            }
        }
        d.put("journal_html", sb.toString());
    }

    private void loadPasseport(String pid, Map<String, String> d) throws Exception {
        String json = SupabaseClient.select("passeport_sante",
                "select=*&patient_id=eq." + pid + "&limit=1");
        List<String> rows = Json.array(json);
        if (!rows.isEmpty()) {
            String r = rows.get(0);
            for (String f : new String[]{"groupe_sanguin","allergies","maladies_chroniques","antecedents","medecin_traitant"}) {
                d.put(f, Json.get(r, f));
            }
        }
    }

    // ─── Actions dossier ──────────────────────────────────────────────────

    private void saveFiche(HttpExchange ex, Map<String, String> session, String type) throws Exception {
        Map<String, String> form = Http.form(ex);
        String pid = form.get("patient_id");

        String filter = "patient_id=eq." + pid;
        // UPSERT hospitalisation
        String json = Json.build(Json.map(
                "patient_id", pid,
                "medecin_id", session.get("userId"),
                "diagnostic", form.getOrDefault("diagnostic",""),
                "date_entree", form.getOrDefault("date_entree",""),
                "date_sortie", form.getOrDefault("date_sortie",""),
                "medecin_referent", form.getOrDefault("medecin_referent",""),
                "notes", form.getOrDefault("notes","")));

        String existJson = SupabaseClient.select("hospitalisations",
                "select=id&patient_id=eq." + pid + "&limit=1");
        if (Json.array(existJson).isEmpty()) {
            SupabaseClient.insert("hospitalisations", json);
        } else {
            SupabaseClient.update("hospitalisations", filter, json);
        }

        Http.redirect(ex, "/medecin/dossier?id=" + pid + "&onglet=hospitalisation");
    }

    private void nouveauRdv(HttpExchange ex, Map<String, String> session, String method) throws Exception {
        String pid = Http.param(ex, "id");
        if ("POST".equals(method)) {
            Map<String, String> form = Http.form(ex);
            pid = form.getOrDefault("patient_id", pid);
            String json = Json.build(Json.map(
                    "patient_id", pid,
                    "medecin_id", session.get("userId"),
                    "date_rdv", form.get("date_rdv"),
                    "heure", form.get("heure"),
                    "motif", form.getOrDefault("motif",""),
                    "statut", "planifie"));
            SupabaseClient.insert("rendezvous", json);
            Http.redirect(ex, "/medecin/dossier?id=" + pid + "&onglet=rendezvous");
        } else {
            Http.html(ex, 200, layout("Nouveau RDV", session.get("nom"), rdvForm(pid)));
        }
    }

    private void nouveauJournal(HttpExchange ex, Map<String, String> session, String method) throws Exception {
        String pid = Http.param(ex, "id");
        if ("POST".equals(method)) {
            Map<String, String> form = Http.form(ex);
            pid = form.getOrDefault("patient_id", pid);
            String json = Json.build(Json.map(
                    "patient_id", pid,
                    "medecin_id", session.get("userId"),
                    "date_entree", form.getOrDefault("date_entree",""),
                    "contenu", form.getOrDefault("contenu","")));
            SupabaseClient.insert("journal_suivi", json);
            Http.redirect(ex, "/medecin/dossier?id=" + pid + "&onglet=journal");
        } else {
            Http.html(ex, 200, layout("Nouveau journal", session.get("nom"), journalForm(pid)));
        }
    }

    private void nouveauQuestionnaire(HttpExchange ex, Map<String, String> session, String method) throws Exception {
        String pid = Http.param(ex, "id");
        if ("POST".equals(method)) {
            Map<String, String> form = Http.form(ex);
            pid = form.getOrDefault("patient_id", pid);
            String json = Json.build(Json.map(
                    "patient_id", pid,
                    "medecin_id", session.get("userId"),
                    "titre", form.getOrDefault("titre",""),
                    "questions", form.getOrDefault("questions",""),
                    "statut", "en_attente"));
            SupabaseClient.insert("questionnaires", json);
            Http.redirect(ex, "/medecin/dossier?id=" + pid + "&onglet=questionnaires");
        } else {
            Http.html(ex, 200, layout("Nouveau questionnaire", session.get("nom"), questionnaireForm(pid)));
        }
    }

    private String layout(String title, String nom, String content) {
        return Html.layout(title, "medecin", nom, content);
    }

    private String rdvForm(String pid) {
        return """
        <h1>Nouveau rendez-vous</h1>
        <form method="post" action="/medecin/dossier/nouveau-rdv" class="form-card">
          <input type="hidden" name="patient_id" value="%s">
          <div class="form-group"><label>Date</label>
            <input type="date" name="date_rdv" required class="form-control"></div>
          <div class="form-group"><label>Heure</label>
            <input type="time" name="heure" class="form-control"></div>
          <div class="form-group"><label>Motif</label>
            <input name="motif" class="form-control"></div>
          <button type="submit" class="btn btn-primary">Planifier</button>
          <a href="/medecin/dossier?id=%s&onglet=rendezvous" class="btn btn-outline">Annuler</a>
        </form>
        """.formatted(pid, pid);
    }

    private String journalForm(String pid) {
        return """
        <h1>Entrée journal</h1>
        <form method="post" action="/medecin/dossier/nouveau-journal" class="form-card">
          <input type="hidden" name="patient_id" value="%s">
          <div class="form-group"><label>Date</label>
            <input type="date" name="date_entree" required class="form-control"></div>
          <div class="form-group"><label>Observation</label>
            <textarea name="contenu" rows="5" required class="form-control"></textarea></div>
          <button type="submit" class="btn btn-primary">Enregistrer</button>
          <a href="/medecin/dossier?id=%s&onglet=journal" class="btn btn-outline">Annuler</a>
        </form>
        """.formatted(pid, pid);
    }

    private String questionnaireForm(String pid) {
        return """
        <h1>Envoyer un questionnaire</h1>
        <form method="post" action="/medecin/dossier/nouveau-questionnaire" class="form-card">
          <input type="hidden" name="patient_id" value="%s">
          <div class="form-group"><label>Titre</label>
            <input name="titre" required class="form-control"></div>
          <div class="form-group"><label>Questions (une par ligne)</label>
            <textarea name="questions" rows="6" class="form-control"></textarea></div>
          <button type="submit" class="btn btn-primary">Envoyer</button>
          <a href="/medecin/dossier?id=%s&onglet=questionnaires" class="btn btn-outline">Annuler</a>
        </form>
        """.formatted(pid, pid);
    }

    private static String esc(String s) { return Tpl.escHtml(s); }
}
