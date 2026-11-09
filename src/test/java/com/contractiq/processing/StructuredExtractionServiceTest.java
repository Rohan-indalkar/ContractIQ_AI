package com.contractiq.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class StructuredExtractionServiceTest {
    private final StructuredExtractionService service =
            new StructuredExtractionService(mock(ChatClient.Builder.class), new ObjectMapper());

    @Test
    void parsesOnlyExactEvidenceAndExplicitDueDates() throws Exception {
        String text = "The agreement starts on 2026-01-01. Supplier shall deliver the report by 2026-03-01.";
        String output = """
                {"clauses":[{"type":"EFFECTIVE_DATE","value":{"date":"2026-01-01"},"evidence":"The agreement starts on 2026-01-01."}],
                 "obligations":[{"description":"deliver the report by 2026-03-01","dueDate":"2026-03-01","evidence":"Supplier shall deliver the report by 2026-03-01."}]}
                """;

        var extracted = service.parseOutput(output, new ExtractedPage(3, text));

        assertEquals(1, extracted.clauses().size());
        assertEquals(3, extracted.clauses().getFirst().pageNumber());
        assertEquals("EFFECTIVE_DATE", extracted.clauses().getFirst().type());
        assertEquals(1, extracted.obligations().size());
        assertEquals(java.time.LocalDate.of(2026, 3, 1), extracted.obligations().getFirst().dueDate());
    }

    @Test
    void rejectsEvidenceNotPresentInThePage() {
        String output = """
                {"clauses":[{"type":"TERM","value":{"text":"Five years"},"evidence":"The term is five years."}],
                 "obligations":[]}
                """;
        assertThrows(IllegalArgumentException.class,
                () -> service.parseOutput(output, new ExtractedPage(1, "The contract may be terminated for cause.")));
    }

    @Test
    void rejectsUnparseableObligationDates() {
        String output = """
                {"clauses":[],"obligations":[{"description":"Pay the invoice","dueDate":"soon","evidence":"Pay the invoice."}]}
                """;
        assertThrows(IllegalArgumentException.class,
                () -> service.parseOutput(output, new ExtractedPage(1, "Pay the invoice.")));
    }

    @Test
    void rejectsClauseValuesThatAreNotSupportedByTheEvidenceQuote() {
        String output = """
                {"clauses":[{"type":"TERM","value":{"text":"Ten years"},"evidence":"The term is five years."}],
                 "obligations":[]}
                """;
        assertThrows(IllegalArgumentException.class,
                () -> service.parseOutput(output, new ExtractedPage(1, "The term is five years.")));
    }
}
