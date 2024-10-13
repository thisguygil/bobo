package bobo.commands.voice;

import bobo.Bobo;
import bobo.utils.AudioReceiveListener;
import bobo.utils.SQLConnection;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;
import okhttp3.MediaType;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClipCommand extends AbstractVoice {
    private static final Logger logger = LoggerFactory.getLogger(ClipCommand.class);

    private static final String selectSQL = "SELECT channel_id FROM clips_channels WHERE guild_id = ?";

    /**
     * Creates a new clip command.
     */
    public ClipCommand() {
        super(Commands.slash("clip", "Clips the most recent 30 seconds of the voice channel.")
                .addOption(OptionType.STRING, "name", "The name of the clip. Defaults to current date/time.", false)
        );
    }

    @Override
    public String getName() {
        return "clip";
    }

    @Override
    protected void handleVoiceCommand() {
        event.deferReply().queue();

        Guild guild = event.getGuildChannel().getGuild();
        AudioReceiveHandler receiveHandler = guild.getAudioManager().getReceivingHandler();

        if (receiveHandler == null) {
            hook.editOriginal("I am not connected to a voice channel.").queue();
            return;
        }
        if (Objects.requireNonNull(guild.getSelfMember().getVoiceState()).isDeafened()) {
            hook.editOriginal("I can't be deafened when using this command.").queue();
            return;
        }

        OptionMapping nameOption = event.getOption("name");
        String name = nameOption == null ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) : nameOption.getAsString();
        Pair<File, byte[]> pair = ((AudioReceiveListener) receiveHandler).createClip(30, name);
        if (pair == null) {
            hook.editOriginal("Clip creation failed.").queue();
            return;
        }

        File file = pair.getLeft();
        if (file != null) {
            GuildChannel channel;
            try (Connection connection = SQLConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(selectSQL)) {
                statement.setString(1, guild.getId());
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    channel = Bobo.getJDA().getGuildChannelById(resultSet.getString("channel_id"));
                } else {
                    channel = null;
                }
            } catch (SQLException e) {
                logger.warn("Failed to get clips channel for guild {}", guild.getId());
                channel = null;
            }

            byte[] waveform = pair.getRight();
            try (FileUpload fileUpload = FileUpload.fromData(file)
                    .asVoiceMessage(MediaType.parse("audio/wav"), waveform, 30)) {
                if (channel != null && channel != event.getChannel()) {
                    ((GuildMessageChannel) channel).sendFiles(fileUpload).queue();
                }
                hook.editOriginalAttachments(fileUpload).queue(_ -> {
                    if (!file.delete()) {
                        logger.error("Failed to delete file: {}", file.getName());
                    }
                });
            } catch (IOException e) {
                logger.error("Failed to upload file: {}", file.getName());
                hook.editOriginal("Clip creation failed.").queue();
            }
        } else {
            hook.editOriginal("Clip creation failed.").queue();
        }
    }

    @Override
    public String getHelp() {
        return """
                Clips the most recent 30 seconds of the voice channel with an optional given name.
                No name defaults to the current date and time.
                Usage: `/clip <name>`""";
    }

    @Override
    protected List<Permission> getVoiceCommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_ATTACH_FILES));
    }
}