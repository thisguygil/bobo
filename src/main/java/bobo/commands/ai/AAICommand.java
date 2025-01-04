package bobo.commands.ai;

import bobo.Config;
import bobo.commands.ADualCommand;
import bobo.commands.CommandResponse;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.ArrayList;
import java.util.List;

public abstract class AAICommand extends ADualCommand {
    protected static final OpenAIClient openAI = OpenAIOkHttpClient.builder()
            .apiKey(Config.get("OPENAI_API_KEY"))
            .build();

    /**
     * Creates a new AI command.
     *
     * @param commandData The command data.
     */
    public AAICommand(CommandData commandData) {
        super(commandData);
    }

    @Override
    protected CommandResponse handleCommand() {
        return handleAICommand();
    }

    /**
     * Handles the AI command.
     */
    protected abstract CommandResponse handleAICommand();

    @Override
    public String getHelp() {
        return "AI command.";
    }

    @Override
    protected List<Permission> getCommandPermissions() {
        List<Permission> permissions = getAICommandPermissions();
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        return permissions;
    }

    /**
     * Gets the permissions required for the AI command.
     *
     * @return The permissions required for the AI command.
     */
    protected List<Permission> getAICommandPermissions() {
        return new ArrayList<>();
    }
}