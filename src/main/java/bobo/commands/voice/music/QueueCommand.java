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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.*;
import java.util.*;
import java.util.List;

public class QueueCommand extends AbstractMusic {
    /**
     * Creates a new queue command.
     */
    public QueueCommand() {
        super(Commands.slash("queue", "View or manipulate the music queue.")
                .addSubcommands(new SubcommandData("show", "Shows the currently queued tracks."))
                .addSubcommands(new SubcommandData("shuffle", "Shuffles the queue (does not stop the current track)."))
                .addSubcommands(new SubcommandData("clear", "Clears queue and stops current track."))
                .addSubcommands(new SubcommandData("remove", "Removes track at given position in the queue.")
                        .addOption(OptionType.INTEGER, "position", "What position in the queue to remove the track from", true))
        );
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (currentTrack == null) {
            hook.editOriginal("The queue is currently empty.").queue();
            return;
        }

        String subcommandName = Objects.requireNonNull(event.getSubcommandName());
        switch (subcommandName) {
            case "show" -> show();
            case "shuffle" -> shuffle();
            case "clear" -> clear();
            case "remove" -> remove();
        }
    }

    /**
     * Shows the current queue.
     */
    private void show() {
        final List<TrackScheduler.TrackChannelPair> trackList = new ArrayList<>(queue);
        final List<Page> pages = new ArrayList<>();
        trackList.add(0, new TrackScheduler.TrackChannelPair(currentTrack, event.getMessageChannel()));

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

        for (int i = 1; i <= numPages; i++) {
            track = trackList.get(i).track();
            info = track.getInfo();
            builder.addField(i + ":", "[" + info.title + "](" + info.uri + ") by **" + info.author + "** [" +
                    (i == 1 ? TimeFormat.formatTime(track.getDuration() - track.getPosition()) : TimeFormat.formatTime(track.getDuration())) +
                    (i == 1 ? (scheduler.looping ? " left] (currently looping)\n" : " left] (currently playing)\n") : "]\n"), false);
            count++;
            if (count == 10) {
                pages.add(InteractPage.of(builder.build()));
                builder = new EmbedBuilder()
                        .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                        .setTitle("Current Queue")
                        .setColor(Color.red)
                        .setFooter("Page " + ((int) Math.ceil((double) i / 10) + 1) + "/" + ((int) Math.ceil((double) numPages / 10)));
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

    /**
     * Shuffles the queue.
     */
    private void shuffle() {
        List<TrackScheduler.TrackChannelPair> trackList = new ArrayList<>();
        queue.drainTo(trackList);
        Collections.shuffle(trackList);
        queue.clear();
        queue.addAll(trackList);
        hook.editOriginal("Shuffled.").queue();
    }

    /**
     * Clears the queue.
     */
    private void clear() {
        queue.clear();
        scheduler.looping = false;
        player.stopTrack();
        player.setPaused(false);
        hook.editOriginal("Queue cleared.").queue();
    }

    /**
     * Removes a track from the queue.
     */
    private void remove() {
        int position = Objects.requireNonNull(event.getOption("position")).getAsInt();
        if (position < 1 || position > queue.size() + 1) {
            hook.editOriginal("Please enter an integer corresponding to a track's position in the queue.").queue();
            return;
        }

        if (position == 1) {
            scheduler.nextTrack();
            scheduler.looping = false;
        } else {
            int count = 1;
            Iterator<TrackScheduler.TrackChannelPair> iterator = queue.iterator();
            while (iterator.hasNext()) {
                if (count == position) {
                    iterator.remove();
                }
                count++;
                iterator.next();
            }
            if (count == position) {
                iterator.remove();
            }
        }

        hook.editOriginal("Removed track at position **" + position + "**.").queue();
    }

    @Override
    public String getName() {
        return "queue";
    }
}