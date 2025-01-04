package bobo.commands.lastfm;

import bobo.commands.CommandResponse;
import bobo.utils.api_clients.LastfmAPI;
import bobo.utils.api_clients.MusicBrainzAPI;
import bobo.utils.TimeFormat;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackCommand extends ALastFMCommand {
    /**
     * Creates a new track command.
     */
    public TrackCommand() {
        super(Commands.slash("track", "Gets information about a given track or last played track on Last.fm")
                .addOption(OptionType.STRING, "track", "The track to get information of (no input defaults to last played track)", false)
        );
    }

    @Override
    public String getName() {
        return "track";
    }

    @Override
    protected CommandResponse handleLastFMCommand() {
        String username = getUserName(getUser().getId());
        if (username == null) { // Should never happen, but just in case
            return new CommandResponse("You are not logged in to Last.fm. Use `/fmlogin` to log in.");
        }

        Member member = getMember();

        String trackOption, trackName, artistName;
        try {
            trackOption = getMultiwordOptionValue("track", 0);
        } catch (RuntimeException ignored) {
            trackOption = null;
        }
        
        if (trackOption == null) {
            // Get the last played track with a GET request to user.getRecentTracks
            String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "user.getRecentTracks", "user", username, "limit", "1")), false);
            if (response == null) {
                return new CommandResponse("An error occurred while getting your last played track.");
            }

            JSONObject responseObject = new JSONObject(response);
            JSONObject trackObject = responseObject.getJSONObject("recenttracks")
                    .getJSONArray("track")
                    .getJSONObject(0);
            trackName = trackObject.getString("name");
            artistName = trackObject.getJSONObject("artist")
                    .getString("#text");
        } else {
            // Search the track information with a GET request to track.search
            String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "track.search", "track", trackOption, "limit", "1")), false);
            if (response == null) {
                return new CommandResponse("An error occurred while searching for the track.");
            }

            JSONObject responseObject = new JSONObject(response);
            if (responseObject.getJSONObject("results").getInt("opensearch:totalResults") == 0) {
                return new CommandResponse("No results found for the track.");
            }

            // Parse the track's name and artist
            JSONObject trackObject = responseObject.getJSONObject("results")
                    .getJSONObject("trackmatches")
                    .getJSONArray("track")
                    .getJSONObject(0);
            trackName = trackObject.getString("name");
            artistName = trackObject.getString("artist");
        }

        // Get the track's information with a GET request to track.getInfo
        String trackResponse = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "track.getInfo", "track", trackName, "artist", artistName, "username", username)), false);
        if (trackResponse == null) {
            return new CommandResponse("An error occurred while getting the track's information.");
        }

        // Parse the track's information
        JSONObject responseObject = new JSONObject(trackResponse);
        JSONObject trackObject;
        try {
            trackObject = responseObject.getJSONObject("track");
        } catch (JSONException e) {
            return new CommandResponse("No results found for the track. This usually happens for recently released tracks. Check back later.");
        }
        String trackUrl = trackObject.getString("url");
        String trackImage = null;
        String albumName = null;
        try {
            JSONObject trackAlbumObject = trackObject.getJSONObject("album");
            albumName = trackAlbumObject.getString("title");
            JSONArray imageArray = trackAlbumObject.getJSONArray("image");
            trackImage = imageArray.getJSONObject(imageArray.length() - 1).getString("#text");
        } catch (Exception ignored) {}
        String trackDuration = trackObject.getString("duration");
        int trackListeners = Integer.parseInt(trackObject.getString("listeners"));
        int trackPlayCount = Integer.parseInt(trackObject.getString("playcount"));
        int userPlayCount = Integer.parseInt(trackObject.getString("userplaycount"));
        String trackSummary = null;
        try { // Track summary is not guaranteed to be present
            trackSummary = trackObject.getJSONObject("wiki").getString("summary").replaceAll("<[^>]*>.*", ""); // Remove HTML tags and everything after
        } catch (Exception ignored) {}

        // Get the track's release date from MusicBrainz API. First need to get the mbid from the album with a GET request to album.getInfo
        String releaseDate = null;
        if (albumName != null) {
            String albumResponse = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "album.getInfo", "album", albumName, "artist", artistName, "username", username)), false);
            if (albumResponse != null) {
                try {
                    JSONObject albumObject = new JSONObject(albumResponse);
                    String albumMbid = albumObject.getString("mbid");
                    String albumInfo = MusicBrainzAPI.getAlbumInfo(albumMbid);
                    if (albumInfo != null) {
                        JSONObject albumInfoObject = new JSONObject(albumInfo);
                        releaseDate = createDiscordTimestamp(albumInfoObject.getString("date"));
                    }
                } catch (JSONException ignored) {}
            }
        }

        // Send the track's information in an embed
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                .setTitle(trackName + " by " + artistName, trackUrl)
                .setColor(Color.RED)
                .addField("Stats", backQuotes(String.valueOf(trackListeners)) + " listeners\n" + backQuotes(String.valueOf(trackPlayCount)) + " global play" + (trackPlayCount == 1 ? "" : "s") + "\n" + backQuotes(String.valueOf(userPlayCount)) + " play" + (userPlayCount == 1 ? "" : "s") + " by you", true);
        if (trackImage != null && !trackImage.isBlank()) {
            embed.setThumbnail(trackImage);
        }
        if (!trackDuration.equals("0")) {
            embed.addField("Duration", backQuotes(TimeFormat.formatTime(Long.parseLong(trackDuration))), true);
        }
        if (releaseDate != null && !releaseDate.isBlank()) {
            embed.setDescription("Released on " + releaseDate);
        }
        if (trackSummary != null && !trackSummary.isBlank()) {
            embed.addField("Summary", trackSummary, false);
        }

        return new CommandResponse(embed.build());
    }

    @Override
    public String getHelp() {
        return super.getHelp() + """
                Gets information about a given track or last played track on Last.fm
                "Usage: `/track <track>`
                No input defaults to last played track.""";
    }

    @Override
    protected List<Permission> getLastFMCommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_ATTACH_FILES));
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}