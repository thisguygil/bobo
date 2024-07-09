package bobo.commands.ai;

import bobo.Config;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.image.CreateImageRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ImageCommand extends AbstractAI {
    private static final String IMAGE_MODEL = Config.get("IMAGE_MODEL");

    /**
     * Creates a new image command.
     */
    public ImageCommand() {
        super(Commands.slash("image", "Uses OpenAI (DALL-E 3) to generate an image of the given prompt.")
                .addOption(OptionType.STRING, "prompt", "Image to generate", true));
    }

    @Override
    protected void handleAICommand() {
        event.deferReply().queue();
        var currentHook = hook;
        String prompt = Objects.requireNonNull(event.getOption("prompt")).getAsString();

        CreateImageRequest createImageRequest = CreateImageRequest.builder()
                .model(IMAGE_MODEL)
                .prompt(prompt)
                .build();

        CompletableFuture.supplyAsync(() -> {
            try {
                return service.createImage(createImageRequest)
                        .getData()
                        .get(0)
                        .getUrl();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(imageUrl -> {
            Member member = event.getMember();
            assert member != null;
            MessageEmbed embed = new EmbedBuilder()
                    .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                    .setTitle(prompt.substring(0, Math.min(prompt.length(), 256)))
                    .setColor(Color.red)
                    .setImage(imageUrl)
                    .build();
            currentHook.editOriginalEmbeds(embed).queue();
        }).exceptionally(e -> {
            Throwable cause = e.getCause();
            if (cause instanceof OpenAiHttpException exception) {
                if (exception.statusCode == 429) {
                    currentHook.editOriginal("DALL-E 3 rate limit reached. Please try again in a few seconds.").queue();
                } else {
                    currentHook.editOriginal(cause.getMessage()).queue();
                    e.printStackTrace();
                }
            } else {
                currentHook.editOriginal(cause.getMessage()).queue();
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public String getName() {
        return "image";
    }

    @Override
    public String getHelp() {
        return super.getHelp() + " " + """
                Generates an image of the given prompt.
                Usage: `/image <prompt>`""";
    }

    @Override
    protected List<Permission> getAICommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_ATTACH_FILES));
    }
}