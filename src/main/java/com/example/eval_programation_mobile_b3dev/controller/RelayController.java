package com.example.eval_programation_mobile_b3dev.controller;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@RestController
public class RelayController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final DockerClient dockerClient = createDockerClient();

    @Value("${ROLE:RELAIS}")
    private String myRole;

    @GetMapping("/proxy")
    public String proxy(
            @RequestParam String target,
            @RequestParam(defaultValue = "0") int nodes,
            @RequestParam(defaultValue = "1") int nextNodeId) { // <-- On passe le numéro du prochain nœud explicitement

        System.out.println("[" + myRole + "] Requête reçue. Cible: " + target + " | Sauts restants: " + nodes);

        if (nodes <= 0) {
            System.out.println("-> Dernier nœud atteint. Appel direct de : " + target);
            return restTemplate.getForObject(target, String.class);
        }

        if ("CHEF".equals(myRole)) {
            System.out.println("-> Mode CHEF : Création de la chaîne de " + nodes + " conteneurs...");
            List<String> containerIds = new ArrayList<>();

            try {
                for (int i = 1; i <= nodes; i++) {
                    String containerName = "tor-dynamic-node-" + i;

                    try { dockerClient.removeContainerCmd(containerName).withForce(true).exec(); } catch (Exception ignored) {}

                    CreateContainerResponse container = dockerClient.createContainerCmd("eval_programation_mobile_b3dev:latest")
                            .withName(containerName)
                            .withNetworkMode("tor-network")
                            .exec();

                    dockerClient.startContainerCmd(container.getId()).exec();
                    containerIds.add(container.getId());

                    boolean isHealthy = false;
                    for (int check = 0; check < 20; check++) {
                        var inspect = dockerClient.inspectContainerCmd(container.getId()).exec();
                        if (inspect.getState().getHealth() != null && "healthy".equals(inspect.getState().getHealth().getStatus())) {
                            isHealthy = true;
                            break;
                        }
                        Thread.sleep(500);
                    }
                    if (!isHealthy) throw new RuntimeException("Le nœud " + containerName + " a échoué à démarrer.");
                }

                 String firstNodeUrl = UriComponentsBuilder.fromUriString("http://tor-dynamic-node-1:8080/proxy")
                        .queryParam("target", target)
                        .queryParam("nodes", nodes - 1)
                        .queryParam("nextNodeId", 2)
                        .toUriString();

                System.out.println("-> Chef envoie vers : " + firstNodeUrl);
                return restTemplate.getForObject(firstNodeUrl, String.class);

            } catch (Exception e) {
                e.printStackTrace();
                return "Erreur infrastructure : " + e.getMessage();
            } finally {
                System.out.println("-> Mode CHEF : Nettoyage des conteneurs.");
                for (String id : containerIds) {
                    try { dockerClient.stopContainerCmd(id).exec(); dockerClient.removeContainerCmd(id).exec(); } catch (Exception ignored) {}
                }
            }
        }

        else {
            String nextNodeUrl = UriComponentsBuilder.fromUriString("http://tor-dynamic-node-" + nextNodeId + ":8080/proxy")
                    .queryParam("target", target)
                    .queryParam("nodes", nodes - 1)
                    .queryParam("nextNodeId", nextNodeId + 1)
                    .toUriString();

            System.out.println("-> Mode RELAIS : Passage de relais vers " + nextNodeUrl);
            return restTemplate.getForObject(nextNodeUrl, String.class);
        }
    }

    private DockerClient createDockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://host.docker.internal:2375").build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost()).maxConnections(100).build();
        return DockerClientImpl.getInstance(config, httpClient);
    }
}