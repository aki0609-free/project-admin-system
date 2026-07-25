package com.project.backend.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Tag;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Tag("integration")
public abstract class ContainerIntegrationTest {
}
