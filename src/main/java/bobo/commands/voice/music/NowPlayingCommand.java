package bobo.commands.voice.music;

import bobo.utils.SpotifyLink;
import bobo.utils.TimeFormat;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import se.michaelthelin.spotify.SpotifyApi;

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

        AudioTrack currentAudioTrack = currentTrack.track();
        AudioTrackInfo info = currentAudioTrack.getInfo();
        String title = info.title;
        String uri = info.uri;
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor("Now " + (info.isStream ? "Streaming" : (musicManager.scheduler.looping == LoopCommand.looping.TRACK ? "Looping" : "Playing")))
                .addField("Requested by", currentTrack.member().getAsMention(), true)
                .setColor(Color.red);

        switch (currentTrack.trackType()) {
            case TRACK -> {
                embed.setTitle(title, uri)
                        .addField("Author", info.author, true)
                        .setThumbnail(info.artworkUrl);

                if (!info.isStream) {
                    embed.setFooter(TimeFormat.formatTime(currentAudioTrack.getDuration() - currentAudioTrack.getPosition()) + " left");
                }

                // Add the album name if the track is from Spotify
                try {
                    String spotifyRegex = "^(https?://)?open.spotify.com/.*";
                    if (spotifyRegex.matches(uri)) {
                        SpotifyApi spotifyApi = SpotifyLink.getSpotifyApi();
                        String id = uri.split("/")[uri.split("/").length - 1];
                        String albumName = spotifyApi.getTrack(id)
                                .build()
                                .execute()
                                .getAlbum()
                                .getName();
                        embed.addField("Album", albumName, true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            case FILE -> embed.setTitle(title, uri)
                    .addField("Author", info.author, true)
                    .setFooter(TimeFormat.formatTime(currentAudioTrack.getDuration() - currentAudioTrack.getPosition()) + " left");
            case TTS -> embed.setTitle("TTS Message")
                    .setDescription(TTSCommand.getTTSMessage(currentAudioTrack));
        }

        hook.editOriginalEmbeds(embed.build()).queue();
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