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
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class TimelineOrganizerServiceTest {

    private TimelineOrganizerService timelineOrganizerService;

    @Mock
    private PhotosonoConfig config;
    @Mock
    private DateExtractorService dateExtractorService;
    @Mock
    private HashService hashService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        timelineOrganizerService = new TimelineOrganizerService(config, dateExtractorService, hashService);
    }

    @Test
    void testOrganizeFile() throws IOException, NoSuchAlgorithmException {
        Path sourceFile = tempDir.resolve("input.jpg");
        Files.writeString(sourceFile, "content");

        Path timelineDir = tempDir.resolve("timeline");
        Files.createDirectories(timelineDir);

        when(config.getTimelineDir()).thenReturn(timelineDir.toString());

        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.FEBRUARY, 8, 10, 0, 0);
        Date testDate = cal.getTime();

        when(dateExtractorService.extractCreationDate(sourceFile)).thenReturn(Optional.of(testDate));
        when(hashService.calculateSHA256(any())).thenReturn("hash123");

        timelineOrganizerService.organizeFile(sourceFile);

        Path expectedPath = timelineDir.resolve("2026/02/08/2026-02-08-10-00-00.jpg");
        assertTrue(Files.exists(expectedPath));
    }

    @Test
    void testOrganizeFileWithConflictDuplicateHash() throws IOException, NoSuchAlgorithmException {
        Path sourceFile = tempDir.resolve("input.jpg");
        Files.writeString(sourceFile, "content");

        Path timelineDir = tempDir.resolve("timeline");
        Path existingFileDir = timelineDir.resolve("2026/02/08");
        Files.createDirectories(existingFileDir);
        Path existingFile = existingFileDir.resolve("2026-02-08-10-00-00.jpg");
        Files.writeString(existingFile, "content"); // Same content -> same hash

        when(config.getTimelineDir()).thenReturn(timelineDir.toString());

        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.FEBRUARY, 8, 10, 0, 0);
        Date testDate = cal.getTime();

        when(dateExtractorService.extractCreationDate(sourceFile)).thenReturn(Optional.of(testDate));
        when(hashService.calculateSHA256(any())).thenReturn("hash123");

        timelineOrganizerService.organizeFile(sourceFile);

        // Verification: The second copy should be skipped, so no -1 file should exist
        Path conflictPath = existingFileDir.resolve("2026-02-08-10-00-00-1.jpg");
        assertTrue(!Files.exists(conflictPath));
    }

    @Test
    void testOrganizeFileWithConflictDifferentHash() throws IOException, NoSuchAlgorithmException {
        Path sourceFile = tempDir.resolve("input.jpg");
        Files.writeString(sourceFile, "content2");

        Path timelineDir = tempDir.resolve("timeline");
        Path existingFileDir = timelineDir.resolve("2026/02/08");
        Files.createDirectories(existingFileDir);
        Path existingFile = existingFileDir.resolve("2026-02-08-10-00-00.jpg");
        Files.writeString(existingFile, "content1"); // Different content -> different hash

        when(config.getTimelineDir()).thenReturn(timelineDir.toString());

        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.FEBRUARY, 8, 10, 0, 0);
        Date testDate = cal.getTime();

        when(dateExtractorService.extractCreationDate(sourceFile)).thenReturn(Optional.of(testDate));

        // Return different hashes for source and existing file
        when(hashService.calculateSHA256(sourceFile)).thenReturn("hash_new");
        when(hashService.calculateSHA256(existingFile)).thenReturn("hash_old");

        timelineOrganizerService.organizeFile(sourceFile);

        // Verification: The second copy should be created with -1
        Path conflictPath = existingFileDir.resolve("2026-02-08-10-00-00-1.jpg");
        assertTrue(Files.exists(conflictPath));
    }
}
