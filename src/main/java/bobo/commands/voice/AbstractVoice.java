package bobo.commands.voice;

import bobo.commands.AbstractCommand;

public abstract class AbstractVoice extends AbstractCommand {
    @Override
    public void handleCommand() {
        handleVoiceCommand();
    }

    /**
     * Handles the voice command.
     */
    protected abstract void handleVoiceCommand();
}
