package bobo.commands.lastfm;

import bobo.commands.CommandResponse;
import bobo.utils.api_clients.SQLConnection;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FMLogoutCommand extends ALastFMCommand {
    private static final Logger logger = LoggerFactory.getLogger(FMLogoutCommand.class);

    private static final String deleteTokenSQL = "DELETE FROM lastfmtokens WHERE user_id = ?";

    /**
     * Creates a new fmlogout command.
     */
    public FMLogoutCommand() {
        super(Commands.slash("fmlogout", "Logout of Last.fm"));
    }

    @Override
    public String getName() {
        return "fmlogout";
    }

    @Override
    protected CommandResponse handleLastFMCommand() {
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

    @Override
    public String getHelp() {
        return super.getHelp() + """
                Log out of Last.fm.
                Usage: `/fmlogout`
                Note: You'll need to use this and then log in again if you deauthorize the bot or change your username.""";
    }

    @Override
    public Boolean shouldBeInvisible() {
        return true;
    }
}
