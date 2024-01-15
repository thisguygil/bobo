package bobo.utils;

import bobo.Bobo;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        TextChannel channel;
        try (Connection connection = SQLConnection.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectFortniteChannelsSQL);

            File file = convertBufferedImageToFile(FortniteAPI.getShopImage(), "shop");
            FileUpload upload;
            if (file != null) {
                upload = FileUpload.fromData(file);
            } else {
                return;
            }

            while (resultSet.next()) {
                channel = Bobo.getJDA().getTextChannelById(resultSet.getString("channel_id"));
                if (channel != null) {
                    channel.sendFiles(upload).queue(success -> {
                        if (!file.delete()) {
                            System.err.println("Failed to delete file: " + file.getAbsolutePath());
                        }
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}