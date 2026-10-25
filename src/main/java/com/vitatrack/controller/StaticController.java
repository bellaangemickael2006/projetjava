package com.vitatrack.controller;

import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.file.*;

public class StaticController implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath(); // /assets/css/style.css
        String resource = path; // maps directly to /assets/...

        try (InputStream is = getClass().getResourceAsStream(resource)) {
            if (is == null) {
                ex.sendResponseHeaders(404, -1);
                ex.getResponseBody().close();
                return;
            }
            byte[] bytes = is.readAllBytes();
            String ct = contentType(path);
            ex.getResponseHeaders().set("Content-Type", ct);
            ex.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.getResponseBody().close();
        }
    }

    private String contentType(String path) {
        if (path.endsWith(".css"))  return "text/css";
        if (path.endsWith(".js"))   return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".svg"))  return "image/svg+xml";
        return "application/octet-stream";
    }
}
