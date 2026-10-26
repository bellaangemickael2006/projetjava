package com.vitatrack.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Sessions sans état serveur : les données sont dans le cookie VTKSID, signées en HMAC-SHA256.
 * Pourquoi : sur un hébergement à plusieurs instances (Vercel), une session stockée en mémoire
 * n'existe que sur l'instance qui l'a créée. Ici, n'importe quelle instance peut la vérifier.
 *
 * Format : base64url(champ1).base64url(champ2)...base64url(champN).base64url(signature)
 * Champs : userId, role, email, nom, jwt, createdAt (ms)
 */
public class Session {

    private static final String[] FIELDS = {"userId", "role", "email", "nom", "jwt", "createdAt"};
    private static final long DUREE_MS = 8 * 3600 * 1000L;
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();
    /** Cookies déconnectés (liste de révocation locale à l'instance). */
    private static final Set<String> REVOQUES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static byte[] secret() {
        String s = System.getenv("JWT_SECRET");
        if (s == null || s.isBlank()) s = "vitatrack-dev-secret-change-me";
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret(), "HmacSHA256"));
        return ENC.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    public static String create(String userId, String role, String email, String nom, String jwt) {
        Map<String, String> data = new HashMap<>();
        data.put("userId", userId);
        data.put("role", role);
        data.put("email", email);
        data.put("nom", nom);
        data.put("jwt", jwt);
        data.put("createdAt", String.valueOf(System.currentTimeMillis()));
        StringBuilder sb = new StringBuilder();
        for (String f : FIELDS) {
            String v = data.getOrDefault(f, "");
            sb.append(ENC.encodeToString(v.getBytes(StandardCharsets.UTF_8))).append('.');
        }
        String corps = sb.substring(0, sb.length() - 1);
        try {
            return corps + "." + sign(corps);
        } catch (Exception e) {
            throw new IllegalStateException("Signature de session impossible", e);
        }
    }

    public static Map<String, String> get(String sid) {
        if (sid == null || sid.isBlank() || REVOQUES.contains(sid)) return null;
        String[] parts = sid.split("\\.");
        if (parts.length != FIELDS.length + 1) return null;
        try {
            String corps = String.join(".", Arrays.copyOf(parts, FIELDS.length));
            byte[] attendu = sign(corps).getBytes(StandardCharsets.UTF_8);
            byte[] recu = parts[FIELDS.length].getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(attendu, recu)) return null; // signature invalide

            Map<String, String> data = new HashMap<>();
            for (int i = 0; i < FIELDS.length; i++) {
                data.put(FIELDS[i], new String(DEC.decode(parts[i]), StandardCharsets.UTF_8));
            }
            long created = Long.parseLong(data.getOrDefault("createdAt", "0"));
            if (System.currentTimeMillis() - created > DUREE_MS) return null; // expiré (8h)
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    /** Déconnexion : le cookie est effacé côté navigateur (AuthController). */
    public static void destroy(String sid) {
        if (sid != null) REVOQUES.add(sid);
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
