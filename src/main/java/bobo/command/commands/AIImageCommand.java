package bobo.command.commands;

import bobo.command.Command;
import bobo.utils.URLValidator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import okhttp3.*;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AIImageCommand implements Command {
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String prompt = Objects.requireNonNull(event.getOption("prompt")).getAsString();
        Member member = event.getMember();

        try {
            String imageUrl = generate(prompt);
            if (!URLValidator.isValidURL(imageUrl)) {
                event.getHook().editOriginal("**" + prompt + "**\n" + imageUrl).queue();
                return;
            }
            assert member != null;
            MessageEmbed embed = new EmbedBuilder()
                    .setAuthor(member.getUser().getAsTag(), "https://discord.com/users/" + member.getId(), member.getAvatarUrl())
                    .setTitle(prompt)
                    .setColor(Color.red)
                    .setImage(imageUrl)
                    .build();
            event.getHook().editOriginalEmbeds(embed).queue();
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
                .url("http://localhost:5000/image")
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
        return "ai-image";
    }

    @Override
    public String getHelp() {
        return """
                `/ai-image`
                Uses OpenAI to generate an image of the given prompt
                Usage: `/ai-image <prompt>`""";
    }
}
