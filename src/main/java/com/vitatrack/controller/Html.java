package com.vitatrack.controller;

import com.vitatrack.util.Json;
import com.vitatrack.util.Tpl;
import java.util.*;

/** Génère le HTML de chaque page (templates inline). */
public class Html {

    // ─── LAYOUT ────────────────────────────────────────────────────────────

    public static String layout(String title, String navRole, String navNom, String content) {
        return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width,initial-scale=1">
          <meta name="theme-color" content="#1a73e8">
          <title>%s — VitaTrack</title>
          <link rel="manifest" href="/assets/manifest.json">
          <link rel="apple-touch-icon" href="/assets/icons/icon-180.png">
          <link rel="icon" type="image/png" sizes="192x192" href="/assets/icons/icon-192.png">
          <link rel="stylesheet" href="/assets/css/style.css">
        </head>
        <body>
          <nav class="navbar">
            <a href="/%s" class="brand">❤ VitaTrack</a>
            <div class="nav-info">
              <span class="badge badge-%s">%s</span>
              <span>%s</span>
              <a href="/logout" class="btn btn-sm btn-outline">Déconnexion</a>
            </div>
          </nav>
          <main class="container">%s</main>
          <script src="/assets/js/app.js"></script>
        </body>
        </html>
        """.formatted(title, navRole, navRole, navRole.toUpperCase(), esc(navNom), content);
    }

    // ─── LOGIN ─────────────────────────────────────────────────────────────

    public static String loginPage(String error) {
        String err = error != null
                ? "<div class=\"alert alert-danger\">" + esc(error) + "</div>" : "";
        return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width,initial-scale=1">
          <meta name="theme-color" content="#1a73e8">
          <title>Connexion — VitaTrack</title>
          <link rel="manifest" href="/assets/manifest.json">
          <link rel="apple-touch-icon" href="/assets/icons/icon-180.png">
          <link rel="icon" type="image/png" sizes="192x192" href="/assets/icons/icon-192.png">
          <link rel="stylesheet" href="/assets/css/style.css">
        </head>
        <body class="login-body">
          <div class="login-card">
            <div class="login-logo">❤ VitaTrack</div>
            <p class="login-sub">Suivi post-hospitalisation</p>
            %s
            <form method="post" action="/login">
              <div class="form-group">
                <label>Email</label>
                <input type="email" name="email" required autofocus class="form-control">
              </div>
              <div class="form-group">
                <label>Mot de passe</label>
                <input type="password" name="password" required class="form-control">
              </div>
              <button type="submit" class="btn btn-primary btn-block">Se connecter</button>
            </form>
          </div>
          <script src="/assets/js/app.js"></script>
        </body>
        </html>
        """.formatted(err);
    }

    // ─── ADMIN ─────────────────────────────────────────────────────────────

    public static String adminDashboard(String nom, List<String> medecins, List<String> patients, List<String> logs) {
        var sb = new StringBuilder();
        sb.append("<h1>Tableau de bord administrateur</h1>");
        sb.append("<div class=\"stats-row\">");
        sb.append(statCard("Médecins", medecins.size(), "blue"));
        sb.append(statCard("Patients", patients.size(), "green"));
        sb.append(statCard("Activités", logs.size(), "orange"));
        sb.append("</div>");

        // Tableau médecins
        sb.append("<section><h2>Médecins</h2>");
        sb.append("<div style=\"margin-bottom:12px\"><a href=\"/admin/nouveau-medecin\" class=\"btn btn-primary\">+ Nouveau médecin</a></div>");
        sb.append(userTable(medecins, "medecin"));
        sb.append("</section>");

        // Tableau patients
        sb.append("<section><h2>Patients</h2>");
        sb.append(userTable(patients, "patient"));
        sb.append("</section>");

        return layout("Admin", "admin", nom, sb.toString());
    }

    public static String adminNewMedecin(String error) {
        String err = error != null ? "<div class=\"alert alert-danger\">" + esc(error) + "</div>" : "";
        return layout("Nouveau médecin", "admin", "Admin", err + """
            <h1>Créer un médecin</h1>
            <form method="post" action="/admin/nouveau-medecin" class="form-card">
              <div class="form-group">
                <label>Nom complet</label>
                <input name="nom" required class="form-control">
              </div>
              <div class="form-group">
                <label>Spécialité</label>
                <input name="specialite" class="form-control">
              </div>
              <div class="form-group">
                <label>Email</label>
                <input type="email" name="email" required class="form-control">
              </div>
              <div class="form-group">
                <label>Mot de passe provisoire</label>
                <input type="password" name="password" required minlength="8" class="form-control">
              </div>
              <button type="submit" class="btn btn-primary">Créer le compte</button>
              <a href="/admin" class="btn btn-outline">Annuler</a>
            </form>
        """);
    }

    // ─── MÉDECIN ───────────────────────────────────────────────────────────

    public static String medecinDashboard(String nom, List<String> patients) {
        var sb = new StringBuilder();
        sb.append("<h1>Mes patients</h1>");
        sb.append("<div style=\"margin-bottom:12px\"><a href=\"/medecin/nouveau-patient\" class=\"btn btn-primary\">+ Inviter un patient</a></div>");
        sb.append(patientList(patients));
        return layout("Médecin", "medecin", nom, sb.toString());
    }

    public static String medecinNewPatient(String error, String success) {
        String err = error != null ? "<div class=\"alert alert-danger\">" + esc(error) + "</div>" : "";
        String ok  = success != null ? "<div class=\"alert alert-success\">" + esc(success) + "</div>" : "";
        return layout("Nouveau patient", "medecin", "Médecin", err + ok + """
            <h1>Inviter un patient</h1>
            <form method="post" action="/medecin/nouveau-patient" class="form-card">
              <div class="form-group">
                <label>Nom complet</label>
                <input name="nom" required class="form-control">
              </div>
              <div class="form-group">
                <label>Date de naissance</label>
                <input type="date" name="date_naissance" class="form-control">
              </div>
              <div class="form-group">
                <label>Email du patient</label>
                <input type="email" name="email" required class="form-control">
              </div>
              <div class="form-group">
                <label>Mot de passe provisoire</label>
                <input type="password" name="password" required minlength="8" class="form-control">
              </div>
              <div class="form-group">
                <label>Diagnostic initial</label>
                <textarea name="diagnostic" rows="3" class="form-control"></textarea>
              </div>
              <button type="submit" class="btn btn-primary">Créer & inviter</button>
              <a href="/medecin" class="btn btn-outline">Annuler</a>
            </form>
        """);
    }

    public static String dossierPage(String patientNom, String patientId, String medecinNom,
                                     String onglet, Map<String, String> data) {
        String[] onglets = {"hospitalisation","traitements","rendezvous","signalements","questionnaires","journal","passeport"};
        String[] labels  = {"Hospitalisation","Traitements","Rendez-vous","Signalements","Questionnaires","Journal","Passeport"};

        var tabs = new StringBuilder("<div class=\"tabs\">");
        for (int i = 0; i < onglets.length; i++) {
            String active = onglets[i].equals(onglet) ? " active" : "";
            tabs.append("<a href=\"/medecin/dossier?id=").append(patientId)
                .append("&onglet=").append(onglets[i]).append("\" class=\"tab").append(active).append("\">")
                .append(labels[i]).append("</a>");
        }
        tabs.append("</div>");

        String contenu = switch (onglet) {
            case "traitements"    -> buildTraitements(data);
            case "rendezvous"     -> buildRendezvous(data, patientId);
            case "signalements"   -> buildSignalements(data, patientId);
            case "questionnaires" -> buildQuestionnaires(data, patientId);
            case "journal"        -> buildJournal(data, patientId);
            case "passeport"      -> buildPasseport(data);
            default               -> buildHospitalisation(data, patientId);
        };

        return layout("Dossier " + patientNom, "medecin", medecinNom,
            "<h1>Dossier de " + esc(patientNom) + "</h1>" + tabs.toString() + contenu);
    }

    // ─── PATIENT ───────────────────────────────────────────────────────────

    public static String patientDashboard(String nom, Map<String, String> data) {
        return layout("Mon suivi", "patient", nom, """
            <h1>Bienvenue, %s</h1>
            <div class="card-grid">
              <a href="/patient/traitements" class="dash-card">💊 Traitements</a>
              <a href="/patient/rendezvous"  class="dash-card">📅 Rendez-vous</a>
              <a href="/patient/signalement" class="dash-card">🚨 Signaler</a>
              <a href="/patient/questionnaires" class="dash-card">📋 Questionnaires</a>
              <a href="/patient/passeport"   class="dash-card">🪪 Passeport santé</a>
            </div>
        """.formatted(esc(nom)));
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────

    private static String statCard(String label, int val, String color) {
        return "<div class=\"stat-card stat-" + color + "\"><div class=\"stat-val\">" + val
                + "</div><div class=\"stat-label\">" + label + "</div></div>";
    }

    private static String userTable(List<String> rows, String role) {
        if (rows.isEmpty()) return "<p class=\"empty\">Aucun " + role + " pour l'instant.</p>";
        var sb = new StringBuilder("<table class=\"table\"><thead><tr><th>Nom</th><th>Email</th><th>Statut</th></tr></thead><tbody>");
        for (String r : rows) {
            sb.append("<tr><td>").append(esc(Json.get(r, "nom"))).append("</td>")
              .append("<td>").append(esc(Json.get(r, "email"))).append("</td>")
              .append("<td><span class=\"badge badge-").append(esc(Json.get(r, "statut"))).append("\">").append(esc(Json.get(r, "statut"))).append("</span></td></tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private static String patientList(List<String> rows) {
        if (rows.isEmpty()) return "<p class=\"empty\">Aucun patient. Invitez-en un.</p>";
        var sb = new StringBuilder("<div class=\"patient-grid\">");
        for (String r : rows) {
            String id   = Json.get(r, "id");
            String nom  = Json.get(r, "nom");
            sb.append("<a href=\"/medecin/dossier?id=").append(esc(id)).append("&onglet=hospitalisation\" class=\"patient-card\">")
              .append("<div class=\"pc-nom\">").append(esc(nom)).append("</div>")
              .append("<div class=\"pc-email\">").append(esc(Json.get(r, "email"))).append("</div>")
              .append("</a>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    // ─── ONGLETS DOSSIER ───────────────────────────────────────────────────

    private static String buildHospitalisation(Map<String, String> d, String pid) {
        return """
        <div class="form-card">
          <h3>Informations d'hospitalisation</h3>
          <form method="post" action="/medecin/dossier/hospitalisation">
            <input type="hidden" name="patient_id" value="%s">
            <div class="form-group"><label>Diagnostic</label>
              <textarea name="diagnostic" rows="4" class="form-control">%s</textarea></div>
            <div class="form-group"><label>Date d'entrée</label>
              <input type="date" name="date_entree" class="form-control" value="%s"></div>
            <div class="form-group"><label>Date de sortie</label>
              <input type="date" name="date_sortie" class="form-control" value="%s"></div>
            <div class="form-group"><label>Médecin référent</label>
              <input name="medecin_referent" class="form-control" value="%s"></div>
            <div class="form-group"><label>Notes cliniques</label>
              <textarea name="notes" rows="4" class="form-control">%s</textarea></div>
            <button type="submit" class="btn btn-primary">Enregistrer</button>
          </form>
        </div>
        """.formatted(pid,
             esc(d.getOrDefault("diagnostic","")),
             esc(d.getOrDefault("date_entree","")),
             esc(d.getOrDefault("date_sortie","")),
             esc(d.getOrDefault("medecin_referent","")),
             esc(d.getOrDefault("notes","")));
    }

    private static String buildTraitements(Map<String, String> d) {
        String liste = d.getOrDefault("traitements_html", "<p class=\"empty\">Aucun traitement.</p>");
        return "<div class=\"form-card\"><h3>Traitements prescris</h3>{{{traitements_html}}}</div>"
                .replace("{{{traitements_html}}}", liste);
    }

    private static String buildRendezvous(Map<String, String> d, String pid) {
        String liste = d.getOrDefault("rdv_html", "<p class=\"empty\">Aucun rendez-vous.</p>");
        return "<div class=\"form-card\"><h3>Rendez-vous</h3><a href=\"/medecin/dossier/nouveau-rdv?id=" + pid + "\" class=\"btn btn-primary btn-sm\">+ Nouveau</a><br><br>" + liste + "</div>";
    }

    private static String buildSignalements(Map<String, String> d, String pid) {
        String liste = d.getOrDefault("sign_html", "<p class=\"empty\">Aucun signalement.</p>");
        return "<div class=\"form-card\"><h3>Signalements</h3>" + liste + "</div>";
    }

    private static String buildQuestionnaires(Map<String, String> d, String pid) {
        String liste = d.getOrDefault("quest_html", "<p class=\"empty\">Aucun questionnaire.</p>");
        return "<div class=\"form-card\"><h3>Questionnaires</h3><a href=\"/medecin/dossier/nouveau-questionnaire?id=" + pid + "\" class=\"btn btn-primary btn-sm\">+ Envoyer un questionnaire</a><br><br>" + liste + "</div>";
    }

    private static String buildJournal(Map<String, String> d, String pid) {
        String liste = d.getOrDefault("journal_html", "<p class=\"empty\">Aucune entrée.</p>");
        return "<div class=\"form-card\"><h3>Journal de suivi</h3><a href=\"/medecin/dossier/nouveau-journal?id=" + pid + "\" class=\"btn btn-primary btn-sm\">+ Ajouter une entrée</a><br><br>" + liste + "</div>";
    }

    private static String buildPasseport(Map<String, String> d) {
        return """
        <div class="form-card">
          <h3>Passeport santé</h3>
          <table class="table">
            <tr><th>Groupe sanguin</th><td>%s</td></tr>
            <tr><th>Allergies</th><td>%s</td></tr>
            <tr><th>Maladies chroniques</th><td>%s</td></tr>
            <tr><th>Antécédents chirurgicaux</th><td>%s</td></tr>
            <tr><th>Médecin traitant</th><td>%s</td></tr>
          </table>
        </div>
        """.formatted(
            esc(d.getOrDefault("groupe_sanguin","—")),
            esc(d.getOrDefault("allergies","—")),
            esc(d.getOrDefault("maladies_chroniques","—")),
            esc(d.getOrDefault("antecedents","—")),
            esc(d.getOrDefault("medecin_traitant","—")));
    }

    // ─── SIGNALEMENT PATIENT ───────────────────────────────────────────────

    public static String signalementPage(String nom, String error, String success) {
        String err = error != null ? "<div class=\"alert alert-danger\">" + esc(error) + "</div>" : "";
        String ok  = success != null ? "<div class=\"alert alert-success\">" + esc(success) + "</div>" : "";
        return layout("Signaler un symptôme", "patient", nom, err + ok + """
            <h1>Signaler un symptôme</h1>
            <form method="post" action="/patient/signalement" class="form-card">
              <div class="form-group">
                <label>Type de symptôme</label>
                <select name="type" class="form-control">
                  <option value="douleur">Douleur</option>
                  <option value="fievre">Fièvre</option>
                  <option value="nausee">Nausée</option>
                  <option value="fatigue">Fatigue intense</option>
                  <option value="autre">Autre</option>
                </select>
              </div>
              <div class="form-group">
                <label>Intensité (1-10)</label>
                <input type="range" name="intensite" min="1" max="10" value="5" class="form-control">
              </div>
              <div class="form-group">
                <label>Description</label>
                <textarea name="description" rows="4" class="form-control" required></textarea>
              </div>
              <button type="submit" class="btn btn-primary">Envoyer au médecin</button>
            </form>
        """);
    }

    public static String patientTraitements(String nom, List<String> traitements) {
        var sb = new StringBuilder("<h1>Mes traitements</h1>");
        if (traitements.isEmpty()) {
            sb.append("<p class=\"empty\">Aucun traitement prescrit.</p>");
        } else {
            sb.append("<div class=\"traitement-list\">");
            for (String t : traitements) {
                sb.append("<div class=\"traitement-card\">")
                  .append("<h3>").append(esc(Json.get(t, "medicament"))).append("</h3>")
                  .append("<p><strong>Dosage :</strong> ").append(esc(Json.get(t, "dosage"))).append("</p>")
                  .append("<p><strong>Fréquence :</strong> ").append(esc(Json.get(t, "frequence"))).append("</p>")
                  .append("<p><strong>Durée :</strong> ").append(esc(Json.get(t, "duree"))).append("</p>")
                  .append("</div>");
            }
            sb.append("</div>");
        }
        return layout("Traitements", "patient", nom, sb.toString());
    }

    public static String patientRendezvous(String nom, List<String> rdvs) {
        var sb = new StringBuilder("<h1>Mes rendez-vous</h1>");
        if (rdvs.isEmpty()) {
            sb.append("<p class=\"empty\">Aucun rendez-vous planifié.</p>");
        } else {
            sb.append("<table class=\"table\"><thead><tr><th>Date</th><th>Heure</th><th>Médecin</th><th>Motif</th><th>Statut</th></tr></thead><tbody>");
            for (String r : rdvs) {
                sb.append("<tr>")
                  .append("<td>").append(esc(Json.get(r, "date_rdv"))).append("</td>")
                  .append("<td>").append(esc(Json.get(r, "heure"))).append("</td>")
                  .append("<td>").append(esc(Json.get(r, "medecin_nom"))).append("</td>")
                  .append("<td>").append(esc(Json.get(r, "motif"))).append("</td>")
                  .append("<td><span class=\"badge\">").append(esc(Json.get(r, "statut"))).append("</span></td>")
                  .append("</tr>");
            }
            sb.append("</tbody></table>");
        }
        return layout("Rendez-vous", "patient", nom, sb.toString());
    }

    public static String passeportPage(String nom, String data) {
        Map<String, String> d = new java.util.HashMap<>();
        String[] fields = {"groupe_sanguin","allergies","maladies_chroniques","antecedents","medecin_traitant"};
        for (String f : fields) d.put(f, Json.get(data, f));
        return layout("Passeport santé", "patient", nom, buildPasseport(d));
    }

    public static String notFoundPage() {
        return """
        <!DOCTYPE html><html lang="fr"><head><meta charset="UTF-8"><title>404 — VitaTrack</title>
        <link rel="stylesheet" href="/assets/css/style.css"></head>
        <body class="login-body"><div class="login-card">
        <h2>Page introuvable</h2><a href="/">Retour à l'accueil</a></div></body></html>
        """;
    }

    public static String errorPage(String msg) {
        return "<div class=\"alert alert-danger\">" + esc(msg) + "</div>";
    }

    private static String esc(String s) { return Tpl.escHtml(s); }
}
