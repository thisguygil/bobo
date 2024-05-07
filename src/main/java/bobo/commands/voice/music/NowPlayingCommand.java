package bobo.commands.voice.music;

import bobo.lavaplayer.GuildMusicManager;
import bobo.utils.SpotifyLink;
import bobo.utils.TimeFormat;
import bobo.utils.TrackRecord;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.*;

public class NowPlayingCommand extends AbstractMusic {
    /**
     * Creates a new now-playing command.
     */
    public NowPlayingCommand() {
        super(Commands.slash("now-playing", "Shows the currently playing track."));
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (currentTrack == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
            return;
        }

        hook.editOriginalEmbeds(createEmbed(currentTrack, musicManager)).queue();
    }

    /**
     * Creates an embed with information about the currently playing track.
     *
     * @param currentTrack The currently playing track.
     * @param musicManager The guild music manager.
     * @return The embed.
     */
    public static MessageEmbed createEmbed(TrackRecord currentTrack, GuildMusicManager musicManager) {
        AudioTrack currentAudioTrack = currentTrack.track();
        AudioTrackInfo info = currentAudioTrack.getInfo();
        String title = info.title;
        String uri = info.uri;
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor("Now " + (info.isStream ? "Streaming" : (musicManager.scheduler.looping == LoopCommand.looping.TRACK ? "Looping" : "Playing")))
                .addField("Requested by", currentTrack.member().getAsMention(), true)
                .setColor(Color.red);

        // Add track type-specific information
        switch (currentTrack.trackType()) {
            case TRACK -> {
                embed.setTitle(title, uri)
                        .addField("Author", info.author, true)
                        .setThumbnail(info.artworkUrl);

                if (!info.isStream) { // Only show duration or time left for non-streams
                    long position = currentAudioTrack.getPosition();
                    embed.setFooter(TimeFormat.formatTime(info.length - position) + (position == 0 ? "" : " left"));
                }

                // Add album name if available
                String albumName = SpotifyLink.getAlbumName(info.uri);
                if (albumName != null) {
                    embed.addField("Album", albumName, true);
                }
            }
            case FILE -> embed.setTitle(title, uri)
                    .addField("Author", info.author, true)
                    .setFooter(TimeFormat.formatTime(currentAudioTrack.getDuration() - currentAudioTrack.getPosition()) + " left");
            case TTS -> embed.setTitle("TTS Message")
                    .setDescription(TTSCommand.getTTSMessage(currentAudioTrack));
        }

        return embed.build();
    }

    @Override
    public String getName() {
        return "now-playing";
    }

    @Override
    public String getHelp() {
        return """
                Shows the currently playing track.
                Usage: `/now-playing`""";
    }
}