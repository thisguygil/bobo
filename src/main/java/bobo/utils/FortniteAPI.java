package bobo.utils;

import bobo.Config;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class FortniteAPI {
    private static final String API_KEY = Config.get("FORTNITE_API_KEY");
    private static final String baseURL = "https://fortnite-api.com";
    private static final String backgroundImagePath = "resources/images/shop_background.jpg";
    private  static final String fontPath = "resources/fonts/FortniteFont.otf";

    private FortniteAPI() {} // Prevent instantiation

    private static final int margin = 20;
    private static final double paddingPercentage = 0.04;
    private static final double textPaddingPercentage = 0.02;
    private static final double fontSizePercentage = 0.07;
    private static final double availableWidthPerImagePercentage = 0.89;

    /**
     * Gets the current Fortnite shop image.
     *
     * @return The current Fortnite shop image.
     */
    @Nullable
    public static BufferedImage getShopImage() {
        String jsonResponse = sendGetRequest("/v2/shop");
        List<ShopItem> shopItems = parseShopItems(jsonResponse);
        String vbuckIconUrl = parseVbuckIconUrl(jsonResponse);

        try {
            BufferedImage background = ImageIO.read((Paths.get(backgroundImagePath).toAbsolutePath()).toUri().toURL());
            int backgroundWidth = background.getWidth();
            int backgroundHeight = background.getHeight();
            int contentWidth = backgroundWidth - (2 * margin);
            int availableWidthPerSquare = contentWidth;
            Graphics2D g2d = background.createGraphics();

            int imagesPerRow = 1;
            while (((double) shopItems.size() / imagesPerRow) * availableWidthPerSquare > backgroundHeight) {
                imagesPerRow++;
                availableWidthPerSquare = contentWidth / imagesPerRow;
            }

            int padding = (int) (availableWidthPerSquare * paddingPercentage);
            int availableWidthPerImage = (int) (availableWidthPerSquare * availableWidthPerImagePercentage);
            int fontSize = (int) (availableWidthPerSquare * fontSizePercentage);
            int textPadding = (int) (availableWidthPerSquare * textPaddingPercentage);

            Font fortniteFont = Font.createFont(Font.TRUETYPE_FONT, new File(fontPath)).deriveFont((float) fontSize);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(fortniteFont);
            g2d.setFont(fortniteFont);

            int x = margin;
            int y = margin;
            int rowHeight = 0;

            BufferedImage vbuckIcon = resizeImage(ImageIO.read((new URI(vbuckIconUrl)).toURL()), fontSize);

            for (ShopItem item : shopItems) {
                String itemUrl = item.imageUrl();

                // Deals with webp images, which is the type that the API returns for items with backgrounds
                BufferedImage unsizedImage;
                if (itemUrl.endsWith(".webp")) {
                    ImageReader reader = ImageIO.getImageReadersByMIMEType("image/webp").next();
                    ImageInputStream stream = ImageIO.createImageInputStream((new URI(itemUrl)).toURL().openStream());
                    reader.setInput(stream);
                    unsizedImage = reader.read(0);
                } else {
                    unsizedImage = ImageIO.read((new URI(itemUrl)).toURL());
                }

                BufferedImage itemImage = resizeImage(unsizedImage, availableWidthPerImage);

                if (x + itemImage.getWidth() > backgroundWidth - margin) {
                    x = margin;
                    y += rowHeight + padding;
                    rowHeight = 0;
                }

                g2d.drawImage(itemImage, x, y, null);

                g2d.setColor(Color.WHITE);

                FontMetrics metrics = g2d.getFontMetrics();
                int stringHeight = metrics.getHeight();

                int itemNameWidth = metrics.stringWidth(item.name());
                int itemNameX = x + (itemImage.getWidth() - itemNameWidth) / 2;
                int itemNameY = y + itemImage.getHeight() + fontSize - textPadding - 2 * stringHeight;
                g2d.drawString(item.name(), itemNameX, itemNameY);

                int itemPriceWidth = metrics.stringWidth(String.valueOf(item.price()));
                int itemPriceX = x + (itemImage.getWidth() - itemPriceWidth) / 2;
                int itemPriceY = itemNameY + fontSize;
                g2d.drawString(String.valueOf(item.price()), itemPriceX, itemPriceY);

                int vBuckIconX = itemPriceX - vbuckIcon.getWidth() - textPadding;
                int vbuckIconY = itemPriceY - vbuckIcon.getHeight() + textPadding;
                g2d.drawImage(vbuckIcon, vBuckIconX, vbuckIconY, null);

                x += itemImage.getWidth() + padding;

                int totalHeight = itemImage.getHeight() + vbuckIcon.getHeight();
                if (totalHeight > rowHeight) {
                    rowHeight = totalHeight;
                }
            }

            g2d.dispose();
            return background;
        } catch (IOException | URISyntaxException | FontFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Resizes a {@link BufferedImage}.
     *
     * @param originalImage The image to resize.
     * @param targetWidth The new width.
     * @return The resized image.
     */
    @Nonnull
    public static BufferedImage resizeImage(@Nonnull BufferedImage originalImage, int targetWidth) {
        int newHeight = (int) (originalImage.getHeight() * ((double) targetWidth / originalImage.getWidth()));
        Image resultingImage = originalImage.getScaledInstance(targetWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage outputImage = new BufferedImage(targetWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(resultingImage, 0, 0, null);
        g2d.dispose();

        return outputImage;
    }

    /**
     * Gets the current Fortnite map image.
     *
     * @return The current Fortnite map image.
     */
    @Nullable
    public static BufferedImage getMapImage() {
        String jsonResponse = sendGetRequest("/v1/map");
        String mapImageUrl = parseMapImageUrl(jsonResponse);

        if (!mapImageUrl.isEmpty()) {
            try {
                return ImageIO.read((new URI(mapImageUrl)).toURL());
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Parses the JSON response from the Fortnite API to get the shop images.
     *
     * @param jsonResponse The JSON response from the Fortnite API.
     * @return The shop images.
     */
    @Nonnull
    private static List<ShopItem> parseShopItems(String jsonResponse) {
        List<ShopItem> shopItems = new ArrayList<>();

        JSONObject shopData = new JSONObject(jsonResponse);

        if (shopData.has("data")) {
            JSONArray items = shopData.getJSONObject("data").getJSONArray("entries");
            String imageUrl;
            String itemName = "";

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);

                // Check if the item is a bundle, if so then get the bundle image and name
                JSONObject bundle;
                try {
                    bundle = item.getJSONObject("bundle");
                } catch (JSONException e) {
                    bundle = null;
                }

                if (bundle != null) {
                    imageUrl = bundle.getString("image");
                    itemName = bundle.getString("name");
                } else {
                    // Check if the item has an asset, if not then skip it
                    if (!item.has("newDisplayAsset")) {
                        continue;
                    }

                    JSONObject displayAsset = item.getJSONObject("newDisplayAsset");

                    // Gets the item name. Note that only one of these will be non-null, as we verified that the item is not a bundle
                    try {
                        itemName = item.getJSONArray("brItems").getJSONObject(0).getString("name");
                    } catch (JSONException ignored) {}
                    try {
                        itemName = item.getJSONArray("tracks").getJSONObject(0).getString("title");
                    } catch (JSONException ignored) {}
                    try {
                        itemName = item.getJSONArray("instruments").getJSONObject(0).getString("name");
                    } catch (JSONException ignored) {}
                    try {
                        itemName = item.getJSONArray("cars").getJSONObject(0).getString("name");
                    } catch (JSONException ignored) {}

                    // Gets the item image URL, prioritizing the background image over the offer image if it has one
                    JSONObject images = displayAsset.getJSONArray("materialInstances").getJSONObject(0).getJSONObject("images");
                    if (images.has("Background")) {
                        imageUrl = images.getString("Background");
                    } else {
                        imageUrl = images.getString("OfferImage");
                    }
                }

                // Get the item price and add the finl item to the list
                int itemPrice = item.getInt("finalPrice");
                shopItems.add(new ShopItem(itemName, itemPrice, imageUrl));
            }
        }

        return shopItems;
    }

    /**
     * Parses the JSON response from the Fortnite API to get the vbuck icon URL.
     *
     * @param jsonResponse The JSON response from the Fortnite API.
     * @return The vbuck icon URL.
     */
    @Nonnull
    private static String parseVbuckIconUrl(String jsonResponse) {
        JSONObject shopData = new JSONObject(jsonResponse);

        if (shopData.has("data")) {
            JSONObject data = shopData.getJSONObject("data");
            if (data.has("vbuckIcon")) {
                return data.getString("vbuckIcon");
            }
        }

        return "";
    }

    /**
     * Parses the JSON response from the Fortnite API to get the map image URL.
     *
     * @param jsonResponse The JSON response from the Fortnite API.
     * @return The map image URL.
     */
    private static String parseMapImageUrl(String jsonResponse) {
        JSONObject mapData = new JSONObject(jsonResponse);

        if (mapData.has("data")) {
            JSONObject data = mapData.getJSONObject("data");
            if (data.has("images") && data.getJSONObject("images").has("pois")) {
                return data.getJSONObject("images").getString("pois");
            }
        }

        return "";
    }

    /**
     * Sends a GET request to the Fortnite API.
     *
     * @param endpoint The endpoint to send the request to.
     * @return The response from the API.
     */
    @Nullable
    private static String sendGetRequest(String endpoint) {
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(baseURL + endpoint);

            httpGet.setHeader("Authorization", API_KEY);

            HttpResponse response = httpClient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return EntityUtils.toString(entity);
                }
            } else {
                System.out.println("GET request failed. Response Code: " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Record representing a shop item.
     */
    private record ShopItem(String name, int price, String imageUrl) {}
}