package bobo;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

public class Config {
    private static final Dotenv dotenv;
    private static final boolean useSystemEnv = Boolean.parseBoolean(System.getenv("USE_SYSTEM_ENV"));

    static {
        Dotenv tempDotenv = null;
        if (!useSystemEnv) {
            try {
                tempDotenv = Dotenv.load();
            } catch (DotenvException e) {
                System.exit(2);
            }
        }
        dotenv = tempDotenv;
    }

    /**
     * Get the value of the key from the .env file
     *
     * @param key the key to get the value of
     * @return value of the key
     */
    public static String get(String key) {
        if (useSystemEnv) {
            return System.getenv(key);
        } else {
            return dotenv.get(key);
        }
    }
}