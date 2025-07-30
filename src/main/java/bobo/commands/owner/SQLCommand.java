package bobo.commands.owner;

import bobo.commands.CommandResponse;
import bobo.utils.api_clients.SQLConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class SQLCommand extends AOwnerCommand {
    /**
     * Creates a new sql command.
     */
    public SQLCommand() {}

    @Override
    public String getName() {
        return "sql";
    }

    @Override
    protected CommandResponse handleOwnerCommand() {
        String statementStr;
        try {
            statementStr = getMultiwordOptionValue(0);
        } catch (RuntimeException e) {
            return CommandResponse.text("Please provide a statement to execute.");
        }

        try (Connection connection = SQLConnection.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                if (isUpdateQuery(statementStr)) {
                    int affectedRows = statement.executeUpdate(statementStr);
                    return CommandResponse.text("Query executed successfully. Rows affected: " + affectedRows);
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
                        return CommandResponse.text("The result is too long to display.");
                    } else {
                        return CommandResponse.text(result.toString());
                    }
                }
            }
        } catch (Exception e) {
            return CommandResponse.text("Error executing statement: " + e.getMessage());
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

    @Override
    public Boolean isHidden() {
        return false;
    }
}
