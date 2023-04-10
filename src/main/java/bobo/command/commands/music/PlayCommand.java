package bobo.command.commands.music;

import bobo.command.ICommand;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.URLValidator;
import bobo.utils.YouTubeUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PlayCommand implements ICommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Member invoking command must be in a vc
        if (event.getMember().getVoiceState().getChannel() == null) {
            event.reply("You must be connected to a voice channel to use this command.").queue();
            return;
        }

        String track = event.getOption("track").getAsString();
        String trackURL;
        if (!URLValidator.isValidURL(track)) {
            try {
                trackURL = YouTubeUtil.searchForVideo(track);
            } catch (Exception e) {
                event.reply("Nothing found by **" + track + "**").queue();
                return;
            }
        } else {
            trackURL = track;
        }
        PlayerManager.getInstance().loadAndPlay(event, trackURL);
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getHelp() {
        return "`/play`\n" +
                "Joins the voice channel and plays given or searched YouTube link\n" +
                "Usage: `/play <YouTube link/query>`";
    }
}