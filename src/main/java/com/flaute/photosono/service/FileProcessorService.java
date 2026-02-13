package com.flaute.photosono.service;

import com.flaute.photosono.config.PhotosonoConfig;
import com.drew.imaging.ImageMetadataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

@Service
public class FileProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessorService.class);

    private final PhotosonoConfig config;
    private final HashService hashService;

    public enum Result {
        PROCESSED,
        SKIPPED,
        UNKNOWN_TYPE,
        ERROR,
        CORRUPTED
    }

    private static final Map<String, String> EXTENSION_NORMALIZATION = Map.of(
            "jpeg", "jpg",
            "jpg", "jpg");

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", // Photos
            "mp4", "mov", "avi" // Videos
    );

    public FileProcessorService(PhotosonoConfig config, HashService hashService) {
        this.config = config;
        this.hashService = hashService;
    }

    public Result processFile(Path file) {
        logger.info("Processing file for deduplication: {}", file);
        try {
            String extension = getExtension(file).toLowerCase();
            String normalizedExtension = EXTENSION_NORMALIZATION.getOrDefault(extension, extension);
            String sha256 = hashService.calculateSHA256(file);

            if (!SUPPORTED_EXTENSIONS.contains(extension)) {
                return copyToUnknownType(file, sha256, extension);
            }

            if (!isValidMedia(file)) {
                return moveToCorrupted(file, sha256, extension);
            }

            Path originalsDir = Paths.get(config.getOriginalsDir(), sha256.substring(0, 1), sha256.substring(1, 2));
            Files.createDirectories(originalsDir);

            Path targetFile = originalsDir.resolve(sha256 + "." + normalizedExtension);

            if (Files.exists(targetFile)) {
                logger.info("File already exists in originals, skipping: {}", targetFile);
                return Result.SKIPPED;
            }

            Files.copy(file, targetFile);
            logger.info("Copied {} to {}", file, targetFile);
            return Result.PROCESSED;

        } catch (Exception e) {
            logger.error("Error processing file: {}", file, e);
            return Result.ERROR;
        }
    }

    private boolean isValidMedia(Path path) {
        try {
            // For images and videos, metadata-extractor will throw an exception if the file
            // structure is invalid
            ImageMetadataReader.readMetadata(path.toFile());
            return true;
        } catch (Exception e) {
            logger.warn("File validation failed for {}: {}", path, e.getMessage());
            return false;
        }
    }

    private Result moveToCorrupted(Path source, String sha256, String extension) throws Exception {
        Path corruptedBaseDir = Paths.get(config.getCorruptedDir(), sha256.substring(0, 1), sha256.substring(1, 2));
        Files.createDirectories(corruptedBaseDir);

        String fileName = sha256 + (extension.isEmpty() ? "" : "." + extension);
        Path targetFile = corruptedBaseDir.resolve(fileName);

        if (Files.exists(targetFile)) {
            logger.info("Corrupted file already exists, skipping: {}", targetFile);
            return Result.SKIPPED;
        }

        Files.copy(source, targetFile);
        logger.warn("Moved corrupted file {} to {}", source, targetFile);
        return Result.CORRUPTED;
    }

    private Result copyToUnknownType(Path source, String sha256, String extension) throws Exception {
        Path unknownTypeBaseDir = Paths.get(config.getUnknownTypeDir(), sha256.substring(0, 1), sha256.substring(1, 2));
        Files.createDirectories(unknownTypeBaseDir);

        String fileName = sha256 + (extension.isEmpty() ? "" : "." + extension);
        Path targetFile = unknownTypeBaseDir.resolve(fileName);

        if (Files.exists(targetFile)) {
            logger.info("Unknown type file already exists, skipping: {}", targetFile);
            return Result.SKIPPED;
        }

        Files.copy(source, targetFile);
        logger.info("Copied unknown type file {} to {}", source, targetFile);
        return Result.UNKNOWN_TYPE;
    }

    private String getExtension(Path file) {
        String fileName = file.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1);
    }
}
