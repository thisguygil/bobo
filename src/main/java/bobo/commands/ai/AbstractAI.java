package bobo.commands.ai;

import bobo.Bobo;
import bobo.commands.AbstractCommand;
import com.theokanning.openai.service.OpenAiService;

public abstract class AbstractAI extends AbstractCommand {
    protected OpenAiService service;

    @Override
    public void handleCommand() {
        service = Bobo.getService();

        handleAICommand();
    }

    /**
     * Handles the AI command.
     */
    protected abstract void handleAICommand();
}
