package bobo.command.commands.general;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import javax.annotation.Nonnull;

public class SteelixCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        InteractionHook hook = event.getHook();

        hook.editOriginal("https://assets.pokemon.com/assets/cms2/img/pokedex/full/208.png").queue();
    }

    @Override
    public String getName() {
        return "steelix";
    }
}
