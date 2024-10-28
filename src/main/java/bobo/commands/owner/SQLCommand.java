package bobo.commands.owner;

import bobo.utils.api_clients.SQLConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class SQLCommand extends AbstractOwner {
    /**
     * Creates a new sql command.
     */
    public SQLCommand() {}

    @Override
    public String getName() {
        return "sql";
    }

    @Override
    protected void handleOwnerCommand() {
        if (args.length == 0) {
            event.getChannel().sendMessage("Invalid usage. Use `/help sql` for more information.").queue();
            return;
        }

        String statementStr = String.join(" ", args);
        try (Connection connection = SQLConnection.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                if (isUpdateQuery(statementStr)) {
                    int affectedRows = statement.executeUpdate(statementStr);
                    event.getChannel().sendMessage("Query executed successfully. Rows affected: " + affectedRows).queue();
                } else {
                    ResultSet resultSet = statement.executeQuery(statementStr);
                    StringBuilder result = new StringBuilder("Query Results:\n");

                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        result.append(metaData.getColumnName(i)).append("\t");
                    }
                    result.append("\n");

                    while (resultSet.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            result.append(resultSet.getString(i)).append("\t");
                        }
                        result.append("\n");
                        if (result.length() > 2000) {
                            break;
                        }
                    }

                    if (result.length() > 2000) {
                        event.getChannel().sendMessage("The result is too long to display.").queue();
                    } else {
                        event.getChannel().sendMessage(result.toString()).queue();
                    }
                }
            }
        } catch (Exception e) {
            event.getChannel().sendMessage("Error executing statement: " + e.getMessage()).queue();
        }
    }

    /**
     * Checks if the given statement is an update statement.
     *
     * @param statement The statement to check.
     * @return True if the statement is an update statement, false otherwise.
     */
    private boolean isUpdateQuery(String statement) {
        String statementLower = statement.toLowerCase();
        return statementLower.startsWith("insert") || statementLower.startsWith("update") || statementLower.startsWith("delete");
    }

    @Override
    public String getHelp() {
        return """
                Executes a given SQL statement.
                Usage: `""" + PREFIX + "sql <statement>`";
    }
}
