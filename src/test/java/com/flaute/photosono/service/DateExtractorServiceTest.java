package com.flaute.photosono.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DateExtractorServiceTest {

    private final DateExtractorService dateExtractorService = new DateExtractorService();

    @Test
    void testExtractCreationDateNoMetadata(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("test.jpg");
        Files.writeString(testFile, "mock content without metadata");

        Optional<Date> date = dateExtractorService.extractCreationDate(testFile);

        // Now that filesystem fallback is removed, this should be empty
        assertFalse(date.isPresent());
    }
}
