package com.flaute.photosono.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "photosono")
public class PhotosonoConfig {

    private String inputDir;
    private String originalsDir;
    private String timelineDir;
    private String unknownDateDir;
    private String unknownTypeDir;
    private String corruptedDir;
    private String invalidSizeDir;
    private int minWidth = 100; // Default values
    private int minHeight = 100;
    private Deduplication deduplication = new Deduplication();
    private Timeline timeline = new Timeline();

    public String getInputDir() {
        return inputDir;
    }

    public void setInputDir(String inputDir) {
        this.inputDir = inputDir;
    }

    public String getOriginalsDir() {
        return originalsDir;
    }

    public void setOriginalsDir(String originalsDir) {
        this.originalsDir = originalsDir;
    }

    public String getTimelineDir() {
        return timelineDir;
    }

    public void setTimelineDir(String timelineDir) {
        this.timelineDir = timelineDir;
    }

    public String getUnknownDateDir() {
        return unknownDateDir;
    }

    public void setUnknownDateDir(String unknownDateDir) {
        this.unknownDateDir = unknownDateDir;
    }

    public String getUnknownTypeDir() {
        return unknownTypeDir;
    }

    public void setUnknownTypeDir(String unknownTypeDir) {
        this.unknownTypeDir = unknownTypeDir;
    }

    public String getCorruptedDir() {
        return corruptedDir;
    }

    public void setCorruptedDir(String corruptedDir) {
        this.corruptedDir = corruptedDir;
    }

    public String getInvalidSizeDir() {
        return invalidSizeDir;
    }

    public void setInvalidSizeDir(String invalidSizeDir) {
        this.invalidSizeDir = invalidSizeDir;
    }

    public int getMinWidth() {
        return minWidth;
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Timeline {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
