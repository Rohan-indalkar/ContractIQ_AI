package com.contractiq.processing;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DocumentTextExtractorTest {
    @Test
    void usesOcrForPagesWithoutTextLayer() throws Exception {
        byte[] pdfBytes;
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            pdf.addPage(new PDPage());
            pdf.save(output);
            pdfBytes = output.toByteArray();
        }
        OcrEngine ocr = mock(OcrEngine.class);
        when(ocr.recognize(any())).thenReturn("Scanned agreement text");

        var pages = new DocumentTextExtractor(ocr).extract(pdfBytes, "application/pdf");

        assertEquals(1, pages.size());
        assertEquals(1, pages.getFirst().pageNumber());
        assertEquals("Scanned agreement text", pages.getFirst().text());
        verify(ocr).recognize(any());
    }
}
