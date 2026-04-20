# Photosono

A photo and video deduplication and timeline organization tool built with Java 21 and Spring Boot 4.

Photosono processes a collection of photos and videos in two phases: it first deduplicates files using SHA-256 hashing, then organizes unique originals into a date-based timeline directory structure using metadata extracted from EXIF and container formats.

## Features

- **SHA-256 deduplication** -- Files are hashed and stored by their hash, ensuring only unique files are kept regardless of how many copies exist across the input directory tree.
- **File extension normalization** -- Extensions like `.JPG`, `.JPEG`, `.jpeg` are all normalized to `.jpg` in the originals directory.
- **Metadata-based date extraction** -- Creation dates are read from EXIF data (DateTimeOriginal, CreateDate, ModifyDate) as well as MP4, QuickTime/MOV, and AVI container metadata.
- **Timeline organization** -- Unique originals are symlinked into a `YYYY/MM/DD` directory structure with filenames following the pattern `YYYYMMDD-HHmmss.<ext>`. Duplicate timestamps for different files are resolved with an incrementing counter.
- **Corrupted file detection** -- Files that cannot be parsed by the metadata reader are separated into a dedicated corrupted directory.
- **Minimum dimension validation** -- Images and videos below configurable minimum width/height thresholds are moved to an invalid-size directory.
- **Unknown type handling** -- Files with unsupported extensions are stored separately rather than silently ignored.
- **Unknown date handling** -- Files where no creation date can be extracted from metadata are symlinked to a separate unknown-date directory.
- **Configurable execution modes** -- Run deduplication only, timeline only, or both via command-line arguments or configuration.

## Supported Formats

**Photos:** JPG/JPEG, PNG, GIF, BMP, WebP, HEIC, HEIF

**Videos:** MP4, MOV, AVI

## How It Works

### Phase 1: Deduplication (Input -> Originals)

1. Recursively scans the configured input directory for all files.
2. Computes the SHA-256 hash of each file.
3. Checks if the file extension is a supported media type (unsupported types go to `unknown-type`).
4. Validates that the file is not corrupted (corrupted files go to `corrupted`).
5. Validates minimum dimensions (too-small files go to `invalid-size`).
6. Copies the file to the originals directory, named `<sha256>.<normalized-ext>`, in a subdirectory structure based on the first two hash characters (`<h0>/<h1>/`).
7. If a file with the same hash already exists, the copy is skipped (deduplication).

### Phase 2: Timeline Organization (Originals -> Timeline)

1. Recursively scans the originals directory.
2. Extracts the creation date from file metadata using this priority order:
   - EXIF DateTimeOriginal
   - EXIF DateTimeDigitized (CreateDate)
   - EXIF DateTime (ModifyDate)
   - MP4 creation time
   - QuickTime/MOV creation time
   - AVI DateTimeOriginal
3. Creates a symbolic link in the timeline directory under `YYYY/MM/DD/YYYYMMDD-HHmmss.<ext>`.
4. If a file with the same timestamp already exists, the SHA-256 hashes are compared. Identical files are skipped; different files get an incremented counter suffix (`YYYYMMDD-HHmmss-1.<ext>`).
5. Files without extractable dates are symlinked into the `unknown-date` directory.

## Directory Structure

| Directory      | Purpose                                                                   |
| -------------- | ------------------------------------------------------------------------- |
| `input`        | Source directory scanned for new media files                              |
| `originals`    | Deduplicated files stored by SHA-256 hash (`<h0>/<h1>/<hash>.<ext>`)      |
| `timeline`     | Date-organized symlinks to originals (`YYYY/MM/DD/YYYYMMDD-HHmmss.<ext>`) |
| `unknown-date` | Symlinks to originals where no creation date could be extracted           |
| `unknown-type` | Files with unsupported extensions                                         |
| `corrupted`    | Files that failed metadata validation                                     |
| `invalid-size` | Files below the minimum width/height threshold                            |

## Configuration

All settings are configurable via environment variables or `application.properties`.

### Environment Variables

| Variable                          | Default          | Description                                   |
| --------------------------------- | ---------------- | --------------------------------------------- |
| `PHOTOSONO_INPUT_DIR`             | `./input`        | Input directory to scan                       |
| `PHOTOSONO_ORIGINALS_DIR`         | `./originals`    | Directory for deduplicated original files     |
| `PHOTOSONO_TIMELINE_DIR`          | `./timeline`     | Directory for date-organized symlinks         |
| `PHOTOSONO_UNKNOWN_DATE_DIR`      | `./unknown-date` | Directory for files without extractable dates |
| `PHOTOSONO_UNKNOWN_TYPE_DIR`      | `./unknown-type` | Directory for unsupported file types          |
| `PHOTOSONO_CORRUPTED_DIR`         | `./corrupted`    | Directory for corrupted files                 |
| `PHOTOSONO_INVALID_SIZE_DIR`      | `./invalid-size` | Directory for files below minimum dimensions  |
| `PHOTOSONO_DEDUPLICATION_ENABLED` | `true`           | Enable/disable the deduplication phase        |
| `PHOTOSONO_TIMELINE_ENABLED`      | `true`           | Enable/disable the timeline phase             |
| `PHOTOSONO_MIN_WIDTH`             | `100`            | Minimum image/video width in pixels           |
| `PHOTOSONO_MIN_HEIGHT`            | `100`            | Minimum image/video height in pixels          |

## Usage

### Docker Compose (recommended)

Create a `docker-compose.yaml`:

```yaml
services:
  photosono:
    build: .
    restart: "no"
    volumes:
      - /path/to/your/photos:/input
      - /path/to/originals:/originals
      - /path/to/timeline:/timeline
      - /path/to/unknown-date:/unknown-date
      - /path/to/unknown-type:/unknown-type
      - /path/to/corrupted:/corrupted
      - /path/to/invalid-size:/invalid-size
    environment:
      - PHOTOSONO_INPUT_DIR=/input
      - PHOTOSONO_ORIGINALS_DIR=/originals
      - PHOTOSONO_TIMELINE_DIR=/timeline
      - PHOTOSONO_UNKNOWN_DATE_DIR=/unknown-date
      - PHOTOSONO_UNKNOWN_TYPE_DIR=/unknown-type
      - PHOTOSONO_CORRUPTED_DIR=/corrupted
      - PHOTOSONO_INVALID_SIZE_DIR=/invalid-size
      - PHOTOSONO_MIN_WIDTH=100
      - PHOTOSONO_MIN_HEIGHT=100
```

```bash
docker compose up --build
```

### Docker

```bash
docker build -t photosono .

docker run --rm \
  -v /path/to/your/photos:/input \
  -v /path/to/originals:/originals \
  -v /path/to/timeline:/timeline \
  -v /path/to/unknown-date:/unknown-date \
  -v /path/to/unknown-type:/unknown-type \
  -v /path/to/corrupted:/corrupted \
  -v /path/to/invalid-size:/invalid-size \
  photosono
```

### Command-Line Arguments

You can selectively run individual phases by passing arguments:

```bash
# Run only deduplication
docker run --rm -v ... photosono dedupe

# Run only timeline organization
docker run --rm -v ... photosono timeline

# Run both (also the default when no arguments are given)
docker run --rm -v ... photosono dedupe timeline
```

### Local Development

Requires Java 21 and Maven.

```bash
mvn clean package
java -jar target/photosono-0.0.1-SNAPSHOT.jar
```

## Tech Stack

- Java 21
- Spring Boot 4.0
- Maven
- [metadata-extractor](https://github.com/drewnoakes/metadata-extractor) for EXIF and media metadata reading
- Docker (multi-stage build with Eclipse Temurin)

## License

See the repository for license information.
