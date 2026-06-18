package com.example.eval_programation_mobile_b3dev.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class RelayController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ROLE:RELAIS}")
    private String myRole;

    @Value("${REGISTRY_URL:http://registre:8090}")
    private String registryUrl;

    @Value("${CONTAINER_NAME:unknown}")
    private String containerNameEnv;

    private static final String AUTH_USER = "admin";
    private static final String AUTH_PASSWORD = "secret_password";

    @GetMapping("/proxy")
    public ResponseEntity<String> proxy(
            @RequestParam String target,
            @RequestParam(defaultValue = "0") int nodes,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String password) {

        if ("CHEF".equals(myRole)) {
            if (!AUTH_USER.equals(user) || !AUTH_PASSWORD.equals(password)) {
                System.out.println("⚠️ Tentative d'accès refusée : mauvais identifiants.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("🔒 Accès refusé : Login ou mot de passe incorrect.");
            }
        }

        String myName = "CHEF".equals(myRole) ? "chef_proxy" : containerNameEnv;
        if ("unknown".equals(myName) || myName.isEmpty()) {
            try { myName = java.net.InetAddress.getLocalHost().getHostName(); } catch (Exception ignored) {}
        }

        if (nodes <= 0) {
            try {
                String htmlContent = restTemplate.getForObject(target, String.class);
                return ResponseEntity.ok()
                        .header("X-Tor-Path", myName)
                        .body(htmlContent);
            } catch (Exception e) {
                return ResponseEntity.internalServerError()
                        .header("X-Tor-Path", myName + " -> [ERREUR CIBLE]")
                        .body("Erreur cible: " + e.getMessage());
            }
        }

        try {
            String registryCallUrl = registryUrl + "/registry/random?exclude=" + myName;
            String nextRelayHost = restTemplate.getForObject(registryCallUrl, String.class);

            String nextNodeUrl = UriComponentsBuilder.fromUriString("http://" + nextRelayHost + ":8080/proxy")
                    .queryParam("target", target)
                    .queryParam("nodes", nodes - 1)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(nextNodeUrl, String.class);

            String childPath = response.getHeaders().getFirst("X-Tor-Path");
            String finalReturnedPath = myName + " -> " + (childPath != null ? childPath : nextRelayHost);

            if ("CHEF".equals(myRole)) {
                DashboardController.addLog(target, finalReturnedPath);
            }

            return ResponseEntity.ok()
                    .header("X-Tor-Path", finalReturnedPath)
                    .body(response.getBody());

        } catch (Exception e) {
            String errorPath = myName + " -> [ÉCHEC REBOND]";
            if ("CHEF".equals(myRole)) {
                DashboardController.addLog(target, errorPath);
            }
            return ResponseEntity.internalServerError()
                    .header("X-Tor-Path", errorPath)
                    .body("Erreur routage: " + e.getMessage());
        }
    }
}