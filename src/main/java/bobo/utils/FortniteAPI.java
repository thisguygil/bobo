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

    /**
     * Enum representing the different types of items in the Fortnite shop.
     */
    private enum ShopItemType {
        BUNDLE,
        BR_ITEM,
        INSTRUMENT,
        CAR,
        TRACK
    }

    private static final int margin = 20;
    private static final double paddingPercentage = 0.04;
    private static final double textPaddingPercentage = 0.02;
    private static final double fontSizePercentage = 0.12;
    private static final double availableWidthPerImagePercentage = 0.96;

    /**
     * Gets the current Fortnite shop image.
     *
     * @return The current Fortnite shop image.
     */
    @Nullable
    public static BufferedImage getShopImage() {
        // Get the shop items and vbuck icon URL
        String jsonResponse = sendGetRequest("/v2/shop");
        List<ShopItem> shopItems = parseShopItems(jsonResponse);
        String vbuckIconUrl = parseVbuckIconUrl(jsonResponse);

        try {
            // Load the background image and get its dimensions
            BufferedImage background = ImageIO.read((Paths.get(backgroundImagePath).toAbsolutePath()).toUri().toURL());
            int backgroundWidth = background.getWidth();
            int backgroundHeight = background.getHeight();
            int contentWidth = backgroundWidth - (2 * margin);
            int contentHeight = backgroundHeight - (2 * margin);
            Graphics2D g2d = background.createGraphics();

            // Calculate the available width per square
            int imagesPerRow = 1;
            int availableWidthPerSquare = contentWidth;
            while (((double) shopItems.size() / imagesPerRow) * availableWidthPerSquare > contentHeight) {
                imagesPerRow++;
                availableWidthPerSquare = contentWidth / (imagesPerRow + 1);
            }

            // Calculate the number of rows, font size, and padding
            int numRows = (int) Math.ceil((double) shopItems.size() / imagesPerRow);
            float fontSize = (float) (availableWidthPerSquare * fontSizePercentage);
            int padding = (int) (availableWidthPerSquare * paddingPercentage);
            int textPadding = (int) (availableWidthPerSquare * textPaddingPercentage);

            // Load the font
            Font fortniteFont = Font.createFont(Font.TRUETYPE_FONT, new File(fontPath)).deriveFont(fontSize);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(fortniteFont);
            g2d.setFont(fortniteFont);

            // Initialize the x and y coordinates
            int x = (contentWidth - (availableWidthPerSquare * imagesPerRow) + padding) / 2 + margin;
            int y = (contentHeight - (availableWidthPerSquare * numRows) + padding) / 2 + margin;

            // Save the initial x coordinate so that we can reset it when we move to the next row
            int initialX = x;
            int countRows = 1;

            // Get and resize the vbuck icon
            BufferedImage vbuckIcon = resizeImage(ImageIO.read((new URI(vbuckIconUrl)).toURL()), (int) fontSize);
            int vbuckIconHeight = vbuckIcon.getHeight();
            int vbuckIconWidth = vbuckIcon.getWidth();

            for (ShopItem item : shopItems) {
                String itemUrl = item.imageUrl();

                // Deals with webp images, which is the shopItemType that the API returns for items with backgrounds
                BufferedImage unsizedImage;
                if (itemUrl.endsWith(".webp")) {
                    ImageReader reader = ImageIO.getImageReadersByMIMEType("image/webp").next();
                    ImageInputStream stream = ImageIO.createImageInputStream((new URI(itemUrl)).toURL().openStream());
                    reader.setInput(stream);
                    unsizedImage = reader.read(0);
                } else {
                    unsizedImage = ImageIO.read((new URI(itemUrl)).toURL());
                }

                // Resize the image to fit the available width
                BufferedImage itemImage = resizeImage(unsizedImage, (int) (availableWidthPerSquare * availableWidthPerImagePercentage));
                int itemImageHeight = itemImage.getHeight();
                int itemImageWidth = itemImage.getWidth();

                // Move to the next row if the image won't fit
                if (x + itemImageWidth > backgroundWidth - margin) {
                    countRows++;

                    // If this is the last row, center the images
                    if (countRows == numRows) {
                        int imagesInLastRow = shopItems.size() - ((countRows - 1) * imagesPerRow);
                        x = (contentWidth - (availableWidthPerSquare * imagesInLastRow) + padding) / 2 + margin;
                    } else {
                        x = initialX;
                    }

                    // Move to the next row
                    y += itemImageWidth + padding;
                }

                // Draw the image
                g2d.drawImage(itemImage, x, y, null);

                FontMetrics metrics = g2d.getFontMetrics();
                int stringHeight = metrics.getHeight();

                // Draw an opaque gray rectangle behind the text
                Color opaqueBackground = new Color(128, 128, 128, 128); // RGBA: Gray with 50% opacity
                g2d.setColor(opaqueBackground);

                int rectangleX = x;
                int rectangleHeight = (stringHeight + textPadding) * 2;
                int rectangleY = y + itemImageHeight - rectangleHeight;
                g2d.fillRect(rectangleX, rectangleY, itemImageWidth, rectangleHeight);

                // Change the color to white for the text
                g2d.setColor(Color.WHITE);

                // If the item name is too long, resize the font to fit it
                int itemNameWidth = metrics.stringWidth(item.name());
                if (itemNameWidth > itemImageWidth) {
                    float newFontSize = fontSize * ((float) itemImageWidth / itemNameWidth);
                    g2d.setFont(fortniteFont.deriveFont(newFontSize));
                    metrics = g2d.getFontMetrics();
                    itemNameWidth = metrics.stringWidth(item.name());
                }

                // Draw the item name
                int itemNameX = x + (itemImageWidth - itemNameWidth) / 2;
                int itemNameY =  y + itemImageHeight + (int) fontSize - rectangleHeight;
                g2d.drawString(item.name(), itemNameX, itemNameY);

                // Reset the font to the original size
                g2d.setFont(fortniteFont.deriveFont(fontSize));
                metrics = g2d.getFontMetrics();

                // Draw the item price
                int itemPriceWidth = metrics.stringWidth(String.valueOf(item.price()));
                int itemPriceX = x + (itemImageWidth - itemPriceWidth) / 2;
                int itemPriceY = itemNameY + (int) fontSize;
                g2d.drawString(String.valueOf(item.price()), itemPriceX, itemPriceY);

                // Draw the vbuck icon
                int vBuckIconX = itemPriceX - vbuckIconWidth - textPadding;
                int vbuckIconY = itemPriceY - vbuckIconHeight + textPadding;
                g2d.drawImage(vbuckIcon, vBuckIconX, vbuckIconY, null);

                // Move to the next image
                x += itemImageWidth + padding;
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

        if (!shopData.has("data")) {
            return shopItems;
        }

        JSONArray items = shopData.getJSONObject("data").getJSONArray("entries");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            ShopItemType shopItemType = null;
            String itemName = "";
            String imageUrl;
            String rarity = "";
            String set = "";
            String itemType = "";

            // Check if the item has an asset, if not then skip it
            if (!item.has("newDisplayAsset")) {
                continue;
            }
            // Gets the item image URL
            JSONObject images = item.getJSONObject("newDisplayAsset").getJSONArray("materialInstances").getJSONObject(0).getJSONObject("images");
            if (images.has("Background")) {
                imageUrl = images.getString("Background");
            } else {
                imageUrl = images.getString("OfferImage");
            }

            // Get the shop item type, name, and item type. For all except bundles and tracks, we'll also get the rarity and set if it exists
            try {
                itemName = item.getJSONObject("bundle").getString("name");
                shopItemType = ShopItemType.BUNDLE;
            } catch (JSONException e) {
                try {
                    JSONObject itemObject = item.getJSONArray("brItems").getJSONObject(0);
                    itemName = itemObject.getString("name");
                    shopItemType = ShopItemType.BR_ITEM;
                    rarity = itemObject.getJSONObject("rarity").getString("value");
                    try {
                        set = itemObject.getJSONObject("set").getString("value");
                    } catch (JSONException ignored) {}
                    itemType = itemObject.getJSONObject("type").getString("value");
                } catch (JSONException ignored) {}
                try {
                    itemName = item.getJSONArray("tracks").getJSONObject(0).getString("title");
                    shopItemType = ShopItemType.TRACK;
                } catch (JSONException ignored) {}
                try {
                    JSONObject itemObject = item.getJSONArray("instruments").getJSONObject(0);
                    itemName = itemObject.getString("name");
                    shopItemType = ShopItemType.INSTRUMENT;
                    rarity = itemObject.getJSONObject("rarity").getString("value");
                    try {
                        set = itemObject.getJSONObject("set").getString("value");
                    } catch (JSONException ignored) {}
                    itemType = itemObject.getJSONObject("type").getString("value");
                } catch (JSONException ignored) {}
                try {
                    JSONObject itemObject = item.getJSONArray("cars").getJSONObject(0);
                    itemName = itemObject.getString("name");
                    shopItemType = ShopItemType.CAR;
                    rarity = itemObject.getJSONObject("rarity").getString("value");
                    try {
                        set = itemObject.getJSONObject("set").getString("value");
                    } catch (JSONException ignored) {}
                    itemType = itemObject.getJSONObject("type").getString("value");
                } catch (JSONException ignored) {}
            }

            // Get the item price and add the item to the list
            int itemPrice = item.getInt("finalPrice");
            shopItems.add(new ShopItem(shopItemType, itemName, itemPrice, imageUrl, rarity, set, itemType));
        }

        // Sort and return the items
        shopItems.sort(ShopItem::compareTo);
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
     * Record representing a shop item. Implements {@link Comparable} so that the items can be sorted.
     */
    private record ShopItem(ShopItemType shopItemType, String name, int price, String imageUrl, String rarity, String set, String itemType) implements Comparable<ShopItem> {
        /**
         * Compares the items by whether they are bundles, then by shop item type, then by rarity, then by set, then by item type, then by name
         * @param item2 the other item to be compared.
         * @return a negative integer, zero, or a positive integer as this item is less than, equal to, or greater than the other item.
         */
        @Override
        public int compareTo(@Nonnull ShopItem item2) {
            // Bundle comparison
            if (this.shopItemType == ShopItemType.BUNDLE && item2.shopItemType != ShopItemType.BUNDLE) {
                return -1;
            } else if (this.shopItemType != ShopItemType.BUNDLE && item2.shopItemType == ShopItemType.BUNDLE) {
                return 1;
            }

            // Type comparison
            int typeComparison = this.shopItemType.compareTo(item2.shopItemType);
            if (typeComparison != 0) return typeComparison;

            // Bundles and tracks don't have rarity, set, or item type, so we shouldn't compare them
            // Instruments don't have sets, so we shouldn't compare them
            if (this.shopItemType != ShopItemType.BUNDLE && item2.shopItemType != ShopItemType.TRACK) {
                // Rarity comparison
                int rarityComparison = this.rarity.compareTo(item2.rarity);
                if (rarityComparison != 0) return rarityComparison;

                // Set comparison. Must check if the set is empty, as some items don't have a set
                if (this.shopItemType != ShopItemType.INSTRUMENT) {
                    if (this.set.isEmpty() && !item2.set.isEmpty()) {
                        return -1;
                    } else if (!this.set.isEmpty() && item2.set.isEmpty()) {
                        return 1;
                    } else {
                        int setComparison = this.set.compareTo(item2.set);
                        if (setComparison != 0) return setComparison;
                    }
                }

                // Item type comparison. (for some reason the car type is "skin")
                List<String> itemTypeOrder = List.of("outfit", "backpack", "pickaxe", "glider", "contrail", "aura", "emote", "wrap", "music", "loadingscreen", "guitar", "keyboard", "bass", "microphone", "drums", "skin");
                int thisItemTypeIndex = itemTypeOrder.indexOf(this.itemType);
                int item2ItemTypeIndex = itemTypeOrder.indexOf(item2.itemType);

                if (thisItemTypeIndex == -1 && item2ItemTypeIndex != -1) {
                    return 1;
                } else if (thisItemTypeIndex != -1 && item2ItemTypeIndex == -1) {
                    return -1;
                } else if (thisItemTypeIndex != -1) {
                    int itemTypeComparison = Integer.compare(thisItemTypeIndex, item2ItemTypeIndex);
                    if (itemTypeComparison != 0) return itemTypeComparison;
                }
            }

            // Name comparison. Returns here as it is the last comparison
            return this.name.compareTo(item2.name);
        }
    }
}