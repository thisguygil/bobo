package bobo.command.commands;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import okhttp3.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Objects;

public class AICommand implements ICommand {
    private static final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String prompt = Objects.requireNonNull(event.getOption("prompt")).getAsString();
        String response = "**" + prompt + "**";
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
                .url("http://localhost:5000/")
                .post(formBody)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new RuntimeException("Unexpected code " + response);
            assert response.body() != null;
            return response.body().string();
        }
    }

    @Override
    public String getName() {
        return "ai";
    }

    @Override
    public String getHelp() {
        return """
                `/ai`
                Uses OpenAI to generate a response to the given prompt
                Usage: `/ai <prompt>`""";
    }
}
