package de.InVinoVeritas;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.SubmissionKind;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.references.SubmissionReference;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;
import java.util.logging.*;

/**
 * RyzomRedditBot fetches release notes from a specified website and posts new updates to a Reddit subreddit.
 */
public class RyzomRedditBot {
    // URL of the website containing release notes
    private static final String URL = "https://app.ryzom.com/app_releasenotes/index.php?lang=en&ig=1";

    // File to keep track of posted news IDs
    private static final String POSTED_FILE = "posted_news.txt";

    // Set to store IDs of news already posted
    private static final Set<Integer> postedNewsIds = new HashSet<>();

    // Logger for logging information and errors
    private static final Logger logger = Logger.getLogger(RyzomRedditBot.class.getName());

    /**
     * Main method to execute the bot.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) throws IOException {
        setupLogging();

        logger.info("Starting RyzomRedditBot in " + System.getProperty("user.dir") + "...");

        // Export configuration file if it doesn't exist
        exportConfigIfNotExists();

        // Load previously posted news IDs to avoid duplicates
        loadPostedNews();

        // Load configuration parameters
        Config config = new Config();
        logger.info("Using username '" + config.getRedditUsername() + "', client id '" + config.getClientId() +
                "' and subreddit '" + config.getSubreddit() + "' from the config.");

        // Initialize Reddit client with credentials
        RedditClient reddit = createRedditClient(config);
        logger.info("Reddit client initialized.");

        // Fetch and parse the release notes webpage
        Document doc = Jsoup.connect(URL).get();
        logger.info("Fetched website content.");

        // Extract news items from HTML
        List<NewsItem> newsItems = fetchNewsFromHtml(doc);
        logger.info("Found " + newsItems.size() + " news entries.");

        // Oldest news items should be posted first
        Collections.reverse(newsItems);

        // Iterate through each news item
        for (NewsItem news : newsItems) {
            // Check if this news has already been posted
            if (!postedNewsIds.contains(news.hashCode())) {
                try {
                    // Generate the Markdown content for the news
                    String content = generateNewsMarkdown(news);

                    // Post the news to Reddit
                    postToReddit(reddit, news, content, config.getSubreddit(), "Release Note");

                    // Record the posted news ID
                    savePostedNews(news.hashCode());
                    logger.info("Posted news ID: " + news.hashCode());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error processing news: " + news.hashCode(), e);
                }
            } else {
                logger.info("News ID already posted: " + news.hashCode());
            }
        }

        logger.info("RyzomRedditBot finished.");

        // Cleanup resources
        for (Handler handler : Logger.getLogger("").getHandlers()) {
            handler.close();
        }

        // Force JVM shutdown
        System.exit(0);
    }

    /**
     * Parses the HTML document to extract news items.
     *
     * @param doc Jsoup Document object representing the webpage
     * @return List of NewsItem objects
     */
    @NotNull
    private static List<NewsItem> fetchNewsFromHtml(Document doc) {
        List<NewsItem> newsItems = new ArrayList<>();

        // Find the main table with style containing 'margin: 0 auto'
        Element mainTable = doc.selectFirst("table[style*=margin: 0 auto]");
        if (mainTable == null) {
            // No main table found
            return newsItems;
        }

        // Get all rows within the main table
        Elements rows = mainTable.select("tr");
        String currentDateTitle;
        String currentUrl;
        String currentImageUrl;
        List<Headline> currentHeadlines;

        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements headerLinks = row.select("a[target=_blank]");

            if (!headerLinks.isEmpty()) {
                String linkText = Objects.requireNonNull(headerLinks.first()).text().trim();
                String linkHref = Objects.requireNonNull(headerLinks.first()).attr("href").trim();

                if (!linkText.equalsIgnoreCase("Release-Information")) {
                    // This is a date/title header
                    currentDateTitle = linkText; // e.g., "2025-04-15: New Event Hide n Hype"
                    currentUrl = linkHref;

                    // Reset image and headlines
                    currentImageUrl = null;
                    currentHeadlines = new ArrayList<>();

                    // Next row contains image and nested news table
                    if (i + 1 < rows.size()) {
                        Element imageNewsRow = rows.get(i + 1);
                        // Extract image URL if present
                        Element img = imageNewsRow.selectFirst("img");
                        if (img != null) {
                            currentImageUrl = img.attr("src");
                        }

                        // Extract nested news table
                        Element nestedTable = imageNewsRow.selectFirst("table");
                        if (nestedTable != null) {
                            currentHeadlines = parseHeadlinesAndPoints(nestedTable);
                        }

                        // Skip the next row as it's processed
                        i++;

                        // Save the news item
                        NewsItem item = new NewsItem(currentDateTitle, currentUrl, currentImageUrl, currentHeadlines);
                        newsItems.add(item);
                    }
                }
            }
        }
        return newsItems;
    }

    /**
     * Parses the nested news table to extract headlines and key points.
     *
     * @param newsTable The nested table element containing headlines and key points
     * @return List of Headline objects
     */
    private static List<Headline> parseHeadlinesAndPoints(Element newsTable) {
        List<Headline> headlines = new ArrayList<>();
        Elements rows = newsTable.select("tr");
        String currentHeadlineTitle = null;
        List<String> currentKeyPoints = null;

        for (Element row : rows) {
            Elements tds = row.select("td");
            if (tds.isEmpty()) continue;

            Element firstTd = tds.first();

            boolean isHeadline = false;
            String headlineTitle = null;

            // Check for nested table with style indicating headline
            assert firstTd != null;
            Element nestedTable = firstTd.selectFirst("table");

            if (nestedTable != null) {
                // Look for bold text within nested table
                Elements bolds = nestedTable.select("b");
                if (!bolds.isEmpty()) {
                    isHeadline = true;
                    headlineTitle = Objects.requireNonNull(bolds.first()).text().trim();
                } else {
                    // Fallback: use text from styleTd
                    Element styleTd = nestedTable.selectFirst("td[style*=border-bottom]");
                    if (styleTd != null) {
                        headlineTitle = styleTd.text().trim();
                        isHeadline = true;
                    }
                }
            } else {
                // Check if the row contains bold text indicating a headline
                Elements bolds = firstTd.select("b");
                if (!bolds.isEmpty()) {
                    isHeadline = true;
                    headlineTitle = Objects.requireNonNull(bolds.first()).text().trim();
                }
            }

            if (isHeadline) {
                // Save previous headline if exists
                if (currentHeadlineTitle != null) {
                    headlines.add(new Headline(currentHeadlineTitle, currentKeyPoints));
                }

                // Start new headline
                currentHeadlineTitle = headlineTitle;
                currentKeyPoints = new ArrayList<>();
            } else {
                // Parse key points under current headline
                if (currentHeadlineTitle != null && tds.size() > 1) {
                    Element keyPointsTd = tds.get(1);
                    String htmlContent = keyPointsTd.html();

                    // Split key points by the span containing the bullet symbol
                    String[] parts = htmlContent.split("<span[^>]*style=\"[^\"]*color: #08c[^\"]*\"[^>]*>✪</span>");
                    for (int j = 1; j < parts.length; j++) {
                        String part = parts[j];
                        String bulletPoint = Jsoup.parse(part).text().trim();
                        if (!bulletPoint.isEmpty()) {
                            currentKeyPoints.add(bulletPoint);
                        }
                    }
                }
            }
        }

        // Add the last headline if exists
        if (currentHeadlineTitle != null) {
            headlines.add(new Headline(currentHeadlineTitle, currentKeyPoints));
        }
        return headlines;
    }

    /**
     * Generates Markdown formatted text for a news item.
     *
     * @param item The news item
     * @return Markdown string representing the news content
     */
    private static String generateNewsMarkdown(NewsItem item) {
        StringBuilder sb = new StringBuilder();

        // Add each headline and its key points
        for (Headline headline : item.headlines) {
            sb.append("## ").append(headline.title).append("\n");
            if (headline.keyPoints != null && !headline.keyPoints.isEmpty()) {
                for (String point : headline.keyPoints) {
                    sb.append("- ").append(point).append("\n");
                }
                sb.append("\n");
            }
        }

        // Add the posting date
        if (!item.date.isEmpty()) {
            sb.append("\nOriginally published on ").append(item.date).append("\n\n");
        }

        // Add image if available
        if (item.imageUrl != null && !item.imageUrl.trim().isEmpty()) {
            sb.append("[View news image](").append(item.imageUrl).append(") \n");
        }

        // Add main link
        sb.append("[Read more here](").append(item.url).append(")\n");

        return sb.toString();
    }

    /**
     * Loads the set of posted news IDs from the file.
     */
    private static void loadPostedNews() {
        File file = new File(POSTED_FILE);
        try {
            if (!file.exists()) {
                boolean created = file.createNewFile();
                if (created) {
                    logger.info("Posted news file not found. Created new file: " + POSTED_FILE);
                } else {
                    logger.warning("Failed to create posted news file: " + POSTED_FILE);
                }
            }

            // Read existing IDs from file
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    try {
                        postedNewsIds.add(Integer.parseInt(line.trim()));
                    } catch (NumberFormatException e) {
                        logger.warning("Invalid news ID in posted news file: " + line);
                    }
                }
                logger.info("Loaded " + postedNewsIds.size() + " posted news IDs.");
            }
        } catch (IOException e) {
            logger.warning("Error reading posted news file: " + POSTED_FILE + " - " + e.getMessage());
        }
    }

    /**
     * Saves a news ID to the posted news file.
     *
     * @param newsId ID of the news to record
     */
    private static void savePostedNews(Integer newsId) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(POSTED_FILE, true))) {
            bw.write(newsId.toString());
            bw.newLine();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save posted news ID: " + newsId, e);
        }
    }

    /**
     * Creates and authenticates a Reddit client using provided configuration.
     *
     * @param config Configuration containing credentials
     * @return Authenticated RedditClient instance
     */
    private static RedditClient createRedditClient(Config config) {
        // Credentials for script-type Reddit app
        Credentials oauthCredentials = Credentials.script(
                config.getRedditUsername(),
                config.getRedditPassword(),
                config.getClientId(),
                config.getClientSecret()
        );

        // UserAgent for identification
        UserAgent userAgent = new UserAgent("RyzomRedditBot", config.getClientId(), "1.0.0", config.getRedditUsername());

        // Authenticate and create Reddit client
        return OAuthHelper.automatic(new OkHttpNetworkAdapter(userAgent), oauthCredentials);
    }

    /**
     * Posts a news update to a specified subreddit with a given flair.
     *
     * @param reddit    The authenticated Reddit client
     * @param news      The news item
     * @param content   Markdown content of the post
     * @param subreddit Target subreddit name
     * @param flairText Flair text to assign
     */
    private static void postToReddit(RedditClient reddit, NewsItem news, String content, String subreddit, String flairText) {
        try {
            StringBuilder title = new StringBuilder();

            // Compose the post title with date and optional news title
            if (news.title != null && !news.title.isEmpty()) {
                title.append(news.title).append("\n");
            } else {
                title.append(news.date).append("\n");
            }

            // Submit the post
            SubmissionReference submissionRef = reddit.subreddit(subreddit).submit(SubmissionKind.SELF, title.toString(), content, false);
            logger.info("Posted to Reddit with ID: " + submissionRef.getId());

            // Set flair
            submissionRef.flair("ryzom").updateToTemplate("", flairText);
            logger.info("Flair set to: " + flairText);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to post to Reddit with flair", e);
        }
    }

    /**
     * Sets up logging configuration for console and file output.
     */
    private static void setupLogging() {
        try {
            LogManager.getLogManager().reset();

            Logger rootLogger = Logger.getLogger("");

            // Remove default handlers
            for (Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }

            // Console handler
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new ShortLogFormatter());
            rootLogger.addHandler(consoleHandler);

            // File handler
            FileHandler fileHandler = new FileHandler("app.log", true);
            fileHandler.setFormatter(new ShortLogFormatter());
            rootLogger.addHandler(fileHandler);

            rootLogger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
        }
    }

    /**
     * Exports the configuration file from resources if it does not already exist.
     */
    private static void exportConfigIfNotExists() {
        String configFileName = "config.properties";
        File configFile = new File(configFileName);
        if (!configFile.exists()) {
            try (InputStream in = RyzomRedditBot.class.getResourceAsStream("/" + configFileName);
                 OutputStream out = new FileOutputStream(configFile)) {
                if (in == null) {
                    logger.severe("Resource " + configFileName + " not found in jar.");
                    return;
                }
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                logger.info("Exported " + configFileName + " to working directory.");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to export " + configFileName, e);
            }
        }
    }

    /**
     * Represents a news item with associated details.
     */
    static class NewsItem {
        final String date;      // e.g., "2025-03-07"
        final String title;     // e.g., "Rotate Outposts and Autocomplete !"
        final String url;
        final String imageUrl;
        final List<Headline> headlines;

        /**
         * Constructs a NewsItem, parsing date and title from the combined string.
         *
         * @param dateTitle Combined date and title string (e.g., "2025-03-07: Rotate Outposts")
         * @param url       Link to the news
         * @param imageUrl  Associated image URL
         * @param headlines List of headlines with key points
         */
        public NewsItem(String dateTitle, String url, String imageUrl, List<Headline> headlines) {
            if (dateTitle != null && dateTitle.contains(":")) {
                String[] parts = dateTitle.split(":", 2);
                this.date = parts[0].trim();
                this.title = parts.length > 1 ? parts[1].trim() : "";
            } else {
                this.date = dateTitle != null ? dateTitle.trim() : "";
                this.title = "";
            }
            this.url = url;
            this.imageUrl = imageUrl;
            this.headlines = headlines;
        }

        /**
         * Generates a hash code based on the URL, assuming uniqueness.
         *
         * @return Hash code for the news item
         */
        @Override
        public int hashCode() {
            return url.hashCode();
        }
    }

    /**
     * Represents a headline with associated key points.
     */
    record Headline(String title, List<String> keyPoints) {
    }
}