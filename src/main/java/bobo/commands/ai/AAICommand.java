package bobo.commands.ai;

import bobo.Config;
import bobo.commands.ADualCommand;
import bobo.commands.CommandResponse;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import net.dv8tion.jda.api.Permission;
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
        String markdownPattern = "\\[[^]]+]\\([^)]+\\)";
        var matcher = Pattern.compile(markdownPattern).matcher(message);

        // Collect ranges of Markdown links
        List<int[]> markdownRanges = new ArrayList<>();
        while (matcher.find()) {
            markdownRanges.add(new int[]{matcher.start(), matcher.end()});
        }

        int candidate = end;

        // Prefer split at empty line (double newline or newline followed by optional space + newline)
        int emptyLine = message.lastIndexOf("\n\n", end - 1);
        if (emptyLine == -1) { // Try Windows newline pattern
            emptyLine = message.lastIndexOf("\r\n\r\n", end - 1);
        }

        if (emptyLine > start) {
            candidate = emptyLine + 2; // Keep newline as start of the next chunk
        } else {
            // Fall back to space
            int lastSpace = message.lastIndexOf(' ', end - 1);
            if (lastSpace > start) {
                candidate = lastSpace;
            }
        }

        // Check if the candidate is inside a Markdown link
        for (int[] range : markdownRanges) {
            if (candidate > range[0] && candidate < range[1]) {
                candidate = range[0]; // Move before the Markdown link
                break;
            }
        }

        return Math.max(candidate, start + 1);
    }

    /**
     * Updates the formatting state based on the text content.
     * Toggles the active formatting tags based on unescaped occurrences.
     *
     * @param activeFormat the set of currently active formatting tags
     * @param text the text to analyze for formatting
     */
    private static void updateFormattingState(Set<String> activeFormat, String text) {
        final List<String> FORMATS = List.of(
                "**", // Bold
                "*",  // Italic
                "__", // Underline
                "~~", // Strikethrough
                "`"   // Inline code
        );

        for (String fmt : FORMATS) {
            int count = countUnescapedOccurrences(text, fmt);
            if (count % 2 != 0) {
                if (activeFormat.contains(fmt)) {
                    activeFormat.remove(fmt); // Close tag
                } else {
                    activeFormat.add(fmt);    // Open tag
                }
            }
        }
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