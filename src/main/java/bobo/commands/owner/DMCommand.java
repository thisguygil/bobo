package bobo.commands.owner;

public class DMCommand extends AbstractOwner {
    @Override
    protected void handleOwnerCommand() {
        if (args.length < 2) {
            event.getChannel().sendMessage("Invalid usage. Use `/help dm` for more information.").queue();
            return;
        }

        String userId = args[0];
        String message = String.join(" ", args).substring(userId.length() + 1);

        event.getJDA().retrieveUserById(userId).queue(user -> {
            user.openPrivateChannel().queue(channel -> channel.sendMessage(message).queue(
                    success -> event.getChannel().sendMessage("Message sent to " + user.getAsMention()).queue(),
                    error -> event.getChannel().sendMessage("Failed to send message to " + user.getAsMention()).queue()
            ));
        }, error -> event.getChannel().sendMessage("User not found.").queue());
    }

    @Override
    public String getName() {
        return "dm";
    }

    @Override
    public String getHelp() {
        return """
                DMs a user.
                Usage: `!dm <user-id> <message>`""";
    }
}