package com.flaute.photosono.service;

import com.flaute.photosono.config.PhotosonoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@Service
public class TimelineOrganizerService {

    private static final Logger logger = LoggerFactory.getLogger(TimelineOrganizerService.class);

    private final PhotosonoConfig config;
    private final DateExtractorService dateExtractorService;
    private final HashService hashService;

    public TimelineOrganizerService(PhotosonoConfig config, DateExtractorService dateExtractorService,
            HashService hashService) {
        this.config = config;
        this.dateExtractorService = dateExtractorService;
        this.hashService = hashService;
    }

    public void organizeFile(Path file) {
        dateExtractorService.extractCreationDate(file).ifPresentOrElse(
                date -> copyToTimeline(file, date),
                () -> copyToUnknown(file));
    }

    private void copyToTimeline(Path source, Date date) {
        try {
            SimpleDateFormat dirFormatter = new SimpleDateFormat("yyyy/MM/dd");
            SimpleDateFormat fileFormatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

            String datePath = dirFormatter.format(date);
            String baseFileName = fileFormatter.format(date);
            String extension = getExtension(source);

            Path targetDir = Paths.get(config.getTimelineDir(), datePath);
            Files.createDirectories(targetDir);

            Optional<Path> targetFile = resolveTargetFile(source, targetDir, baseFileName, extension);

            if (targetFile.isPresent()) {
                Files.copy(source, targetFile.get());
                logger.info("Organized {} into timeline: {}", source, targetFile.get());
            } else {
                logger.debug("Identical file already exists in timeline for {}, skipping.", baseFileName);
            }

        } catch (Exception e) {
            logger.error("Error organizing file to timeline: {}", source, e);
        }
    }

    private void copyToUnknown(Path source) {
        try {
            String sha256 = hashService.calculateSHA256(source);
            String extension = getExtension(source);
            String targetFileName = sha256 + "." + (extension.isEmpty() ? "" : extension.toLowerCase());

            Path unknownDir = Paths.get(config.getUnknownDir());
            Files.createDirectories(unknownDir);

            Path targetFile = unknownDir.resolve(targetFileName);

            if (Files.exists(targetFile)) {
                logger.debug("File already exists in unknown folder: {}", targetFile);
                return;
            }

            Files.copy(source, targetFile);
            logger.info("No date found, moved {} to unknown: {}", source, targetFile);

        } catch (Exception e) {
            logger.error("Error copying file to unknown folder: {}", source, e);
        }
    }

    private Optional<Path> resolveTargetFile(Path source, Path targetDir, String baseName, String extension)
            throws IOException, NoSuchAlgorithmException {
        Path target = targetDir.resolve(baseName + "." + extension);
        if (!Files.exists(target)) {
            return Optional.of(target);
        }

        // Check if existing file is identical
        String sourceHash = hashService.calculateSHA256(source);
        if (sourceHash.equals(hashService.calculateSHA256(target))) {
            return Optional.empty(); // Identical file, skip
        }

        int counter = 1;
        while (true) {
            target = targetDir.resolve(baseName + "-" + counter + "." + extension);
            if (!Files.exists(target)) {
                return Optional.of(target);
            }
            if (sourceHash.equals(hashService.calculateSHA256(target))) {
                return Optional.empty(); // Identical file with counter, skip
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
