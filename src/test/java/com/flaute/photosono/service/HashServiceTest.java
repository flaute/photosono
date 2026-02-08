package com.flaute.photosono.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HashServiceTest {

    private final HashService hashService = new HashService();

    @Test
    void testCalculateSHA256(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, Photosono!");

        // Expected SHA-256 for "Hello, Photosono!"
        String expectedHash = "830ed3a5e2ff1036d0709136ad2ff94f2188d217d180a1e1c5c14e18081d497e";
        String actualHash = hashService.calculateSHA256(testFile);

        assertEquals(expectedHash, actualHash);
    }
}
