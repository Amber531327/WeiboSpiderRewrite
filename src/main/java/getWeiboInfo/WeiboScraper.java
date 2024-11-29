package getWeiboInfo;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.TimeUnit;

public class WeiboScraper {

    private static final Logger logger = Logger.getLogger(WeiboScraper.class.getName());
    private Map<String, Object> userConfig;
    private Map<String, Object> user;
    private Set<String> writeMode = new HashSet<>();
    private int longSleepCountBeforeEachUser = 0;
    private String headers = "your_headers_here"; // Placeholder for headers

    public WeiboScraper(Map<String, Object> userConfig) {
        this.userConfig = userConfig;
    }

    public int getUserInfo() throws Exception {
        String containerId = "100505" + userConfig.get("user_id").toString();
        Map<String, String> params = new HashMap<>();
        params.put("containerid", containerId);

        if (longSleepCountBeforeEachUser > 0) {
            int sleepTime = new Random().nextInt(31) + 30;
            logger.info("短暂sleep " + sleepTime + "秒，避免被ban");
            TimeUnit.SECONDS.sleep(sleepTime);
            logger.info("sleep结束");
        }
        longSleepCountBeforeEachUser++;

        JSONObject js = getJson(params);
        if (js == null || js.getInt("status_code") != 200) {
            logger.info("被ban了，需要等待一段时间");
            System.exit(1);
        }

        if (js.getBoolean("ok")) {
            JSONObject userInfo = js.getJSONObject("data").getJSONObject("userInfo");
            Map<String, String> userInformation = new HashMap<>();
            userInformation.put("id", userConfig.get("user_id").toString());
            userInformation.put("screen_name", userInfo.optString("screen_name", ""));
            userInformation.put("gender", userInfo.optString("gender", ""));

            // Additional user info
            Map<String, String> additionalInfo = getAdditionalUserInfo(userConfig.get("user_id").toString());
            userInformation.putAll(additionalInfo);

            // Process user stats and details
            userInformation.put("statuses_count", String.valueOf(userInfo.optInt("statuses_count", 0)));
            userInformation.put("followers_count", String.valueOf(userInfo.optInt("followers_count", 0)));
            userInformation.put("follow_count", String.valueOf(userInfo.optInt("follow_count", 0)));
            userInformation.put("description", userInfo.optString("description", ""));
            userInformation.put("profile_url", userInfo.optString("profile_url", ""));
            userInformation.put("profile_image_url", userInfo.optString("profile_image_url", ""));
            userInformation.put("avatar_hd", userInfo.optString("avatar_hd", ""));
            userInformation.put("urank", String.valueOf(userInfo.optInt("urank", 0)));
            userInformation.put("mbrank", String.valueOf(userInfo.optInt("mbrank", 0)));
            userInformation.put("verified", String.valueOf(userInfo.optBoolean("verified", false)));
            userInformation.put("verified_type", String.valueOf(userInfo.optInt("verified_type", -1)));
            userInformation.put("verified_reason", userInfo.optString("verified_reason", ""));

            this.user = standardizeInfo(userInformation);
            userToDatabase();

            return 0;
        } else {
            logger.info("user_id_list中 " + userConfig.get("user_id") + " id出错");
            return -1;
        }
    }

    private Map<String, String> getAdditionalUserInfo(String userId) throws Exception {
        Map<String, String> result = new HashMap<>();
        String containerId = "230283" + userId + "_-_INFO";
        Map<String, String> params = new HashMap<>();
        params.put("containerid", containerId);

        JSONObject js = getJson(params);
        if (js != null && js.getBoolean("ok")) {
            JSONArray cards = js.getJSONObject("data").getJSONArray("cards");
            if (cards.length() > 1) {
                JSONArray cardList = cards.getJSONObject(0).getJSONArray("card_group");
                cardList.putAll(cards.getJSONObject(1).getJSONArray("card_group"));
                List<String> zhList = Arrays.asList("生日", "所在地", "小学", "初中", "高中", "大学", "公司", "注册时间", "阳光信用");
                List<String> enList = Arrays.asList("birthday", "location", "education", "education", "education", "education", "company", "registration_time", "sunshine");

                for (int i = 0; i < cardList.length(); i++) {
                    JSONObject card = cardList.getJSONObject(i);
                    String itemName = card.optString("item_name");
                    if (zhList.contains(itemName)) {
                        result.put(enList.get(zhList.indexOf(itemName)), card.optString("item_content", ""));
                    }
                }
            }
        }
        return result;
    }

    private JSONObject getJson(Map<String, String> params) throws Exception {
        String urlString = "https://weibo.com/api_endpoint";  // Placeholder for the actual URL
        URL url = new URL(urlString + "?" + getParamsString(params));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", headers);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return new JSONObject(response.toString());
    }

    private String getParamsString(Map<String, String> params) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            try {
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                result.append("&");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return result.toString();
    }

    private Map<String, String> standardizeInfo(Map<String, String> userInfo) {
        // Here you can apply any standardization rules to the data
        return userInfo;
    }

    private void userToDatabase() throws SQLException {
        if (writeMode.contains("mysql")) {
            userToMySQL();
        }
        if (writeMode.contains("mongo")) {
            userToMongoDB();
        }
        if (writeMode.contains("sqlite")) {
            userToSQLite();
        }
    }

    private void userToMySQL() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/weibo";
        String user = "root";
        String password = "123456";
        Connection connection = DriverManager.getConnection(url, user, password);

        String insertSQL = "INSERT INTO user (id, screen_name, gender, statuses_count, followers_count, " +
                "follow_count, registration_time, sunshine, birthday, location, education, company, description, " +
                "profile_url, profile_image_url, avatar_hd, urank, mbrank, verified, verified_type, verified_reason) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pstmt = connection.prepareStatement(insertSQL);
        pstmt.setString(1, user.get("id").toString());
        pstmt.setString(2, user.get("screen_name").toString());
        pstmt.setString(3, user.get("gender").toString());
        pstmt.setInt(4, Integer.parseInt(user.get("statuses_count").toString()));
        pstmt.setInt(5, Integer.parseInt(user.get("followers_count").toString()));
        pstmt.setInt(6, Integer.parseInt(user.get("follow_count").toString()));
        pstmt.setString(7, user.get("registration_time").toString());
        pstmt.setString(8, user.get("sunshine").toString());
        pstmt.setString(9, user.get("birthday").toString());
        pstmt.setString(10, user.get("location").toString());
        pstmt.setString(11, user.get("education").toString());
        pstmt.setString(12, user.get("company").toString());
        pstmt.setString(13, user.get("description").toString());
        pstmt.setString(14, user.get("profile_url").toString());
        pstmt.setString(15, user.get("profile_image_url").toString());
        pstmt.setString(16, user.get("avatar_hd").toString());
        pstmt.setInt(17, Integer.parseInt(user.get("urank").toString()));
        pstmt.setInt(18, Integer.parseInt(user.get("mbrank").toString()));
        pstmt.setBoolean(19, Boolean.parseBoolean(user.get("verified").toString()));
        pstmt.setInt(20, Integer.parseInt(user.get("verified_type").toString()));
        pstmt.setString(21, user.get("verified_reason").toString());

        pstmt.executeUpdate();
        connection.close();
    }

    private void userToSQLite() throws SQLException {
        // SQLite insertion code here
    }

    private void userToMongoDB() {
        // MongoDB insertion code here
    }

    public static void main(String[] args) {
        try {
            Map<String, Object> userConfig = new HashMap<>();
            userConfig.put("user_id", "123456789");
            WeiboScraper scraper = new WeiboScraper(userConfig);
            scraper.getUserInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
