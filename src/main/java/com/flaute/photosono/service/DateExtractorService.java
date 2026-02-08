package com.flaute.photosono.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;
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
     * Priority:
     * 1. DateTimeOriginal
     * 2. CreateDate
     * 3. ModifyDate
     * 4. SubSecDateTimeOriginal
     * 5. SubSecCreateDate
     * 6. SubSecModifyDate
     * 7. FileModifyDate
     */
    public Optional<Date> extractCreationDate(Path path) {
        File file = path.toFile();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            // 1, 4: DateTimeOriginal
            Optional<Date> originalDate = getDateFromDirectory(metadata, ExifSubIFDDirectory.class,
                    ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (originalDate.isPresent())
                return originalDate;

            // 2, 5: CreateDate (Digitized)
            Optional<Date> createDate = getDateFromDirectory(metadata, ExifSubIFDDirectory.class,
                    ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED);
            if (createDate.isPresent())
                return createDate;

            // 3, 6: ModifyDate
            Optional<Date> modifyDate = getDateFromDirectory(metadata, ExifIFD0Directory.class,
                    ExifIFD0Directory.TAG_DATETIME);
            if (modifyDate.isPresent())
                return modifyDate;

            // 7: FileModifyDate (Filesystem)
            Optional<Date> fileModifyDate = getDateFromDirectory(metadata, FileSystemDirectory.class,
                    FileSystemDirectory.TAG_FILE_MODIFIED_DATE);
            if (fileModifyDate.isPresent())
                return fileModifyDate;

        } catch (Exception e) {
            logger.warn("Could not extract metadata from {}: {}", path, e.getMessage());
        }

        // Fallback to basic file attribute if metadata reader failed or found nothing
        try {
            return Optional.of(new Date(file.lastModified()));
        } catch (Exception e) {
            return Optional.empty();
        }
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
