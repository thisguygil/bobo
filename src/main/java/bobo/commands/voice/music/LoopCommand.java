package bobo.commands.voice.music;

public class LoopCommand extends AbstractMusic {
    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (player.getPlayingTrack() == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
            return;
        }

        scheduler.looping = !scheduler.looping;
        hook.editOriginal("The player has been set to **" + (scheduler.looping ? "" : "not ") + "looping**.").queue();
    }

    @Override
    public String getName() {
        return "loop";
    }
}
