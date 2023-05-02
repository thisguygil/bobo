package bobo.command.commands;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import okhttp3.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ChatCommand implements ICommand {
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connection timeout to 60 seconds
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String prompt = Objects.requireNonNull(event.getOption("prompt")).getAsString();
        String response = "**" + prompt + "**\n";
        try {
            response += generate(prompt);
            event.getHook().editOriginal(response).queue();
        } catch (IOException e) {
            event.getHook().editOriginal("An error occurred while generating the response.").queue();
            e.printStackTrace();
        }
    }

    private String generate(String prompt) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("prompt", prompt)
                .build();
        Request request = new Request.Builder()
                .url("http://localhost:5000/chat")
                .post(formBody)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new RuntimeException(String.valueOf(response));
            assert response.body() != null;
            return response.body().string();
        }
    }

    @Override
    public String getName() {
        return "chat";
    }

    @Override
    public String getHelp() {
        return """
                `/chat`
                Uses OpenAI to generate a response to the given prompt
                Usage: `/chat <prompt>`""";
    }
}
