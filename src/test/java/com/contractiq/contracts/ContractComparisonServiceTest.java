package com.contractiq.contracts;

import com.contractiq.processing.ChunkRepository;
import com.contractiq.shared.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ContractComparisonServiceTest {
    private final ContractComparisonService service = new ContractComparisonService(
            mock(ContractRepository.class), mock(DocumentRepository.class), mock(ChunkRepository.class),
            mock(ChatClient.Builder.class), new ObjectMapper(), mock(AuditService.class));

    @Test
    void acceptsDifferencesOnlyWhenBothQuotesMatchTheirContractPages() throws Exception {
        var first = contract("first", "Supplier Agreement");
        var second = contract("second", "Customer Agreement");
        var firstSources = List.of(new ContractComparisonService.SourceText("doc-a", 2, "Payment is due within 30 days."));
        var secondSources = List.of(new ContractComparisonService.SourceText("doc-b", 4, "Payment is due within 45 days."));
        String output = """
                {"summary":"ignored model summary","differences":[{"topic":"Payment terms",
                "first":{"documentId":"doc-a","pageNumber":2,"quote":"within 30 days"},
                "second":{"documentId":"doc-b","pageNumber":4,"quote":"within 45 days"}}]}
                """;

        var result = service.parseOutput(output, first, second, firstSources, secondSources);

        assertEquals("COMPARED", result.status());
        assertEquals(1, result.differences().size());
        assertEquals("within 30 days", result.differences().getFirst().first().text());
        assertFalse(result.summary().contains("ignored"));
    }

    @Test
    void rejectsQuotesAttributedToAnotherDocumentOrPage() {
        var first = contract("first", "Supplier Agreement");
        var second = contract("second", "Customer Agreement");
        var firstSources = List.of(new ContractComparisonService.SourceText("doc-a", 2, "Payment is due within 30 days."));
        var secondSources = List.of(new ContractComparisonService.SourceText("doc-b", 4, "Payment is due within 45 days."));
        String output = """
                {"differences":[{"topic":"Payment terms",
                "first":{"documentId":"doc-a","pageNumber":2,"quote":"within 30 days"},
                "second":{"documentId":"doc-b","pageNumber":3,"quote":"within 45 days"}}]}
                """;

        assertThrows(IllegalArgumentException.class,
                () -> service.parseOutput(output, first, second, firstSources, secondSources));
    }

    private ContractEntity contract(String id, String title) {
        ContractEntity contract = new ContractEntity();
        contract.id = id;
        contract.title = title;
        return contract;
    }
}
