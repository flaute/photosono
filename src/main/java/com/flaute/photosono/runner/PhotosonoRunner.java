package com.flaute.photosono.runner;

import com.flaute.photosono.config.PhotosonoConfig;
import com.flaute.photosono.service.FileScannerService;
import com.flaute.photosono.service.TimelineScannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class PhotosonoRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PhotosonoRunner.class);

    private final FileScannerService fileScannerService;
    private final TimelineScannerService timelineScannerService;
    private final ApplicationContext context;
    private final PhotosonoConfig config;

    public PhotosonoRunner(FileScannerService fileScannerService, TimelineScannerService timelineScannerService,
            ApplicationContext context, PhotosonoConfig config) {
        this.fileScannerService = fileScannerService;
        this.timelineScannerService = timelineScannerService;
        this.context = context;
        this.config = config;
    }

    @Override
    public void run(String... args) {
        try {
            boolean runDedupe = false;
            boolean runTimeline = false;

            if (args.length == 0) {
                runDedupe = config.getDeduplication().isEnabled();
                runTimeline = config.getTimeline().isEnabled();
                logger.info("No arguments provided. Using configuration: dedupe={}, timeline={}", runDedupe,
                        runTimeline);
            } else {
                for (String arg : args) {
                    if ("dedupe".equalsIgnoreCase(arg)) {
                        runDedupe = true;
                    } else if ("timeline".equalsIgnoreCase(arg)) {
                        runTimeline = true;
                    }
                }
                logger.info("Arguments provided. Executing specified phases: dedupe={}, timeline={}", runDedupe,
                        runTimeline);
            }

            if (runDedupe) {
                logger.info("Executing Phase 1: Deduplication (Input -> Originals)");
                fileScannerService.scanInputDirectory();
            }

            if (runTimeline) {
                logger.info("Executing Phase 2: Timeline Organization (Originals -> Timeline)");
                timelineScannerService.scanOriginalsDirectory();
            }

            logger.info("Processing complete. Application will now exit.");

        } catch (Exception e) {
            logger.error("Error during execution", e);
        } finally {
            SpringApplication.exit(context, () -> 0);
        }
    }
}
