package bobo.commands.general;

import bobo.Config;
import bobo.commands.CommandResponse;
import bobo.utils.TimeFormat;
import bobo.utils.api_clients.LastfmAPI;
import bobo.utils.api_clients.MusicBrainzAPI;
import bobo.utils.api_clients.SQLConnection;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static bobo.utils.StringUtils.createDiscordTimestamp;
import static bobo.utils.StringUtils.markdownCode;

public class FMCommand extends AGeneralCommand {
    private static final Logger logger = LoggerFactory.getLogger(FMCommand.class);

    private static final String API_KEY = Config.get("LASTFM_API_KEY");

    private static final String createTableSQL = "CREATE TABLE IF NOT EXISTS lastfmlogins (user_id VARCHAR(255) PRIMARY KEY, session_key VARCHAR(255) NOT NULL, lastfm_username VARCHAR(255) NOT NULL)";
    private static final String insertKeySQL = "INSERT INTO lastfmlogins (user_id, session_key, lastfm_username) VALUES (?, ?, ?)";
    private static final String selectKeySQL = "SELECT session_key, lastfm_username FROM lastfmlogins WHERE user_id = ?";
    private static final String removeTokenSQL = "DELETE FROM lastfmtokens WHERE user_id = ?";
    private static final String createTokenSQL = "CREATE TABLE IF NOT EXISTS lastfmtokens (user_id VARCHAR(255) PRIMARY KEY, token VARCHAR(255) NOT NULL)";
    private static final String insertTokenSQL = "INSERT INTO lastfmtokens (user_id, token) VALUES (?, ?) ON DUPLICATE KEY UPDATE token = ?";
    private static final String deleteTokenSQL = "DELETE FROM lastfmtokens WHERE user_id = ?";
    private static final String selectUsernameSQL = "SELECT lastfm_username FROM lastfmlogins WHERE user_id = ?";
    private static final String selectTokenSQL = "SELECT token FROM lastfmtokens WHERE user_id = ?";

    /**
     * Creates a new fm command.
     */
    public FMCommand() {
        super(Commands.slash("fm", "Last.fm commands")
                .addSubcommands(
                        new SubcommandData("auth", "Log in or out of Last.fm")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "action", "The action to perform", true)
                                                .addChoices(
                                                        new Command.Choice("Log in", "login"),
                                                        new Command.Choice("Log out", "logout")
                                                )
                                ),
                        new SubcommandData("info", "Get information about a track, album, or artist (no input defaults to last played)")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "type", "The type of information", true)
                                                .addChoices(
                                                        new Command.Choice("Track", "track"),
                                                        new Command.Choice("Album", "album"),
                                                        new Command.Choice("Artist", "artist")
                                                ),
                                        new OptionData(OptionType.STRING, "name", "The name of the item", false)
                                )
                )
        );
    }

    @Override
    protected CommandResponse handleGeneralCommand() {
        String subcommand;
        try {
            subcommand = getSubcommandName(0);
        } catch (RuntimeException e) {
            return new CommandResponse("Invalid usage. Use `/help fm` for more information.");
        }

        return switch (subcommand) {
            case "auth" -> {
                if (source == CommandSource.SLASH_COMMAND) {
                    slashEvent.deferReply(true).queue();
                }

                String action;
                try {
                    action = getOptionValue("action", 1);
                } catch (RuntimeException e) {
                    yield new CommandResponse("Invalid usage. Use `/help fm` for more information.");
                }

                yield switch (action) {
                    case "login" -> login();
                    case "logout" -> logout();
                    default -> new CommandResponse("Invalid usage. Use `/help fm` for more information.");
                };
            }
            case "info" -> {
                if (!validateLogin()) {
                    yield new CommandResponse("You are not logged in to Last.fm. Use `/fm auth login` to log in.");
                }

                switch (source) {
                    case SLASH_COMMAND -> slashEvent.deferReply().queue();
                    case MESSAGE_COMMAND -> messageEvent.getChannel().sendTyping().queue();
                }

                String type;
                try {
                    type = getOptionValue("type", 1);
                } catch (RuntimeException e) {
                    yield new CommandResponse("Invalid usage. Use `/help fm` for more information.");
                }

                String option;
                try {
                    option = getMultiwordOptionValue("name", 2);
                } catch (RuntimeException e) {
                    option = null;
                }

                yield switch (type) {
                    case "track" -> track(option);
                    case "album" ->  album(option);
                    case "artist" -> artist(option);
                    default -> new CommandResponse("Invalid usage. Use `/help fm` for more information.");
                };
            }
            default -> new CommandResponse("Invalid usage. Use `/help fm` for more information.");
        };
    }

    /**
     * Logs the user in to Last.fm.
     *
     * @return The command response.
     */
    private CommandResponse login() {
        String userId = getUser().getId();

        // Make sure user isn't already logged in
        if (getUserName(userId) != null) {
            return new CommandResponse("You are already logged in to Last.fm.");
        }

        // Send GET request to get token
        String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "auth.getToken")), false);
        if (response == null) {
            return new CommandResponse("An error occurred while getting the token.");
        }

        JSONObject responseObject = new JSONObject(response);
        String token = responseObject.getString("token");

        // Create (if not exists) and insert into the table the map of the user id to its token
        try (Connection connection = SQLConnection.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(createTokenSQL);
            }

            try (PreparedStatement statement = connection.prepareStatement(insertTokenSQL)) {
                statement.setString(1, userId);
                statement.setString(2, token);
                statement.setString(3, token);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Failed to insert token for user {}", userId);
        }

        // Send link back to Discord to log in
        return new CommandResponse("Log in to Last.fm [here](" + "https://www.last.fm/api/auth?api_key=" + API_KEY + "&token=" + token + ").");

        // TODO: Add a callback to ensure the user actually logs in after running the command, where if they don't log in within a certain period of time, the token is deleted. For now, we assume the user logs in.
    }

    private CommandResponse logout() {
        String userId = getUser().getId();

        try (Connection connection = SQLConnection.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(deleteTokenSQL)) {
                preparedStatement.setString(1, userId);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("An error occurred while logging out of Last.fm. for user {}", userId, e);
            return new CommandResponse("An error occurred while logging out of Last.fm.");
        }

        return new CommandResponse("Successfully logged out of Last.fm.");
    }

    private CommandResponse track(String option) {
        String username = getUserName(getUser().getId());
        if (username == null) { // Should never happen, but just in case
            return new CommandResponse("You are not logged in to Last.fm. Use `/fmlogin` to log in.");
        }

        Member member = getMember();
        String trackName, artistName;
        if (option == null) {
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
            String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "track.search", "track", option, "limit", "1")), false);
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
                .addField("Stats", markdownCode(String.valueOf(trackListeners)) + " listeners\n" + markdownCode(String.valueOf(trackPlayCount)) + " global play" + (trackPlayCount == 1 ? "" : "s") + "\n" + markdownCode(String.valueOf(userPlayCount)) + " play" + (userPlayCount == 1 ? "" : "s") + " by you", true);
        if (trackImage != null && !trackImage.isBlank()) {
            embed.setThumbnail(trackImage);
        }
        if (!trackDuration.equals("0")) {
            embed.addField("Duration", markdownCode(TimeFormat.formatTime(Long.parseLong(trackDuration))), true);
        }
        if (releaseDate != null && !releaseDate.isBlank()) {
            embed.setDescription("Released on " + releaseDate);
        }
        if (trackSummary != null && !trackSummary.isBlank()) {
            embed.addField("Summary", trackSummary, false);
        }

        return new CommandResponse(embed.build());
    }

    private CommandResponse album(String option) {
        String username = getUserName(getUser().getId());
        if (username == null) { // Should never happen, but just in case
            return new CommandResponse("You are not logged in to Last.fm. Use `/fmlogin` to log in.");
        }

        Member member = getMember();
        String albumName, artistName;
        if (option == null) {
            // Get the last played artist with a GET request to user.getRecentTracks
            String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "user.getRecentTracks", "user", username, "limit", "1")), false);
            if (response == null) {
                return new CommandResponse("An error occurred while getting your last played album.");
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
            String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "album.search", "album", option, "limit", "1")), false);
            if (response == null) {
                return new CommandResponse("An error occurred while searching for the album.");
            }

            JSONObject responseObject = new JSONObject(response);
            if (responseObject.getJSONObject("results").getInt("opensearch:totalResults") == 0) {
                return new CommandResponse("No results found for the album.");
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
            return new CommandResponse("An error occurred while getting the album's information.");
        }

        // Parse the album's information
        JSONObject responseObject = new JSONObject(response);
        JSONObject albumObject;
        try {
            albumObject = responseObject.getJSONObject("album");
        } catch (JSONException e) {
            return new CommandResponse("No results found for the album. This usually happens for recently released albums. Check back later.");
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
                .addField("Stats", markdownCode(String.valueOf(albumListeners)) + " listeners\n" + markdownCode(String.valueOf(albumPlayCount)) + " global play" + (albumPlayCount == 1 ? "" : "s") + "\n" + markdownCode(String.valueOf(userPlayCount)) + " play" + (userPlayCount == 1 ? "" : "s") + " by you", true);
        if (albumImage != null && !albumImage.isBlank()) {
            embed.setThumbnail(albumImage);
        }
        if (releaseDate != null && !releaseDate.isBlank()) {
            embed.setDescription("Released on " + releaseDate);
        }
        if (albumSummary != null && !albumSummary.isBlank()) {
            embed.addField("Summary", albumSummary, false);
        }

        return new CommandResponse(embed.build());
    }

    private CommandResponse artist(String option) {
        String username = getUserName(getUser().getId());
        if (username == null) { // Should never happen, but just in case
            return new CommandResponse("You are not logged in to Last.fm. Use `/fmlogin` to log in.");
        }

        Member member = getMember();
        String artistName;
        if (option == null) {
            // Get the last played artist with a GET request to user.getRecentTracks
            String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "user.getRecentTracks", "user", username, "limit", "1")), false);
            if (response == null) {
                return new CommandResponse("An error occurred while getting your last played artist.");
            }

            JSONObject responseObject = new JSONObject(response);
            JSONObject trackObject = responseObject.getJSONObject("recenttracks")
                    .getJSONArray("track")
                    .getJSONObject(0);
            artistName = trackObject.getJSONObject("artist")
                    .getString("#text");
        } else {
            // Search the artist information with a GET request to artist.search
            String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "artist.search", "artist", option, "limit", "1")), false);
            if (response == null) {
                return new CommandResponse("An error occurred while searching for the artist.");
            }

            JSONObject responseObject = new JSONObject(response);
            if (responseObject.getJSONObject("results").getInt("opensearch:totalResults") == 0) {
                return new CommandResponse("No results found for the artist.");
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
            return new CommandResponse("An error occurred while getting the artist's information.");
        }

        // Parse the artist's information
        JSONObject responseObject = new JSONObject(response);
        JSONObject artistObject;
        try {
            artistObject = responseObject.getJSONObject("artist");
        } catch (JSONException e) {
            return new CommandResponse("No results found for the artist. This usually happens for recently released artists. Check back later.");
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
                .addField("Stats", markdownCode(String.valueOf(artistListeners)) + " listeners\n" + markdownCode(String.valueOf(artistPlayCount)) + " global play" + (artistPlayCount == 1 ? "" : "s") + "\n" + markdownCode(String.valueOf(userPlayCount)) + " play" + (userPlayCount == 1 ? "" : "s") + " by you", true);
        if (artistImage != null && !artistImage.isBlank()) {
            embed.setThumbnail(artistImage);
        }
        if (!artistDescription.isBlank()) {
            embed.setDescription(artistDescription);
        }
        if (artistSummary != null && !artistSummary.isBlank()) {
            embed.addField("Summary", artistSummary, false);
        }

        return new CommandResponse(embed.build());
    }

    /**
     * Validates the user's login to Last.fm.
     * Should not be called if the user is using login or logout subcommands.
     * (Login for obvious reasons, and logout in case there's an error where the user needs to reset their login.)
     *
     * @return Whether the user is logged in to Last.fm.
     */
    private boolean validateLogin() {
        String userId = getUser().getId();

        // Check if the session key and username are already stored in the database
        try (Connection connection = SQLConnection.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectKeySQL)) {
                preparedStatement.setString(1, userId);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to check if user {} is logged in to Last.fm", userId);
        }

        // Check if the user's one-use token is stored in the database
        Map<String, String> sessionKeyAndUsername = getSessionKeyAndUsernameFromToken(userId);
        if (sessionKeyAndUsername == null) {
            return false;
        }

        // Store the session key and username in the database
        try (Connection connection = SQLConnection.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(createTableSQL);
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(insertKeySQL)) {
                preparedStatement.setString(1, userId);
                preparedStatement.setString(2, sessionKeyAndUsername.get("sessionKey"));
                preparedStatement.setString(3, sessionKeyAndUsername.get("username"));
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Failed to store Last.fm session key and username for user {}", userId);
        }

        // Remove the token from the database
        try (Connection connection = SQLConnection.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(removeTokenSQL)) {
                preparedStatement.setString(1, userId);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Failed to remove Last.fm token for user {}", userId);
        }

        return true;
    }

    /**
     * Gets the Last.fm username of the user.
     *
     * @param userId The user id.
     * @return The Last.fm username, or null if the user is not logged in.
     */
    @Nullable
    private static String getUserName(String userId) {
        String username = null;
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectUsernameSQL)) {
            statement.setString(1, userId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                username = resultSet.getString("lastfm_username");
            }
        } catch (SQLException e) {
            logger.error("Failed to get Last.fm username for user {}", userId);
        }

        return username;
    }

    /**
     * Gets the session key and username from the token.
     *
     * @param userId The user id.
     * @return The session key and username, or null if the user is not logged in.
     */
    @Nullable
    public static Map<String, String> getSessionKeyAndUsernameFromToken(String userId) {
        String token = getToken(userId);
        if (token == null) {
            return null;
        }

        String response = LastfmAPI.sendGetRequest(new HashMap<>(Map.of("method", "auth.getSession", "token", token)), true);
        if (response == null) {
            return null;
        }

        // Parse XML response because Last.fm's API is annoying and doesn't allow JSON for this request
        Map<String, String> sessionInfo = new HashMap<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(response.getBytes()));
            document.getDocumentElement().normalize();

            // Check for an error in the response
            Element root = document.getDocumentElement();
            String status = root.getAttribute("status");
            if ("failed".equals(status)) {
                return null;
            }

            // If no error, proceed to parse the session info
            NodeList sessionList = document.getElementsByTagName("session");
            for (int i = 0; i < sessionList.getLength(); i++) {
                Node node = sessionList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String username = element.getElementsByTagName("name").item(0).getTextContent();
                    String sessionKey = element.getElementsByTagName("key").item(0).getTextContent();

                    sessionInfo.put("username", username);
                    sessionInfo.put("sessionKey", sessionKey);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse Last.fm session info for user {}", userId);
            return null;
        }

        return sessionInfo;
    }

    /**
     * Gets the token of the user.
     *
     * @param userId The user id.
     * @return The token, or null if the user is not logged in.
     */
    private static String getToken(String userId) {
        String token = null;
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectTokenSQL)) {
            statement.setString(1, userId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                token = resultSet.getString("token");
            }
        } catch (SQLException e) {
            logger.error("Failed to get Last.fm token for user {}", userId);
        }

        return token;
    }

    @Nullable
    @Override
    public Boolean shouldBeInvisible() {
        return null; // Ephemeral for auth subcommands, non-ephemeral for everything else
    }

    @Override
    public String getName() {
        return "fm";
    }

    @Override
    public String getHelp() {
        return """
                Get your Last.fm information.
                Usage: `/fm <subcommand>`
                Subcommands: **Note all non-auth subcommands require you to be logged in to Last.fm.**
                * `auth` - Log in or out of Last.fm.
                    * `login` - Log in
                    * `logout` - Log out
                * `info` - Get information about a track, album, or artist (no input defaults to last played).
                    * `track` - Get information about a track.
                    * `album` - Get information about an album.
                    * `artist` - Get information about an artist.""";
    }

    @Override
    protected List<Permission> getGeneralCommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_EMBED_LINKS));
    }
}
