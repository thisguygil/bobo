package bobo;

import bobo.commands.ai.ChatCommand;
import bobo.commands.general.RandomCommand;
import bobo.commands.voice.JoinCommand;
import bobo.commands.voice.music.LoopCommand;
import bobo.commands.voice.music.SearchCommand;
import bobo.commands.voice.music.TTSCommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.AudioReceiveListener;
import bobo.utils.SQLConnection;
import bobo.utils.TrackRecord;
import bobo.lavaplayer.TrackScheduler;
import bobo.utils.TrackType;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
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
import java.sql.*;
import java.util.concurrent.BlockingQueue;

import static bobo.commands.admin.ConfigCommand.*;

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
     * <br>
     * Sends all thread messages to the AI
     *
     * @param event the message event
     */
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            GuildChannel channel = Bobo.getJDA().getGuildChannelById(Long.parseLong(Config.get("DM_LOG_CHANNEL_ID")));
            MessageCreateData message = new MessageCreateBuilder()
                    .addContent("**" + event.getAuthor().getGlobalName() + "**\n" + event.getMessage().getContentDisplay())
                    .addEmbeds(event.getMessage().getEmbeds())
                    .build();
            assert channel != null;
            ((GuildMessageChannel) channel).sendMessage(message).queue();
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
        AudioChannelUnion channelJoined = event.getChannelJoined();
        AudioChannelUnion channelLeft = event.getChannelLeft();

        if (event.getEntity().equals(guild.getSelfMember())) {
            // Bot is the one who joined or left a voice channel
            if (channelJoined != null) {
                // Bot joined a voice channel
                if (channelLeft != null) {
                    // Moved to a different voice channel, so don't do anything
                    return;
                }

                guild.getAudioManager().setReceivingHandler(new AudioReceiveListener(1));
            }

            if (channelLeft != null) {
                // Bot left a voice channel
                final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);
                final AudioPlayer player = musicManager.player;
                final TrackScheduler scheduler = musicManager.scheduler;
                final AudioManager audioManager = guild.getAudioManager();
                final BlockingQueue<TrackRecord> queue = scheduler.queue;

                for (TrackRecord record : queue) {
                    if (record.trackType() == TrackType.TTS) {
                        TTSCommand.removeTTSMessage(record.track());
                    }
                }
                audioManager.setReceivingHandler(null);
                queue.clear();
                scheduler.looping = LoopCommand.looping.NONE;
                scheduler.currentTrack = null;
                player.stopTrack();
                player.setPaused(false);
            }
        }
    }

    /**
     * Deletes the guild from all tables when the bot leaves or is kicked
     *
     * @param event the guild leave event
     */
    @Override
    public void onGuildLeave(@Nonnull GuildLeaveEvent event) {
        String guildId = event.getGuild().getId();

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement1 = connection.prepareStatement(resetClipsSQL);
             PreparedStatement statement2 = connection.prepareStatement(resetQuotesSQL);
             PreparedStatement statement3 = connection.prepareStatement(resetFortniteShopSQL)) {
            statement1.setString(1, guildId);
            statement1.executeUpdate();

            statement2.setString(1, guildId);
            statement2.executeUpdate();

            statement3.setString(1, guildId);
            statement3.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles reaction events for the search command
     *
     * @param event the message reaction add event
     */
    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        if (event.retrieveUser().complete().isBot()) {
            return;
        }

        SearchCommand.handleReaction(event);
    }

    /**
     * Loads quotes on bot startup
     * <br>
     * Prints "Bobo is ready!" to console when finally ready
     *
     * @param event the ready event
     */
    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        RandomCommand.loadQuotesMap();
        RandomCommand.loadClipsMap();

        // Join voice channels that the bot was in before shutdown
        String createTableSQL = "CREATE TABLE IF NOT EXISTS voice_channels_shutdown (channel_id VARCHAR(255) NOT NULL)";
        String selectSQL = "SELECT channel_id FROM voice_channels_shutdown";
        try (Connection connection = SQLConnection.getConnection()) {
            try (Statement createStatement = connection.createStatement()) {
                createStatement.execute(createTableSQL);
            }

            try (PreparedStatement selectStatement = connection.prepareStatement(selectSQL);
                 ResultSet resultSet = selectStatement.executeQuery()) {

                while (resultSet.next()) {
                    String channelId = resultSet.getString("channel_id");
                    VoiceChannel channel = Bobo.getJDA().getVoiceChannelById(channelId);

                    if (channel != null) {
                        JoinCommand.join(channel);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Bobo is ready!");
    }

    /**
     * Gets the command manager
     *
     * @return the command manager
     */
    public CommandManager getManager() {
        return manager;
    }
}