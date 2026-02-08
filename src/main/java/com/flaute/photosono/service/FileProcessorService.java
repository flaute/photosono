package com.flaute.photosono.service;

import com.flaute.photosono.config.PhotosonoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

@Service
public class FileProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessorService.class);

    private final PhotosonoConfig config;
    private final TimelineOrganizerService organizerService;
    private final HashService hashService;

    private static final Map<String, String> EXTENSION_NORMALIZATION = Map.of(
            "jpeg", "jpg",
            "jpg", "jpg");

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", // Photos
            "mp4", "mov", "avi", "mkv", "wmv", "flv", "webm" // Videos
    );

    public FileProcessorService(PhotosonoConfig config, TimelineOrganizerService organizerService,
            HashService hashService) {
        this.config = config;
        this.organizerService = organizerService;
        this.hashService = hashService;
    }

    public void processFile(Path file) {
        try {
            String extension = getExtension(file).toLowerCase();
            if (!SUPPORTED_EXTENSIONS.contains(extension)) {
                logger.debug("Skipping unsupported file type: {}", file);
                return;
            }

            String normalizedExtension = EXTENSION_NORMALIZATION.getOrDefault(extension, extension);
            String sha256 = hashService.calculateSHA256(file);

            Path outputDir = Paths.get(config.getOutputDir(), sha256.substring(0, 2));
            Files.createDirectories(outputDir);

            Path targetFile = outputDir.resolve(sha256 + "." + normalizedExtension);

            if (Files.exists(targetFile)) {
                logger.debug("File already exists in output, skipping: {}", targetFile);
                organizerService.organizeFile(targetFile); // Still try to organize if not already in timeline
                return;
            }

            Files.copy(file, targetFile);
            logger.info("Copied {} to {}", file, targetFile);

            organizerService.organizeFile(targetFile);

        } catch (Exception e) {
            logger.error("Error processing file: {}", file, e);
        }
    }

    private String getExtension(Path file) {
        String fileName = file.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1);
    }
}
