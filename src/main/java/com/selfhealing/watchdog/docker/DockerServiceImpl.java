package com.selfhealing.watchdog.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.selfhealing.watchdog.config.WatchdogProperties;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DockerServiceImpl implements DockerService {

    private final DockerClient dockerClient;
    private final Set<String> targetContainers;

    public DockerServiceImpl(DockerClient dockerClient, WatchdogProperties properties) {
        this.dockerClient = dockerClient;
        this.targetContainers = Set.copyOf(properties.getTargetContainers());
    }

    @Override
    public List<String> listTargetContainers() {
        return dockerClient.listContainersCmd().withShowAll(true).exec().stream()
                .flatMap(container -> Arrays.stream(container.getNames()))
                .map(DockerServiceImpl::stripLeadingSlash)
                .filter(targetContainers::contains)
                .sorted()
                .toList();
    }

    @Override
    public ContainerStatus inspect(String containerName) {
        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerName).exec();
        InspectContainerResponse.ContainerState state = response.getState();
        return new ContainerStatus(
                stripLeadingSlash(response.getName()),
                response.getId(),
                Boolean.TRUE.equals(state.getRunning()),
                state.getStatus(),
                toContainerHealth(state.getHealth()));
    }

    @Override
    public void restart(String containerName) {
        dockerClient.restartContainerCmd(containerName).exec();
    }

    @Override
    public void stop(String containerName) {
        dockerClient.stopContainerCmd(containerName).exec();
    }

    @Override
    public void remove(String containerName) {
        dockerClient.removeContainerCmd(containerName).withForce(true).exec();
    }

    private static ContainerHealth toContainerHealth(HealthState health) {
        if (health == null || health.getStatus() == null) {
            return ContainerHealth.NONE;
        }
        return switch (health.getStatus()) {
            case "healthy" -> ContainerHealth.HEALTHY;
            case "unhealthy" -> ContainerHealth.UNHEALTHY;
            case "starting" -> ContainerHealth.STARTING;
            default -> ContainerHealth.NONE;
        };
    }

    private static String stripLeadingSlash(String name) {
        return name != null && name.startsWith("/") ? name.substring(1) : name;
    }
}
