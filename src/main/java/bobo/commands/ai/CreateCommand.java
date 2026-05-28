package bobo.commands.ai;

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

public class CreateCommand extends AAICommand {
    /**
     * Creates a new image command.
     */
    public CreateCommand() {
        super(Commands.slash("create", "Uses OpenAI to generate an image of the given prompt.")
                .addOption(OptionType.STRING, "prompt", "Image to generate", true));
    }

    @Override
    protected CommandResponse handleAICommand() {
        return CommandResponse.text("This command is temporarily disabled due to an error");
        /*
        String prompt;
        try {
            prompt = getMultiwordOptionValue("prompt", 0);
        } catch (RuntimeException e) {
            return CommandResponse.text("Invalid usage. Use `/help create` for more information.");
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
                return CommandResponse.text("Failed to generate image.");
            }

            imageUrl = images.getFirst()
                    .url()
                    .orElse(null);
        } catch (OpenAIException e) {
            return CommandResponse.text("Failed to generate image: " + e.getMessage());
        }

        if (imageUrl == null) {
            return CommandResponse.text("Failed to generate image.");
        }

        Member member = getMember();
        MessageEmbed embed = new EmbedBuilder()
                .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                .setTitle(prompt.substring(0, Math.min(prompt.length(), 256)))
                .setColor(Color.red)
                .setImage(imageUrl)
                .build();
        return CommandResponse.embed(embed);
         */
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getHelp() {
        return super.getHelp() + " " + """
                AI generates an image of the given prompt.
                Usage: `/create <prompt>`""";
    }

    @Override
    protected List<Permission> getAICommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_ATTACH_FILES));
    }

    @Override
    public Boolean isHidden() {
        return false;
    }
}