package com.datakasauti;

import com.datakasauti.config.UploadProperties;
import com.datakasauti.service.FileService;
import com.datakasauti.service.TempFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storedUploadUsesGeneratedNameAndIsDeletedWhenClosed() throws Exception {
        UploadProperties properties = properties();
        FileService fileService = new FileService(properties);

        MockMultipartFile upload = new MockMultipartFile(
                "file",
                "../../customer-data.csv",
                "text/csv",
                "id,name\n1,Alice\n".getBytes(StandardCharsets.UTF_8)
        );

        Path storedPath;
        try (TempFile stored = fileService.store(upload)) {
            storedPath = stored.path();
            assertTrue(Files.exists(storedPath));
            assertTrue(storedPath.getFileName().toString().startsWith("upload-"));
            assertFalse(storedPath.getFileName().toString().contains("customer-data"));
            assertTrue(Files.readString(storedPath).contains("Alice"));
        }

        assertFalse(Files.exists(storedPath));
    }

    @Test
    void startupCleanupRemovesOnlyFilesOlderThanRetentionLimit() throws Exception {
        UploadProperties properties = properties();
        Path staleFile = Files.writeString(tempDirectory.resolve("stale.csv"), "old");
        Path freshFile = Files.writeString(tempDirectory.resolve("fresh.csv"), "new");
        Files.setLastModifiedTime(
                staleFile,
                FileTime.from(Instant.now().minusSeconds(2 * 60 * 60))
        );

        FileService fileService = new FileService(properties);
        fileService.cleanupStaleFiles();

        assertFalse(Files.exists(staleFile));
        assertTrue(Files.exists(freshFile));
    }

    private UploadProperties properties() {
        UploadProperties properties = new UploadProperties();
        properties.setTempDir(tempDirectory.toString());
        properties.setStaleFileMinutes(60);
        return properties;
    }
}
