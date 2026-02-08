package com.flaute.photosono.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "photosono")
public class PhotosonoConfig {

    private String inputDir;
    private String outputDir;
    private String timelineDir;
    private Deduplication deduplication = new Deduplication();
    private Timeline timeline = new Timeline();

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

    public String getTimelineDir() {
        return timelineDir;
    }

    public void setTimelineDir(String timelineDir) {
        this.timelineDir = timelineDir;
    }

    public Deduplication getDeduplication() {
        return deduplication;
    }

    public void setDeduplication(Deduplication deduplication) {
        this.deduplication = deduplication;
    }

    public Timeline getTimeline() {
        return timeline;
    }

    public void setTimeline(Timeline timeline) {
        this.timeline = timeline;
    }

    public static class Deduplication {
        private boolean enabled = true;
        private String scanInterval = "PT10S";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getScanInterval() {
            return scanInterval;
        }

        public void setScanInterval(String scanInterval) {
            this.scanInterval = scanInterval;
        }
    }

    public static class Timeline {
        private boolean enabled = true;
        private String scanInterval = "PT10S";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getScanInterval() {
            return scanInterval;
        }

        public void setScanInterval(String scanInterval) {
            this.scanInterval = scanInterval;
        }
    }
}
