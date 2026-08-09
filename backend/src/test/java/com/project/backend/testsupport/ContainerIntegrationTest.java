package com.project.backend.testsupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.project.backend.app.tenant.context.TenantContext;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Tag("integration")
public abstract class ContainerIntegrationTest {

    protected static final String TEST_TENANT_ID = "integration-test";

    @Autowired
    protected AdjustableTestClock testClock;

    @BeforeEach
    void initializeIntegrationTestContext() {
        TenantContext.setTenantId(TEST_TENANT_ID);
        testClock.setInstant(TestcontainersConfiguration.DEFAULT_TEST_INSTANT);
    }

    @AfterEach
    void clearIntegrationTestContext() {
        TenantContext.clear();
    }
}
