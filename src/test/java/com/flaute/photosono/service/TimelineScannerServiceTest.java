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

class TimelineScannerServiceTest {

    private TimelineScannerService timelineScannerService;

    @Mock
    private PhotosonoConfig config;
    @Mock
    private PhotosonoConfig.Timeline timeline;
    @Mock
    private TimelineOrganizerService organizerService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        timelineScannerService = new TimelineScannerService(config, organizerService);
        when(config.getTimeline()).thenReturn(timeline);
        when(timeline.isEnabled()).thenReturn(true);
    }

    @Test
    void testScanOutputDirectory() throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        Path subDir = outputDir.resolve("a/b");
        Files.createDirectories(subDir);
        Path file1 = subDir.resolve("hash.jpg");
        Files.writeString(file1, "content");

        when(config.getOutputDir()).thenReturn(outputDir.toString());
        when(organizerService.organizeFile(any())).thenReturn(TimelineOrganizerService.Result.TIMELINE);

        timelineScannerService.scanOutputDirectory();

        verify(organizerService).organizeFile(file1);
    }

    @Test
    void testScanDisabled() throws IOException {
        when(timeline.isEnabled()).thenReturn(false);

        timelineScannerService.scanOutputDirectory();

        verifyNoInteractions(organizerService);
    }
}
