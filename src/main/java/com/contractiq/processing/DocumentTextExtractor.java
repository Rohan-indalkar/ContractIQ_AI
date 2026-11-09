package com.contractiq.processing;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentTextExtractor {
    private final OcrEngine ocr;

    public DocumentTextExtractor(OcrEngine ocr) {
        this.ocr = ocr;
    }

    public List<ExtractedPage> extract(byte[] bytes, String mediaType) throws IOException {
        if ("application/pdf".equals(mediaType)) {
            try (var pdf = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                PDFRenderer renderer = new PDFRenderer(pdf);
                List<ExtractedPage> pages = new ArrayList<>();
                for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    String text = normalize(stripper.getText(pdf));
                    if (text.isBlank()) {
                        text = normalize(ocr.recognize(renderer.renderImageWithDPI(page - 1, 180)));
                    }
                    pages.add(new ExtractedPage(page, text));
                }
                return pages;
            }
        }
        String text;
        try {
            text = new Tika().parseToString(new ByteArrayInputStream(bytes));
        } catch (org.apache.tika.exception.TikaException exception) {
            throw new IOException("Unable to extract document text", exception);
        }
        return List.of(new ExtractedPage(1, normalize(text)));
    }

    private String normalize(String text) {
        return text.replace("\u0000", "").replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                .replaceAll("[ \\t]+", " ").replaceAll("\\R{3,}", "\n\n").trim();
    }
}
