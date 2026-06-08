package com.selfhealing.watchdog.docker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Liest State und Health der laufenden Ziel-Container über die echte Docker-API.
 * Wird übersprungen, wenn kein Docker-Daemon erreichbar ist (z. B. in CI ohne Docker).
 */
@SpringBootTest
@ActiveProfiles("test")
class DockerServiceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(DockerServiceIntegrationTest.class);

    @Autowired
    private DockerService dockerService;

    @Autowired
    private DockerClient dockerClient;

    @BeforeEach
    void requireDocker() {
        assumeTrue(dockerReachable(), "Docker-Daemon nicht erreichbar — Test übersprungen.");
    }

    @Test
    void readsStateAndHealthOfTargetContainers() {
        log.info("Ziel-Container in Docker: {}", dockerService.listTargetContainers());

        ContainerStatus a = dockerService.inspect("target-service-a");
        ContainerStatus b = dockerService.inspect("target-service-b");
        log.info("target-service-a -> running={}, state={}, health={}", a.running(), a.state(), a.health());
        log.info("target-service-b -> running={}, state={}, health={}", b.running(), b.state(), b.health());

        assertThat(a.running()).isTrue();
        assertThat(a.health()).isEqualTo(ContainerHealth.HEALTHY);

        assertThat(b.running()).isTrue();
        assertThat(b.health()).isEqualTo(ContainerHealth.NONE);
    }

    private boolean dockerReachable() {
        try {
            dockerClient.pingCmd().exec();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
