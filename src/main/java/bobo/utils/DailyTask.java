package bobo.utils;

import bobo.Bobo;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static bobo.commands.general.FortniteCommand.convertBufferedImageToFile;

public class DailyTask {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final String selectFortniteChannelsSQL = "SELECT channel_id FROM fortnite_shop_channels";

    /**
     * Starts the daily task.
     */
    public void startDailyTask(Runnable task, LocalDateTime nextRun, long period) {
        long initialDelay = computeInitialDelay(nextRun);
        scheduler.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    /**
     * Computes the initial delay for the daily task.
     *
     * @return the initial delay
     */
    private long computeInitialDelay(LocalDateTime nextRun) {
        LocalDateTime now = LocalDateTime.now();

        if (!now.isBefore(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }

        return Duration.between(now, nextRun).toMillis();
    }

    /**
     * Sends the Fortnite shop image to all registered Discord channels.
     */
    public void sendFortniteShopImage() {
        ZonedDateTime nowInUTC = ZonedDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        final String message = "# Fortnite Shop" + "\n" + "## " + nowInUTC.format(formatter);

        try (Connection connection = SQLConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(selectFortniteChannelsSQL)) {

            List<BufferedImage> images = FortniteAPI.getShopImages();
            if (images == null) {
                return;
            }

            List<File> files = images.stream()
                    .map(image -> convertBufferedImageToFile(image, "shop"))
                    .filter(Objects::nonNull)
                    .toList();

            if (files.isEmpty()) {
                return;
            }

            List<FileUpload> fileUploads = files.stream()
                    .map(FileUpload::fromData)
                    .collect(Collectors.toList());

            while (resultSet.next()) {
                GuildMessageChannel messageChannel = (GuildMessageChannel) Bobo.getJDA().getGuildChannelById(resultSet.getString("channel_id"));
                if (messageChannel != null) {
                    messageChannel.sendMessage(message)
                            .setFiles(fileUploads)
                            .queue(success -> files.forEach(file -> {
                                if (!file.delete()) {
                                    System.err.println("Failed to delete file: " + file.getAbsolutePath());
                                }
                            }));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}