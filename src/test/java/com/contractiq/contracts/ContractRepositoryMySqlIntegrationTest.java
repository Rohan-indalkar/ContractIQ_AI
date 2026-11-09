package com.contractiq.contracts;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class ContractRepositoryMySqlIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired private ContractRepository contracts;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void flywaySchemaValidatesAndContractQueriesRemainTenantScoped() {
        jdbc.update("INSERT INTO organizations (id, name) VALUES (?, ?)", "org-a", "Organization A");
        jdbc.update("INSERT INTO organizations (id, name) VALUES (?, ?)", "org-b", "Organization B");
        jdbc.update("INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, ?, ?)",
                "user-a", "a@example.test", "hash", "A");
        jdbc.update("INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, ?, ?)",
                "user-b", "b@example.test", "hash", "B");

        ContractEntity contract = new ContractEntity();
        contract.id = "contract-b";
        contract.organizationId = "org-b";
        contract.title = "Confidential agreement";
        contract.status = "ACTIVE";
        contract.createdBy = "user-b";
        contracts.saveAndFlush(contract);

        assertTrue(contracts.findByIdAndOrganizationId("contract-b", "org-b").isPresent());
        assertTrue(contracts.findByIdAndOrganizationId("contract-b", "org-a").isEmpty());
        assertEquals(0, contracts.findAllByOrganizationId("org-a", org.springframework.data.domain.PageRequest.of(0, 10))
                .getTotalElements());
        ContractService service = new ContractService(contracts, mock(com.contractiq.shared.AuditService.class));
        var ownContracts = service.list(new com.contractiq.identity.UserPrincipal(
                        "user-a", "org-a", "a@example.test", com.contractiq.identity.Role.MEMBER),
                org.springframework.data.domain.PageRequest.of(0, 10),
                new ContractService.ContractFilters("Confidential", null, null, null, null, null));
        assertEquals(0, ownContracts.getTotalElements());
        assertEquals(64, jdbc.queryForObject(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'obligations' AND column_name = 'source_key'",
                Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'email_outbox'",
                Integer.class));
    }
}
