package bobo.commands.voice;

import bobo.commands.AbstractCommand;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class AbstractVoice extends AbstractCommand {
    /**
     * Creates a new voice command.
     *
     * @param commandData The command data.
     */
    public AbstractVoice(CommandData commandData) {
        super(commandData);
    }

    @Override
    public void handleCommand() {
        handleVoiceCommand();
    }

    /**
     * Handles the voice command.
     */
    protected abstract void handleVoiceCommand();
}
