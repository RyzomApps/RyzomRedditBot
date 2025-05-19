package de.invinoveritas;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private final String redditUsername;
    private final String redditPassword;
    private final String clientId;
    private final String clientSecret;
    private final String subreddit;

    public Config() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream("config.properties")) {
            props.load(in);
            this.redditUsername = getPropertyOrThrow(props, "reddit.username");
            this.redditPassword = getPropertyOrThrow(props, "reddit.password");
            this.clientId = getPropertyOrThrow(props, "reddit.client.id");
            this.clientSecret = getPropertyOrThrow(props, "reddit.client.secret");
            this.subreddit = getPropertyOrThrow(props, "reddit.subreddit");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration: " + e.getMessage(), e);
        }
    }

    private String getPropertyOrThrow(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Missing property: " + key);
        }
        return value.trim();
    }

    public String getRedditUsername() {
        return redditUsername;
    }

    public String getRedditPassword() {
        return redditPassword;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getSubreddit() {
        return subreddit;
    }
}