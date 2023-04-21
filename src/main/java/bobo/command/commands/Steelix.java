package bobo.command.commands;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;

public class Steelix implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.reply("https://assets.pokemon.com/assets/cms2/img/pokedex/full/208.png").queue();
    }

    @Override
    public String getName() {
        return "steelix";
    }

    @Override
    public String getHelp() {
        return "`/steelix`\n" +
                "steelix";
    }
}
