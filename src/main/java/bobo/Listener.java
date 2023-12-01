package bobo;

import bobo.commands.ai.ChatCommand;
import bobo.commands.ai.TTSCommand;
import bobo.commands.general.GetQuoteCommand;
import bobo.commands.voice.music.SearchCommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.TrackChannelTypeRecord;
import bobo.lavaplayer.TrackScheduler;
import bobo.utils.TrackType;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.concurrent.BlockingQueue;

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
     * Sends all thread messages to the AI
     *
     * @param event the message event
     */
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            TextChannel channel = Bobo.getJDA().getTextChannelById(Long.parseLong(Config.get("DM_LOG_CHANNEL_ID")));
            MessageCreateData message = new MessageCreateBuilder()
                    .addContent("**" + event.getAuthor().getGlobalName() + "**\n" + event.getMessage().getContentDisplay())
                    .addEmbeds(event.getMessage().getEmbeds())
                    .build();
            assert channel != null;
            channel.sendMessage(message).queue();
        } else if (event.isFromType(ChannelType.GUILD_PRIVATE_THREAD)) {
            ChatCommand.handleThreadMessage(event);
        }
    }

    /**
     * Handles thread deletion
     *
     * @param event the channel delete event
     */
    @Override
    public void onChannelDelete(@Nonnull ChannelDeleteEvent event) {
        if (event.getChannel().getType() == ChannelType.GUILD_PRIVATE_THREAD) {
            ChatCommand.handleThreadDelete(event);
        }
    }

    /**
     * Clears queue and resets audio listener if bot disconnects
     *
     * @param event the voice update event
     */
    @Override
    public void onGuildVoiceUpdate(@Nonnull GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        if (event.getEntity().equals(guild.getSelfMember()) && event.getChannelLeft() != null) {
            if (event.getChannelJoined() != null) {
                return;
            }

            final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);
            final AudioPlayer player = musicManager.player;
            final TrackScheduler scheduler = musicManager.scheduler;
            final AudioManager audioManager = guild.getAudioManager();
            final BlockingQueue<TrackChannelTypeRecord> queue = scheduler.queue;

            for (TrackChannelTypeRecord record : queue) {
                if (record.trackType() == TrackType.TTS) {
                    File file = new File(record.track().getInfo().uri);
                    if (file.exists() && !file.delete()) {
                        System.err.println("Failed to delete TTS file: " + file.getName());
                    }
                    TTSCommand.removeTTSMessage(file.getName());
                }
            }
            audioManager.setReceivingHandler(null);
            queue.clear();
            scheduler.looping = false;
            player.stopTrack();
            player.setPaused(false);
        }
    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        if (event.retrieveUser().complete().isBot()) {
            return;
        }

        SearchCommand.handleReaction(event);
    }

    /**
     * Loads quotes on bot startup
     * Prints "Bobo is ready!" to console when finally ready
     *
     * @param event the ready event
     */
    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        GetQuoteCommand.loadMap();
        System.out.println("Bobo is ready!");
    }
}