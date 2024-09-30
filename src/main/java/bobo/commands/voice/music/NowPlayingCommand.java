package bobo.commands.voice.music;

import bobo.lavaplayer.GuildMusicManager;
import bobo.utils.SpotifyLink;
import bobo.utils.TimeFormat;
import bobo.utils.TrackRecord;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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
        Member member = currentTrack.member();
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor("Now " + (info.isStream ? "Streaming" : (musicManager.scheduler.looping == LoopCommand.looping.TRACK ? "Looping" : "Playing")))
                .addField("Requested by", member == null ? "Unknown Member" : member.getAsMention(), true)
                .setColor(Color.red);

        // Add track type-specific information
        switch (currentTrack.trackType()) {
            case TRACK -> {
                embed.setTitle(title, uri)
                        .addField("Author", info.author, true)
                        .setThumbnail(info.artworkUrl);

                if (!info.isStream) { // Only show duration or time left for non-streams
                    embed.setFooter(footerText(info, currentAudioTrack));
                }

                // Add album name if available
                String albumName = SpotifyLink.getAlbumName(info.uri);
                if (albumName != null) {
                    embed.addField("Album", albumName, true);
                }
            }
            case FILE -> embed.setTitle(title, uri)
                    .addField("Author", info.author, true)
                    .setFooter(footerText(info, currentAudioTrack));
            case TTS -> embed.setTitle("TTS Message")
                    .setDescription(TTSCommand.getTTSMessage(musicManager.guild, currentAudioTrack));
        }

        return embed.build();
    }

    /**
     * Gets the footer text for the currently playing track.
     *
     * @param info The track info.
     * @param currentAudioTrack The currently playing track.
     * @return The footer text.
     */
    private static String footerText(AudioTrackInfo info, AudioTrack currentAudioTrack) {
        long position = currentAudioTrack.getPosition();
        return TimeFormat.formatTime(info.length - position) + (position == 0 ? "" : " left");
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

    @Override
    protected List<Permission> getMusicCommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS));
    }
}