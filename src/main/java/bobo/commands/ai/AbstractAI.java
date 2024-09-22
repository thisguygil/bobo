package bobo.commands.ai;

import bobo.Config;
import bobo.commands.AbstractSlashCommand;
import io.github.sashirestela.openai.SimpleOpenAI;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;

public abstract class AbstractAI extends AbstractSlashCommand {
    protected static final SimpleOpenAI openAI = SimpleOpenAI.builder()
            .apiKey(Config.get("OPENAI_API_KEY"))
            .build();

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