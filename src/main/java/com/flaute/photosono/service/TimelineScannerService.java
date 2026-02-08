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
public class TimelineScannerService {

    private static final Logger logger = LoggerFactory.getLogger(TimelineScannerService.class);

    private final PhotosonoConfig config;
    private final TimelineOrganizerService organizerService;

    public TimelineScannerService(PhotosonoConfig config, TimelineOrganizerService organizerService) {
        this.config = config;
        this.organizerService = organizerService;
    }

    @Scheduled(fixedDelayString = "${photosono.timeline.scan-interval:PT10S}")
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

        try (Stream<Path> paths = Files.walk(outputPath)) {
            paths.filter(Files::isRegularFile)
                    .forEach(organizerService::organizeFile);
        } catch (IOException e) {
            logger.error("Error scanning output directory: {}", outputPath, e);
        }
    }
}
