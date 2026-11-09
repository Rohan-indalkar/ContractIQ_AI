package com.contractiq.contracts;

import java.io.InputStream;

public interface DocumentStorage {
    void put(String objectKey, byte[] content, String contentType);
    InputStream get(String objectKey);
    void delete(String objectKey);
}
