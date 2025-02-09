package bobo.commands.general;

import bobo.commands.CommandResponse;
import bobo.commands.CommandResponseBuilder;
import bobo.utils.api_clients.FortniteAPI;
import net.dv8tion.jda.api.Permission;
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

public class FortniteCommand extends AGeneralCommand {
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
    protected CommandResponse handleGeneralCommand() {
        var currentHook = slashEvent.getHook();
        String subcommand;
        try {
            subcommand = getSubcommandName(0);
        } catch (RuntimeException e) {
            return new CommandResponse("Invalid usage. Use `/help fortnite` for more information.");
        }

        CompletableFuture<CommandResponse> responseFuture = switch (subcommand) {
            case "info" -> {
                String info;
                try {
                    info = getOptionValue("info", 1);
                } catch (RuntimeException e) {
                     yield CompletableFuture.completedFuture(new CommandResponse("Invalid usage. Use `/help fortnite` for more information."));
                }

                yield switch (info) {
                    case "shop" -> processShopCommand();
                    case "news" -> processNewsCommand();
                    case "map" -> processMapCommand();
                    default -> CompletableFuture.completedFuture(new CommandResponse("Invalid usage. Use `/help fortnite` for more information."));
                };
            }
            case "stats" -> {
                String username;
                try {
                    username = getOptionValue("username", 1);
                } catch (RuntimeException e) {
                    yield CompletableFuture.completedFuture(new CommandResponse("Invalid usage. Use `/help fortnite` for more information."));
                }

                yield processStatsCommand(username);
            }
            default -> CompletableFuture.completedFuture(new CommandResponse("Invalid usage. Use `/help fortnite` for more information."));
        };

        return responseFuture.join();
    }

    /**
     * Processes the shop command.
     *
     * @return The future command response.
     */
    private CompletableFuture<CommandResponse> processShopCommand() {
        // Attach the current date as a header.
        ZonedDateTime nowInUTC = ZonedDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        final String message = "# Fortnite Shop" + "\n" + "## " + nowInUTC.format(formatter);

        // Get the shop images and send them.
        return CompletableFuture.supplyAsync(() -> {
            List<BufferedImage> images = FortniteAPI.getShopImages();
            if (images == null) {
                return new CommandResponse("Failed to get shop images.");
            }

            return new CommandResponseBuilder().setContent(message)
                    .addAttachments(images.stream()
                            .map(image -> convertBufferedImageToFile(image, "shop"))
                            .filter(Objects::nonNull)
                            .toList()
                            .stream()
                            .map(FileUpload::fromData)
                            .toList()
                    ).build();
        });
    }

    /**
     * Processes the map command.
     *
     * @return The future command response.
     */
    private CompletableFuture<CommandResponse> processMapCommand() {
        final String message = "# Fortnite Map";

        // Get the map image and send it.
        return CompletableFuture.supplyAsync(() -> {
            File image = convertBufferedImageToFile(FortniteAPI.getMapImage(), "map");
            if (image == null) {
                return new CommandResponse("Failed to get map image.");
            }

            return new CommandResponseBuilder().addAttachments(FileUpload.fromData(image))
                    .build();
        });
    }

    /**
     * Processes the stats command.
     *
     * @param username The username of the player.
     * @return The future command response.
     */
    private CompletableFuture<CommandResponse> processStatsCommand(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String imageUrl = FortniteAPI.getStatsImage(username);
            return new CommandResponse(imageUrl);
        });
    }

    /**
     * Processes the news command.
     *
     * @return The future command response.
     */
    private CompletableFuture<CommandResponse> processNewsCommand() {
        return CompletableFuture.supplyAsync(() -> {
            String imageUrl = FortniteAPI.getNewsImage();
            return new CommandResponse(imageUrl);
        });
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
            logger.error("Failed to convert image to file.");
            return null;
        }
    }

    @Override
    public String getHelp() {
        return """
                Get info about Fortnite.
                Usage: `/fortnite <subcommand>`
                Subcommands:
                * `stats <username>` - Gets the stats of `<username>` in Fortnite
                * `info` - Get info about Fortnite.
                    * `shop` - Get the Fortnite shop.
                    * `news` - Get the Fortnite news.
                    * `map` - Get the Fortnite map.""";
    }

    @Override
    protected List<Permission> getGeneralCommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_ATTACH_FILES));
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}