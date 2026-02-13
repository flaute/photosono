package com.flaute.photosono.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.avi.AviDirectory;
import com.drew.metadata.bmp.BmpHeaderDirectory;
import com.drew.metadata.gif.GifHeaderDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.mov.media.QuickTimeVideoDirectory;
import com.drew.metadata.mp4.media.Mp4VideoDirectory;
import com.drew.metadata.png.PngDirectory;
import com.drew.metadata.webp.WebpDirectory;
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
        ERROR,
        CORRUPTED,
        INVALID_SIZE
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

            if (!hasMinimumDimensions(file)) {
                return moveToInvalidSize(file, sha256, extension);
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

    private boolean hasMinimumDimensions(Path path) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());
            Integer width = null;
            Integer height = null;

            // Try various directories to find dimensions
            JpegDirectory jpegDir = metadata.getFirstDirectoryOfType(JpegDirectory.class);
            if (jpegDir != null) {
                width = jpegDir.getInteger(JpegDirectory.TAG_IMAGE_WIDTH);
                height = jpegDir.getInteger(JpegDirectory.TAG_IMAGE_HEIGHT);
            }

            if (width == null) {
                PngDirectory pngDir = metadata.getFirstDirectoryOfType(PngDirectory.class);
                if (pngDir != null) {
                    width = pngDir.getInteger(PngDirectory.TAG_IMAGE_WIDTH);
                    height = pngDir.getInteger(PngDirectory.TAG_IMAGE_HEIGHT);
                }
            }

            if (width == null) {
                WebpDirectory webpDir = metadata.getFirstDirectoryOfType(WebpDirectory.class);
                if (webpDir != null) {
                    width = webpDir.getInteger(WebpDirectory.TAG_IMAGE_WIDTH);
                    height = webpDir.getInteger(WebpDirectory.TAG_IMAGE_HEIGHT);
                }
            }

            if (width == null) {
                GifHeaderDirectory gifDir = metadata.getFirstDirectoryOfType(GifHeaderDirectory.class);
                if (gifDir != null) {
                    width = gifDir.getInteger(GifHeaderDirectory.TAG_IMAGE_WIDTH);
                    height = gifDir.getInteger(GifHeaderDirectory.TAG_IMAGE_HEIGHT);
                }
            }

            if (width == null) {
                BmpHeaderDirectory bmpDir = metadata.getFirstDirectoryOfType(BmpHeaderDirectory.class);
                if (bmpDir != null) {
                    width = bmpDir.getInteger(BmpHeaderDirectory.TAG_IMAGE_WIDTH);
                    height = bmpDir.getInteger(BmpHeaderDirectory.TAG_IMAGE_HEIGHT);
                }
            }

            if (width == null) {
                Mp4VideoDirectory mp4Dir = metadata.getFirstDirectoryOfType(Mp4VideoDirectory.class);
                if (mp4Dir != null) {
                    width = mp4Dir.getInteger(Mp4VideoDirectory.TAG_WIDTH);
                    height = mp4Dir.getInteger(Mp4VideoDirectory.TAG_HEIGHT);
                }
            }

            if (width == null) {
                QuickTimeVideoDirectory qtDir = metadata.getFirstDirectoryOfType(QuickTimeVideoDirectory.class);
                if (qtDir != null) {
                    width = qtDir.getInteger(QuickTimeVideoDirectory.TAG_WIDTH);
                    height = qtDir.getInteger(QuickTimeVideoDirectory.TAG_HEIGHT);
                }
            }

            if (width == null) {
                AviDirectory aviDir = metadata.getFirstDirectoryOfType(AviDirectory.class);
                if (aviDir != null) {
                    width = aviDir.getInteger(AviDirectory.TAG_WIDTH);
                    height = aviDir.getInteger(AviDirectory.TAG_HEIGHT);
                }
            }

            if (width == null || height == null) {
                logger.warn("Could not determine dimensions for {}. Assuming valid size.", path);
                return true;
            }

            boolean isValid = width >= config.getMinWidth() && height >= config.getMinHeight();
            if (!isValid) {
                logger.info("File {} has invalid size: {}x{} (min: {}x{})", path, width, height, config.getMinWidth(),
                        config.getMinHeight());
            }
            return isValid;

        } catch (Exception e) {
            logger.warn("Error checking dimensions for {}: {}. Assuming valid size.", path, e.getMessage());
            return true;
        }
    }

    private Result moveToInvalidSize(Path source, String sha256, String extension) throws Exception {
        Path invalidSizeBaseDir = Paths.get(config.getInvalidSizeDir(), sha256.substring(0, 1), sha256.substring(1, 2));
        Files.createDirectories(invalidSizeBaseDir);

        String fileName = sha256 + (extension.isEmpty() ? "" : "." + extension);
        Path targetFile = invalidSizeBaseDir.resolve(fileName);

        if (Files.exists(targetFile)) {
            logger.info("Invalid size file already exists, skipping: {}", targetFile);
            return Result.SKIPPED;
        }

        Files.copy(source, targetFile);
        logger.info("Moved invalid size file {} to {}", source, targetFile);
        return Result.INVALID_SIZE;
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
