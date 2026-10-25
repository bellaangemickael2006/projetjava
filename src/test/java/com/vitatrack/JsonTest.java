package com.vitatrack;

import com.vitatrack.util.Json;
import java.util.*;

public class JsonTest {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        test_get_string();
        test_get_number();
        test_array();
        test_build();
        test_escape();

        System.out.println("\n=== Résultats : " + passed + " OK, " + failed + " ÉCHEC ===");
        if (failed > 0) System.exit(1);
    }

    static void test_get_string() {
        String json = "{\"email\":\"admin@vitatrack.demo\",\"role\":\"admin\"}";
        assertEquals("admin@vitatrack.demo", Json.get(json, "email"), "get email");
        assertEquals("admin", Json.get(json, "role"), "get role");
        assertEquals(null, Json.get(json, "absent"), "get absent");
    }

    static void test_get_number() {
        String json = "{\"intensite\":8,\"actif\":true}";
        assertEquals("8", Json.get(json, "intensite"), "get number");
        assertEquals("true", Json.get(json, "actif"), "get bool");
    }

    static void test_array() {
        String json = "[{\"id\":\"1\",\"nom\":\"Alice\"},{\"id\":\"2\",\"nom\":\"Bob\"}]";
        List<String> arr = Json.array(json);
        assertEquals("2", String.valueOf(arr.size()), "array size");
        assertEquals("Alice", Json.get(arr.get(0), "nom"), "array[0] nom");
        assertEquals("Bob",   Json.get(arr.get(1), "nom"), "array[1] nom");
        assertEquals("0", String.valueOf(Json.array("[]").size()), "empty array");
    }

    static void test_build() {
        String json = Json.build(Json.map("role", "medecin", "actif", true));
        assertEquals("true", Json.get(json, "actif"), "build boolean");
        assertEquals("medecin", Json.get(json, "role"), "build string");
    }

    static void test_escape() {
        String json = "{\"message\":\"Il a dit \\\"bonjour\\\"\"}";
        // pas d'exception
        String val = Json.get(json, "message");
        assert val != null : "escape: valeur nulle";
        pass("escape JSON");
    }

    // ─── Helpers ──────────────────────────────────────────────────────
    static void assertEquals(String expected, String actual, String name) {
        if (Objects.equals(expected, actual)) {
            pass(name);
        } else {
            fail(name + " — attendu [" + expected + "] reçu [" + actual + "]");
        }
    }

    static void pass(String name) {
        passed++;
        System.out.println("  ✓ " + name);
    }

    static void fail(String name) {
        failed++;
        System.out.println("  ✗ " + name);
    }
}
