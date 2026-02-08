package com.flaute.photosono.service;

import com.flaute.photosono.config.PhotosonoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class TimelineOrganizerService {

    private static final Logger logger = LoggerFactory.getLogger(TimelineOrganizerService.class);

    private final PhotosonoConfig config;
    private final DateExtractorService dateExtractorService;

    public TimelineOrganizerService(PhotosonoConfig config, DateExtractorService dateExtractorService) {
        this.config = config;
        this.dateExtractorService = dateExtractorService;
    }

    public void organizeFile(Path file) {
        dateExtractorService.extractCreationDate(file).ifPresentOrElse(
                date -> copyToTimeline(file, date),
                () -> logger.warn("No creation date found for file: {}", file));
    }

    private void copyToTimeline(Path source, Date date) {
        try {
            SimpleDateFormat dirFormatter = new SimpleDateFormat("yyyy/MM/dd");
            SimpleDateFormat fileFormatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

            String datePath = dirFormatter.format(date);
            String baseFileName = fileFormatter.format(date);
            String extension = getExtension(source);

            Path targetDir = Paths.get(config.getFinalOutputDir(), datePath);
            Files.createDirectories(targetDir);

            Path targetFile = resolveTargetFile(targetDir, baseFileName, extension);

            Files.copy(source, targetFile);
            logger.info("Organized {} into timeline: {}", source, targetFile);

        } catch (IOException e) {
            logger.error("Error organizing file to timeline: {}", source, e);
        }
    }

    private Path resolveTargetFile(Path targetDir, String baseName, String extension) {
        Path target = targetDir.resolve(baseName + "." + extension);
        if (!Files.exists(target)) {
            return target;
        }

        int counter = 1;
        while (true) {
            target = targetDir.resolve(baseName + "-" + counter + "." + extension);
            if (!Files.exists(target)) {
                return target;
            }
            counter++;
        }
    }

    private String getExtension(Path file) {
        String fileName = file.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1);
    }
}
