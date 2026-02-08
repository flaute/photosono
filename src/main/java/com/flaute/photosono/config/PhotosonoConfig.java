package com.flaute.photosono.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "photosono")
public class PhotosonoConfig {

    private String inputDir;
    private String outputDir;
    private String scanInterval = "PT10S"; // Default 10 seconds

    public String getInputDir() {
        return inputDir;
    }

    public void setInputDir(String inputDir) {
        this.inputDir = inputDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getScanInterval() {
        return scanInterval;
    }

    public void setScanInterval(String scanInterval) {
        this.scanInterval = scanInterval;
    }
}
