package bobo;

public class Config {
    /**
     * Get the value of the key from the .env file
     *
     * @param key the key to get the value of
     * @return value of the key
     */
    public static String get(String key) {
        return System.getenv(key);
    }
}