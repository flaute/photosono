package com.flaute.photosono.runner;

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

    public PhotosonoRunner(FileScannerService fileScannerService, TimelineScannerService timelineScannerService,
            ApplicationContext context) {
        this.fileScannerService = fileScannerService;
        this.timelineScannerService = timelineScannerService;
        this.context = context;
    }

    @Override
    public void run(String... args) {
        try {
            logger.info("Starting sequential execution...");

            logger.info("Executing Phase 1: Deduplication (Input -> Output)");
            fileScannerService.scanInputDirectory();

            logger.info("Executing Phase 2: Timeline Organization (Output -> Timeline)");
            timelineScannerService.scanOutputDirectory();

            logger.info("Processing complete. Application will now exit.");

        } catch (Exception e) {
            logger.error("Error during sequential execution", e);
        } finally {
            SpringApplication.exit(context, () -> 0);
        }
    }
}
