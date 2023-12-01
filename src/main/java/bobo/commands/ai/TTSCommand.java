package bobo.commands.ai;

import bobo.commands.voice.JoinCommand;
import bobo.utils.TrackType;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.SQLConnection;
import com.theokanning.openai.audio.CreateSpeechRequest;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/* NOTE: This also classifies as a music command (as it must use lavaplayer to play tts),
   but Java doesn't allow multiple inheritance. However, I decided to make this an AI command
   since the AI is the bulk of the command's logic.
 */
public class TTSCommand extends AbstractAI {
    private static final Map<String, String> fileMessageMap = new HashMap<>(); // For retrieving the message associated with a TTS file.
    private static final String selectSQL = "SELECT voice FROM tts_voice WHERE guild_id = ?";

    /**
     * Creates a new TTS command.
     */
    public TTSCommand() {
        super(Commands.slash("tts", "Generates an AI voice message for the voice channel.")
                .addOption(OptionType.STRING, "message", "Message to be said.", true));
    }

    @Override
    public String getName() {
        return "tts";
    }

    @Override
    protected void handleAICommand() {
        event.deferReply().queue();

        Guild guild = event.getGuildChannel().getGuild();
        AudioManager audioManager = guild.getAudioManager();
        if (!audioManager.isConnected()) {
            if (!JoinCommand.join(event)) {
                return;
            }
        } else {
            AudioChannelUnion memberChannel = Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel();
            if (memberChannel == null) {
                event.getHook().editOriginal("You must be connected to a voice channel to use this command.").queue();
                return;
            } else if (memberChannel != audioManager.getConnectedChannel()) {
                if (!JoinCommand.join(event)) {
                    return;
                }
            }
        }

        String voice = "onyx";
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSQL)) {
            statement.setString(1, guild.getId());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                voice = resultSet.getString("voice");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String message = Objects.requireNonNull(event.getOption("message")).getAsString();
        CreateSpeechRequest createSpeechRequest = CreateSpeechRequest.builder()
                .model("tts-1")
                .input(message)
                .voice(voice)
                .build();

        String fileName;
        File file;
        try {
            byte[] bytes = service.createSpeech(createSpeechRequest).bytes();

            fileName = "tts-" + UUID.randomUUID() + ".mp3";
            file = new File(fileName);
            Files.write(file.toPath(), bytes);
        } catch (IOException e) {
            hook.editOriginal(e.getMessage()).queue();
            e.printStackTrace();
            return;
        }

        PlayerManager.getInstance().loadAndPlay(event, fileName, TrackType.TTS);
        fileMessageMap.put(fileName, message);
    }

    /**
     * Gets the message associated with the given file name.
     *
     * @param fileName The file name.
     */
    public static String getTTSMessage(String fileName) {
        return fileMessageMap.get(fileName);
    }

    /**
     * Removes the message associated with the given file name.
     *
     * @param fileName The file name.
     */
    public static void removeTTSMessage(String fileName) {
        fileMessageMap.remove(fileName);
    }
}