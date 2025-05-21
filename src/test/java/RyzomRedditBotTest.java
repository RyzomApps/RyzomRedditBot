import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RyzomRedditBotTest {
    private static final String TEST_POSTED_FILE = "test_posted_news.txt";

    // Helper methods to test static methods (simulate)
    private static void loadPostedNews(String filename, Set<String> set) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                set.add(line.trim());
            }
        }
    }

    private static void savePostedNews(String filename, String newsId) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true))) {
            bw.write(newsId);
            bw.newLine();
        }
    }

    @BeforeEach
    public void setup() throws IOException {
        // Ensure test file is clean before each test
        Files.deleteIfExists(Path.of(TEST_POSTED_FILE));
    }

    @AfterEach
    public void cleanup() throws IOException {
        Files.deleteIfExists(Path.of(TEST_POSTED_FILE));
    }

    @Test
    public void testLoadPostedNews_emptyFile() throws IOException {
        // Create empty file
        Files.createFile(Path.of(TEST_POSTED_FILE));
        // Load
        Set<String> loaded = new HashSet<>();
        loadPostedNews(TEST_POSTED_FILE, loaded);
        assertTrue(loaded.isEmpty(), "Loaded set should be empty for empty file");
    }

    @Test
    public void testLoadPostedNews_withData() throws IOException {
        String[] lines = {"abc123", "def456"};
        Files.writeString(Path.of(TEST_POSTED_FILE), String.join("\n", lines));
        Set<String> loaded = new HashSet<>();
        loadPostedNews(TEST_POSTED_FILE, loaded);
        assertEquals(2, loaded.size());
        assertTrue(loaded.contains("abc123"));
        assertTrue(loaded.contains("def456"));
    }

    @Test
    public void testSavePostedNews_appends() throws IOException {
        // Write initial data
        Path path = Path.of(TEST_POSTED_FILE);

        Files.writeString(path, "initial");
        savePostedNews(TEST_POSTED_FILE, "xyz");
        String content = Files.readString(path);
        assertTrue(content.contains("initial"));
        assertTrue(content.contains("xyz"));
    }
}