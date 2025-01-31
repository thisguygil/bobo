package bobo.commands.lastfm;

import bobo.commands.CommandResponse;
import bobo.utils.api_clients.LastfmAPI;
import bobo.utils.api_clients.SQLConnection;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class FMLoginCommand extends ALastFMCommand {
    private static final Logger logger = LoggerFactory.getLogger(FMLoginCommand.class);

    private static final String createTokenSQL = "CREATE TABLE IF NOT EXISTS lastfmtokens (user_id VARCHAR(255) PRIMARY KEY, token VARCHAR(255) NOT NULL)";
    private static final String insertTokenSQL = "INSERT INTO lastfmtokens (user_id, token) VALUES (?, ?) ON DUPLICATE KEY UPDATE token = ?";
    private static final String selectSessionKeySQL = "SELECT session_key FROM lastfmlogins WHERE user_id = ?";
    private static final String selectTokenSQL = "SELECT token FROM lastfmtokens WHERE user_id = ?";

    /**
     * Creates a new fmlogin command.
     */
    public FMLoginCommand() {
        super(Commands.slash("fmlogin", "Login to Last.fm"));
    }

    @Override
    public String getName() {
        return "fmlogin";
    }

    @Override
    protected CommandResponse handleLastFMCommand() {
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

        // TODO: Add some sort of listener/callback to ensure the user actually logs in after running the command, where if they don't log in within a certain amount of time, the token is deleted from the database. For now, we will just assume the user logs in.
    }

    /**
     * Gets the session key of the user.
     *
     * @param userId The user id.
     * @return The session key, or null if the user is not logged in.
     */
    private static String getSessionKey(String userId) {
        String sessionKey = null;
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSessionKeySQL)) {
            statement.setString(1, userId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                sessionKey = resultSet.getString("session_key");
            }
        } catch (SQLException e) {
            logger.error("Failed to get Last.fm session key for user {}", userId);
        }

        return sessionKey;
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

    @Override
    public String getHelp() {
        return """
                Log in to Last.fm.
                Usage: `/fmlogin`
                Note: You must complete the login process on the Last.fm website after running this command.""";
    }

    @Override
    public Boolean shouldBeInvisible() {
        return true;
    }
}