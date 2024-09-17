package bobo;

import bobo.utils.DailyTask;
import bobo.utils.SQLConnection;
import com.github.ygimenez.model.PaginatorBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Bobo {
    private static JDA jda;

    private static final String insertVoiceChannelsShutdownTableSQL = "INSERT INTO voice_channels_shutdown (channel_id) VALUES (?)";
    private static final String resetVoiceChannelsShutdownTableSQL = "DELETE FROM voice_channels_shutdown";

    public static void main(String[] args) {
        jda = JDABuilder.createDefault(Config.get("TOKEN"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .enableCache(CacheFlag.VOICE_STATE)
                .build();

        jda.addEventListener(new Listener());

        createPaginator();
        startDailyTasks();
        setShutdownHook();
    }

    /**
     * Creates the paginator.
     */
    public static void createPaginator() {
        try {
            PaginatorBuilder.createPaginator(jda)
                    .shouldRemoveOnReact(false)
                    .shouldEventLock(false)
                    .setDeleteOnCancel(true)
                    .activate();
        } catch (Exception e) {
            System.err.println("Failed to create paginator.");
        }
    }

    /**
     * Starts daily tasks. THIS DEPENDS ON THE TIME ZONE OF THE BOT. HERE IT IS UTC.
     */
    public static void startDailyTasks() {
        // Get the current time in UTC
        ZonedDateTime nowUTC = ZonedDateTime.now(ZoneId.of("UTC"));

        // Set the Fortnite shop reset time at 00:01 UTC
        ZonedDateTime zonedFortniteShopResetTime = nowUTC.withHour(0).withMinute(1).withSecond(0).withNano(0);

        // Ensure the Fortnite shop reset time is set to the next occurrence if the current time is past today's reset time
        if (nowUTC.isAfter(zonedFortniteShopResetTime)) {
            zonedFortniteShopResetTime = zonedFortniteShopResetTime.plusDays(1);
        }

        // Set the bot restart time at 08:00 UTC
        ZonedDateTime zonedBotRestartTime = nowUTC.withHour(8).withMinute(0).withSecond(0).withNano(0);

        // Ensure the bot restart time is set to the next occurrence if the current time is past today's restart time
        if (nowUTC.isAfter(zonedBotRestartTime)) {
            zonedBotRestartTime = zonedBotRestartTime.plusDays(1);
        }

        // Get LocalDateTime objects for all the necessary times
        LocalDateTime FortniteShopResetTime = zonedFortniteShopResetTime.toLocalDateTime();
        LocalDateTime botRestartTime = zonedBotRestartTime.toLocalDateTime();

        // Sends Fortnite shop image to all registered Discord channels.
        DailyTask dailyFortniteShop = new DailyTask();
        dailyFortniteShop.startDailyTask(dailyFortniteShop::sendFortniteShopImage, FortniteShopResetTime, TimeUnit.DAYS.toMillis(1));

        // Restarts the bot daily at 8:00 AM UTC.
        DailyTask dailyBotRestart = new DailyTask();
        dailyBotRestart.startDailyTask(Bobo::restart, botRestartTime, TimeUnit.DAYS.toMillis(1));
    }

    /**
     * Sets the shutdown hook.
     */
    public static void setShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");

            // Save voice channels that the bot is connected to, so that it can reconnect to them on restart.
            List<String> connectedChannels = new ArrayList<>();
            for (Guild guild : jda.getGuilds()) {
                AudioChannelUnion channel = Objects.requireNonNull(guild.getSelfMember().getVoiceState()).getChannel();
                if (channel != null) {
                    connectedChannels.add(channel.getId());
                }
            }

            jda.getGuilds().forEach(guild -> guild.getAudioManager().closeAudioConnection());

            try (Connection connection = SQLConnection.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(resetVoiceChannelsShutdownTableSQL);
                }

                try (PreparedStatement statement = connection.prepareStatement(insertVoiceChannelsShutdownTableSQL)) {
                    for (String channelId : connectedChannels) {
                        statement.setString(1, channelId);
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
            } catch (SQLException e) {
                System.err.println("Failed to save voice channels to database.");
            }

            jda.shutdownNow();
        }));
    }

    /**
     * Gets the JDA instance.
     *
     * @return JDA instance
     */
    public static JDA getJDA() {
        return jda;
    }

    /**
     * Restarts the bot.
     */
    public static void restart() {
        System.exit(1);
        // Start script should handle the actual restarting (exit code 1 indicates that the bot should restart).
    }
}