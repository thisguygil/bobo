package bobo.commands.general;

import bobo.utils.FortniteAPI;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
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
                .addOptions(
                        new OptionData(OptionType.STRING, "info", "The Fortnite info to get", true)
                                .addChoices(
                                        new Command.Choice("shop", "shop"),
                                        new Command.Choice("news", "news"),
                                        new Command.Choice("stats", "stats"),
                                        new Command.Choice("map", "map")
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
        event.deferReply().queue();
        String info = event.getOption("info").getAsString();
        switch (info) {
            case "shop" -> processShopCommand();
            case "news" -> processNewsCommand();
            case "stats" -> processStatsCommand();
            case "map" -> processMapCommand();
        }
    }

    /**
     * Processes the shop command.
     */
    private void processShopCommand() {
        // Attach the current date as a header.
        ZonedDateTime nowInUTC = ZonedDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        final String message = "# Fortnite Shop" + "\n" + "## " + nowInUTC.format(formatter);

        // Get the shop images and send them.
        CompletableFuture.supplyAsync(() -> {
                    List<BufferedImage> images = FortniteAPI.getShopImages();
                    return images.stream()
                            .map(image -> convertBufferedImageToFile(image, "shop"))
                            .filter(Objects::nonNull)
                            .toList();
                }).thenAccept(files -> handleFilesResponse(files, message, "Failed to get shop images."))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    hook.editOriginal("Error processing shop command.").queue();
                    return null;
                });
    }

    /**
     * Processes the map command.
     */
    private void processMapCommand() {
        final String message = "# Fortnite Map";

        // Get the map image and send it.
        CompletableFuture.supplyAsync(() -> convertBufferedImageToFile(FortniteAPI.getMapImage(), "map"))
                .thenAccept(file -> handleFileResponse(file, message, "Failed to get map image."))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    hook.editOriginal("Error processing map command.").queue();
                    return null;
                });
    }

    /**
     * Processes the stats command.
     */
    private void processStatsCommand() {
        String username = Objects.requireNonNull(event.getOption("username")).getAsString();

        // Get the stats image and send it.
        // Note the output is always a non-null string, so even if the command fails, the user will get a response.
        String imageUrl = FortniteAPI.getStatsImage(username);
        hook.editOriginal(imageUrl).queue();
    }

    /**
     * Processes the news command.
     */
    private void processNewsCommand() {
        // Get the stats image and send it.
        // Note the output is always a non-null string, so even if the command fails, the user will get a response.
        String imageUrl = FortniteAPI.getNewsImage();
        hook.editOriginal(imageUrl).queue();
    }

    /**
     * Handles the response for a file.
     * @param file The file to send.
     * @param errorMessage The error message to send if the file is null.
     */
    private void handleFileResponse(File file, String message, String errorMessage) {
        if (file != null) {
            hook.editOriginal(message).setAttachments(FileUpload.fromData(file)).queue(success -> {
                if (!file.delete()) {
                    System.err.println("Failed to delete file: " + file.getAbsolutePath());
                }
            });
        } else {
            hook.editOriginal(errorMessage).queue();
        }
    }

    /**
     * Handles the response for a list of files.
     * @param files The files to send.
     * @param errorMessage The error message to send if the file list is null or empty.
     */
    private void handleFilesResponse(List<File> files, String message, String errorMessage) {
        if (files != null && !files.isEmpty()) {
            hook.editOriginal(message)
                    .setAttachments(files.stream()
                            .map(FileUpload::fromData)
                            .toArray(FileUpload[]::new))
                    .queue(success -> {
                        for (File file : files) {
                            if (!file.delete()) {
                                System.err.println("Failed to delete file: " + file.getAbsolutePath());
                            }
                        }
                    });
        } else {
            hook.editOriginal(errorMessage).queue();
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