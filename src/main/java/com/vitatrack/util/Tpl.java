package com.vitatrack.util;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Moteur de templates minimaliste : remplace {{clé}} et {{#if clé}}...{{/if}}. */
public class Tpl {

    public static String render(String templatePath, Map<String, Object> vars) {
        String tpl = load(templatePath);
        if (tpl == null) return "<h1>Template introuvable : " + templatePath + "</h1>";
        return process(tpl, vars);
    }

    private static String load(String path) {
        try (InputStream is = Tpl.class.getResourceAsStream("/templates/" + path)) {
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public static String process(String tpl, Map<String, Object> vars) {
        // {{#if key}} ... {{/if}}
        tpl = processIf(tpl, vars);
        // {{#unless key}} ... {{/unless}}
        tpl = processUnless(tpl, vars);
        // {{clé}}
        if (vars != null) {
            for (var e : vars.entrySet()) {
                String val = e.getValue() == null ? "" : e.getValue().toString();
                tpl = tpl.replace("{{" + e.getKey() + "}}", escHtml(val));
                tpl = tpl.replace("{{{" + e.getKey() + "}}}", val); // triple = HTML brut
            }
        }
        return tpl;
    }

    private static String processIf(String tpl, Map<String, Object> vars) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\\{\\{#if (\\w+)\\}\\}(.*?)\\{\\{/if\\}\\}", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(tpl);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            Object v = vars != null ? vars.get(key) : null;
            boolean ok = v != null && !"false".equals(v.toString()) && !"".equals(v.toString());
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(ok ? m.group(2) : ""));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String processUnless(String tpl, Map<String, Object> vars) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\\{\\{#unless (\\w+)\\}\\}(.*?)\\{\\{/unless\\}\\}", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(tpl);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            Object v = vars != null ? vars.get(key) : null;
            boolean empty = v == null || "false".equals(v.toString()) || "".equals(v.toString());
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(empty ? m.group(2) : ""));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Encode les caractères HTML dangereux. */
    public static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
