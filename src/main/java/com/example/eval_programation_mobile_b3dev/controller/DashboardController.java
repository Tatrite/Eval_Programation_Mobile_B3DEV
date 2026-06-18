package com.example.eval_programation_mobile_b3dev.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
public class DashboardController {

    private static final List<String> requestLogs = Collections.synchronizedList(new ArrayList<>());

    @Value("${ROLE:RELAIS}")
    private String myRole;

    public static void addLog(String target, String path) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String targetShort = target.length() > 50 ? target.substring(0, 47) + "..." : target;

        String newRow = "<tr>" +
                "<td><span class='badge bg-secondary'>" + time + "</span></td>" +
                "<td><code class='text-dark'>" + targetShort + "</code></td>" +
                "<td><span class='text-success font-monospace fw-bold'>" + path + "</span></td>" +
                "</tr>";
        requestLogs.add(0, newRow);
    }

    @GetMapping("/dashboard")
    public String getDashboard() {
        if (!"CHEF".equals(myRole)) {
            return "Disponible uniquement sur le nœud CHEF.";
        }

        StringBuilder rows = new StringBuilder();
        for (String log : requestLogs) {
            rows.append(log);
        }

        return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <title>Tor Proxy Dashboard</title>
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
            <meta http-equiv="refresh" content="3">
        </head>
        <body class="bg-light">
            <div class="container mt-5">
                <div class="card shadow-sm">
                    <div class="card-header bg-dark text-white text-center">
                        <h2 class="mb-0">🛡️ Tor Onion Proxy - Tableau de Bord</h2>
                    </div>
                    <div class="card-body">
                        <table class="table table-striped align-middle">
                            <thead class="table-dark">
                                <tr>
                                    <th>Horodatage</th>
                                    <th>Cible</th>
                                    <th>Circuit de rebonds réel</th>
                                </tr>
                            </thead>
                            <tbody>
                                """ + (rows.isEmpty() ? "<tr><td colspan='3' class='text-center text-muted'>Aucun trafic...</td></tr>" : rows.toString()) + """
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """;
    }
}