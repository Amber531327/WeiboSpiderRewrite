package getWeiboInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
        import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
        import java.util.stream.Collectors;

public class getWeiboConfig {

    private static final Logger logger = LoggerFactory.getLogger(WeiboCrawler.class);

    private List<Map<String, Object>> userConfigList;  // List of user configurations
    private Map<String, Object> userConfig;  // A single user configuration
    private List<Map<String, Object>> weibo;  // Weibo data
    private String query;  // Query string for crawling
    private int gotCount;  // Count of Weibo posts collected
    private List<String> weiboIdList;  // List of Weibo IDs
    private String userConfigFilePath;  // Path to the user config file
    private String sinceDate = "2024-01-01T00:00:00";  // Default since date
    private List<String> queryList = new ArrayList<>();  // Default query list

    // Constructor
    public WeiboCrawler() {
        this.userConfigList = new ArrayList<>();
        this.weibo = new ArrayList<>();
        this.weiboIdList = new ArrayList<>();
    }

    // Method to read user configuration from a file
    public List<Map<String, Object>> getUserConfigList(String filePath) {
        try {
            List<String> lines = Files.readAllLines(new File(filePath).toPath(), StandardCharsets.UTF_8);
            List<Map<String, Object>> userConfigList = new ArrayList<>();

            // Parse each line in the configuration file
            for (String line : lines) {
                String[] info = line.trim().split(" ");
                if (info.length > 0 && isNumeric(info[0])) {
                    Map<String, Object> userConfig = new HashMap<>();
                    userConfig.put("user_id", info[0]);

                    // Handle the `since_date` based on the configuration line
                    if (info.length == 3) {
                        String dateValue = info[2];
                        if (isDateTime(dateValue)) {
                            userConfig.put("since_date", dateValue);
                        } else if (isDate(dateValue)) {
                            userConfig.put("since_date", dateValue + "T00:00:00");
                        } else if (isNumeric(dateValue)) {
                            int daysAgo = Integer.parseInt(dateValue);
                            Calendar calendar = Calendar.getInstance();
                            calendar.add(Calendar.DATE, -daysAgo);
                            userConfig.put("since_date", calendar.getTime().toString());
                        } else {
                            logger.error("since_date format is incorrect. Please check the configuration.");
                            System.exit(1);
                        }
                    } else {
                        userConfig.put("since_date", this.sinceDate);
                    }

                    // Handle the query list if available
                    if (info.length > 3) {
                        userConfig.put("query_list", Arrays.asList(info[3].split(",")));
                    } else {
                        userConfig.put("query_list", this.queryList);
                    }

                    if (!userConfigList.contains(userConfig)) {
                        userConfigList.add(userConfig);
                    }
                }
            }

            return userConfigList;
        } catch (IOException e) {
            logger.error("Error reading the file: {}", filePath, e);
            System.exit(1);
        }
        return new ArrayList<>();
    }

    // Method to initialize crawler information
    public void initializeInfo(Map<String, Object> userConfig) {
        this.weibo = new ArrayList<>();
        this.userConfig = userConfig;
        this.gotCount = 0;
        this.weiboIdList = new ArrayList<>();
    }

    // Method to start the crawling process
    public void start() {
        try {
            for (Map<String, Object> userConfig : this.userConfigList) {
                List<String> queryList = (List<String>) userConfig.get("query_list");
                if (queryList != null && !queryList.isEmpty()) {
                    for (String query : queryList) {
                        this.query = query;
                        initializeInfo(userConfig);
                        getPages();
                    }
                } else {
                    initializeInfo(userConfig);
                    getPages();
                }
                logger.info("Crawling completed for user configuration.");
                logger.info("*".repeat(100));

                if (this.userConfigFilePath != null && !this.userConfig.isEmpty()) {
                    updateUserConfigFile(this.userConfigFilePath);
                }
            }
        } catch (Exception e) {
            logger.error("Error during the crawling process.", e);
        }
    }

    // Simulated method to get pages (to be implemented with actual crawling logic)
    private void getPages() {
        // Logic for crawling pages
        logger.info("Getting pages for query: {}", this.query);
    }

    // Method to update the user configuration file
    private void updateUserConfigFile(String filePath) {
        // Logic to update user config file
        logger.info("Updating user config file: {}", filePath);
    }

    // Helper methods for date and numeric validation
    private boolean isNumeric(String str) {
        return str != null && str.matches("[0-9]+");
    }

    private boolean isDate(String dateStr) {
        // Add logic to check if the string is a valid date
        return true;
    }

    private boolean isDateTime(String dateTimeStr) {
        // Add logic to check if the string is a valid datetime
        return true;
    }

    // Helper method to rename keys in the configuration
    private static void handleConfigRenaming(Map<String, Object> config, String oldName, String newName) {
        if (config.containsKey(oldName) && !config.containsKey(newName)) {
            config.put(newName, config.get(oldName));
            config.remove(oldName);
        }
    }

    // Method to get configuration from a JSON file
    public Map<String, Object> getConfig() {
        String configPath = new File(WeiboCrawler.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile() + File.separator + "config.json";
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            logger.warn("Config file config.json not found in path: {}", configPath);
            System.exit(1);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
            String jsonContent = reader.lines().collect(Collectors.joining("\n"));
            Map<String, Object> config = new HashMap<>();  // Assuming JSON to Map conversion (using a library like Jackson or Gson)
            // handleConfigRenaming(config, oldName="filter", newName="only_crawl_original");
            // handleConfigRenaming(config, oldName="result_dir_name", newName="user_id_as_folder_name");
            return config;
        } catch (IOException e) {
            logger.error("Error reading config.json file.", e);
            System.exit(1);
        }
        return new HashMap<>();
    }

    public static void main(String[] args) {
        WeiboCrawler weiboCrawler = new WeiboCrawler();

        // Example of getting the user configuration list
        String filePath = "path/to/your/config/file.txt";
        List<Map<String, Object>> userConfigList = weiboCrawler.getUserConfigList(filePath);
        System.out.println("User Config List: " + userConfigList);

        // Example of starting the crawling process
        weiboCrawler.start();
    }
}
