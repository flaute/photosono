package com.flaute.photosono.service;

import com.flaute.photosono.config.PhotosonoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
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

        AtomicInteger total = new AtomicInteger(0);
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger unknownType = new AtomicInteger(0);
        AtomicInteger corrupted = new AtomicInteger(0);
        AtomicInteger invalidSize = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        try (Stream<Path> paths = Files.walk(inputPath)) {
            paths.filter(Files::isRegularFile)
                    .forEach(file -> {
                        total.incrementAndGet();
                        FileProcessorService.Result result = processorService.processFile(file);
                        switch (result) {
                            case PROCESSED -> processed.incrementAndGet();
                            case SKIPPED -> skipped.incrementAndGet();
                            case UNKNOWN_TYPE -> unknownType.incrementAndGet();
                            case CORRUPTED -> corrupted.incrementAndGet();
                            case INVALID_SIZE -> invalidSize.incrementAndGet();
                            case ERROR -> errors.incrementAndGet();
                        }
                    });
        } catch (IOException e) {
            logger.error("Error scanning directory: {}", inputPath, e);
        }

        logger.info("--- Deduplication Summary ---");
        logger.info("Total files found:   {}", total.get());
        logger.info("Unique files copied: {}", processed.get());
        logger.info("Duplicates skipped:  {}", skipped.get());
        logger.info("Unknown type:        {}", unknownType.get());
        logger.info("Corrupted files:      {}", corrupted.get());
        logger.info("Invalid size:        {}", invalidSize.get());
        if (errors.get() > 0) {
            logger.error("Errors encountered:  {}", errors.get());
        }
        logger.info("-----------------------------");
    }
}
