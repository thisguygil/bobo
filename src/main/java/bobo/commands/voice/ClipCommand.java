package bobo.commands.voice;

import bobo.Bobo;
import bobo.lavaplayer.AudioReceiveListener;
import bobo.utils.SQLConnection;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ClipCommand extends AbstractVoice {
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
        File file = ((AudioReceiveListener) receiveHandler).createFile(30, name);
        if (file != null) {
            TextChannel channel;
            try (Connection connection = SQLConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(selectSQL)) {
                statement.setString(1, event.getGuildChannel().getGuild().getId());
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    channel = Bobo.getJDA().getTextChannelById(resultSet.getString("channel_id"));
                } else {
                    hook.editOriginal("No clips channel has been configured. Please configure one with **/config clips**").queue();
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                hook.editOriginal("Clip creation failed.").queue();
                return;
            }

            assert channel != null;
            channel.sendFiles(FileUpload.fromData(file)).queue();
            hook.editOriginalAttachments(FileUpload.fromData(file)).queue(success -> {
                if (!file.delete()) {
                    System.err.println("Failed to delete file: " + file.getName());
                }
            });
        } else {
            hook.editOriginal("Clip creation failed.").queue();
        }
    }
}