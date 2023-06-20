package bobo.command.commands;

import bobo.Bobo;
import bobo.command.ICommand;
import bobo.utils.URLValidator;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.service.OpenAiService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;

public class AIImageCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        OpenAiService service = Bobo.getService();
        String prompt = Objects.requireNonNull(event.getOption("prompt")).getAsString();
        CreateImageRequest createImageRequest = CreateImageRequest.builder()
                .prompt(prompt)
                .build();
        String imageUrl = service.createImage(createImageRequest).getData().get(0).getUrl();
        if (!URLValidator.isValidURL(imageUrl)) {
            event.getHook().editOriginal("**" + prompt + "**\n" + imageUrl).queue();
            return;
        }

        Member member = event.getMember();
        assert member != null;
        MessageEmbed embed = new EmbedBuilder()
                .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getAvatarUrl())
                .setTitle(prompt)
                .setColor(Color.red)
                .setImage(imageUrl)
                .build();
        event.getHook().editOriginalEmbeds(embed).queue();
    }

    @Override
    public String getName() {
        return "ai-image";
    }
}
