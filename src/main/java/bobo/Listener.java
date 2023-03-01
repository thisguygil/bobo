package bobo;

import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.lavaplayer.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
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
                    .applyMessage(event.getMessage())
                    .addEmbeds(event.getMessage().getEmbeds())
                    .build();
            channel.sendMessage("**" + event.getAuthor().getAsTag() + "**").queue();
            channel.sendMessage(message).queue();
        }
    }

    /**
     * Clears queue if bot disconnects
     *
     * @param event the voice update event
     */
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        AudioChannelUnion channel = event.getChannelLeft();
        if (event.getEntity().equals(event.getJDA().getSelfUser()) && channel != null) {
            final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(channel.getGuild());
            final AudioPlayer audioPlayer = musicManager.audioPlayer;
            final TrackScheduler scheduler = musicManager.scheduler;
            scheduler.queue.clear();
            scheduler.looping = false;
            audioPlayer.stopTrack();
            audioPlayer.setPaused(false);
            musicManager.removeEventListener();
        }
    }
}
