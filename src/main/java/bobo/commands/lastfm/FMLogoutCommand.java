package bobo.commands.lastfm;

import bobo.utils.SQLConnection;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FMLogoutCommand extends AbstractLastFM {
    private static final String deleteTokenSQL = "DELETE FROM lastfmtokens WHERE user_id = ?";

    /**
     * Creates a new fmlogout command.
     */
    public FMLogoutCommand() {
        super(Commands.slash("fmlogin", "Login to Last.fm"));
    }

    @Override
    public String getName() {
        return "fmlogout";
    }

    @Override
    protected void handleLastFMCommand() {
        event.deferReply().setEphemeral(true).queue();

        String userId = event.getUser().getId();

        try (Connection connection = SQLConnection.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(deleteTokenSQL)) {
                preparedStatement.setString(1, userId);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            hook.editOriginal("An error occurred while logging out of Last.fm.").queue();
            return;
        }

        hook.editOriginal("Successfully logged out of Last.fm.").queue();
    }

    @Override
    public String getHelp() {
        return super.getHelp() + """
                Log out of Last.fm.
                Usage: `/fmlogout`
                Note: You'll need to use this and then log in again if you deauthorize the bot or change your username.""";
    }
}
