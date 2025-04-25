package bobo.commands.ai;

import bobo.Config;
import bobo.commands.CommandResponse;
import com.openai.errors.OpenAIException;
import com.openai.models.images.Image;
import com.openai.models.images.ImageGenerateParams;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ImageCommand extends AAICommand {
    private static final String IMAGE_MODEL = Config.get("IMAGE_MODEL");

    /**
     * Creates a new image command.
     */
    public ImageCommand() {
        super(Commands.slash("image", "Uses OpenAI (DALL-E 3) to generate an image of the given prompt.")
                .addOption(OptionType.STRING, "prompt", "Image to generate", true));
    }

    @Override
    protected CommandResponse handleAICommand() {
        String prompt;
        try {
            prompt = getMultiwordOptionValue("prompt", 0);
        } catch (RuntimeException e) {
            return new CommandResponse("Invalid usage. Use `/help image` for more information.");
        }

        ImageGenerateParams createImageRequest = ImageGenerateParams.builder()
                .model(IMAGE_MODEL)
                .prompt(prompt)
                .build();

        List<Image> images;
        String imageUrl;
        try {
            images = openAI.images()
                    .generate(createImageRequest)
                    .data()
                    .orElse(null);

            if (images == null) {
                return new CommandResponse("Failed to generate image.");
            }

            imageUrl = images.getFirst()
                    .url()
                    .orElse(null);
        } catch (OpenAIException e) {
            return new CommandResponse("Failed to generate image: " + e.getMessage());
        }

        if (imageUrl == null) {
            return new CommandResponse("Failed to generate image.");
        }

        Member member = getMember();
        MessageEmbed embed = new EmbedBuilder()
                .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                .setTitle(prompt.substring(0, Math.min(prompt.length(), 256)))
                .setColor(Color.red)
                .setImage(imageUrl)
                .build();
        return new CommandResponse(embed);

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

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}