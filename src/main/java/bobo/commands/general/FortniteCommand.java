package bobo.commands.general;

import bobo.utils.api_clients.FortniteAPI;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(FortniteCommand.class);

    /**
     * Creates a new Fortnite command.
     */
    public FortniteCommand() {
        super(Commands.slash("fortnite", "Get info about Fortnite.")
                .addSubcommands( // Make two subcommands because stats requires an input and the other commands don't.
                        new SubcommandData("stats", "Get stats for a Fortnite player.")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "username", "The username of the player", true)
                                ),
                        new SubcommandData("info", "Get info about Fortnite.")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "info", "The info to get", true)
                                                .addChoices(
                                                        new Command.Choice("shop", "shop"),
                                                        new Command.Choice("news", "news."),
                                                        new Command.Choice("map", "map")
                                                )
                                )
                )
        );
    }

    @Override
    public String getName() {
        return "fortnite";
    }

    @Override
    protected void handleGeneralCommand() {
        var currentHook = hook;
        String subcommand = event.getSubcommandName();
        switch (subcommand) {
            case "info" -> {
                String info = event.getOption("info").getAsString();
                switch (info) {
                    case "shop" -> processShopCommand(currentHook);
                    case "news" -> processNewsCommand(currentHook);
                    case "map" -> processMapCommand(currentHook);
                }
            }
            case "stats" -> {
                String username = event.getOption("username").getAsString();
                processStatsCommand(currentHook, username);
            }
            default -> throw new IllegalStateException("Unexpected value: " + subcommand);
        }

    }

    /**
     * Processes the shop command.
     */
    private void processShopCommand(InteractionHook currentHook) {
        // Attach the current date as a header.
        ZonedDateTime nowInUTC = ZonedDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        final String message = "# Fortnite Shop" + "\n" + "## " + nowInUTC.format(formatter);

        // Get the shop images and send them.
        CompletableFuture.supplyAsync(() -> {
                    List<BufferedImage> images = FortniteAPI.getShopImages();
                    if (images == null) {
                        return null;
                    }

                    return images.stream()
                            .map(image -> convertBufferedImageToFile(image, "shop"))
                            .filter(Objects::nonNull)
                            .toList();
                }).thenAccept(files -> handleFilesResponse(files, message, "Failed to get shop images.", currentHook))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    currentHook.editOriginal("Error processing shop command.").queue();
                    return null;
                });
    }

    /**
     * Processes the map command.
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
     */
    private void processStatsCommand(InteractionHook currentHook, String username) {
        // Get the stats image and send it.
        // Note the output is always a non-null string, so even if the command fails, the user will get a response.
        String imageUrl = FortniteAPI.getStatsImage(username);
        currentHook.editOriginal(imageUrl).queue();
    }

    /**
     * Processes the news command.
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
     */
    private void handleFileResponse(File file, String message, String errorMessage, InteractionHook currentHook) {
        if (file != null) {
            currentHook.editOriginal(message).setAttachments(FileUpload.fromData(file)).queue(_ -> {
                if (!file.delete()) {
                    logger.error("Failed to delete file: {}", file.getAbsolutePath());
                }
            });
        } else {
            currentHook.editOriginal(errorMessage).queue();
        }
    }

    /**
     * Handles the response for a list of files.
     * @param files The files to send.
     * @param errorMessage The error message to send if the file list is null or empty.
     */
    private void handleFilesResponse(List<File> files, String message, String errorMessage, InteractionHook currentHook) {
        if (files != null && !files.isEmpty()) {
            currentHook.editOriginal(message)
                    .setAttachments(files.stream()
                            .map(FileUpload::fromData)
                            .toArray(FileUpload[]::new))
                    .queue(_ -> {
                        for (File file : files) {
                            if (!file.delete()) {
                                logger.error("Failed to delete file: {}", file.getAbsolutePath());
                            }
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

    @Override
    public Boolean shouldBeEphemeral() {
        return false;
    }
}