package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.Location;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class CommentCsvLogger implements AutoCloseable {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
    private static final String HEADER = "timestamp_utc,player_name,player_uuid,comment,world,x,y,z";

    private final Path csvPath;
    private final Logger logger;
    private final ExecutorService executor;

    public CommentCsvLogger(Path csvPath, Logger logger) {
        this.csvPath = csvPath;
        this.logger = logger;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "play-consultant-comment-csv-writer");
            thread.setDaemon(true);
            return thread;
        });

        initializeFile();
    }

    public void logComment(UUID playerId, String playerName, String comment, Location loc) {
        String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());
        String worldName = loc != null && loc.getWorld() != null ? loc.getWorld().getName() : "";
        String x = loc != null ? Integer.toString(loc.getBlockX()) : "";
        String y = loc != null ? Integer.toString(loc.getBlockY()) : "";
        String z = loc != null ? Integer.toString(loc.getBlockZ()) : "";
        String line = csvRecord(timestamp, playerName, playerId.toString(), comment, worldName, x, y, z);

        executor.execute(() -> {
            try {
                Files.writeString(
                        csvPath,
                        line + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            } catch (IOException exception) {
                logger.warning("Failed to write comment log entry: " + exception.getMessage());
            }
        });
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void initializeFile() {
        try {
            Path parent = csvPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (Files.notExists(csvPath) || Files.size(csvPath) == 0L) {
                Files.writeString(
                        csvPath,
                        HEADER + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                );
            }
        } catch (IOException exception) {
            logger.warning("Failed to initialize comment CSV log at " + csvPath + ": " + exception.getMessage());
        }
    }

    private String csvRecord(String... fields) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(csvEscape(fields[i] == null ? "" : fields[i]));
        }
        return builder.toString();
    }

    private String csvEscape(String value) {
        boolean needsQuoting = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;

        String escaped = value.replace("\"", "\"\"");
        if (needsQuoting) {
            return '"' + escaped + '"';
        }
        return escaped;
    }
}

