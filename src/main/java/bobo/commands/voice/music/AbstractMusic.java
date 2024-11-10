package bobo.commands.voice.music;

import bobo.commands.voice.AbstractVoice;
import bobo.commands.voice.JoinCommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.TrackRecord;
import bobo.lavaplayer.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.List;
import java.util.concurrent.BlockingDeque;

public abstract class AbstractMusic extends AbstractVoice {
    protected PlayerManager playerManager;
    protected GuildMusicManager musicManager;
    protected AudioPlayer player;
    protected TrackScheduler scheduler;
    protected BlockingDeque<TrackRecord> queue;
    protected TrackRecord currentTrack;
    protected TrackRecord previousTrack;

    /**
     * Creates a new music command.
     *
     * @param commandData The command data.
     */
    public AbstractMusic(CommandData commandData) {
        super(commandData);
    }

    @Override
    protected void handleVoiceCommand() {
        this.playerManager = PlayerManager.getInstance();
        this.musicManager = playerManager.getMusicManager(event.getGuildChannel().getGuild());
        this.player = musicManager.player;
        this.scheduler = musicManager.scheduler;
        this.queue = scheduler.queue;
        this.currentTrack = scheduler.currentTrack;
        this.previousTrack = scheduler.previousTrack;

        handleMusicCommand();
    }

    /**
     * Ensures that the user is connected to a voice channel, moving the bot to the user's channel if necessary.
     *
     * @param event The slash command interaction event.
     * @return {@code true} if the user is connected to a voice channel, {@code false} otherwise.
     */
    protected static boolean ensureConnected(SlashCommandInteractionEvent event) {
        AudioManager audioManager = event.getGuildChannel().getGuild().getAudioManager();
        if (!audioManager.isConnected()) {
            return JoinCommand.join(event);
        } else {
            AudioChannelUnion memberChannel = event.getMember().getVoiceState().getChannel();
            if (memberChannel == null) {
                event.getHook().editOriginal("You must be connected to a voice channel to use this command.").queue();
                return false;
            } else if (memberChannel != audioManager.getConnectedChannel()) {
                return JoinCommand.join(event);
            }
        }

        return true;
    }

    /**
     * Ensures that the user is connected to a voice channel, moving the bot to the user's channel if necessary.
     *
     * @param member The member whose voice channel to check.
     * @return {@code true} if the user is connected to a voice channel, {@code false} otherwise.
     */
    protected static boolean ensureConnected(Member member) {
        AudioManager audioManager = member.getGuild().getAudioManager();
        if (!audioManager.isConnected()) {
            return JoinCommand.join(member);
        } else {
            AudioChannelUnion memberChannel = member.getVoiceState().getChannel();
            if (memberChannel == null) {
                return false;
            } else if (memberChannel != audioManager.getConnectedChannel()) {
                return JoinCommand.join(member);
            }
        }

        return true;
    }

    /**
     * Handles the music command.
     */
    protected abstract void handleMusicCommand();

    @Override
    protected List<Permission> getVoiceCommandPermissions() {
        return getMusicCommandPermissions();
    }

    /**
     * Gets the permissions required for the music command.
     *
     * @return The permissions required for the music command.
     */
    protected abstract List<Permission> getMusicCommandPermissions();
}
