package com.vitatrack.util;

import java.util.*;
import java.util.regex.*;

/** Mini-parser JSON sans dépendance externe. */
public class Json {

    /** Extrait la valeur d'une clé simple de niveau 1 (chaîne ou nombre). */
    public static String get(String json, String key) {
        if (json == null) return null;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(?:\"((?:[^\"\\\\]|\\\\.)*)\"|(-?[0-9.]+(?:[eE][+-]?[0-9]+)?)|true|false|null)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            if (m.group(1) != null) return m.group(1);
            if (m.group(2) != null) return m.group(2);
            return m.group(0).contains("true") ? "true" : m.group(0).contains("false") ? "false" : null;
        }
        return null;
    }

    /** Retourne true si le JSON contient une clé avec la valeur donnée. */
    public static boolean has(String json, String key, String value) {
        String v = get(json, key);
        return value.equals(v);
    }

    /** Parse un tableau JSON de lignes et retourne la liste de JSON bruts. */
    public static List<String> array(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.isBlank() || json.strip().equals("[]")) return list;
        // Séparer les objets du tableau de premier niveau
        int depth = 0;
        int start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    list.add(json.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return list;
    }

    /** Construit un objet JSON simple depuis une map. */
    public static String build(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else sb.append("\"").append(SupabaseClient.esc(v.toString())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    public static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i].toString(), kv[i + 1]);
        return m;
    }
}
