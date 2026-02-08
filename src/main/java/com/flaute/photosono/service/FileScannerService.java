package com.flaute.photosono.service;

import com.flaute.photosono.config.PhotosonoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Service
public class FileScannerService {

    private static final Logger logger = LoggerFactory.getLogger(FileScannerService.class);

    private final PhotosonoConfig config;
    private final FileProcessorService processorService;

    public FileScannerService(PhotosonoConfig config, FileProcessorService processorService) {
        this.config = config;
        this.processorService = processorService;
    }

    @Scheduled(fixedDelayString = "${photosono.deduplication.scan-interval:PT10S}")
    public void scanInputDirectory() {
        if (!config.getDeduplication().isEnabled()) {
            return;
        }

        logger.info("Scanning input directory for deduplication: {}", config.getInputDir());
        Path inputPath = Paths.get(config.getInputDir());

        if (!Files.exists(inputPath)) {
            logger.warn("Input directory does not exist: {}", inputPath);
            return;
        }

        try (Stream<Path> paths = Files.walk(inputPath)) {
            paths.filter(Files::isRegularFile)
                    .forEach(processorService::processFile);
        } catch (IOException e) {
            logger.error("Error scanning directory: {}", inputPath, e);
        }
    }
}
