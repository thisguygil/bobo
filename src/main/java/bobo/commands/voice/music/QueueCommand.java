package bobo.commands.voice.music;

import bobo.lavaplayer.TrackScheduler;
import bobo.utils.TimeFormat;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class QueueCommand extends AbstractMusic {
    /**
     * Creates a new queue command.
     */
    public QueueCommand() {
        super(Commands.slash("queue", "Shows the currently queued tracks."));
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();
        final List<TrackScheduler.TrackChannelPair> trackList = new ArrayList<>(queue);
        final List<Page> pages = new ArrayList<>();

        if (currentTrack != null) {
            trackList.add(0, new TrackScheduler.TrackChannelPair(currentTrack, event.getMessageChannel()));
        } else {
            hook.editOriginal("The queue is currently empty.").queue();
            return;
        }

        AudioTrack track;
        AudioTrackInfo info;
        int count = 0;
        int numPages = trackList.size();
        Member member = event.getMember();
        assert member != null;
        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                .setTitle("Current Queue")
                .setColor(Color.red)
                .setFooter("Page 1/" + (int) Math.ceil((double) numPages / 10));

        for (int i = 0; i < numPages; i++) {
            track = trackList.get(i).track();
            info = track.getInfo();
            builder.addField((i + 1) + ":", "[" + info.title + "](" + info.uri + ") by **" + info.author + "** [" +
                    (i == 0 ? TimeFormat.formatTime(track.getDuration() - track.getPosition()) : TimeFormat.formatTime(track.getDuration())) +
                    (i == 0 ? (scheduler.looping ? " left] (currently looping)\n" : " left] (currently playing)\n") : "]\n"), false);
            count++;
            if (count == 10) {
                pages.add(InteractPage.of(builder.build()));
                builder = new EmbedBuilder()
                        .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                        .setTitle("Current Queue")
                        .setColor(Color.red)
                        .setFooter("Page " + ((int) Math.ceil((double) (i + 1) / 10) + 1) + "/" + (int) Math.ceil((double) numPages / 10));
                count = 0;
            }
        }
        if (!builder.getFields().isEmpty()) {
            pages.add(InteractPage.of(builder.build()));
        }

        if (pages.size() == 1) {
            hook.editOriginalEmbeds((MessageEmbed) pages.get(0).getContent()).queue();
        } else {
           hook.editOriginalEmbeds((MessageEmbed) pages.get(0).getContent()).queue(success -> Pages.paginate(success, pages, true));
        }
    }

    @Override
    public String getName() {
        return "queue";
    }
}
