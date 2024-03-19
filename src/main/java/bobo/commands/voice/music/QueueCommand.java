package bobo.commands.voice.music;

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
import java.util.*;
import java.util.List;

public class QueueCommand extends AbstractMusic {
    /**
     * Creates a new queue command.
     */
    public QueueCommand() {
        super(Commands.slash("queue", "View or change the music queue.")
                .addSubcommands(
                        new SubcommandData("show", "Shows the currently queued tracks."),
                        new SubcommandData("shuffle", "Shuffles the queue."),
                        new SubcommandData("loop", "Loop the queue."),
                        new SubcommandData("clear", "Clears the queue and stops current track."),
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
            case "loop" -> loop();
            case "clear" -> clear();
            case "remove" -> remove();
        }
    }

    /**
     * Shows the current queue.
     */
    private void show() {
        // Initialize all necessary variables
        final List<TrackChannelTypeRecord> trackList = new ArrayList<>(queue);
        trackList.add(0, new TrackChannelTypeRecord(currentTrack.track(), event.getMessageChannel(), currentTrack.trackType()));
        List<EmbedBuilder> builders = new ArrayList<>();
        List<Page> pages = new ArrayList<>();
        StringBuilder tracksField = new StringBuilder();
        int charCount = 0;
        int trackCounter = 1;
        int beginTrackCounter = 1;
        Member member = event.getMember();
        assert member != null;

        // Initialize the embed params and the first page
        String memberName = member.getEffectiveName();
        String memberUrl = "https://discord.com/users/" + member.getId();
        String memberAvatar = member.getEffectiveAvatarUrl();
        String embedTitle = "Current Queue" + (scheduler.looping == LoopCommand.looping.QUEUE ? " - Looping" : "");
        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(memberName, memberUrl, memberAvatar)
                .setTitle(embedTitle)
                .setColor(Color.red);

        // Add all tracks to the pages
        for (TrackChannelTypeRecord record : trackList) {
            String trackInfo = formatTrackInfo(trackCounter, record);
            if (charCount + trackInfo.length() > 1024) {
                // Create a page for the current tracks
                builder.addField(beginTrackCounter == trackCounter ? "Track " + beginTrackCounter : "Tracks " + beginTrackCounter + " - " + (trackCounter - 1), tracksField.toString(), false);
                builders.add(builder);

                // Reset for the next page
                builder = new EmbedBuilder()
                        .setAuthor(memberName, memberUrl, memberAvatar)
                        .setTitle(embedTitle)
                        .setColor(Color.red);
                tracksField = new StringBuilder();
                charCount = 0;
                beginTrackCounter = trackCounter;
            }

            tracksField.append(trackInfo);
            charCount += trackInfo.length();
            trackCounter++;
        }

        // Add any remaining tracks to the last page
        if (!tracksField.toString().isEmpty()) {
            trackCounter--;
            builder.addField(beginTrackCounter == trackCounter ? "Track " + beginTrackCounter : "Tracks " + beginTrackCounter + " - " + trackCounter, tracksField.toString(), false);
            builders.add(builder);
        }

        // Add page counts to the footers and construct the pages
        int pageCount = 1;
        for (EmbedBuilder embedBuilder : builders) {
            embedBuilder.setFooter("Page " + pageCount + " of " + builders.size());
            pages.add(InteractPage.of(embedBuilder.build()));
            pageCount++;
        }

        // Send only one page if there is only one page, otherwise paginate
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
        List<TrackChannelTypeRecord> trackList = new ArrayList<>();
        queue.drainTo(trackList);
        Collections.shuffle(trackList);
        queue.clear();
        queue.addAll(trackList);
        hook.editOriginal("Shuffled.").queue();
    }

    /**
     * Loops the queue.
     */
    private void loop() {
        switch (scheduler.looping) {
            case NONE, TRACK -> {
                scheduler.looping = LoopCommand.looping.QUEUE;
                hook.editOriginal("The queue has been set to loop.").queue();
            }
            case QUEUE -> {
                scheduler.looping = LoopCommand.looping.NONE;
                hook.editOriginal("Looping has been turned off.").queue();
            }
        }
    }

    /**
     * Clears the queue.
     */
    private void clear() {
        for (TrackChannelTypeRecord record : queue) {
            if (record.trackType() == TrackType.TTS) {
                TTSCommand.removeTTSMessage(record.track());
            }
        }
        queue.clear();
        scheduler.looping = LoopCommand.looping.NONE;
        scheduler.currentTrack = null;
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
            if (queue.isEmpty()) {
                scheduler.looping = LoopCommand.looping.NONE;
            }

            if (scheduler.looping == LoopCommand.looping.TRACK) {
                scheduler.looping = LoopCommand.looping.NONE;
            }
            scheduler.nextTrack();
            if (currentTrack.trackType() == TrackType.TTS) {
                TTSCommand.removeTTSMessage(currentTrack.track());
            }
        } else {
            int count = 1;
            Iterator<TrackChannelTypeRecord> iterator = queue.iterator();
            TrackChannelTypeRecord currentTrack = null;
            while (iterator.hasNext()) {
                if (count == position) {
                    if (currentTrack.trackType() == TrackType.TTS) {
                        TTSCommand.removeTTSMessage(currentTrack.track());
                    }
                    iterator.remove();
                }
                count++;
                currentTrack = iterator.next();
            }
            if (count == position) {
                if (currentTrack.trackType() == TrackType.TTS) {
                    TTSCommand.removeTTSMessage(currentTrack.track());
                }
                iterator.remove();
            }
        }

        hook.editOriginal("Removed track at position **" + position + "**.").queue();
    }

    /**
     * Formats the track info.
     * @param index the index of the track
     * @param record the track record
     * @return the formatted track info
     */
    private String formatTrackInfo(int index, TrackChannelTypeRecord record) {
        AudioTrack track = record.track();
        AudioTrackInfo info = track.getInfo();
        String timeLeft = String.format("[%s]\n", (index == 1 ? TimeFormat.formatTime(track.getDuration() - track.getPosition()) + " left" + (scheduler.looping == LoopCommand.looping.TRACK ? " - looping" : "") : TimeFormat.formatTime(track.getDuration())));
        String trackDetails = "";
        switch (record.trackType()) {
            case TRACK, FILE -> trackDetails = String.format("**%d**. [%s](%s) by **%s** %s", index, info.title, info.uri, info.author, timeLeft);
            case TTS -> trackDetails = String.format("**%d**. TTS Message: \"%s\" %s", index, info.title, timeLeft);
        }
        return trackDetails;
    }

    @Override
    public String getName() {
        return "queue";
    }
}