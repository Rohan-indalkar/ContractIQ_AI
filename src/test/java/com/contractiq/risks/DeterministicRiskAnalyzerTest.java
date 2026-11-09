package com.contractiq.risks;

import com.contractiq.processing.ExtractedPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicRiskAnalyzerTest {
    private final DeterministicRiskAnalyzer analyzer = new DeterministicRiskAnalyzer();

    @Test
    void emitsHumanReviewFindingWithSourcePageAndEvidence() {
        var findings = analyzer.analyze("org-a", "contract-a", "doc-a",
                List.of(new ExtractedPage(7, "The parties agree to unlimited liability for this breach.")));

        assertEquals(1, findings.size());
        assertEquals("UNLIMITED_LIABILITY", findings.getFirst().category);
        assertEquals(7, findings.getFirst().pageNumber);
        assertTrue(findings.getFirst().evidenceText.contains("unlimited liability"));
        assertEquals("PENDING", findings.getFirst().reviewStatus);
    }

    @Test
    void givesNoFindingWithoutRuleEvidence() {
        assertTrue(analyzer.analyze("org-a", "contract-a", "doc-a",
                List.of(new ExtractedPage(1, "The agreement starts on 1 January."))).isEmpty());
    }
}
