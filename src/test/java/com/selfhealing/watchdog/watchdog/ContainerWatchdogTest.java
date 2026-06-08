package com.selfhealing.watchdog.watchdog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.selfhealing.watchdog.config.WatchdogProperties;
import com.selfhealing.watchdog.docker.ContainerHealth;
import com.selfhealing.watchdog.docker.ContainerStatus;
import com.selfhealing.watchdog.docker.DockerService;
import java.util.List;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContainerWatchdogTest {

    @Mock
    private DockerService dockerService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RuntimeService runtimeService;

    private WatchdogProperties properties;
    private ContainerWatchdog watchdog;

    @BeforeEach
    void setUp() {
        properties = new WatchdogProperties();
        watchdog = new ContainerWatchdog(dockerService, properties, runtimeService);
    }

    @Test
    void flagsGoneNotRunningAndUnhealthyAsFailures() {
        properties.setTargetContainers(List.of("c-gone", "c-stopped", "c-unhealthy"));
        // c-gone fehlt in der Liste der existierenden Container -> GONE
        when(dockerService.listTargetContainers()).thenReturn(List.of("c-stopped", "c-unhealthy"));
        when(dockerService.inspect("c-stopped"))
                .thenReturn(status("c-stopped", false, ContainerHealth.NONE, "exited"));
        when(dockerService.inspect("c-unhealthy"))
                .thenReturn(status("c-unhealthy", true, ContainerHealth.UNHEALTHY, "running"));

        assertThat(watchdog.detectFailures()).containsExactlyInAnyOrder(
                new ContainerFailure("c-gone", FailureReason.GONE),
                new ContainerFailure("c-stopped", FailureReason.NOT_RUNNING),
                new ContainerFailure("c-unhealthy", FailureReason.UNHEALTHY));
    }

    @Test
    void doesNotFlagRunningHealthyStartingOrWithoutHealthcheck() {
        properties.setTargetContainers(List.of("c-healthy", "c-none", "c-starting"));
        when(dockerService.listTargetContainers())
                .thenReturn(List.of("c-healthy", "c-none", "c-starting"));
        when(dockerService.inspect("c-healthy"))
                .thenReturn(status("c-healthy", true, ContainerHealth.HEALTHY, "running"));
        when(dockerService.inspect("c-none"))
                .thenReturn(status("c-none", true, ContainerHealth.NONE, "running"));
        when(dockerService.inspect("c-starting"))
                .thenReturn(status("c-starting", true, ContainerHealth.STARTING, "running"));

        assertThat(watchdog.detectFailures()).isEmpty();
    }

    @Test
    void startsRecoveryProcessWhenNoActiveInstanceExists() {
        properties.setTargetContainers(List.of("target-x"));
        when(dockerService.listTargetContainers()).thenReturn(List.of()); // target-x ist GONE
        when(activeCountQuery()).thenReturn(0L);

        watchdog.poll();

        verify(runtimeService).startProcessInstanceByKey(
                eq(ContainerWatchdog.RECOVERY_PROCESS_KEY), eq("target-x"), anyMap());
    }

    @Test
    void skipsRecoveryWhenActiveInstanceAlreadyExists() {
        properties.setTargetContainers(List.of("target-x"));
        when(dockerService.listTargetContainers()).thenReturn(List.of()); // target-x ist GONE
        when(activeCountQuery()).thenReturn(1L);

        watchdog.poll();

        verify(runtimeService, never())
                .startProcessInstanceByKey(anyString(), anyString(), anyMap());
    }

    @Test
    void pollSurvivesWhenDockerIsUnavailable() {
        properties.setTargetContainers(List.of("target-x"));
        when(dockerService.listTargetContainers())
                .thenThrow(new RuntimeException("Docker-Daemon nicht erreichbar"));

        assertThatCode(() -> watchdog.poll()).doesNotThrowAnyException();
        verify(runtimeService, never())
                .startProcessInstanceByKey(anyString(), anyString(), anyMap());
    }

    /** Fluent De-Dup-Query auf dem Deep-Stub-Mock — liefert die Anzahl aktiver Instanzen. */
    private long activeCountQuery() {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(anyString())
                .processInstanceBusinessKey(anyString())
                .active()
                .count();
    }

    private static ContainerStatus status(String name, boolean running, ContainerHealth health, String state) {
        return new ContainerStatus(name, "id-" + name, running, state, health);
    }
}
