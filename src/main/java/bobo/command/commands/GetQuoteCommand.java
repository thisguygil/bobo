package bobo.command.commands;

import bobo.command.ICommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class GetQuoteCommand implements ICommand {
    private static final List<Message> allMessages = new ArrayList<>();

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        loadMessages(event.getJDA());
        int randomIndex = (int) (Math.random() * allMessages.size());
        Message randomMessage = allMessages.get(randomIndex);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        String finalMessage = randomMessage.getContentDisplay() + "\n" + randomMessage.getTimeCreated().format(formatter);
        event.getHook().editOriginal(finalMessage).queue();
    }

    /**
     * Compiles all messages from #boquafiquotes channel into allMessages ArrayList
     *
     * @param jda the JDA being accessed
     */
    public void loadMessages(JDA jda) {
        TextChannel channel = jda.getTextChannelById("826951218135826463");
        for (Message message : channel.getIterableHistory()) {
            try {
                if (!allMessages.contains(message)) {
                    if (message.getContentDisplay().contains("\"")) {
                        allMessages.add(message);
                    }
                } else {
                    break;
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public String getName() {
        return "getquote";
    }

    @Override
    public String getHelp() {
        return "`/getquote`\n" +
                "Sends a random quote from #boquafiquotes";
    }
}
