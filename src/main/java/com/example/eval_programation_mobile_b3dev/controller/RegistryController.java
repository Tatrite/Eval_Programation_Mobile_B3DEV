package com.example.eval_programation_mobile_b3dev.controller;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Random;

@RestController
public class RegistryController {
    private final com.github.dockerjava.core.DockerClientConfig config = com.github.dockerjava.core.DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("tcp://host.docker.internal:2375")
            .build();
    private final com.github.dockerjava.transport.DockerHttpClient httpClient = new com.github.dockerjava.httpclient5.ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .maxConnections(100)
            .build();
    private final DockerClient dockerClient = com.github.dockerjava.core.DockerClientImpl.getInstance(config, httpClient);

    @Value("${ROLE:RELAIS}")
    private String myRole;

    @GetMapping("/registry/random")
    public ResponseEntity<String> getRandomRelay(@RequestParam(required = false) String exclude) {
        List<Container> containers = dockerClient.listContainersCmd().exec();

        List<String> relayNames = containers.stream()
                .map(c -> c.getNames()[0].replace("/", ""))
                .filter(name -> name.startsWith("relay-") && !name.equalsIgnoreCase(exclude))
                .toList();

        if (relayNames.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun relais disponible dans le pool");
        }

        String randomRelay = relayNames.get(new Random().nextInt(relayNames.size()));
        return ResponseEntity.ok(randomRelay);
    }
}