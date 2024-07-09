package bobo.commands.general;

import bobo.utils.FortniteAPI;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class FortniteCommand extends AbstractGeneral {
    /**
     * Creates a new Fortnite command.
     */
    public FortniteCommand() {
        super(Commands.slash("fortnite", "Get info about Fortnite.")
                .addSubcommands(
                        new SubcommandData("shop", "Get the current Fortnite Item Shop."),
                        new SubcommandData("news", "Get the current Fortnite (Battle Royale) news."),
                        new SubcommandData("stats", "Get stats for a Fortnite player.")
                                .addOption(OptionType.STRING, "username", "The Epic Games username of the player.", true),
                        new SubcommandData("map", "Get the current Fortnite Map.")
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
            case "news" -> processNewsCommand(currentHook);
            case "stats" -> processStatsCommand(currentHook);
            case "map" -> processMapCommand(currentHook);
        }
    }

    /**
     * Processes the shop command.
     * @param currentHook The current interaction hook.
     */
    private void processShopCommand(InteractionHook currentHook) {
        // Attach the current date as a header.
        ZonedDateTime nowInUTC = ZonedDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        final String message = "# Fortnite Item Shop" + "\n" + "## " + nowInUTC.format(formatter);

        // Get the shop image and send it.
        CompletableFuture.supplyAsync(() -> convertBufferedImageToFile(FortniteAPI.getShopImage(), "shop"))
                .thenAccept(file -> handleFileResponse(file, message, "Failed to get shop image.", currentHook))
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
        final String message = "# Fortnite Map";

        // Get the map image and send it.
        CompletableFuture.supplyAsync(() -> convertBufferedImageToFile(FortniteAPI.getMapImage(), "map"))
                .thenAccept(file -> handleFileResponse(file, message, "Failed to get map image.", currentHook))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    currentHook.editOriginal("Error processing map command.").queue();
                    return null;
                });
    }

    /**
     * Processes the stats command.
     * @param currentHook The current interaction hook.
     */
    private void processStatsCommand(InteractionHook currentHook) {
        String username = Objects.requireNonNull(event.getOption("username")).getAsString();

        // Get the stats image and send it.
        // Note the output is always a non-null string, so even if the command fails, the user will get a response.
        String imageUrl = FortniteAPI.getStatsImage(username);
        currentHook.editOriginal(imageUrl).queue();
    }

    /**
     * Processes the news command.
     * @param currentHook The current interaction hook.
     */
    private void processNewsCommand(InteractionHook currentHook) {
        // Get the stats image and send it.
        // Note the output is always a non-null string, so even if the command fails, the user will get a response.
        String imageUrl = FortniteAPI.getNewsImage();
        currentHook.editOriginal(imageUrl).queue();
    }

    /**
     * Handles the response for a file.
     * @param file The file to send.
     * @param errorMessage The error message to send if the file is null.
     * @param currentHook The current interaction hook.
     */
    private void handleFileResponse(File file, String message, String errorMessage, InteractionHook currentHook) {
        if (file != null) {
            currentHook.editOriginal(message).setAttachments(FileUpload.fromData(file)).queue(success -> {
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

    @Override
    public String getHelp() {
        return """
                Get info about Fortnite.
                Usage: `/fortnite <subcommand>`
                Subcommands:
                * `shop` - Get the current Fortnite Item Shop.
                * `news` - Get the current Fortnite (Battle Royale) news.
                * `stats` - Get stats for a Fortnite player.
                * `map` - Get the current Fortnite Map.""";
    }

    @Override
    protected List<Permission> getGeneralCommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_ATTACH_FILES));
    }
}