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

    public enum Result {
        TIMELINE,
        UNKNOWN_DATE,
        SKIPPED,
        ERROR
    }

    public TimelineOrganizerService(PhotosonoConfig config, DateExtractorService dateExtractorService,
            HashService hashService) {
        this.config = config;
        this.dateExtractorService = dateExtractorService;
        this.hashService = hashService;
    }

    public Result organizeFile(Path file) {
        logger.info("Processing file for timeline organization: {}", file);
        return dateExtractorService.extractCreationDate(file)
                .map(date -> linkToTimeline(file, date))
                .orElseGet(() -> linkToUnknownDate(file));
    }

    private Result linkToTimeline(Path source, Date date) {
        try {
            SimpleDateFormat dirFormatter = new SimpleDateFormat("yyyy/MM/dd");
            SimpleDateFormat fileFormatter = new SimpleDateFormat("yyyyMMdd-HHmmss");

            String datePath = dirFormatter.format(date);
            String baseFileName = fileFormatter.format(date);
            String extension = getExtension(source);

            Path targetDir = Paths.get(config.getTimelineDir(), datePath);
            Files.createDirectories(targetDir);

            Optional<Path> targetFile = resolveTargetFile(source, targetDir, baseFileName, extension);

            if (targetFile.isPresent()) {
                createRelativeSymlink(source, targetFile.get());
                logger.info("Linked {} into timeline: {}", source, targetFile.get());
                return Result.TIMELINE;
            } else {
                logger.info("Identical file already exists in timeline for {}, skipping.", baseFileName);
                return Result.SKIPPED;
            }

        } catch (Exception e) {
            logger.error("Error linking file to timeline: {}", source, e);
            return Result.ERROR;
        }
    }

    private Result linkToUnknownDate(Path source) {
        try {
            String sha256 = hashService.calculateSHA256(source);
            String extension = getExtension(source);
            String targetFileName = sha256 + "." + (extension.isEmpty() ? "" : extension.toLowerCase());

            // Use structured directory for unknown files:
            // /unknown-date/{h0}/{h1}/{hash}.ext
            Path unknownDir = Paths.get(config.getUnknownDateDir(), sha256.substring(0, 1), sha256.substring(1, 2));
            Files.createDirectories(unknownDir);

            Path targetFile = unknownDir.resolve(targetFileName);

            if (Files.exists(targetFile)) {
                logger.info("Link already exists in unknown-date folder: {}", targetFile);
                return Result.SKIPPED;
            }

            createRelativeSymlink(source, targetFile);
            logger.info("No date found, linked {} to unknown-date: {}", source, targetFile);
            return Result.UNKNOWN_DATE;

        } catch (Exception e) {
            logger.error("Error linking file to unknown-date folder: {}", source, e);
            return Result.ERROR;
        }
    }

    private void createRelativeSymlink(Path source, Path target) throws IOException {
        // Ensure parent directories exist (redundant but safe)
        Files.createDirectories(target.getParent());
        Path relativeSource = target.getParent().relativize(source);
        Files.createSymbolicLink(target, relativeSource);
    }

    private Optional<Path> resolveTargetFile(Path source, Path targetDir, String baseName, String extension)
            throws IOException, NoSuchAlgorithmException {
        Path target = targetDir.resolve(baseName + "." + extension);
        if (!Files.exists(target)) {
            return Optional.of(target);
        }

        // Check if existing file (or link target) is identical
        String sourceHash = hashService.calculateSHA256(source);
        String targetHash = hashService.calculateSHA256(target);

        if (sourceHash.equals(targetHash)) {
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
