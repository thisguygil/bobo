package bobo.commands.owner;

import bobo.commands.CommandResponse;

public class DMCommand extends AOwnerCommand {
    @Override
    protected CommandResponse handleOwnerCommand() {
        String userId, message;
        try {
            userId = getOptionValue(0);
            message = getMultiwordOptionValue(1);
        } catch (RuntimeException e) {
            return CommandResponse.text("Invalid usage. Use `/help dm` for more information.");
        }

        CommandResponse response = CommandResponse.text("Sending message to user...");

        event.getJDA().retrieveUserById(userId).queue(user -> {
            user.openPrivateChannel().queue(channel -> channel.sendMessage(message).queue(
                    success -> event.getChannel().sendMessage("Message sent to " + user.getAsMention()).queue(),
                    error -> event.getChannel().sendMessage("Failed to send message to " + user.getAsMention()).queue()
            ));
        }, error -> event.getChannel().sendMessage("User not found.").queue());

        return response;
    }

    @Override
    public String getName() {
        return "dm";
    }

    @Override
    public String getHelp() {
        return """
                DMs a user.
                Usage: `""" + PREFIX + "dm <user-id> <message>`";
    }

    @Override
    public Boolean shouldNotShowTyping() {
        return false;
    }
}