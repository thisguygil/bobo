package bobo.utils;

import net.dv8tion.jda.api.entities.emoji.Emoji;

public enum EmojiType {
    X("❌"),
    ONE("1️⃣"),
    TWO("2️⃣"),
    THREE("3️⃣"),
    FOUR("4️⃣"),
    FIVE("5️⃣");

    private final String unicode;

    EmojiType(String unicode) {
        this.unicode = unicode;
    }

    public Emoji asEmoji() {
        return Emoji.fromUnicode(unicode);
    }
}