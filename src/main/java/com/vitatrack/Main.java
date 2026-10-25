package com.vitatrack;

import com.sun.net.httpserver.HttpServer;
import com.vitatrack.config.AppConfig;
import com.vitatrack.controller.*;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws Exception {
        AppConfig.load();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Routes
        server.createContext("/", new AuthController());
        server.createContext("/login", new AuthController());
        server.createContext("/logout", new AuthController());

        server.createContext("/admin", new AdminController());
        server.createContext("/medecin", new MedecinController());
        server.createContext("/patient", new PatientController());

        server.createContext("/assets", new StaticController());
        server.createContext("/api", new ApiController());

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("VitaTrack démarré sur le port " + port);
    }
}
