package bobo.commands.voice.music;

import bobo.commands.ai.TTSCommand;
import bobo.utils.TrackChannelTypeRecord;
import bobo.utils.TimeFormat;
import bobo.utils.TrackType;
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
import java.io.File;
import java.util.*;
import java.util.List;

public class QueueCommand extends AbstractMusic {
    /**
     * Creates a new queue command.
     */
    public QueueCommand() {
        super(Commands.slash("queue", "View or manipulate the music queue.")
                .addSubcommands(
                        new SubcommandData("show", "Shows the currently queued tracks."),
                        new SubcommandData("shuffle", "Shuffles the queue (does not stop the current track)."),
                        new SubcommandData("clear", "Clears queue and stops current track."),
                        new SubcommandData("remove", "Removes track at given position in the queue.")
                                .addOption(OptionType.INTEGER, "position", "What position in the queue to remove the track from", true)
                )
        );
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (currentTrack == null) {
            hook.editOriginal("The queue is currently empty.").queue();
            return;
        }

        switch (Objects.requireNonNull(event.getSubcommandName())) {
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
        final List<TrackChannelTypeRecord> trackList = new ArrayList<>(queue);
        final List<Page> pages = new ArrayList<>();
        TrackType trackType = currentTrack.trackType();
        trackList.add(0, new TrackChannelTypeRecord(currentTrack.track(), event.getMessageChannel(), trackType));

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
            switch (trackType) {
                case TRACK, FILE -> builder.addField((i + 1) + ":", "[" + info.title + "](" + info.uri + ") by **" + info.author + "** [" +
                        (i == 0 ? TimeFormat.formatTime(track.getDuration() - track.getPosition()) : TimeFormat.formatTime(track.getDuration())) +
                        (i == 0 ? (scheduler.looping ? " left] (currently looping)\n" : " left] (currently playing)\n") : "]\n"), false);
                case TTS -> builder.addField((i + 1) + ":", "TTS Message [" +
                        (i == 0 ? TimeFormat.formatTime(track.getDuration() - track.getPosition()) : TimeFormat.formatTime(track.getDuration())) +
                        (i == 0 ? (scheduler.looping ? " left] (currently looping)\n" : " left] (currently playing)\n") : "]\n"), false);
            }
            count++;
            if (count == 10) {
                pages.add(InteractPage.of(builder.build()));
                builder = new EmbedBuilder()
                        .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                        .setTitle("Current Queue")
                        .setColor(Color.red)
                        .setFooter("Page " + ((int) Math.ceil((double) (i + 1) / 10) + 1) + "/" + ((int) Math.ceil((double) numPages / 10)));
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
        List<TrackScheduler.TrackChannelTypeRecord> trackList = new ArrayList<>();
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
            if (currentTrack.trackType() == TrackType.TTS) {
                File file = new File(currentTrack.track().getInfo().uri);
                if (file.exists() && !file.delete()) {
                    System.err.println("Failed to delete TTS file: " + file.getName());
                }
            }
            scheduler.looping = false;
            scheduler.nextTrack();
        } else {
            int count = 1;
            Iterator<TrackChannelTypeRecord> iterator = queue.iterator();
            TrackChannelTypeRecord currentTrack = null;
            while (iterator.hasNext()) {
                if (count == position) {
                    if (currentTrack.trackType() == TrackType.TTS) {
                        File file = new File(currentTrack.track().getInfo().uri);
                        if (file.exists() && !file.delete()) {
                            System.err.println("Failed to delete TTS file: " + file.getName());
                        }
                    }
                    iterator.remove();
                }
                count++;
                currentTrack = iterator.next();
            }
            if (count == position) {
                if (currentTrack.trackType() == TrackType.TTS) {
                    File file = new File(currentTrack.track().getInfo().uri);
                    if (file.exists() && !file.delete()) {
                        System.err.println("Failed to delete TTS file: " + file.getName());
                    }
                }
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