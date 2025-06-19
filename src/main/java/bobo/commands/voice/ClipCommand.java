package bobo.commands.voice;

import bobo.Bobo;
import bobo.utils.AudioReceiveListener;
import bobo.commands.CommandResponse;
import bobo.utils.api_clients.SQLConnection;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ClipCommand extends AVoiceCommand {
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
    protected CommandResponse handleVoiceCommand() {
        Guild guild = getGuild();
        AudioReceiveHandler receiveHandler = guild.getAudioManager().getReceivingHandler();

        if (receiveHandler == null) {
            return new CommandResponse("I'm not connected to a voice channel.");
        }
        if (guild.getSelfMember().getVoiceState().isDeafened()) {
            return new CommandResponse("I can't be deafened when using this command.");
        }

        String name = getMultiwordOptionValue("name", 0, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
        File file = ((AudioReceiveListener) receiveHandler).createClip(30, name);
        if (file == null) {
            return new CommandResponse("Clip creation failed.");
        }

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

        FileUpload fileUpload = FileUpload.fromData(file);
        if (channel != null && channel != getChannel()) {
            ((GuildMessageChannel) channel).sendFiles(fileUpload).queue();
        }

        return CommandResponse.builder()
                .addAttachments(fileUpload)
                .setPostExecutionAsMessage(success -> {
                    if (!file.delete()) {
                        logger.error("Failed to delete file: {}", file.getName());
                    }
                })
                .setFailureHandler(failure -> {
                    if (!file.delete()) {
                        logger.error("Failed to delete file: {}", file.getName());
                    }
                })
                .build();
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

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}