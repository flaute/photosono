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
public class TimelineScannerService {

    private static final Logger logger = LoggerFactory.getLogger(TimelineScannerService.class);

    private final PhotosonoConfig config;
    private final TimelineOrganizerService organizerService;

    public TimelineScannerService(PhotosonoConfig config, TimelineOrganizerService organizerService) {
        this.config = config;
        this.organizerService = organizerService;
    }

    public void scanOutputDirectory() {
        if (!config.getTimeline().isEnabled()) {
            return;
        }

        logger.info("Scanning unique output directory for timeline organization: {}", config.getOutputDir());
        Path outputPath = Paths.get(config.getOutputDir());

        if (!Files.exists(outputPath)) {
            logger.warn("Output directory does not exist: {}", outputPath);
            return;
        }

        AtomicInteger total = new AtomicInteger(0);
        AtomicInteger timeline = new AtomicInteger(0);
        AtomicInteger unknown = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        try (Stream<Path> paths = Files.walk(outputPath)) {
            paths.filter(Files::isRegularFile)
                    .forEach(file -> {
                        total.incrementAndGet();
                        TimelineOrganizerService.Result result = organizerService.organizeFile(file);
                        switch (result) {
                            case TIMELINE -> timeline.incrementAndGet();
                            case UNKNOWN -> unknown.incrementAndGet();
                            case SKIPPED -> skipped.incrementAndGet();
                            case ERROR -> errors.incrementAndGet();
                        }
                    });
        } catch (IOException e) {
            logger.error("Error scanning output directory: {}", outputPath, e);
        }

        logger.info("--- Timeline Summary ---");
        logger.info("Total files found:     {}", total.get());
        logger.info("Links to Timeline:     {}", timeline.get());
        logger.info("Links to Unknown:      {}", unknown.get());
        logger.info("Existing links skipped: {}", skipped.get());
        if (errors.get() > 0) {
            logger.error("Errors encountered:    {}", errors.get());
        }
        logger.info("------------------------");
    }
}
