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
    void testScanOriginalsDirectory() throws IOException {
        Path originalsDir = tempDir.resolve("originals");
        Files.createDirectories(originalsDir);
        Path subDir = originalsDir.resolve("a/b");
        Files.createDirectories(subDir);
        Path file1 = subDir.resolve("hash.jpg");
        Files.writeString(file1, "content");

        when(config.getOriginalsDir()).thenReturn(originalsDir.toString());
        when(organizerService.organizeFile(any())).thenReturn(TimelineOrganizerService.Result.TIMELINE);

        timelineScannerService.scanOriginalsDirectory();

        verify(organizerService).organizeFile(file1);
    }

    @Test
    void testScanDisabled() throws IOException {
        when(timeline.isEnabled()).thenReturn(false);

        timelineScannerService.scanOriginalsDirectory();

        verifyNoInteractions(organizerService);
    }
}
