package bobo.commands.ai;

import bobo.Config;
import bobo.commands.AbstractCommand;
import com.theokanning.openai.service.OpenAiService;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.time.Duration;

public abstract class AbstractAI extends AbstractCommand {
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
}