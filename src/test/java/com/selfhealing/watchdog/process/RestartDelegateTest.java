package com.selfhealing.watchdog.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.selfhealing.watchdog.config.WatchdogProperties;
import com.selfhealing.watchdog.docker.DockerService;
import java.time.Duration;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestartDelegateTest {

    @Mock
    private DockerService dockerService;

    private WatchdogProperties properties;
    private RestartDelegate delegate;

    @BeforeEach
    void setUp() {
        properties = new WatchdogProperties();
        properties.getBackoff().setBase(Duration.ofSeconds(2));
        properties.getBackoff().setMaxWait(Duration.ofSeconds(30));
        delegate = new RestartDelegate(dockerService, properties);
    }

    @Test
    void waitTimeDoublesPerAttempt() {
        assertThat(timerWaitAfter(0)).isEqualTo("PT2S"); // Versuch 1: base
        assertThat(timerWaitAfter(1)).isEqualTo("PT4S"); // Versuch 2: base*2
        assertThat(timerWaitAfter(2)).isEqualTo("PT8S"); // Versuch 3: base*4
    }

    @Test
    void waitTimeIsCappedAtMaxWait() {
        properties.getBackoff().setMaxWait(Duration.ofSeconds(5));
        assertThat(timerWaitAfter(3)).isEqualTo("PT5S"); // base*8 = 16s -> gedeckelt auf 5s
    }

    /** Führt den Delegate mit gegebenem Vorgänger-Versuchszähler aus und liefert das gesetzte timerWait. */
    private String timerWaitAfter(int previousAttempts) {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("attempts")).thenReturn(previousAttempts);
        when(execution.getVariable("containerName")).thenReturn("target-x");
        when(execution.getVariable("maxAttempts")).thenReturn(3);

        delegate.execute(execution);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(execution).setVariable(eq("timerWait"), captor.capture());
        return (String) captor.getValue();
    }
}
