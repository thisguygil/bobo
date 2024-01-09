package bobo.commands.general;

import bobo.utils.FortniteAPI;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
        var currentHook = hook;
        String subcommandName = Objects.requireNonNull(event.getSubcommandName());

        switch (subcommandName) {
            case "shop" -> processShopCommand(currentHook);
            case "map" -> processMapCommand(currentHook);
        }
    }

    /**
     * Processes the shop command.
     * @param currentHook The current interaction hook.
     */
    private void processShopCommand(InteractionHook currentHook) {
        CompletableFuture.supplyAsync(() -> convertBufferedImageToFile(FortniteAPI.getShopImage(), "shop"))
                .thenAccept(file -> handleFileResponse(file, "Failed to get shop image.", currentHook))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    currentHook.editOriginal("Error processing shop command.").queue();
                    return null;
                });
    }

    /**
     * Processes the map command.
     * @param currentHook The current interaction hook.
     */
    private void processMapCommand(InteractionHook currentHook) {
        CompletableFuture.supplyAsync(() -> convertBufferedImageToFile(FortniteAPI.getMapImage(), "map"))
                .thenAccept(file -> handleFileResponse(file, "Failed to get map image.", currentHook))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    currentHook.editOriginal("Error processing map command.").queue();
                    return null;
                });
    }

    /**
     * Handles the response for a file.
     * @param file The file to send.
     * @param errorMessage The error message to send if the file is null.
     * @param currentHook The current interaction hook.
     */
    private void handleFileResponse(File file, String errorMessage, InteractionHook currentHook) {
        if (file != null) {
            currentHook.editOriginalAttachments(FileUpload.fromData(file)).queue(success -> {
                if (!file.delete()) {
                    System.err.println("Failed to delete file: " + file.getAbsolutePath());
                }
            });
        } else {
            currentHook.editOriginal(errorMessage).queue();
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