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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtistCommand extends AbstractLastFM {
    /**
     * Creates a new artist command.
     */
    public ArtistCommand() {
        super(Commands.slash("artist", "Gets information about a given artist or last played artist on Last.fm")
                .addOption(OptionType.STRING, "artist", "The artist to get information of (no input defaults to last played artist)", false)
        );
    }

    @Override
    public String getName() {
        return "artist";
    }

    @Override
    protected void handleLastFMCommand() {
        String username = getUserName(event.getUser().getId());
        assert username != null;
        Member member = event.getMember();
        assert member != null;

        String artistName;
        OptionMapping artistOption = event.getOption("artist");
        if (artistOption == null) {
            // Get the last played artist with a GET request to user.getRecentTracks
            String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "user.getRecentTracks", "user", username, "limit", "1")), false);
            if (response == null) {
                hook.editOriginal("An error occurred while getting your last played artist.").queue();
                return;
            }

            JSONObject responseObject = new JSONObject(response);
            JSONObject trackObject = responseObject.getJSONObject("recenttracks")
                    .getJSONArray("track")
                    .getJSONObject(0);
            artistName = trackObject.getJSONObject("artist")
                    .getString("#text");
        } else {
            // Search the artist information with a GET request to artist.search
            String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "artist.search", "artist", artistOption.getAsString(), "limit", "1")), false);
            if (response == null) {
                hook.editOriginal("An error occurred while searching for the artist.").queue();
                return;
            }

            JSONObject responseObject = new JSONObject(response);
            if (responseObject.getJSONObject("results").getInt("opensearch:totalResults") == 0) {
                hook.editOriginal("No results found for the artist.").queue();
                return;
            }

            artistName = responseObject.getJSONObject("results")
                    .getJSONObject("artistmatches")
                    .getJSONArray("artist")
                    .getJSONObject(0)
                    .getString("name");
        }

        // Get the artist's image with a GET request to artist.getInfo
        String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "artist.getInfo", "artist", artistName, "username", username)), false);
        if (response == null) {
            hook.editOriginal("An error occurred while getting the artist's information.").queue();
            return;
        }

        // Parse the artist's information
        JSONObject responseObject = new JSONObject(response);
        JSONObject artistObject;
        try {
            artistObject = responseObject.getJSONObject("artist");
        } catch (JSONException e) {
            hook.editOriginal("No results found for the artist. This usually happens for recently released artists. Check back later.").queue();
            return;
        }
        String artistUrl = artistObject.getString("url");
        String artistImage = null;
        try {
            JSONArray imageArray = artistObject.getJSONArray("image");
            artistImage = imageArray.getJSONObject(imageArray.length() - 1).getString("#text");
        } catch (Exception ignored) {}
        JSONObject statsObject = artistObject.getJSONObject("stats");
        int artistListeners = Integer.parseInt(statsObject.getString("listeners"));
        int artistPlayCount = Integer.parseInt(statsObject.getString("playcount"));
        int userPlayCount = Integer.parseInt(statsObject.getString("userplaycount"));
        String artistSummary = null;
        try { // Artist summary is not guaranteed to be present
            artistSummary = artistObject.getJSONObject("bio").getString("summary").replaceAll("<[^>]*>.*", ""); // Remove HTML tags and everything after
        } catch (Exception ignored) {}

        // Get more information about the artist from the MusicBrainz API
        String artistDescription = "";
        try {
            String artistMbid = artistObject.getString("mbid");
            String artistInfo = MusicBrainzAPI.getArtistInfo(artistMbid);
            String artistBorn = null, artistCountry = null, artistType, artistGender;
            if (artistInfo != null) {
                JSONObject artistInfoObject = new JSONObject(artistInfo);
                try {
                    artistBorn = createDiscordTimestamp(artistInfoObject.getJSONObject("life-span").getString("begin"));
                } catch (JSONException ignored) {}
                try {
                    artistCountry = artistInfoObject.getJSONObject("area").getString("name");
                } catch (JSONException ignored) {}
                artistType = artistInfoObject.getString("type");
                artistGender = artistInfoObject.getString("gender");
                if (artistBorn != null && !artistBorn.isBlank()) {
                    artistDescription += "Born on " + artistBorn + "\n";
                }
                if (artistCountry != null && !artistCountry.isBlank()) {
                    artistDescription += artistCountry + "\n";
                }
                if (artistType != null && !artistType.isBlank() && artistGender != null && !artistGender.isBlank()) {
                    artistDescription += artistType + " - " + artistGender;
                } else if (artistType != null && !artistType.isBlank()) {
                    artistDescription += artistType;
                } else if (artistGender != null && !artistGender.isBlank()) {
                    artistDescription += artistGender;
                }
            }
        } catch (Exception ignored) {}

        // Send the artist's information in an embed
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                .setTitle(artistName, artistUrl)
                .setColor(Color.RED)
                .addField("Stats", backQuotes(String.valueOf(artistListeners)) + " listeners\n" + backQuotes(String.valueOf(artistPlayCount)) + " global play" + (artistPlayCount == 1 ? "" : "s") + "\n" + backQuotes(String.valueOf(userPlayCount)) + " play" + (userPlayCount == 1 ? "" : "s") + " by you", true);
        if (artistImage != null && !artistImage.isBlank()) {
            embed.setThumbnail(artistImage);
        }
        if (!artistDescription.isBlank()) {
            embed.setDescription(artistDescription);
        }
        if (artistSummary != null && !artistSummary.isBlank()) {
            embed.addField("Summary", artistSummary, false);
        }

        hook.editOriginalEmbeds(embed.build()).queue();
    }

    @Override
    public String getHelp() {
        return super.getHelp() + """
                Get information about a given artist or the last played artist on Last.fm.
                Usage: `/artist <artist>`
                No input defaults to last played artist.""";
    }

    @Override
    protected List<Permission> getLastFMCommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_ATTACH_FILES));
    }

    @Override
    public Boolean shouldBeEphemeral() {
        return false;
    }
}