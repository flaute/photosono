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

import static org.mockito.Mockito.*;

class FileScannerServiceTest {

    private FileScannerService fileScannerService;

    @Mock
    private PhotosonoConfig config;
    @Mock
    private FileProcessorService processorService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileScannerService = new FileScannerService(config, processorService);
    }

    @Test
    void testScanInputDirectory() throws IOException {
        Path inputDir = tempDir.resolve("input");
        Files.createDirectories(inputDir);
        Path file1 = inputDir.resolve("file1.jpg");
        Path file2 = inputDir.resolve("file2.jpg");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        // Nested directory
        Path subDir = inputDir.resolve("sub");
        Files.createDirectories(subDir);
        Path file3 = subDir.resolve("file3.jpg");
        Files.writeString(file3, "content3");

        when(config.getInputDir()).thenReturn(inputDir.toString());

        fileScannerService.scanInputDirectory();

        verify(processorService).processFile(file1);
        verify(processorService).processFile(file2);
        verify(processorService).processFile(file3);
    }
}
