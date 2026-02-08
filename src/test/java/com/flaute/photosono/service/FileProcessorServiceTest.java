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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class FileProcessorServiceTest {

    private FileProcessorService fileProcessorService;

    @Mock
    private PhotosonoConfig config;
    @Mock
    private TimelineOrganizerService organizerService;
    @Mock
    private HashService hashService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileProcessorService = new FileProcessorService(config, organizerService, hashService);
    }

    @Test
    void testProcessFile() throws IOException, NoSuchAlgorithmException {
        Path inputFile = tempDir.resolve("test.JPG"); // Test extension normalization
        Files.writeString(inputFile, "content");

        Path outputBaseDir = tempDir.resolve("output");
        Files.createDirectories(outputBaseDir);

        when(config.getOutputDir()).thenReturn(outputBaseDir.toString());
        when(hashService.calculateSHA256(inputFile)).thenReturn("aabbccddeeff");

        fileProcessorService.processFile(inputFile);

        // Expected path: output/a/a/aabbccddeeff.jpg
        Path expectedPath = outputBaseDir.resolve("a/a/aabbccddeeff.jpg");
        assertTrue(Files.exists(expectedPath));

        verify(organizerService).organizeFile(expectedPath);
    }

    @Test
    void testProcessUnsupportedFile() throws IOException {
        Path inputFile = tempDir.resolve("test.txt");
        Files.writeString(inputFile, "content");

        fileProcessorService.processFile(inputFile);

        // Should not call organizer service
        verifyNoInteractions(organizerService);
    }
}
