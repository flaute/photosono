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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
        Path inputFile = tempDir.resolve("test.JPG");
        // Minimal JPEG: Start of Image (FF D8) + End of Image (FF D9)
        Files.write(inputFile, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9 });

        Path originalsBaseDir = tempDir.resolve("originals");
        Files.createDirectories(originalsBaseDir);

        when(config.getOriginalsDir()).thenReturn(originalsBaseDir.toString());
        when(hashService.calculateSHA256(inputFile)).thenReturn("aabbccddeeff");

        FileProcessorService.Result result = fileProcessorService.processFile(inputFile);

        assertEquals(FileProcessorService.Result.PROCESSED, result);
        // Expected path: originals/a/a/aabbccddeeff.jpg
        Path expectedPath = originalsBaseDir.resolve("a/a/aabbccddeeff.jpg");
        assertTrue(Files.exists(expectedPath));
    }

    @Test
    void testProcessCorruptedFile() throws IOException, NoSuchAlgorithmException {
        Path inputFile = tempDir.resolve("corrupted.JPG");
        Files.writeString(inputFile, "not a jpeg");

        Path corruptedBaseDir = tempDir.resolve("corrupted");
        Files.createDirectories(corruptedBaseDir);

        when(config.getCorruptedDir()).thenReturn(corruptedBaseDir.toString());
        when(hashService.calculateSHA256(inputFile)).thenReturn("112233445566");

        FileProcessorService.Result result = fileProcessorService.processFile(inputFile);

        assertEquals(FileProcessorService.Result.CORRUPTED, result);
        Path expectedPath = corruptedBaseDir.resolve("1/1/112233445566.jpg");
        assertTrue(Files.exists(expectedPath), "Corrupted file should be moved to corrupted directory");
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
    void testProcessUnknownTypeFileWithDuplicate() throws IOException, NoSuchAlgorithmException {
        Path inputFile1 = tempDir.resolve("test1.txt");
        Files.writeString(inputFile1, "content1");

        Path unknownTypeDir = tempDir.resolve("unknown-type");
        Path nestedDir = unknownTypeDir.resolve("a/a");
        Files.createDirectories(nestedDir);

        // Create an existing file with the same hash-based name
        Files.writeString(nestedDir.resolve("aabbccddeeff.txt"), "existing content");

        when(config.getUnknownTypeDir()).thenReturn(unknownTypeDir.toString());
        when(hashService.calculateSHA256(inputFile1)).thenReturn("aabbccddeeff");

        FileProcessorService.Result result = fileProcessorService.processFile(inputFile1);

        assertEquals(FileProcessorService.Result.SKIPPED, result);
        // Verify no new file was created (like a suffix one)
        Path suffixPath = nestedDir.resolve("aabbccddeeff-1.txt");
        assertFalse(Files.exists(suffixPath), "Collision should result in skipping, not a suffix");
        assertEquals("existing content", Files.readString(nestedDir.resolve("aabbccddeeff.txt")));
    }
}
