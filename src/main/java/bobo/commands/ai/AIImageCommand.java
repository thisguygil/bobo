package bobo.commands.ai;

import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.image.CreateImageRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.util.Objects;

public class AIImageCommand extends AbstractAI {
    @Override
    protected void handleAICommand() {
        event.deferReply().queue();
        String prompt = Objects.requireNonNull(event.getOption("prompt")).getAsString();

        CreateImageRequest createImageRequest = CreateImageRequest.builder()
                .prompt(prompt)
                .build();

        String imageUrl;
        try {
            imageUrl = service.createImage(createImageRequest)
                    .getData()
                    .get(0)
                    .getUrl();
        } catch (OpenAiHttpException e) {
            hook.editOriginal(e.getMessage()).queue();
            return;
        }

        Member member = event.getMember();
        assert member != null;
        MessageEmbed embed = new EmbedBuilder()
                .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getAvatarUrl())
                .setTitle(prompt.substring(0, Math.min(prompt.length(), 256)))
                .setColor(Color.red)
                .setImage(imageUrl)
                .build();
        hook.editOriginalEmbeds(embed).queue();
    }

    @Override
    public String getName() {
        return "ai-image";
    }
}
