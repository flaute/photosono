package com.flaute.photosono.service;

import com.flaute.photosono.config.PhotosonoConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class FileProcessorServiceTest {

    private FileProcessorService fileProcessorService;

    @Mock
    private PhotosonoConfig config;
    @Mock
    private HashService hashService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileProcessorService = new FileProcessorService(config, hashService);
    }

    @Test
    void testProcessFile() throws IOException, NoSuchAlgorithmException {
        Path inputFile = tempDir.resolve("test.JPG"); // Test extension normalization
        Files.writeString(inputFile, "content");

        Path originalsBaseDir = tempDir.resolve("originals");
        Files.createDirectories(originalsBaseDir);

        when(config.getOriginalsDir()).thenReturn(originalsBaseDir.toString());
        when(hashService.calculateSHA256(inputFile)).thenReturn("aabbccddeeff");

        fileProcessorService.processFile(inputFile);

        // Expected path: originals/a/a/aabbccddeeff.jpg
        Path expectedPath = originalsBaseDir.resolve("a/a/aabbccddeeff.jpg");
        assertTrue(Files.exists(expectedPath));
    }

    @Test
    void testProcessUnknownTypeFile() throws IOException, NoSuchAlgorithmException {
        Path inputFile = tempDir.resolve("test.txt");
        Files.writeString(inputFile, "content");

        Path unknownTypeDir = tempDir.resolve("unknown-type");
        Files.createDirectories(unknownTypeDir);

        when(config.getUnknownTypeDir()).thenReturn(unknownTypeDir.toString());
        when(hashService.calculateSHA256(inputFile)).thenReturn("aabbccddeeff");

        FileProcessorService.Result result = fileProcessorService.processFile(inputFile);

        assertEquals(FileProcessorService.Result.UNKNOWN_TYPE, result);
        // Expected path: unknown-type/a/a/aabbccddeeff.txt
        Path expectedPath = unknownTypeDir.resolve("a/a/aabbccddeeff.txt");
        assertTrue(Files.exists(expectedPath), "Unknown type file should be copied to unknown-type directory");
        assertEquals("content", Files.readString(expectedPath));
    }

    @Test
    void testProcessUnknownTypeFileWithCollision() throws IOException, NoSuchAlgorithmException {
        Path inputFile1 = tempDir.resolve("test1.txt");
        Files.writeString(inputFile1, "content1");
        Path inputFile2 = tempDir.resolve("test2.txt");
        Files.writeString(inputFile2, "content2");

        Path unknownTypeDir = tempDir.resolve("unknown-type");
        Path nestedDir = unknownTypeDir.resolve("a/a");
        Files.createDirectories(nestedDir);

        // Simulate a collision by creating a file with the same hash-based name
        Files.writeString(nestedDir.resolve("aabbccddeeff.txt"), "existing content");

        when(config.getUnknownTypeDir()).thenReturn(unknownTypeDir.toString());
        // Both files have same hash for simplicity in this test case to trigger
        // collision logic
        when(hashService.calculateSHA256(any())).thenReturn("aabbccddeeff");

        fileProcessorService.processFile(inputFile1);

        // Expected path: unknown-type/a/a/aabbccddeeff-1.txt
        Path expectedPath = nestedDir.resolve("aabbccddeeff-1.txt");
        assertTrue(Files.exists(expectedPath), "Collision should result in a -1 suffix");
        assertEquals("content1", Files.readString(expectedPath));
    }
}
