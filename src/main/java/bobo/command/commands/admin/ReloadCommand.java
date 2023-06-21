package bobo.command.commands.admin;

import bobo.Bobo;
import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;

public class ReloadCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.reply("Reloading...").queue();
        event.getJDA().shutdown();
        Bobo.main(null);

    }

    @Override
    public String getName() {
        return "reload";
    }
}