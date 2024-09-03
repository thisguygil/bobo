package bobo.commands.voice;

import bobo.Bobo;
import bobo.utils.SQLConnection;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JoinCommand extends AbstractVoice {
    /**
     * Creates a new join command.
     */
    public JoinCommand() {
        super(Commands.slash("join", "Joins the voice channel."));
    }

    @Override
    protected void handleVoiceCommand() {
        event.deferReply().queue();

        AudioManager manager = event.getGuildChannel().getGuild().getAudioManager();
        AudioChannelUnion memberChannel = Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel();
        if (manager.isConnected() && memberChannel != null && memberChannel == manager.getConnectedChannel()) {
            hook.editOriginal("Already connected to " + memberChannel.getAsMention()).queue();
            return;
        }

        if (join(event)) {
            assert memberChannel != null;
            hook.editOriginal("Joined " + memberChannel.getAsMention()).queue();
        }
    }

    /**
     * Joins the voice channel of the user who sent the command.
     *
     * @param event The event that triggered this action.
     * @return Whether the bot successfully joined the voice channel.
     */
    public static boolean join(@Nonnull SlashCommandInteractionEvent event) {
        AudioChannelUnion voiceChannel = event.getMember().getVoiceState().getChannel();
        if (voiceChannel == null) {
            event.getHook().editOriginal("You must be connected to a voice channel to use this command.").queue();
            return false;
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
            e.printStackTrace();
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
        } catch (SQLException e) {
            System.err.println("Error joining voice channels: " + e.getMessage());
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
}