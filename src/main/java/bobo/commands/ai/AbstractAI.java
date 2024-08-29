package bobo.commands.ai;

import bobo.Config;
import bobo.commands.AbstractSlashCommand;
import com.theokanning.openai.service.OpenAiService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.time.Duration;
import java.util.List;

public abstract class AbstractAI extends AbstractSlashCommand {
    protected static final OpenAiService service = new OpenAiService(Config.get("OPENAI_API_KEY"), Duration.ZERO);

    /**
     * Creates a new AI command.
     *
     * @param commandData The command data.
     */
    public AbstractAI(CommandData commandData) {
        super(commandData);
    }

    @Override
    public void handleCommand() {
        handleAICommand();
    }

    /**
     * Handles the AI command.
     */
    protected abstract void handleAICommand();

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
    protected abstract List<Permission> getAICommandPermissions();
}