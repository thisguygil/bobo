package bobo.command.commands.voice.music;

import bobo.command.ICommand;
import bobo.command.commands.voice.JoinCommand;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.YouTubeUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.apache.commons.validator.routines.UrlValidator;

import javax.annotation.Nonnull;
import java.util.Objects;

public class PlayCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        InteractionHook hook = event.getHook();

        JoinCommand.join(event);

        String track = Objects.requireNonNull(event.getOption("track")).getAsString();
        String trackURL;
        if ((new UrlValidator()).isValid(track)) {
            trackURL = track;
        } else {
            try {
                trackURL = YouTubeUtil.searchForVideo(track);
            } catch (Exception e) {
                hook.editOriginal("Nothing found by **" + track + "**").queue();
                e.printStackTrace();
                return;
            }
        }

        PlayerManager.getInstance().loadAndPlay(event, trackURL);
    }

    @Override
    public String getName() {
        return "play";
    }
}