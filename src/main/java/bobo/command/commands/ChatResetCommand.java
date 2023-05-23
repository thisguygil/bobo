package bobo.command.commands;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.annotation.Nonnull;

public class ChatResetCommand implements ICommand {
    private static final OkHttpClient httpClient = new OkHttpClient.Builder().build();

    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        Request request = new Request.Builder()
                .url("http://localhost:5000/reset")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new RuntimeException("Unexpected code " + response);
            assert response.body() != null;
            event.reply(response.body().string()).queue();
        } catch (Exception e) {
            event.reply(e.getMessage()).queue();
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "chat-reset";
    }

    @Override
    public String getHelp() {
        return "`/chat-reset`\n" +
                "Resets the current OpenAI chat conversation";
    }
}
