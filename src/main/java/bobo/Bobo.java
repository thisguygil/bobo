package bobo;

import bobo.commands.owner.SetActivityCommand;
import bobo.utils.DailyFortniteShop;
import bobo.utils.SQLConnection;
import com.github.ygimenez.model.PaginatorBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Bobo {
    private static JDA jda;

    private static final String insertVoiceChannelsShutdownTableSQL = "INSERT INTO voice_channels_shutdown (channel_id) VALUES (?)";
    private static final String resetVoiceChannelsShutdownTableSQL = "DELETE FROM voice_channels_shutdown";

    public static void main(String[] args) {
        jda = JDABuilder.createDefault(Config.get("TOKEN"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        jda.addEventListener(new Listener());

        SetActivityCommand.setActivity();

        DailyFortniteShop dailyFortniteShop = new DailyFortniteShop();
        dailyFortniteShop.startDailyTask(dailyFortniteShop::sendShopImageToDiscord);

        try {
            PaginatorBuilder.createPaginator(jda)
                    .shouldRemoveOnReact(false)
                    .shouldEventLock(false)
                    .setDeleteOnCancel(true)
                    .activate();
        } catch (Exception e) {
            e.printStackTrace();
        }

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
}