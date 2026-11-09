package com.contractiq.processing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {
    private final TextChunker chunker = new TextChunker();

    @Test
    void chunksAreDeterministicAndRetainOrganizationAndPage() {
        String text = "contract sentence. ".repeat(200);
        List<ChunkEntity> first = chunker.chunk("org-a", "doc-a", List.of(new ExtractedPage(3, text)));
        List<ChunkEntity> retry = chunker.chunk("org-a", "doc-a", List.of(new ExtractedPage(3, text)));

        assertTrue(first.size() > 1);
        assertEquals(first.stream().map(c -> c.id).toList(), retry.stream().map(c -> c.id).toList());
        assertTrue(first.stream().allMatch(c -> c.organizationId.equals("org-a") && c.pageNumber == 3));
        assertTrue(first.stream().allMatch(c -> c.content.length() <= 1200));
    }

    @Test
    void emptyTextProducesNoChunks() {
        assertTrue(chunker.chunk("org-a", "doc-a", List.of(new ExtractedPage(1, " \n "))).isEmpty());
    }
}
