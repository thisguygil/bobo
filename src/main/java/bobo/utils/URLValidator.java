package bobo.utils;

import java.net.MalformedURLException;
import java.net.URL;

public class URLValidator {

    public static boolean isValidURL(String urlString) {
        try {
            new URL(urlString);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}