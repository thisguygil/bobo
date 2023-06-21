package bobo.command.commands.general;

import bobo.Bobo;
import bobo.command.ICommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetQuoteCommand implements ICommand {
    private static final List<Message> allMessages = new ArrayList<>();

    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        loadQuotes();

        int randomIndex = (int) (Math.random() * allMessages.size());
        Message randomMessage = allMessages.get(randomIndex);
        String messageContent = spoileredQuote(randomMessage.getContentDisplay());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        String time = randomMessage.getTimeCreated().format(formatter);

        event.reply(messageContent + "\n" + time).queue();
    }

    /**
     * Compiles all messages from #boquafiquotes channel into an ArrayList
     * Keeps ArrayList as static for faster access in subsequent command calls
     */
    public static void loadQuotes() {
        JDA jda = Bobo.getJDA();
        TextChannel channel = jda.getTextChannelById("826951218135826463");
        assert channel != null;
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

    /**
     * Encapsulates a quote's speaker by "||", which spoilers the text in Discord
     *
     * @param quote the quote to be spoilered
     * @return the spoilered quote
     */
    private static String spoileredQuote(String quote) {
        String regex = "(\".*\")\\s*-\\s*(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(quote);
        StringBuilder formattedQuote = new StringBuilder();
        String speaker;
        while (matcher.find()) {
            speaker = matcher.group(2);
            matcher.appendReplacement(formattedQuote, "$1\n-||" + speaker + "||");
        }
        matcher.appendTail(formattedQuote);
        return formattedQuote.toString();
    }

    @Override
    public String getName() {
        return "get-quote";
    }

}
