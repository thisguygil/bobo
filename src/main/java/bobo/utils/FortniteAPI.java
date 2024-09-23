package bobo.utils;

import bobo.Config;
import net.coobird.thumbnailator.Thumbnails;
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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class FortniteAPI {
    private static final String API_KEY = Config.get("FORTNITE_API_KEY");
    private static final String baseURL = "https://fortnite-api.com";

    private FortniteAPI() {} // Prevent instantiation

    // Shop enum & constants
    /**
     * Enum representing the different types of items in the Fortnite shop.
     */
    private enum ShopItemType {
        BUNDLE,
        BR_ITEM,
        INSTRUMENT,
        LEGO_KIT,
        CAR,
        TRACK
    }
    private static final int shopMargin = 20; // Number of pixels between the edge of the image and the content. Might be better to make this a percentage of the background image size, but it works either way
    private static final double shopLengthPerImagePercentage = 0.96; // Percentage of the length per square that each image should take up. Should be (1 - paddingPercentage)
    private static final double shopPaddingPercentage = 0.04; // Percentage of the length per square that should be padding. Should be (1 - availableWidthPerImagePercentage)
    private static final double shopTextPaddingPercentage = 0.02; // Percentage of the length per square that should be padding between the text
    private static final double shopFontSizePercentage = 0.12; // Percentage of the length per square that the font size should be

    /**
     * Gets the current Fortnite shop images.
     *
     * @return The current Fortnite shop images.
     */
    @Nullable
    public static List<BufferedImage> getShopImages() {
        // Get the shop items and vbuck icon URL
        String jsonResponse = sendGetRequest("/v2/shop");
        List<ShopItem> shopItems = parseShopItems(jsonResponse);
        String vbuckIconUrl = parseVbuckIconUrl(jsonResponse);

        return createShopImages(shopItems, vbuckIconUrl);
    }

    /**
     * Creates the shop images.
     *
     * @param shopItems The shop items.
     * @param vbuckIconUrl The vbuck icon URL.
     * @return The shop images.
     */
    private static List<BufferedImage> createShopImages(List<ShopItem> shopItems, String vbuckIconUrl) {
        try (InputStream backgroundInputStream = FortniteAPI.class.getResourceAsStream("/images/shop_background.jpg")) {
            // Check if the input stream is null
            if (backgroundInputStream == null) {
                return null;
            }

            BufferedImage background = ImageIO.read(backgroundInputStream);
            int contentWidth = background.getWidth() - (2 * shopMargin);
            int contentHeight = background.getHeight() - (2 * shopMargin);

            // Separate shop items into two lists: one with tracks and one without tracks
            List<ShopItem> nonTrackItems = shopItems.stream()
                    .filter(item -> item.shopItemType != ShopItemType.TRACK)
                    .toList();

            List<ShopItem> trackItems = shopItems.stream()
                    .filter(item -> item.shopItemType == ShopItemType.TRACK)
                    .toList();

            // Create images for both categories
            BufferedImage imageWithoutTracks = drawItems(nonTrackItems, vbuckIconUrl, contentWidth, contentHeight);
            BufferedImage imageOnlyTracks = drawItems(trackItems, vbuckIconUrl, contentWidth, contentHeight);

            if (imageWithoutTracks == null && imageOnlyTracks == null) {
                return List.of();
            } else if (imageWithoutTracks == null) {
                return List.of(imageOnlyTracks);
            } else if (imageOnlyTracks == null) {
                return List.of(imageWithoutTracks);
            } else {
                return List.of(imageWithoutTracks, imageOnlyTracks);
            }
        } catch (IOException | URISyntaxException | FontFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Draws the shop items on the background image.
     *
     * @param shopItems The shop items.
     * @param vbuckIconUrl The vbuck icon URL.
     * @param contentWidth The width of the content.
     * @param contentHeight The height of the content.
     * @return The image with the shop items drawn on it.
     */
    private static BufferedImage drawItems(List<ShopItem> shopItems, String vbuckIconUrl, int contentWidth, int contentHeight) throws IOException, FontFormatException, URISyntaxException {
        try (InputStream backgroundInputStream = FortniteAPI.class.getResourceAsStream("/images/shop_background.jpg");
             InputStream fontInputStream = FortniteAPI.class.getResourceAsStream("/fonts/FortniteFont.otf")) {
            // Check if the input streams are null
            if (backgroundInputStream == null || fontInputStream == null) {
                return null;
            }

            BufferedImage background = ImageIO.read(backgroundInputStream);
            Graphics2D g2d = background.createGraphics();
            Color opaqueGray = new Color(128, 128, 128, 128); // RGBA: Gray with 50% opacity for the background behind the text

            // Initialize the variables to hold the optimal configuration
            int numItems = shopItems.size();
            if (numItems == 0) {
                return null;
            }

            int maxSquareLength = 0;
            int bestImagesPerRow = 1;
            int bestNumRows = numItems;

            // Loop to find the optimal configuration of images per row and number of rows
            for (int imagesPerRow = 1; imagesPerRow <= numItems / 2; imagesPerRow++) {
                int numRows = (int) Math.ceil((double) numItems / imagesPerRow);

                // Calculate the maximum square length based on width
                int maxSquareLengthWidth = contentWidth / imagesPerRow;

                // Calculate the maximum square length based on height
                int maxSquareLengthHeight = contentHeight / numRows;

                // Determine the smallest of the two to fit both dimensions
                int squareLengthForThisConfiguration = Math.min(maxSquareLengthWidth, maxSquareLengthHeight);

                // Update the maximum square length and best configuration
                if (squareLengthForThisConfiguration > maxSquareLength) {
                    maxSquareLength = squareLengthForThisConfiguration;
                    bestImagesPerRow = imagesPerRow;
                    bestNumRows = numRows;
                }
            }

            // Calculate the number of images in the last row
            int imagesInLastRow = numItems - ((bestNumRows - 1) * bestImagesPerRow);

            // Calculate the font size and padding
            float fontSize = (float) (maxSquareLength * shopFontSizePercentage);
            int padding = (int) (maxSquareLength * shopPaddingPercentage);
            int textPadding = (int) (maxSquareLength * shopTextPaddingPercentage);

            // Load the font and its metrics
            Font fortniteFont = Font.createFont(Font.TRUETYPE_FONT, fontInputStream).deriveFont(fontSize);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(fortniteFont);
            g2d.setFont(fortniteFont);
            final FontMetrics[] fontMetrics = {g2d.getFontMetrics()};
            int stringHeight = fontMetrics[0].getHeight();
            int grayRectangleHeight = (stringHeight + textPadding) * 2;

            // Calculate the length per image
            int lengthPerImage = (int) (maxSquareLength * shopLengthPerImagePercentage);

            // Get and resize the vbuck icon
            BufferedImage vbuckIcon = resizeImage(ImageIO.read((new URI(vbuckIconUrl)).toURL()), (int) fontSize);
            int vbuckIconHeight = vbuckIcon.getHeight();
            int vbuckIconWidth = vbuckIcon.getWidth();

            List<CompletableFuture<BufferedImage>> imageFutures = shopItems.stream()
                    .map(item -> loadImageAsync(item.imageUrl(), lengthPerImage))
                    .toList();
            CompletableFuture<Void> allLoaded = CompletableFuture.allOf(imageFutures.toArray(new CompletableFuture[0]));

            int finalBestImagesPerRow = bestImagesPerRow;
            int finalBestNumRows = bestNumRows;
            int finalMaxSquareLength = maxSquareLength;

            return allLoaded.thenApply(_ -> {
                List<BufferedImage> images = imageFutures.stream()
                        .map(CompletableFuture::join)
                        .toList();

                // Initialize the x and y coordinates
                int x = (contentWidth - (finalMaxSquareLength * finalBestImagesPerRow) + padding) / 2 + shopMargin;
                int y = (contentHeight - (finalMaxSquareLength * finalBestNumRows) + padding) / 2 + shopMargin;

                // Save the initial x coordinate so that we can reset it when we move to the next row
                int initialX = x;

                // Draw the images and text
                int countRows = 1;
                int countImages = 1;
                for (int i = 0; i < numItems; i++) {
                    BufferedImage itemImage = images.get(i);
                    if (itemImage == null) {
                        continue;
                    }

                    int itemImageHeight = itemImage.getHeight();
                    int itemImageWidth = itemImage.getWidth();

                    // Move to the next row if the image won't fit
                    if (countImages > finalBestImagesPerRow) {
                        countImages = 1;
                        countRows++;

                        // If this is the last row, center the images
                        if (countRows == finalBestNumRows) {
                            x = (contentWidth - (finalMaxSquareLength * imagesInLastRow) + padding) / 2 + shopMargin;
                        } else {
                            x = initialX;
                        }

                        // Move to the next row
                        y += itemImageWidth + padding;
                    }

                    // Draw the image
                    g2d.drawImage(itemImage, x, y, null);

                    // Draw an opaque gray rectangle behind the text
                    int grayRectangleX = x;
                    int grayRectangleY = y + itemImageHeight - grayRectangleHeight;
                    g2d.setColor(opaqueGray);
                    g2d.fillRect(grayRectangleX, grayRectangleY, itemImageWidth, grayRectangleHeight);

                    // Change the color to white for the text
                    g2d.setColor(Color.WHITE);

                    // If the item name is too long, resize the font to fit it
                    String itemName = shopItems.get(i).name();
                    int itemNameWidth = fontMetrics[0].stringWidth(itemName);
                    if (itemNameWidth > itemImageWidth) {
                        float newFontSize = fontSize * ((float) itemImageWidth / itemNameWidth);
                        g2d.setFont(fortniteFont.deriveFont(newFontSize));
                        fontMetrics[0] = g2d.getFontMetrics();
                        itemNameWidth = fontMetrics[0].stringWidth(itemName);
                    }

                    // Draw the item name
                    int itemNameX = x + (itemImageWidth - itemNameWidth) / 2;
                    int itemNameY = y + itemImageHeight + (int) fontSize - grayRectangleHeight;
                    g2d.drawString(itemName, itemNameX, itemNameY);

                    // Reset the font to the original size
                    g2d.setFont(fortniteFont.deriveFont(fontSize));
                    fontMetrics[0] = g2d.getFontMetrics();

                    // Draw the item price
                    int itemPrice = shopItems.get(i).price();
                    int itemPriceWidth = fontMetrics[0].stringWidth(String.valueOf(itemPrice));
                    int itemPriceX = x + (itemImageWidth - itemPriceWidth) / 2;
                    int itemPriceY = itemNameY + (int) fontSize;
                    g2d.drawString(String.valueOf(itemPrice), itemPriceX, itemPriceY);

                    // Draw the vbuck icon
                    int vBuckIconX = itemPriceX - vbuckIconWidth - textPadding;
                    int vbuckIconY = itemPriceY - vbuckIconHeight + textPadding;
                    g2d.drawImage(vbuckIcon, vBuckIconX, vbuckIconY, null);

                    // Move to the next image
                    x += itemImageWidth + padding;
                    countImages++;
                }

                g2d.dispose();
                return background;
            }).exceptionally(e -> {
                e.printStackTrace();
                return null;
            }).join();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets the current Fortnite shop image asynchronously to prevent blocking the main thread.
     * @param imageUrl The URL of the image to load.
     * @param targetWidth The width to resize the image to.
     * @return The current Fortnite shop image.
     */
    public static CompletableFuture<BufferedImage> loadImageAsync(String imageUrl, int targetWidth) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Thumbnails.of(ImageIO.read((new URI(imageUrl)).toURL()))
                        .width(targetWidth)
                        .asBufferedImage();
            } catch (IOException | URISyntaxException | ArrayIndexOutOfBoundsException e) {
                System.err.println("Failed to load image: " + imageUrl + ". Error: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Resizes a {@link BufferedImage}.
     *
     * @param originalImage The image to resize.
     * @param targetWidth The new width.
     * @return The resized image.
     */
    @Nonnull
    private static BufferedImage resizeImage(@Nonnull BufferedImage originalImage, int targetWidth) {
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
     * Gets the stats image for a Fortnite player.
     *
     * @param username The Epic Games username of the Fortnite player.
     * @return The stats image for the Fortnite player.
     */
    public static String getStatsImage(String username) {
        String jsonResponse = sendGetRequest("/v2/stats/br/v2?name=" + username + "&image=all");
        if (jsonResponse == null) {
            return "Error getting stats for " + username;
        }

        JSONObject statsData = new JSONObject(jsonResponse);
        return switch (statsData.getInt("status")) {
            case 200 -> {
                // Raises JSONException if the image response is null
                try {
                    yield statsData.getJSONObject("data").getString("image");
                } catch (JSONException e) {
                    yield "Error getting stats for " + username;
                }
            }
            case 403 -> "Account stats are private for " + username;
            case 404 -> "Account does not exist or has no stats for " + username;
            default -> "Error getting stats for " + username;
        };
    }

    /**
     * Gets the current Battle Royale news image.
     *
     * @return The current Battle Royale news image.
     */
    public static String getNewsImage() {
        String jsonResponse = sendGetRequest("/v2/news/br");
        if (jsonResponse == null) {
            return "Error getting news";
        }

        JSONObject newsData = new JSONObject(jsonResponse);
        return switch (newsData.getInt("status")) {
            case 200 -> {
                // Raises JSONException if the image response is null
                try {
                    yield newsData.getJSONObject("data").getString("image");
                } catch (JSONException e) {
                    yield "Error getting current Battle Royale news";
                }
            }
            case 404 -> "News are empty or do not exist";
            default -> "Error getting current Battle Royale news";
        };
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
            String color = "";
            String rarity = "";
            String set = "";
            String itemType = "";

            // Check if the item has an asset, if not then skip it
            if (!item.has("newDisplayAsset")) {
                continue;
            }
            JSONObject newDisplayAsset;
            try {
                newDisplayAsset = item.getJSONObject("newDisplayAsset");
            } catch (JSONException e) {
                continue;
            }

            // Gets the item image URL and hex color if needed
            JSONArray materialInstances = newDisplayAsset.getJSONArray("materialInstances");
            if (!materialInstances.isEmpty()) {
                JSONObject images = materialInstances.getJSONObject(0).getJSONObject("images");
                if (images.has("Background")) {
                    imageUrl = images.getString("Background");
                } else if (images.has("OfferImage")) {
                    imageUrl = images.getString("OfferImage");
                } else if (images.has("CarTexture")) {
                    imageUrl = images.getString("CarTexture");
                } else {
                    continue;
                }
            } else {
                imageUrl = newDisplayAsset.getJSONArray("renderImages").getJSONObject(0).getString("image");
                color = item.getJSONObject("colors").getString("color1");
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
                try {
                    JSONObject itemObject = item.getJSONArray("legoKits").getJSONObject(0);
                    itemName = itemObject.getString("name");
                    shopItemType = ShopItemType.LEGO_KIT;
                    rarity = itemObject.getJSONObject("rarity").getString("value");
                    try {
                        set = itemObject.getJSONObject("set").getString("value");
                    } catch (JSONException ignored) {}
                    itemType = itemObject.getJSONObject("type").getString("value");
                } catch (JSONException ignored) {}
            }

            // Get the item price and add the item to the list
            int itemPrice = item.getInt("finalPrice");
            ShopItem shopItem = new ShopItem(shopItemType, itemName, itemPrice, imageUrl, color, rarity, set, itemType);
            if (!shopItems.contains(shopItem)) {
                shopItems.add(shopItem);
            }
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
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return EntityUtils.toString(entity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Record representing a shop item. Implements {@link Comparable} so that the items can be sorted.
     */
    private record ShopItem(ShopItemType shopItemType, String name, int price, String imageUrl, String backgroundColor, String rarity, String set, String itemType) implements Comparable<ShopItem> {
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

            // ShopItemType comparison
            int typeComparison = this.shopItemType.compareTo(item2.shopItemType);
            if (typeComparison != 0) return typeComparison;

            // Bundles and tracks don't have rarity, set, or item type, so we shouldn't compare them
            // Instruments don't have sets, so we shouldn't compare them
            if (this.shopItemType != ShopItemType.BUNDLE && item2.shopItemType != ShopItemType.TRACK) {
                // Check if items are in the same set. If so, then we don't need to compare rarity or set
                if (this.shopItemType != ShopItemType.INSTRUMENT && !this.set.equals(item2.set)) {
                    // Rarity comparison
                    int rarityComparison = this.rarity.compareTo(item2.rarity);
                    if (rarityComparison != 0) return rarityComparison;

                    // Set comparison
                    if (this.set.isEmpty() && !item2.set.isEmpty()) {
                        return -1;
                    } else if (!this.set.isEmpty() && item2.set.isEmpty()) {
                        return 1;
                    } else if (!this.set.isEmpty()) {
                        int setComparison = this.set.compareTo(item2.set);
                        if (setComparison != 0) return setComparison;
                    }
                } else {
                    if (this.shopItemType != ShopItemType.INSTRUMENT) {
                        // Rarity comparison
                        int rarityComparison = this.rarity.compareTo(item2.rarity);
                        if (rarityComparison != 0) return rarityComparison;
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
                } else {
                    int itemTypeComparison = Integer.compare(thisItemTypeIndex, item2ItemTypeIndex);
                    if (itemTypeComparison != 0) return itemTypeComparison;
                }
            }

            // Name comparison. Returns here as it is the last comparison
            return this.name.compareTo(item2.name);
        }

        /**
         * Checks if the items are equal. This is used to prevent duplicates in the shop items list.
         * @param obj   the reference object with which to compare.
         * @return true if this object is the same as the obj argument; false otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            ShopItem shopItem = (ShopItem) obj;
            return shopItemType == shopItem.shopItemType &&
                    name.equals(shopItem.name) &&
                    price == shopItem.price &&
                    imageUrl.equals(shopItem.imageUrl) &&
                    rarity.equals(shopItem.rarity) &&
                    set.equals(shopItem.set) &&
                    itemType.equals(shopItem.itemType);
        }
    }
}