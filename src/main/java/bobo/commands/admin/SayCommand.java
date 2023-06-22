package bobo.commands.admin;

import java.util.Objects;

public class SayCommand extends AbstractAdmin {
    @Override
    protected void handleAdminCommand() {
        event.deferReply().setEphemeral(true).queue();

        event.getChannel().sendMessage(Objects.requireNonNull(event.getOption("content")).getAsString()).queue();
        hook.editOriginal("Message sent").queue();
    }

    @Override
    public String getName() {
        return "say";
    }
}
