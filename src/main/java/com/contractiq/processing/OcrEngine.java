package com.contractiq.processing;

import java.awt.image.BufferedImage;
import java.io.IOException;

public interface OcrEngine {
    String recognize(BufferedImage image) throws IOException;
}
