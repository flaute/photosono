package com.flaute.photosono.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.drew.metadata.mp4.Mp4Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;

@Service
public class DateExtractorService {

    private static final Logger logger = LoggerFactory.getLogger(DateExtractorService.class);

    /**
     * Extracts creation date from image/video metadata.
     * Only returns dates found within the file metadata (EXIF, etc.).
     * No filesystem fallback is performed here.
     */
    public Optional<Date> extractCreationDate(Path path) {
        File file = path.toFile();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            // 1: DateTimeOriginal (Highest priority)
            Optional<Date> originalDate = getDateFromDirectory(metadata, ExifSubIFDDirectory.class,
                    ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (originalDate.isPresent())
                return originalDate;

            // 2: CreateDate (Digitized)
            Optional<Date> createDate = getDateFromDirectory(metadata, ExifSubIFDDirectory.class,
                    ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED);
            if (createDate.isPresent())
                return createDate;

            // 3: ModifyDate (Internal metadata)
            Optional<Date> modifyDate = getDateFromDirectory(metadata, ExifIFD0Directory.class,
                    ExifIFD0Directory.TAG_DATETIME);
            if (modifyDate.isPresent())
                return modifyDate;

            // 4: Video Creation Date (MP4)
            Optional<Date> mp4Date = getDateFromDirectory(metadata, Mp4Directory.class,
                    Mp4Directory.TAG_CREATION_TIME);
            if (mp4Date.isPresent())
                return mp4Date;

            // 5: Video Creation Date (QuickTime/MOV)
            Optional<Date> movDate = getDateFromDirectory(metadata, QuickTimeDirectory.class,
                    QuickTimeDirectory.TAG_CREATION_TIME);
            if (movDate.isPresent())
                return movDate;

        } catch (Exception e) {
            logger.warn("Could not extract metadata from {}: {}", path, e.getMessage());
        }

        return Optional.empty();
    }

    private <T extends Directory> Optional<Date> getDateFromDirectory(Metadata metadata, Class<T> directoryClass,
            int tag) {
        T directory = metadata.getFirstDirectoryOfType(directoryClass);
        if (directory != null && directory.containsTag(tag)) {
            Date date = directory.getDate(tag);
            if (date != null) {
                return Optional.of(date);
            }
        }
        return Optional.empty();
    }
}
