package com.contractiq.risks;

import com.contractiq.processing.ExtractedPage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeterministicRiskAnalyzer {
    private static final List<Rule> RULES = List.of(
            new Rule("UNLIMITED_LIABILITY", "HIGH", Pattern.compile("unlimited liability|without limitation of liability", Pattern.CASE_INSENSITIVE)),
            new Rule("AUTO_RENEWAL", "MEDIUM", Pattern.compile("automatically renew|automatic renewal|successive renewal term", Pattern.CASE_INSENSITIVE)),
            new Rule("BROAD_INDEMNITY", "MEDIUM", Pattern.compile("indemnify and hold harmless", Pattern.CASE_INSENSITIVE)),
            new Rule("TERMINATION_RESTRICTION", "MEDIUM", Pattern.compile("may not terminate|non-cancellable|irrevocably waives.*termination", Pattern.CASE_INSENSITIVE))
    );

    public List<RiskEntity> analyze(String organizationId, String contractId, String documentId, List<ExtractedPage> pages) {
        List<RiskEntity> findings = new ArrayList<>();
        for (ExtractedPage page : pages) {
            for (Rule rule : RULES) {
                Matcher matcher = rule.pattern().matcher(page.text());
                if (!matcher.find()) continue;
                int start = Math.max(0, matcher.start() - 180);
                int end = Math.min(page.text().length(), matcher.end() + 220);
                RiskEntity risk = new RiskEntity();
                risk.id = UUID.nameUUIDFromBytes((documentId + ":" + page.pageNumber() + ":" + rule.code())
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
                risk.organizationId = organizationId;
                risk.contractId = contractId;
                risk.documentId = documentId;
                risk.category = rule.code();
                risk.severity = rule.severity();
                risk.description = "Potential clause requires human review; this keyword rule is not a legal conclusion.";
                risk.evidenceText = page.text().substring(start, end);
                risk.pageNumber = page.pageNumber();
                risk.reviewStatus = "PENDING";
                findings.add(risk);
            }
        }
        return findings;
    }

    private record Rule(String code, String severity, Pattern pattern) {}
}
