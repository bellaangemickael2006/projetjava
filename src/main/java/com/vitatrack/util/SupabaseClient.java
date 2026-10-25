package com.vitatrack.util;

import com.vitatrack.config.AppConfig;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Client HTTP léger vers l'API REST de Supabase (pas de dépendances Maven). */
public class SupabaseClient {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    // ---------- SELECT -------------------------------------------------
    public static String select(String table, String query) throws Exception {
        String url = AppConfig.SUPABASE_URL + "/rest/v1/" + table + (query != null ? "?" + query : "");
        HttpRequest req = base(url, "GET", AppConfig.SUPABASE_ANON_KEY).build();
        return send(req);
    }

    public static String selectAuth(String table, String jwt, String query) throws Exception {
        String url = AppConfig.SUPABASE_URL + "/rest/v1/" + table + (query != null ? "?" + query : "");
        HttpRequest req = base(url, "GET", AppConfig.SUPABASE_SERVICE_KEY)
                .header("Authorization", "Bearer " + jwt).build();
        return send(req);
    }

    // ---------- INSERT -------------------------------------------------
    public static String insert(String table, String json) throws Exception {
        String url = AppConfig.SUPABASE_URL + "/rest/v1/" + table;
        HttpRequest req = base(url, "POST", AppConfig.SUPABASE_SERVICE_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        return send(req);
    }

    // ---------- UPDATE -------------------------------------------------
    public static String update(String table, String filter, String json) throws Exception {
        String url = AppConfig.SUPABASE_URL + "/rest/v1/" + table + "?" + filter;
        HttpRequest req = base(url, "PATCH", AppConfig.SUPABASE_SERVICE_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json)).build();
        return send(req);
    }

    // ---------- DELETE -------------------------------------------------
    public static String delete(String table, String filter) throws Exception {
        String url = AppConfig.SUPABASE_URL + "/rest/v1/" + table + "?" + filter;
        HttpRequest req = base(url, "DELETE", AppConfig.SUPABASE_SERVICE_KEY)
                .DELETE().build();
        return send(req);
    }

    // ---------- AUTH ---------------------------------------------------
    public static String signIn(String email, String password) throws Exception {
        String url = AppConfig.SUPABASE_URL + "/auth/v1/token?grant_type=password";
        String body = "{\"email\":\"" + esc(email) + "\",\"password\":\"" + esc(password) + "\"}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("apikey", AppConfig.SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return send(req);
    }

    public static String signUp(String email, String password) throws Exception {
        String url = AppConfig.SUPABASE_URL + "/auth/v1/signup";
        String body = "{\"email\":\"" + esc(email) + "\",\"password\":\"" + esc(password) + "\"}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("apikey", AppConfig.SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return send(req);
    }

    // ---------- RPC (fonctions Postgres) ---------------------------------
    public static String rpc(String func, String json, String jwt) throws Exception {
        String url = AppConfig.SUPABASE_URL + "/rest/v1/rpc/" + func;
        var builder = base(url, "POST", AppConfig.SUPABASE_SERVICE_KEY)
                .header("Content-Type", "application/json");
        if (jwt != null) builder.header("Authorization", "Bearer " + jwt);
        HttpRequest req = builder.POST(HttpRequest.BodyPublishers.ofString(json)).build();
        return send(req);
    }

    // ---------- helpers ------------------------------------------------
    private static HttpRequest.Builder base(String url, String method, String apiKey) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json");
    }

    private static String send(HttpRequest req) throws Exception {
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 400) {
            throw new RuntimeException("Supabase " + res.statusCode() + ": " + res.body());
        }
        return res.body();
    }

    /** Échappe les caractères spéciaux JSON simples. */
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
