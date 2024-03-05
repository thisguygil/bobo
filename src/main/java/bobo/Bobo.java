package bobo;

import bobo.commands.owner.SetActivityCommand;
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

        SetActivityCommand.setActivity();

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
            e.printStackTrace();
        }
    }

    /**
     * Starts daily tasks.
     */
    public static void startDailyTasks() {
        // Get the current time in Eastern Time
        ZonedDateTime nowEastern = ZonedDateTime.now(ZoneId.of("America/New_York"));

        // Fortnite shop resets at 0:00 UTC, which is 7:00 PM EST or 8:00 PM EDT depending on whether it is daylight savings time or not
        // While the shop resets at 0:00 EST, we give it an extra minute to make sure the API is updated
        // Therefore, we set the target time to 7:01 PM EST or 8:01 PM EDT depending on whether it is daylight savings time or not
        ZonedDateTime zonedFortniteShopResetTime = nowEastern.withMinute(1).withSecond(0).withNano(0);

        // Set the hour depending on whether it is daylight savings time or not
        if (nowEastern.getZone().getRules().isDaylightSavings(nowEastern.toInstant())) {
            zonedFortniteShopResetTime = zonedFortniteShopResetTime.withHour(20);
        } else {
            zonedFortniteShopResetTime = zonedFortniteShopResetTime.withHour(19);
        }

        // Get LocalDateTime objects for all the necessary times
        LocalDateTime now = nowEastern.toLocalDateTime();
        LocalDateTime FortniteShopResetTime = zonedFortniteShopResetTime.toLocalDateTime();
        LocalDateTime botRestartTime = now.withHour(3).withMinute(0).withSecond(0).withNano(0);

        // Sends Fortnite shop image to all registered Discord channels.
        DailyTask dailyFortniteShop = new DailyTask();
        dailyFortniteShop.startDailyTask(dailyFortniteShop::sendFortniteShopImage, FortniteShopResetTime, TimeUnit.DAYS.toMillis(1));

        // Restarts the bot daily at 3:00 AM EST/EDT.
        DailyTask dailyBotRestart = new DailyTask();
        dailyBotRestart.startDailyTask(Bobo::restart, botRestartTime, TimeUnit.DAYS.toMillis(1));
        // It may seem counterintuitive to set a recurring daily restart since it will only restart once before setting
        // it up again, but this is in case there is an error one day with the restart; it can just try again the next day.
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
                e.printStackTrace();
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