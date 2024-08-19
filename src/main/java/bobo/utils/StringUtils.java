package bobo.utils;

import com.google.common.net.UrlEscapers;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class StringUtils {
    private StringUtils() {} // Prevent instantiation

    /**
     * Decodes a URL.
     *
     * @param url The URL to decode.
     * @return The decoded URL.
     */
    public static String decodeUrl(@NotNull String url) {
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

    /**
     * Encodes a URL.
     *
     * @param url The URL to encode.
     * @return The encoded URL.
     */
    public static String encodeUrl(@NotNull String url) {
        return UrlEscapers.urlFragmentEscaper().escape(url);
    }

    /**
     * Creates a markdown header.
     *
     * @param level The header level. -1 for small header.
     * @param text  The header text.
     * @return The markdown header.
     * @throws IllegalArgumentException If the header level is invalid.
     */
    @Contract(pure = true)
    public static <T> @NotNull String markdownHeader(int level, @NotNull T text) {
        if (level != -1 && (level < 1 || level > 3)) {
            throw new IllegalArgumentException("Header level must be -1 or between 1 and 3.");
        }

        if (level == -1) {
            return "-# " + text;
        } else {
            return "#".repeat(level) + " " + text;
        }
    }

    /**
     * Creates a Markdown link.
     *
     * @param text The link text.
     * @param url  The link URL.
     * @return The Markdown link.
     */
    @Contract(pure = true)
    public static <T> @NotNull String markdownLink(@NotNull T text, @NotNull String url) {
        return "[" + text + "](" + url + ")";
    }

    /**
     * Creates a Markdown link with no embed.
     *
     * @param text The link text.
     * @param url  The link URL.
     * @return The Markdown link with no embed.
     */
    @Contract(pure = true)
    public static <T> @NotNull String markdownLinkNoEmbed(@NotNull T text, @NotNull String url) {
        return "[" + text + "](<" + url + ">)";
    }

    /**
     * Creates a Markdown bold text.
     *
     * @param text The text to make bold.
     * @return The Markdown bold text.
     */
    @Contract(pure = true)
    public static <T> @NotNull String markdownBold(@NotNull T text) {
        return "**" + text + "**";
    }

    /**
     * Creates a Markdown italic text.
     *
     * @param text The text to make italic.
     * @return The Markdown italic text.
     */
    @Contract(pure = true)
    public static <T> @NotNull String markdownItalic(@NotNull T text) {
        return "*" + text + "*";
    }

    /**
     * Creates a Markdown bold and italic text.
     *
     * @param text The text to make bold and italic.
     * @return The Markdown bold and italic text.
     */
    @Contract(pure = true)
    public static <T> @NotNull String markdownBoldItalic(@NotNull T text) {
        return "***" + text + "***";
    }

    /**
     * Creates a Markdown underline text.
     *
     * @param text The text to underline.
     * @return The Markdown underline text.
     */
    @Contract(pure = true)
    public static <T> @NotNull String markdownUnderline(@NotNull T text) {
        return "__" + text + "__";
    }

    /**
     * Creates a Markdown strikethrough text.
     *
     * @param text The text to strikethrough.
     * @return The Markdown strikethrough text.
     */
    @Contract(pure = true)
    public static <T> @NotNull String markdownStrikethrough(@NotNull T text) {
        return "~~" + text + "~~";
    }

    /**
     * Creates a Markdown code text.
     *
     * @param text The text to make code.
     * @return The Markdown code text.
     */
    @Contract(pure = true)
    public static <T> @NotNull String markdownCode(@NotNull T text) {
        return "`" + text + "`";
    }

    /**
     * Creates a Markdown code block.
     *
     * @param text The text to make a code block.
     * @return The Markdown code block.
     */
    @Contract(pure = true)
    public static <T> @NotNull String markdownCodeBlock(@NotNull T text) {
        return "```" + text + "```";
    }

    /**
     * Creates a Markdown code block with a language for syntax highlighting.
     *
     * @param language The language of the code block.
     * @param text     The text to make a code block.
     * @return The Markdown code block with a language.
     */
    @Contract(pure = true)
    public static <T> @NotNull String markdownCodeBlock(String language, @NotNull T text) {
        return "```" + language + "\n" + text + "```";
    }

    /**
     * Creates a Markdown quote.
     *
     * @param text The text to quote.
     * @return The Markdown quote.
     */
    @Contract(pure = true)
    public static <T> @NotNull String markdownQuote(@NotNull T text) {
        return "> " + text;
    }

    /**
     * Creates a Markdown spoiler.
     *
     * @param text The text to make a spoiler.
     * @return The Markdown spoiler.
     */
    @Contract(pure = true)
    public static <T> @NotNull String markdownSpoiler(@NotNull T text) {
        return "||" + text + "||";
    }

    /**
     * Mentions a user.
     *
     * @param userId The user ID to mention.
     * @return The mention.
     */
    @Contract(pure = true)
    public static @NotNull String mentionUser(@NotNull String userId) {
        return "<@" + userId + ">";
    }

    /**
     * Mentions a user.
     *
     * @param userId The user ID to mention.
     * @return The mention.
     */
    @Contract(pure = true)
    public static @NotNull String mentionUser(long userId) {
        return "<@" + userId + ">";
    }

    /**
     * Mentions a role.
     *
     * @param roleId The role ID to mention.
     * @return The mention.
     */
    @Contract(pure = true)
    public static @NotNull String mentionRole(@NotNull String roleId) {
        return "<@&" + roleId + ">";
    }

    /**
     * Mentions a role.
     *
     * @param roleId The role ID to mention.
     * @return The mention.
     */
    @Contract(pure = true)
    public static @NotNull String mentionRole(long roleId) {
        return "<@&" + roleId + ">";
    }

    /**
     * Mentions a channel.
     *
     * @param channelId The channel ID to mention.
     * @return The mention.
     */
    @Contract(pure = true)
    public static @NotNull String mentionChannel(@NotNull String channelId) {
        return "<#" + channelId + ">";
    }

    /**
     * Mentions a channel.
     *
     * @param channelId The channel ID to mention.
     * @return The mention.
     */
    @Contract(pure = true)
    public static @NotNull String mentionChannel(long channelId) {
        return "<#" + channelId + ">";
    }

    /**
     * Creates an emoji.
     *
     * @param emoji The emoji name.
     * @return The emoji.
     */
    @Contract(pure = true)
    public static @NotNull String emoji(@NotNull String emoji) {
        return ":" + emoji + ":";
    }
}