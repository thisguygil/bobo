package bobo.commands.ai;

import bobo.Config;
import bobo.commands.ADualCommand;
import bobo.commands.CommandResponse;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.*;
import java.util.regex.Pattern;

public abstract class AAICommand extends ADualCommand {
    protected static final OpenAIClient openAI = OpenAIOkHttpClient.builder()
            .apiKey(Config.get("OPENAI_API_KEY"))
            .build();
    protected static final String CHAT_MODEL = Config.get("CHAT_MODEL");
    protected static final String IMAGE_MODEL = Config.get("IMAGE_MODEL");

    /**
     * Creates a new AI command.
     *
     * @param commandData The command data.
     */
    public AAICommand(CommandData commandData) {
        super(commandData);
    }

    @Override
    protected CommandResponse handleCommand() {
        return handleAICommand();
    }

    /**
     * Handles the AI command.
     */
    protected abstract CommandResponse handleAICommand();

    /**
     * Splits a message into chunks to fit within Discord's message limits.
     * Handles Markdown formatting to ensure they are properly closed and opened across chunks.
     * Note that this is only required for AI messages as we can't require the AI to fit within Discord's limits.
     *
     * @param message the message to split
     */
    protected static List<String> splitMessage(String message) {
        int maxMessageLength = 2000;
        List<String> chunks = new ArrayList<>();
        int start = 0;
        Set<String> activeFormat = new LinkedHashSet<>();

        while (start < message.length()) {
            int remaining = message.length() - start;

            if (remaining <= maxMessageLength) { // No need to split — add remaining message and break
                String finalChunk = message.substring(start);
                updateFormattingState(activeFormat, finalChunk);
                chunks.add(finalChunk);
                break;
            }

            int end = Math.min(start + maxMessageLength, message.length());
            int splitAt = findSafeSplitPoint(message, start, end);

            String chunk = message.substring(start, splitAt);

            // Determine formatting state at this chunk
            updateFormattingState(activeFormat, chunk);

            // Close formatting at the end, resume in next
            String closing = closingTags(activeFormat);
            String opening = openingTags(activeFormat);

            String modifiedChunk = chunk.trim() + closing;
            chunks.add(modifiedChunk);

            start = splitAt;
            while (start < message.length() && Character.isWhitespace(message.charAt(start))) {
                start++;
            }

            if (start < message.length()) {
                // Resume formatting in the next chunk
                message = message.substring(0, start) + opening + message.substring(start);
            }
        }

        return chunks;
    }

    /**
     * Sends the message chunks sequentially to avoid hitting Discord's rate limits.
     * Each chunk is sent only after the previous one has been successfully sent.
     *
     * @param channel the channel to send the messages to
     * @param chunks the list of message chunks to send
     */
    protected static void sendChunksSequentially(MessageChannelUnion channel, List<String> chunks, int index) {
        if (index >= chunks.size()) return;

        channel.sendMessage(chunks.get(index))
                .setSuppressEmbeds(true)
                .queue(sent -> sendChunksSequentially(channel, chunks, index + 1));
    }

    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[[^]]+]\\([^)]+\\)");
    private static final List<String> MARKDOWN_FORMATS = List.of(
            "**", // Bold
            "*",  // Italic
            "__", // Underline
            "~~", // Strikethrough
            "`"   // Inline code
    );
    private static final String CODE_BLOCK = "```"; // Handle code blocks separately

    /**
     * Finds a safe index to split a message into chunks.
     * Preferred split points are at empty lines or spaces, avoiding Markdown links.
     *
     * @param message the message to analyze
     * @param start the starting index for the split
     * @param end the ending index for the split
     * @return a safe index to split the message
     */
    private static int findSafeSplitPoint(String message, int start, int end) {
        // Regex to match Markdown links: [text](url)
        var matcher = MARKDOWN_LINK_PATTERN.matcher(message);

        // Collect ranges of Markdown links
        List<int[]> markdownRanges = new ArrayList<>();
        while (matcher.find()) {
            markdownRanges.add(new int[]{matcher.start(), matcher.end()});
        }

        int candidate = end;
        int lastSpace = -1;
        int lastEmptyLine = -1;

        for (int i = start + 1; i < end - 1; i++) {
            char c = message.charAt(i);
            if (c == ' ') lastSpace = i;

            // Fast check for "\n\n" or "\r\n\r\n"
            if (c == '\n' && message.charAt(i - 1) == '\n') {
                lastEmptyLine = i + 1;
            } else if (c == '\n' && message.charAt(i - 1) == '\r') {
                if (i >= 3 && message.charAt(i - 2) == '\n' && message.charAt(i - 3) == '\r') {
                    lastEmptyLine = i + 1;
                }
            }
        }

        if (lastEmptyLine > start) {
            candidate = lastEmptyLine;
        } else if (lastSpace > start) {
            candidate = lastSpace;
        }

        // Check if the candidate is inside a Markdown link
        for (int[] range : markdownRanges) {
            if (candidate > range[0] && candidate < range[1]) {
                candidate = range[0]; // Move before the Markdown link
                break;
            }
        }

        if (candidate <= start + 10 && message.length() > start + 10) {
            // Avoid micro-chunks by forcing larger step
            candidate = start + Math.min(200, message.length() - start);
        }
        return candidate;
    }

    /**
     * Updates the formatting state based on the text content.
     * Toggles the active formatting tags based on unescaped occurrences.
     *
     * @param activeFormat the set of currently active formatting tags
     * @param text the text to analyze for formatting
     */
    protected static void updateFormattingState(Set<String> activeFormat, String text) {
        // Track full code blocks that use ``` on their own line or with language tag
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(CODE_BLOCK)) {
                Optional<String> activeCodeBlock = activeFormat.stream()
                        .filter(f -> f.startsWith(CODE_BLOCK))
                        .findFirst();

                if (activeCodeBlock.isPresent()) {
                    activeFormat.remove(activeCodeBlock.get()); // Close current code block
                } else {
                    if (trimmed.matches("```\\w+")) { // e.g., ```java
                        activeFormat.add(trimmed);
                    } else {
                        activeFormat.add(CODE_BLOCK);
                    }
                }
            }
        }

        // Handle inline formats
        for (String fmt : MARKDOWN_FORMATS) {
            int count = countUnescapedOccurrences(text, fmt);
            if (count % 2 != 0) {
                if (activeFormat.contains(fmt)) {
                    activeFormat.remove(fmt);
                } else {
                    activeFormat.add(fmt);
                }
            }
        }
    }

    /**
     * Counts the number of full code blocks in the text.
     * A full code block is defined as a pair of opening and closing code block markers (```) on separate lines.
     *
     * @param text the text to analyze
     * @return the count of full code blocks
     */
    protected static int countCodeBlocks(String text) {
        int count = 0;
        for (String line : text.split("\n")) {
            if (line.trim().equals(CODE_BLOCK)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts unescaped occurrences of a token in the text.
     * An occurrence is considered unescaped if it is not preceded by a backslash.
     *
     * @param text the text to search in
     * @param token the token to count
     * @return the count of unescaped occurrences
     */
    private static int countUnescapedOccurrences(String text, String token) {
        int count = 0;
        int index = -1;
        while ((index = text.indexOf(token, index + 1)) != -1) {
            // Check if escaped with backslash
            if (index == 0 || text.charAt(index - 1) != '\\') {
                count++;
            }
        }
        return count;
    }

    /**
     * Generates closing tags for the currently active formatting.
     * The tags are reversed to close in the correct order.
     *
     * @param activeFormat the set of currently active formatting tags
     * @return a string containing the closing tags
     */
    private static String closingTags(Set<String> activeFormat) {
        StringBuilder sb = new StringBuilder();
        List<String> reversed = new ArrayList<>(activeFormat);
        Collections.reverse(reversed);
        reversed.forEach(sb::append);
        return sb.toString();
    }

    /**
     * Generates opening tags for the currently active formatting.
     * The tags are concatenated in the order they were added.
     *
     * @param activeFormat the set of currently active formatting tags
     * @return a string containing the opening tags
     */
    private static String openingTags(Set<String> activeFormat) {
        StringBuilder sb = new StringBuilder();
        activeFormat.forEach(sb::append);
        return sb.toString();
    }

    @Override
    public String getHelp() {
        return "AI command.";
    }

    @Override
    protected List<Permission> getCommandPermissions() {
        List<Permission> permissions = getAICommandPermissions();
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        return permissions;
    }

    /**
     * Gets the permissions required for the AI command.
     *
     * @return The permissions required for the AI command.
     */
    protected List<Permission> getAICommandPermissions() {
        return new ArrayList<>();
    }
}