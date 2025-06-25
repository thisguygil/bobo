package bobo.commands.owner;

import bobo.Bobo;
import bobo.commands.CommandResponse;
import bobo.commands.voice.music.AMusicCommand;
import bobo.utils.AudioReceiveListener;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import org.jetbrains.annotations.Nullable;

public class ListenCommand extends AOwnerCommand {
    /**
     * Creates a new listen command.
     */
    public ListenCommand() {}

    @Override
    protected CommandResponse handleOwnerCommand() {
        Guild userGuild = event.getGuild();
        if (AudioReceiveListener.isListening(userGuild)) {
            AudioReceiveListener.stopListening(userGuild);
            return CommandResponse.text("Stopped listening to audio.");
        }

        Member member = event.getMember();
        if (!AMusicCommand.ensureConnected(member)) {
            return CommandResponse.text("You must be connected to a voice channel to use this command.");
        }

        AudioChannel channel;
        String voiceChannelId;
        try {
            voiceChannelId = getOptionValue(0);
            channel = Bobo.getJDA().getChannelById(AudioChannel.class, voiceChannelId);
        } catch (RuntimeException e) {
            return CommandResponse.text("Please provide a valid voice channel id.");
        }

        if (channel == null) {
            return CommandResponse.text("Invalid voice channel id.");
        }

        Guild channelGuild = channel.getGuild();

        if (userGuild.equals(channelGuild)) {
            return CommandResponse.text("You must provide a voice channel id from a different server.");
        }

        if (!channelGuild.getAudioManager().isConnected()) {
            return CommandResponse.text("The bot is not connected to this voice channel.");
        }

        ((AudioReceiveListener) channelGuild.getAudioManager().getReceivingHandler()).setListener(channelGuild, userGuild);
        return CommandResponse.text("Listening to audio in voice channel %s.", channel.getAsMention());
    }

    @Nullable
    @Override
    public Boolean shouldNotShowTyping() {
        return false;
    }

    @Override
    public String getName() {
        return "listen";
    }

    @Override
    public String getHelp() {
        return """
                Listens to the audio in the given voice channel.
                Usage: `/listen <voice channel id>`
                """;
    }
}