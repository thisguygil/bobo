package bobo.commands.voice;

import bobo.Bobo;
import bobo.commands.CommandResponse;
import bobo.utils.api_clients.SQLConnection;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JoinCommand extends AVoiceCommand {
    private static final Logger logger = LoggerFactory.getLogger(JoinCommand.class);

    /**
     * Creates a new join command.
     */
    public JoinCommand() {
        super(Commands.slash("join", "Joins the voice channel."));
    }

    @Override
    protected CommandResponse handleVoiceCommand() {
        AudioManager manager = getGuild().getAudioManager();
        AudioChannelUnion memberChannel = getMember().getVoiceState().getChannel();
        if (manager.isConnected() && memberChannel != null && memberChannel == manager.getConnectedChannel()) {
            return new CommandResponse("Already connected to " + memberChannel.getAsMention());
        }

        try {
            if (join(getMember())) {
                return new CommandResponse("Joined " + memberChannel.getAsMention());
            } else {
                return new CommandResponse("Failed to join " + memberChannel.getAsMention());
            }
        } catch (IllegalArgumentException e) {
            return new CommandResponse(e.getMessage());
        }
    }

    /**
     * Joins the voice channel of the member given.
     *
     * @param member The member whose voice channel to join.
     * @return Whether the bot successfully joined the voice channel.
     */
    public static boolean join(@Nonnull Member member) {
        AudioChannelUnion voiceChannel = member.getVoiceState().getChannel();
        if (voiceChannel == null) {
            throw new IllegalArgumentException("You must be connected to a voice channel to use this command.");
        }

        return join(voiceChannel);
    }

    /**
     * Joins the audio channel given.
     *
     * @param audioChannel The audio channel to join.
     * @return Whether the bot successfully joined the voice channel.
     */
    public static boolean join(@Nonnull AudioChannelUnion audioChannel) {
        try {
            AudioManager audioManager = audioChannel.getGuild().getAudioManager();
            audioManager.openAudioConnection(audioChannel);
        } catch (Exception e) {
            logger.error("Failed to join voice channel: {}", audioChannel.getName());
            return false;
        }

        return true;
    }

    /**
     * Joins voice channels that the bot was in before it was previously shut down.
     */
    public static void joinShutdown() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS voice_channels_shutdown (channel_id VARCHAR(255) NOT NULL)";
        String selectSQL = "SELECT channel_id FROM voice_channels_shutdown";
        try (Connection connection = SQLConnection.getConnection()) {
            try (Statement createStatement = connection.createStatement()) {
                createStatement.execute(createTableSQL);
            }

            try (PreparedStatement selectStatement = connection.prepareStatement(selectSQL);
                 ResultSet resultSet = selectStatement.executeQuery()) {

                while (resultSet.next()) {
                    String channelId = resultSet.getString("channel_id");

                    AudioChannelUnion channel;

                    VoiceChannel voiceChannel = Bobo.getJDA().getVoiceChannelById(channelId);
                    StageChannel stageChannel = Bobo.getJDA().getStageChannelById(channelId);
                    if (voiceChannel != null) {
                        channel = (AudioChannelUnion) voiceChannel;
                    } else if (stageChannel != null) {
                        channel = (AudioChannelUnion) stageChannel;
                    } else {
                        continue;
                    }

                    join(channel);
                }
            }
            logger.info("Joined previous voice channels.");
        } catch (SQLException e) {
            logger.warn("Failed to join previous voice channels.");
        }
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getHelp() {
        return """
                Joins the voice channel that the user is connected to. If the bot is already connected to a different voice channel, it will join the new one.
                Usage: `/join`""";
    }

    @Override
    protected List<Permission> getVoiceCommandPermissions() {
        return new ArrayList<>();
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}