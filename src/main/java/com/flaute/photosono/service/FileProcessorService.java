package com.flaute.photosono.service;

import com.flaute.photosono.config.PhotosonoConfig;
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
        ERROR
    }

    private static final Map<String, String> EXTENSION_NORMALIZATION = Map.of(
            "jpeg", "jpg",
            "jpg", "jpg");

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", // Photos
            "mp4", "mov", "avi", "mkv", "wmv", "flv", "webm" // Videos
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

    private Result copyToUnknownType(Path source, String sha256, String extension) throws Exception {
        Path unknownTypeBaseDir = Paths.get(config.getUnknownTypeDir(), sha256.substring(0, 1), sha256.substring(1, 2));
        Files.createDirectories(unknownTypeBaseDir);

        String baseName = sha256;
        String ext = (extension.isEmpty() ? "" : "." + extension);
        Path targetFile = unknownTypeBaseDir.resolve(baseName + ext);

        int counter = 1;
        while (Files.exists(targetFile)) {
            targetFile = unknownTypeBaseDir.resolve(baseName + "-" + counter + ext);
            counter++;
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
