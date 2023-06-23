package bobo.commands.general;

import bobo.commands.AbstractCommand;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class AbstractGeneral extends AbstractCommand {
    /**
     * Creates a new general command.
     *
     * @param commandData The command data.
     */
    public AbstractGeneral(CommandData commandData) {
        super(commandData);
    }

    @Override
    public void handleCommand() {
        handleGeneralCommand();
    }

    /**
     * Handles the general command.
     */
    protected abstract void handleGeneralCommand();
}
