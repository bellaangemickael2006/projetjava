package com.vitatrack.config;

public class AppConfig {
    public static String SUPABASE_URL;
    public static String SUPABASE_ANON_KEY;
    public static String SUPABASE_SERVICE_KEY;
    public static String JWT_SECRET;
    public static String APP_NAME = "VitaTrack";

    public static void load() {
        SUPABASE_URL    = getEnv("SUPABASE_URL",         "https://your-project.supabase.co");
        SUPABASE_ANON_KEY   = getEnv("SUPABASE_ANON_KEY",    "your-anon-key");
        SUPABASE_SERVICE_KEY= getEnv("SUPABASE_SERVICE_KEY", "your-service-key");
        JWT_SECRET      = getEnv("JWT_SECRET",           "vitatrack-secret-2026-change-me");
    }

    private static String getEnv(String key, String def) {
        String v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v : def;
    }
}
