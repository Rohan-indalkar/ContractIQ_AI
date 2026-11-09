package com.contractiq.processing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class TesseractOcrEngine implements OcrEngine {
    private final String binary;
    private final String languages;
    private final long timeoutSeconds;

    public TesseractOcrEngine(@Value("${app.ocr.binary:tesseract}") String binary,
                              @Value("${app.ocr.languages:eng}") String languages,
                              @Value("${app.ocr.timeout-seconds:60}") long timeoutSeconds) {
        this.binary = binary;
        this.languages = languages;
        this.timeoutSeconds = timeoutSeconds;
        if (binary.isBlank() || languages.isBlank() || timeoutSeconds < 1)
            throw new IllegalArgumentException("OCR binary, languages, and a positive timeout must be configured");
    }

    @Override
    public String recognize(BufferedImage image) throws IOException {
        Path temporaryDirectory = Files.createTempDirectory("contractiq-ocr-");
        try {
            Path input = temporaryDirectory.resolve("page.png");
            Path output = temporaryDirectory.resolve("result.txt");
            if (!ImageIO.write(image, "png", input.toFile())) {
                throw new IOException("Unable to encode page image for OCR");
            }

            Process process;
            try {
                process = new ProcessBuilder(binary, input.toString(), output.toString().replaceFirst("\\.txt$", ""),
                        "-l", languages, "txt")
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
            } catch (IOException exception) {
                throw new IOException("Tesseract OCR is unavailable; install Tesseract or configure app.ocr.binary", exception);
            }

            try {
                if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    throw new IOException("Tesseract OCR exceeded its configured timeout");
                }
            } catch (InterruptedException exception) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new IOException("Tesseract OCR was interrupted", exception);
            }
            if (process.exitValue() != 0) {
                throw new IOException("Tesseract OCR failed with exit code " + process.exitValue());
            }
            return Files.readString(output, java.nio.charset.StandardCharsets.UTF_8);
        } finally {
            try (var files = Files.list(temporaryDirectory)) {
                files.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        org.slf4j.LoggerFactory.getLogger(TesseractOcrEngine.class)
                                .warn("Unable to remove temporary OCR file");
                    }
                });
            }
            try {
                Files.deleteIfExists(temporaryDirectory);
            } catch (IOException exception) {
                org.slf4j.LoggerFactory.getLogger(TesseractOcrEngine.class)
                        .warn("Unable to remove temporary OCR directory");
            }
        }
    }
}
