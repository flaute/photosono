package com.flaute.photosono.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DateExtractorServiceTest {

    private final DateExtractorService dateExtractorService = new DateExtractorService();

    @Test
    void testExtractCreationDateFallback(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("test.jpg");
        Files.writeString(testFile, "mock content");

        Optional<Date> date = dateExtractorService.extractCreationDate(testFile);

        assertTrue(date.isPresent());
        // Should fallback to file modification time when no EXIF is present
        assertTrue(date.get().getTime() > 0);
    }
}
