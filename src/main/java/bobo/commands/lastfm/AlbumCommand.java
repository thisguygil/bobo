package bobo.commands.lastfm;

import bobo.utils.api_clients.LastfmAPI;
import bobo.utils.api_clients.MusicBrainzAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class AlbumCommand extends AbstractLastFM {
    /**
     * Creates a new artist command.
     */
    public AlbumCommand() {
        super(Commands.slash("album", "Gets information about a given album or last played album on Last.fm")
                .addOption(OptionType.STRING, "album", "The album to get information of (no input defaults to last played album)", false)
        );
    }

    @Override
    public String getName() {
        return "album";
    }

    @Override
    protected void handleLastFMCommand() {
        event.deferReply().queue();

        String username = getUserName(event.getUser().getId());
        assert username != null;
        Member member = event.getMember();
        assert member != null;

        String albumName, artistName;
        OptionMapping albumOption = event.getOption("album");
        if (albumOption == null) {
            // Get the last played artist with a GET request to user.getRecentTracks
            String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "user.getRecentTracks", "user", username, "limit", "1")), false);
            if (response == null) {
                hook.editOriginal("An error occurred while getting your last played album.").queue();
                return;
            }

            JSONObject responseObject = new JSONObject(response);
            JSONObject trackObject = responseObject.getJSONObject("recenttracks")
                    .getJSONArray("track")
                    .getJSONObject(0);
            albumName = trackObject.getJSONObject("album")
                    .getString("#text");
            artistName = trackObject.getJSONObject("artist")
                    .getString("#text");
        } else {
            // Search the artist information with a GET request to album.search
            String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "album.search", "album", albumOption.getAsString(), "limit", "1")), false);
            if (response == null) {
                hook.editOriginal("An error occurred while searching for the album.").queue();
                return;
            }

            JSONObject responseObject = new JSONObject(response);
            if (responseObject.getJSONObject("results").getInt("opensearch:totalResults") == 0) {
                hook.editOriginal("No results found for the album.").queue();
                return;
            }

            JSONObject albumObject = responseObject.getJSONObject("results")
                    .getJSONObject("albummatches")
                    .getJSONArray("album")
                    .getJSONObject(0);
            albumName = albumObject.getString("name");
            artistName = albumObject.getString("artist");
        }

        // Get the album's information with a GET request to album.getInfo
        String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "album.getInfo", "album", albumName, "artist", artistName, "username", username)), false);
        if (response == null) {
            hook.editOriginal("An error occurred while getting the album's information.").queue();
            return;
        }

        // Parse the album's information
        JSONObject responseObject = new JSONObject(response);
        JSONObject albumObject;
        try {
            albumObject = responseObject.getJSONObject("album");
        } catch (JSONException e) {
            hook.editOriginal("No results found for the album. This usually happens for recently released albums. Check back later.").queue();
            return;
        }
        String albumUrl = albumObject.getString("url");
        String albumImage = null;
        try {
            JSONArray imageArray = albumObject.getJSONArray("image");
            albumImage = imageArray.getJSONObject(imageArray.length() - 1).getString("#text");
        } catch (Exception ignored) {}
        int albumListeners = Integer.parseInt(albumObject.getString("listeners"));
        int albumPlayCount = Integer.parseInt(albumObject.getString("playcount"));
        int userPlayCount = albumObject.getInt("userplaycount");
        String albumSummary = null;
        try { // Album summary is not guaranteed to be present
            albumSummary = albumObject.getJSONObject("wiki").getString("summary").replaceAll("<[^>]*>.*", ""); // Remove HTML tags and everything after
        } catch (Exception ignored) {}

        // Get the album's release date from MusicBrainz API
        String releaseDate = null;
        try {
            String albumMbid = albumObject.getString("mbid");
            String albumInfo = MusicBrainzAPI.getAlbumInfo(albumMbid);
            if (albumInfo != null) {
                JSONObject albumInfoObject = new JSONObject(albumInfo);
                releaseDate = createDiscordTimestamp(albumInfoObject.getString("date"));
            }
        } catch (JSONException ignored) {}

        // Send the album's information in an embed
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                .setTitle(albumName + " by " + artistName, albumUrl)
                .setColor(Color.RED)
                .addField("Stats", backQuotes(String.valueOf(albumListeners)) + " listeners\n" + backQuotes(String.valueOf(albumPlayCount)) + " global play" + (albumPlayCount == 1 ? "" : "s") + "\n" + backQuotes(String.valueOf(userPlayCount)) + " play" + (userPlayCount == 1 ? "" : "s") + " by you", true);
        if (albumImage != null && !albumImage.isBlank()) {
            embed.setThumbnail(albumImage);
        }
        if (releaseDate != null && !releaseDate.isBlank()) {
            embed.setDescription("Released on " + releaseDate);
        }
        if (albumSummary != null && !albumSummary.isBlank()) {
            embed.addField("Summary", albumSummary, false);
        }

        hook.editOriginalEmbeds(embed.build()).queue();
    }

    @Override
    public String getHelp() {
        return super.getHelp() + """
                Get information about a given album or the last played album on Last.fm.
                Usage: `/album <album>`
                No input defaults to last played album.""";
    }

    @Override
    protected List<Permission> getLastFMCommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_ATTACH_FILES));
    }
}