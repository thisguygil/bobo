package bobo.command.commands;

import bobo.Bobo;
import bobo.command.ICommand;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SetActivityCommand implements ICommand {
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        String activity = Objects.requireNonNull(event.getOption("activity")).getAsString();
        event.getJDA().getPresence().setActivity(Activity.streaming(activity, "https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        event.reply("Activity set to **" + activity + "**").queue();
    }

    @Override
    public String getName() {
        return "set-activity";
    }
}
