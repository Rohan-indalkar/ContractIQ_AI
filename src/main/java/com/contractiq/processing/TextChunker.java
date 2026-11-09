package com.contractiq.processing;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TextChunker {
    private static final int CHUNK_SIZE = 1200;
    private static final int OVERLAP = 160;

    public List<ChunkEntity> chunk(String organizationId, String documentId, List<ExtractedPage> pages) {
        List<ChunkEntity> result = new ArrayList<>();
        int index = 0;
        for (ExtractedPage page : pages) {
            String text = page.text();
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + CHUNK_SIZE, text.length());
                if (end < text.length()) {
                    int sentenceBoundary = text.lastIndexOf(". ", end);
                    if (sentenceBoundary > start + CHUNK_SIZE / 2) end = sentenceBoundary + 1;
                }
                String content = text.substring(start, end).trim();
                if (!content.isBlank()) {
                    ChunkEntity chunk = new ChunkEntity();
                    chunk.id = UUID.nameUUIDFromBytes((documentId + ":" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
                    chunk.organizationId = organizationId;
                    chunk.documentId = documentId;
                    chunk.chunkIndex = index++;
                    chunk.pageNumber = page.pageNumber();
                    chunk.content = content;
                    chunk.vectorId = chunk.id;
                    result.add(chunk);
                }
                if (end >= text.length()) break;
                start = Math.max(start + 1, end - OVERLAP);
            }
        }
        return result;
    }
}
