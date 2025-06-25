package bobo.commands.admin;

import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class SayCommand extends AAdminCommand {
    /**
     * Creates a new say command.
     */
    public SayCommand() {
        super(Commands.slash("say", "Make Bobo say what you tell it to.")
                .addOption(OptionType.STRING, "content", "What Bobo should say", true));
    }

    @Override
    protected CommandResponse handleAdminCommand() {
        event.getChannel().sendMessage(getOptionValue("content")).queue();
        return CommandResponse.text("Message sent");
    }

    @Override
    public String getName() {
        return "say";
    }

    @Override
    public String getHelp() {
        return super.getHelp() + " " + """
                Make Bobo say what you tell it to.
                Usage: `/say <content>`""";
    }

    @Override
    public Boolean shouldBeEphemeral() {
        return true;
    }
}