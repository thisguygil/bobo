package bobo.commands.general;

import bobo.utils.FortniteAPI;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class FortniteCommand extends AbstractGeneral {
    /**
     * Creates a new Fortnite command.
     */
    public FortniteCommand() {
        super(Commands.slash("fortnite", "Get info about Fortnite.")
                .addSubcommands(
                        new SubcommandData("shop", "Get an image of the current Fortnite Shop."),
                        new SubcommandData("map", "Get an image of the current Fortnite Map.")
                )
        );
    }

    @Override
    public String getName() {
        return "fortnite";
    }

    @Override
    protected void handleGeneralCommand() {
        event.deferReply().queue();

        String subcommandName = Objects.requireNonNull(event.getSubcommandName());
        switch (subcommandName) {
            case "shop" -> {
                File file = convertBufferedImageToFile(FortniteAPI.getShopImage(), "shop");
                if (file != null) {
                    hook.editOriginalAttachments(FileUpload.fromData(file)).queue(success -> {
                        if (!file.delete()) {
                            System.err.println("Failed to delete file: " + file.getAbsolutePath());
                        }
                    });
                } else {
                    hook.editOriginal("Failed to get shop image.").queue();
                }
            }
            case "map" -> {
                File file = convertBufferedImageToFile(FortniteAPI.getMapImage(), "map");
                if (file != null) {
                    hook.editOriginalAttachments(FileUpload.fromData(file)).queue(success -> {
                        if (!file.delete()) {
                            System.err.println("Failed to delete file: " + file.getAbsolutePath());
                        }
                    });
                } else {
                    hook.editOriginal("Failed to get map image.").queue();
                }
            }
        }
    }

    /**
     * Converts a {@link BufferedImage} to a {@link File}.
     *
     * @param image The image to convert.
     * @return The converted image.
     */
    @Nullable
    public static File convertBufferedImageToFile(BufferedImage image, String name) {
        try {
            File outputfile = File.createTempFile(name, ".jpg");
            ImageIO.write(image, "jpg", outputfile);
            return outputfile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}