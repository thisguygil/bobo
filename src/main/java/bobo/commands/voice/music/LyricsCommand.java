package bobo.commands.voice.music;

import com.github.topi314.lavalyrics.LyricsManager;
import com.github.topi314.lavalyrics.lyrics.AudioLyrics;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LyricsCommand extends AbstractMusic {
    public LyricsCommand() {
        super(Commands.slash("lyrics", "Get the lyrics of the currently playing track."));
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (currentTrack == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
            return;
        }

        LyricsManager lyricsManager = playerManager.getLyricsManager();
        AudioLyrics lyrics;
        try {
            lyrics = lyricsManager.loadLyrics(currentTrack.track());
        } catch (Exception e) {
            hook.editOriginal("An error occurred while fetching the lyrics.").queue();
            return;
        }

        if (lyrics == null || lyrics.getLines() == null) {
            hook.editOriginal("No lyrics found for the current track.").queue();
            return;
        }

        StringBuilder lyricsText = new StringBuilder();
        for (AudioLyrics.Line line : lyrics.getLines()) {
            lyricsText.append(line.getLine())
                    .append("\n");
        }

        if (lyricsText.isEmpty()) {
            hook.editOriginal("No lyrics found for the current track.").queue();
            return;
        }

        List<EmbedBuilder> builders = new ArrayList<>();
        Member member = event.getMember();
        assert member != null;
        AudioTrackInfo info = currentTrack.track().getInfo();
        EmbedBuilder embed;
        while (lyricsText.length() > 4096) {
            embed = new EmbedBuilder()
                    .setAuthor(member.getEffectiveName(), "https://discord.com/users/" + member.getId(), member.getUser().getEffectiveAvatarUrl())
                    .setTitle("Lyrics for " + info.title, info.uri)
                    .setThumbnail(info.artworkUrl)
                    .addField("Author", info.author, true)
                    .setDescription(lyricsText.substring(0, 4096))
                    .setColor(Color.RED);

            builders.add(embed);
            lyricsText.delete(0, 4096);
        }

        // Add the remaining lyrics
        embed = new EmbedBuilder()
                .setAuthor(member.getEffectiveName(), "https://discord.com/users/" + member.getId(), member.getUser().getEffectiveAvatarUrl())
                .setTitle("Lyrics for " + info.title, info.uri)
                .setThumbnail(info.artworkUrl)
                .addField("Author", info.author, true)
                .setDescription(lyricsText.toString())
                .setColor(Color.RED);

        builders.add(embed);

        int pageCount = 1;
        List<Page> pages = new ArrayList<>();
        for (EmbedBuilder builder : builders) {
            builder.setFooter("Page " + pageCount + " of " + builders.size());
            pages.add(InteractPage.of(builder.build()));
            pageCount++;
        }

        if (pages.size() == 1) {
            hook.editOriginalEmbeds((MessageEmbed) pages.get(0).getContent()).queue();
        } else {
            hook.editOriginalEmbeds((MessageEmbed) pages.get(0).getContent()).queue(success -> Pages.paginate(success, pages, true));
        }
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
}