package bobo.commands.voice.music;

public class PauseCommand extends AbstractMusic {
    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (currentTrack == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
            return;
        }

        if (!player.isPaused()) {
            player.setPaused(true);
            hook.editOriginal("Paused.").queue();
        } else {
            hook.editOriginal("The player is already paused. Use `/resume` to resume.").queue();
        }
    }

    @Override
    public String getName() {
        return "pause";
    }
}
