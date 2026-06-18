package com.example.eval_programation_mobile_b3dev.controller;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.util.ArrayList;
import java.util.List;

@RestController
public class RelayController {

    private final RestTemplate restTemplate = new RestTemplate();
    // Connexion automatique au démon Docker local
    private final DockerClient dockerClient = createDockerClient();

    private DockerClient createDockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
    @GetMapping("/proxy")
    public String proxy(@RequestParam String target, @RequestParam(defaultValue = "0") int nodes) {

        // CAS 1 : C'est une requête interne d'un sous-nœud (ou fin de chaîne)
        if (nodes <= 0) {
            System.out.println("Dernier nœud atteint. Appel de : " + target);
            return restTemplate.getForObject(target, String.class);
        }

        // CAS 2 : On est le Nœud Principal, on doit créer les instances automatiquement
        System.out.println("Création automatique de " + nodes + " instances Docker...");
        List<String> containerIds = new ArrayList<>();

        try {
            // 1. Créer et démarrer les conteneurs dynamiquement
            for (int i = 1; i <= nodes; i++) {
                String containerName = "tor_dynamic_node_" + i + "_" + System.currentTimeMillis();

                CreateContainerResponse container = dockerClient.createContainerCmd("eval_programation_mobile_b3dev:latest")
                        .withName(containerName)
                        .withNetworkMode("tor-network") // Ils doivent être sur le même réseau Docker
                        .exec();

                dockerClient.startContainerCmd(container.getId()).exec();
                containerIds.add(container.getId());
                System.out.println("Conteneur démarré : " + containerName);
            }

            // 2. Envoyer la requête au PREMIER conteneur de la chaîne que l'on vient de créer
            // On lui passe l'ordre de décrémenter (nodes - 1)
            String firstDynamicNodeUrl = String.format("http://tor_dynamic_node_1:8080/proxy?target=%s&nodes=%d", target, nodes - 1);

            // Attendre un court instant que les conteneurs Spring Boot soient prêts à recevoir du trafic
            Thread.sleep(3000);

            System.out.println("Envoi de la requête au premier nœud dynamique...");
            String response = restTemplate.getForObject(firstDynamicNodeUrl, String.class);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de la génération dynamique du circuit : " + e.getMessage();
        } finally {
            // 3. NETTOYAGE : Une fois la réponse obtenue (chemin inverse), on détruit les conteneurs
            System.out.println("Nettoyage des conteneurs éphémères...");
            for (String id : containerIds) {
                try {
                    dockerClient.stopContainerCmd(id).exec();
                    dockerClient.removeContainerCmd(id).exec();
                } catch (Exception ignored) {}
            }
        }
    }
}