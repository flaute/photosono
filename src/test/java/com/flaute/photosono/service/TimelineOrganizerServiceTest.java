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
        // Output file (source for timeline)
        Path sourceFile = tempDir.resolve("output/a/b/hash123.jpg");
        Files.createDirectories(sourceFile.getParent());
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

        Path expectedPath = timelineDir.resolve("2026/02/08/20260208-100000.jpg");
        assertTrue(Files.exists(expectedPath), "Symlink should exist");
        assertTrue(Files.isSymbolicLink(expectedPath), "File should be a symbolic link");

        Path target = Files.readSymbolicLink(expectedPath);
        assertTrue(!target.isAbsolute(), "Symlink should be relative");
    }

    @Test
    void testOrganizeFileWithConflictDuplicateHash() throws IOException, NoSuchAlgorithmException {
        Path sourceFile = tempDir.resolve("output/a/b/hash123.jpg");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "content");

        Path timelineDir = tempDir.resolve("timeline");
        Path existingFileDir = timelineDir.resolve("2026/02/08");
        Files.createDirectories(existingFileDir);
        Path existingFile = existingFileDir.resolve("20260208-100000.jpg");
        Files.writeString(existingFile, "content"); // Same content -> same hash

        when(config.getTimelineDir()).thenReturn(timelineDir.toString());

        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.FEBRUARY, 8, 10, 0, 0);
        Date testDate = cal.getTime();

        when(dateExtractorService.extractCreationDate(sourceFile)).thenReturn(Optional.of(testDate));
        when(hashService.calculateSHA256(any())).thenReturn("hash123");

        timelineOrganizerService.organizeFile(sourceFile);

        // Verification: The second link should be skipped
        Path conflictPath = existingFileDir.resolve("20260208-100000-1.jpg");
        assertTrue(!Files.exists(conflictPath));
    }

    @Test
    void testOrganizeFileWithConflictDifferentHash() throws IOException, NoSuchAlgorithmException {
        Path sourceFile = tempDir.resolve("output/a/b/hash_new.jpg");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "content2");

        Path timelineDir = tempDir.resolve("timeline");
        Path existingFileDir = timelineDir.resolve("2026/02/08");
        Files.createDirectories(existingFileDir);
        Path existingFile = existingFileDir.resolve("20260208-100000.jpg");
        Files.writeString(existingFile, "content1"); // Different content -> different hash

        when(config.getTimelineDir()).thenReturn(timelineDir.toString());

        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.FEBRUARY, 8, 10, 0, 0);
        Date testDate = cal.getTime();

        when(dateExtractorService.extractCreationDate(sourceFile)).thenReturn(Optional.of(testDate));

        // Return different hashes
        when(hashService.calculateSHA256(sourceFile)).thenReturn("hash_new");
        when(hashService.calculateSHA256(existingFile)).thenReturn("hash_old");

        timelineOrganizerService.organizeFile(sourceFile);

        // Verification: The second link should be created with -1
        Path conflictPath = existingFileDir.resolve("20260208-100000-1.jpg");
        assertTrue(Files.exists(conflictPath));
        assertTrue(Files.isSymbolicLink(conflictPath));
    }

    @Test
    void testOrganizeFileNoDateToUnknown() throws IOException, NoSuchAlgorithmException {
        Path sourceFile = tempDir.resolve("output/a/b/unknownhash.jpg");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "unknown content");

        Path unknownDir = tempDir.resolve("unknown");
        Files.createDirectories(unknownDir);

        when(config.getUnknownDir()).thenReturn(unknownDir.toString());
        when(dateExtractorService.extractCreationDate(sourceFile)).thenReturn(Optional.empty());
        when(hashService.calculateSHA256(sourceFile)).thenReturn("unknownhash");

        timelineOrganizerService.organizeFile(sourceFile);

        Path expectedPath = unknownDir.resolve("unknownhash.jpg");
        assertTrue(Files.exists(expectedPath));
        assertTrue(Files.isSymbolicLink(expectedPath));
    }
}
