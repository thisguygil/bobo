package bobo.commands.admin;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Objects;

public class SayCommand extends AbstractAdmin {
    /**
     * Creates a new say command.
     */
    public SayCommand() {
        super(Commands.slash("say", "Make Bobo say what you tell it to.")
                .addOption(OptionType.STRING, "content", "What Bobo should say", true));
    }

    @Override
    protected void handleAdminCommand() {
        event.getChannel().sendMessage(Objects.requireNonNull(event.getOption("content")).getAsString()).queue();
        hook.editOriginal("Message sent").queue();
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
