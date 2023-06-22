package bobo.commands.general;

import bobo.commands.AbstractCommand;

public abstract class AbstractGeneral extends AbstractCommand {
    @Override
    public void handleCommand() {
        handleGeneralCommand();
    }

    /**
     * Handles the general command.
     */
    protected abstract void handleGeneralCommand();
}
