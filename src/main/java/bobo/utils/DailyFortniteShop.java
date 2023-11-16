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

public class DailyFortniteShop {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final String selectSQL = "SELECT channel_id FROM fortnite_shop_channels";

    /**
     * Starts the daily task.
     */
    public void startDailyTask(Runnable task) {
        long initialDelay = computeInitialDelay();
        long period = TimeUnit.DAYS.toMillis(1);

        scheduler.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    /**
     * Computes the initial delay for the daily task.
     *
     * @return the initial delay
     */
    private long computeInitialDelay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(19).withMinute(1).withSecond(0).withNano(0);

        if (!now.isBefore(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }

        return Duration.between(now, nextRun).toMillis();
    }

    /**
     * Sends the shop image to all registered Discord channel.
     */
    public void sendShopImageToDiscord() {
        TextChannel channel;
        try (Connection connection = SQLConnection.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectSQL);

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
                    channel.sendFiles(upload).queue();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
