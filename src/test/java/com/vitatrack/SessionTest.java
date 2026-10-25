package com.vitatrack;

import com.vitatrack.util.Session;
import java.util.Map;

public class SessionTest {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        test_create_get();
        test_destroy();
        test_extract_sid();
        test_unknown_sid();

        System.out.println("\n=== Résultats : " + passed + " OK, " + failed + " ÉCHEC ===");
        if (failed > 0) System.exit(1);
    }

    static void test_create_get() {
        String sid = Session.create("uid1", "admin", "admin@vitatrack.demo", "Admin", "jwt-tok");
        Map<String, String> s = Session.get(sid);
        assertNotNull(s, "session créée non nulle");
        assertEquals("admin", s.get("role"), "role admin");
        assertEquals("admin@vitatrack.demo", s.get("email"), "email admin");
        assertEquals("jwt-tok", s.get("jwt"), "jwt stocké");
    }

    static void test_destroy() {
        String sid = Session.create("uid2", "patient", "p@v.com", "Paul", "jwt2");
        Session.destroy(sid);
        assertNull(Session.get(sid), "session détruite");
    }

    static void test_extract_sid() {
        String cookie = "theme=dark; VTKSID=abc123; lang=fr";
        assertEquals("abc123", Session.extractSid(cookie), "extract VTKSID");
    }

    static void test_unknown_sid() {
        assertNull(Session.get("does-not-exist"), "SID inconnu → null");
    }

    // helpers
    static void assertNotNull(Object v, String name) {
        if (v != null) pass(name); else fail(name + " — null inattendu");
    }
    static void assertNull(Object v, String name) {
        if (v == null) pass(name); else fail(name + " — non-null inattendu : " + v);
    }
    static void assertEquals(String exp, String act, String name) {
        if (exp == null ? act == null : exp.equals(act)) pass(name);
        else fail(name + " — attendu [" + exp + "] reçu [" + act + "]");
    }
    static void pass(String n) { passed++; System.out.println("  ✓ " + n); }
    static void fail(String n) { failed++; System.out.println("  ✗ " + n); }
}
