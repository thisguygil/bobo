package bobo;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {
    private static final Dotenv dotenv = Dotenv.load();

    /**
     * Get the value of the key from the .env file
     *
     * @param key the key to get the value of
     * @return value of the key
     */
    public static String get(String key) {
        return dotenv.get(key);
    }
}