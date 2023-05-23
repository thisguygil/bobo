package bobo;

import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.lavaplayer.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import javax.annotation.Nonnull;

public class Listener extends ListenerAdapter {
    private final CommandManager manager = new CommandManager();

    /**
     * Sends slash commands to the command manager
     *
     * @param event the slash command event
     */
    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        manager.handle(event);
    }

    /**
     * Sends all DM messages to a private server
     *
     * @param event the message event
     */
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            TextChannel channel = event.getJDA().getTextChannelById("1080252409726644355");
            MessageCreateData message = new MessageCreateBuilder()
                    .addContent("**" + event.getAuthor().getAsTag() + "**\n" + event.getMessage().getContentDisplay())
                    .addEmbeds(event.getMessage().getEmbeds())
                    .build();
            assert channel != null;
            channel.sendMessage(message).queue();
        }
    }

    /**
     * Clears queue if bot disconnects
     *
     * @param event the voice update event
     */
    @Override
    public void onGuildVoiceUpdate(@Nonnull GuildVoiceUpdateEvent event) {
        if (event.getEntity().equals(event.getGuild().getSelfMember()) && event.getChannelLeft() != null) {
            final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
            final AudioPlayer player = musicManager.player;
            final TrackScheduler scheduler = musicManager.scheduler;
            scheduler.queue.clear();
            scheduler.looping = false;
            player.stopTrack();
            player.setPaused(false);
            musicManager.removeEventListener();
        }
    }
}
