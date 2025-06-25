package bobo.commands.voice.music;

import bobo.commands.CommandResponse;
import bobo.utils.api_clients.SpotifyLink;
import com.github.topi314.lavalyrics.LyricsManager;
import com.github.topi314.lavalyrics.lyrics.AudioLyrics;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static bobo.commands.voice.music.QueueCommand.getPagedEmbedResponse;

public class LyricsCommand extends AMusicCommand {
    public LyricsCommand() {
        super(Commands.slash("lyrics", "Get the lyrics of the currently playing track."));
    }

    @Override
    protected CommandResponse handleMusicCommand() {
        if (currentTrack == null) {
            return CommandResponse.text("There is nothing currently playing.");
        }

        LyricsManager lyricsManager = playerManager.getLyricsManager();
        AudioLyrics lyrics;
        try {
            lyrics = lyricsManager.loadLyrics(currentTrack.track());
        } catch (Exception e) {
            return CommandResponse.text("An error occurred while fetching the lyrics.");
        }

        if (lyrics == null || lyrics.getLines() == null) {
            return CommandResponse.text("No lyrics found for the current track.");
        }

        StringBuilder lyricsText = new StringBuilder();
        for (AudioLyrics.Line line : lyrics.getLines()) {
            lyricsText.append(line.getLine())
                    .append("\n");
        }

        if (lyricsText.isEmpty()) {
            return CommandResponse.text("No lyrics found for the current track.");
        }

        List<EmbedBuilder> embedBuilders = new ArrayList<>();
        Member member = getMember();
        AudioTrackInfo info = currentTrack.track().getInfo();
        while (lyricsText.length() > 4096) { // Discord embed description limit; split the lyrics into multiple pages if necessary
            embedBuilders.add(createEmbed(member, info, lyricsText.substring(0, 4096)));
            lyricsText.delete(0, 4096);
        }

        // Add the remaining lyrics
        embedBuilders.add(createEmbed(member, info, lyricsText.toString()));

        return getPagedEmbedResponse(embedBuilders);
    }

    /**
     * Creates an embed with the lyrics of the current track.
     *
     * @param member     The member who requested the lyrics
     * @param info       The info of the current track
     * @param lyricsText The lyrics of the current track
     * @return The embed with the lyrics
     */
    private EmbedBuilder createEmbed(Member member, AudioTrackInfo info, String lyricsText) {
        String uri = info.uri;
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(member.getEffectiveName(), "https://discord.com/users/" + member.getId(), member.getUser().getEffectiveAvatarUrl())
                .setTitle("Lyrics for " + info.title, uri)
                .setThumbnail(info.artworkUrl)
                .addField("Author", info.author, true)
                .setDescription(lyricsText)
                .setColor(Color.RED);

        String albumName = SpotifyLink.getAlbumName(uri);
        if (albumName != null) {
            embed.addField("Album", albumName, true);
        }

        return embed;
    }

    @Override
    public String getName() {
        return "lyrics";
    }

    @Override
    public String getHelp() {
        return """
                Get the lyrics of the currently playing track.
                Usage: `/lyrics`""";
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}