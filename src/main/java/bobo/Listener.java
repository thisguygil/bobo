package bobo;

import bobo.commands.ai.ChatCommand;
import bobo.commands.general.RandomCommand;
import bobo.commands.owner.SetActivityCommand;
import bobo.commands.voice.JoinCommand;
import bobo.commands.voice.music.QueueCommand;
import bobo.commands.voice.music.SearchCommand;
import bobo.commands.voice.music.TTSCommand;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.AudioReceiveListener;
import bobo.utils.SQLConnection;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
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
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.List;

import static bobo.commands.AbstractMessageCommand.PREFIX;
import static bobo.commands.admin.ConfigCommand.*;

public class Listener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(Listener.class);

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
        User author = event.getAuthor();

        // Ignore bot, system, and webhook messages
        if (author.isBot() || author.isSystem() || event.getMessage().isWebhookMessage()) {
            return;
        }

        if (event.isFromType(ChannelType.PRIVATE)) {
            GuildChannel channel = Bobo.getJDA().getGuildChannelById(Long.parseLong(Config.get("DM_LOG_CHANNEL_ID")));
            if (channel == null) {
                return;
            }

            Message originalMessage = event.getMessage();
            MessageCreateBuilder message = new MessageCreateBuilder()
                    .addContent("**Message from " + author.getAsMention() + "**\n");

            String messageContent = originalMessage.getContentDisplay();
            if (!messageContent.isEmpty()) {
                message.addContent(messageContent + "\n");
            }

            originalMessage.getAttachments().forEach(attachment -> message.addContent(attachment.getUrl() + "\n"));

            List<MessageEmbed> embeds = originalMessage.getEmbeds();
            if (!embeds.isEmpty()) {
                message.setEmbeds(embeds);
            }

            ((GuildMessageChannel) channel).sendMessage(message.build()).queue();
            logger.info("DM Message from {} logged.", author.getName());
        } else if (event.isFromType(ChannelType.GUILD_PRIVATE_THREAD)) {
            ChatCommand.handleThreadMessage(event);
        }

        if (event.getMessage().getContentRaw().startsWith(PREFIX)) {
            manager.handle(event);
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

        if (event.getEntity().equals(guild.getSelfMember())) { // Verify that the bot is the one that the event is about
            if (channelJoined != null) { // Joined an audio channel
                if (channelLeft != null) { // Moved to a different audio channel, so don't do anything
                    return;
                }

                guild.getAudioManager()
                        .setReceivingHandler(
                                new AudioReceiveListener(1)
                        );
            }

            if (channelLeft != null) { // Left an audio channel
                QueueCommand.clearQueue(
                        guild,
                        PlayerManager.getInstance()
                                .getMusicManager(guild).scheduler
                );

                TTSCommand.removeGuild(guild);

                guild.getAudioManager()
                        .setReceivingHandler(null);
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
            logger.error("Failed to delete guild from tables.", e);
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
     * Sets the bot's activity, loads quotes and clips, and joins voice channels that the bot was in before it was previously shut down
     * <br>
     * Prints to console when all tasks are complete
     *
     * @param event the ready event
     */
    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        SetActivityCommand.setActivity();

        RandomCommand.loadQuotesMap();
        RandomCommand.loadClipsMap();

        JoinCommand.joinShutdown();

        logger.info("Bobo is ready!");
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