package com.selfhealing.watchdog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WatchdogApplicationTests {

    @Test
    void contextLoads() {
        // Stellt sicher, dass der Spring-Kontext samt Camunda-Engine fehlerfrei hochfährt.
    }
}
