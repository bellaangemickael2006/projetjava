package com.vitatrack.util;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Utilitaires pour lire les requêtes et écrire les réponses. */
public class Http {

    /** Lit le corps de la requête. */
    public static String body(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Parse application/x-www-form-urlencoded. */
    public static Map<String, String> form(HttpExchange ex) throws IOException {
        return parseQuery(body(ex));
    }

    public static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return map;
        for (String part : raw.split("&")) {
            String[] kv = part.split("=", 2);
            String k = decode(kv[0]);
            String v = kv.length == 2 ? decode(kv[1]) : "";
            map.put(k, v);
        }
        return map;
    }

    private static String decode(String s) {
        try { return URLDecoder.decode(s, StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }

    /** Extrait le chemin sans le préfixe, ex. "/medecin/patients" → "patients". */
    public static String subPath(HttpExchange ex, String prefix) {
        String p = ex.getRequestURI().getPath();
        if (p.startsWith(prefix)) p = p.substring(prefix.length());
        if (p.startsWith("/")) p = p.substring(1);
        return p;
    }

    /** Envoie une réponse HTML. */
    public static void html(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    /** Envoie une réponse JSON. */
    public static void json(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    /** Redirige vers une URL. */
    public static void redirect(HttpExchange ex, String url) throws IOException {
        ex.getResponseHeaders().set("Location", url);
        ex.sendResponseHeaders(302, -1);
        ex.getResponseBody().close();
    }

    /** Définit un cookie de session. */
    public static void setCookie(HttpExchange ex, String name, String value, boolean delete) {
        String cookie = name + "=" + value + "; Path=/; HttpOnly; SameSite=Strict"
                + (delete ? "; Max-Age=0" : "; Max-Age=28800");
        ex.getResponseHeaders().add("Set-Cookie", cookie);
    }

    /** Lit le cookie de session. */
    public static String cookie(HttpExchange ex, String name) {
        String header = ex.getRequestHeaders().getFirst("Cookie");
        return Session.extractSid(header);
    }

    /** Retourne le paramètre GET. */
    public static String param(HttpExchange ex, String key) {
        String query = ex.getRequestURI().getRawQuery();
        if (query == null) return null;
        Map<String, String> map = parseQuery(query);
        return map.get(key);
    }
}
