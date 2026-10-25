package com.vitatrack.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Gestion de sessions HTTP en mémoire (cookie VTKSID). */
public class Session {

    private static final Map<String, Map<String, String>> STORE = new ConcurrentHashMap<>();

    public static String create(String userId, String role, String email, String nom, String jwt) {
        String sid = UUID.randomUUID().toString().replace("-", "");
        Map<String, String> data = new HashMap<>();
        data.put("userId", userId);
        data.put("role", role);
        data.put("email", email);
        data.put("nom", nom);
        data.put("jwt", jwt);
        data.put("createdAt", String.valueOf(System.currentTimeMillis()));
        STORE.put(sid, data);
        return sid;
    }

    public static Map<String, String> get(String sid) {
        if (sid == null) return null;
        Map<String, String> s = STORE.get(sid);
        if (s == null) return null;
        // Expiration 8h
        long created = Long.parseLong(s.getOrDefault("createdAt", "0"));
        if (System.currentTimeMillis() - created > 8 * 3600 * 1000L) {
            STORE.remove(sid);
            return null;
        }
        return s;
    }

    public static void destroy(String sid) {
        if (sid != null) STORE.remove(sid);
    }

    /** Extrait la valeur du cookie VTKSID depuis l'en-tête Cookie. */
    public static String extractSid(String cookieHeader) {
        if (cookieHeader == null) return null;
        for (String part : cookieHeader.split(";")) {
            String[] kv = part.strip().split("=", 2);
            if (kv.length == 2 && "VTKSID".equals(kv[0].strip())) return kv[1].strip();
        }
        return null;
    }
}
