package com.contractiq.contracts;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
public class MinioDocumentStorage implements DocumentStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinioDocumentStorage.class);
    private final MinioClient client;
    private final String bucket;

    public MinioDocumentStorage(MinioClient client, @Value("${app.storage.bucket}") String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(objectKey)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType).build());
        } catch (Exception exception) {
            throw unavailable("PUT", exception);
        }
    }

    @Override
    public InputStream get(String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception exception) {
            throw unavailable("GET", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception exception) {
            throw unavailable("DELETE", exception);
        }
    }

    private void ensureBucket() throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private ResponseStatusException unavailable(String operation, Exception exception) {
        LOGGER.warn("Document storage operation failed operation={} exceptionType={}",
                operation, exception.getClass().getSimpleName());
        return new ResponseStatusException(SERVICE_UNAVAILABLE, "Document storage is unavailable", exception);
    }
}
