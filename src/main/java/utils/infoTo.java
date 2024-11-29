import java.io.*;
import java.sql.*;
import java.util.*;
import com.mongodb.client.*;
import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.model.Filters;

public class UserInfoProcessor {

    private Map<String, Object> user;
    private String lastWeiboId;
    private String lastWeiboDate;
    private Set<String> writeMode;
    private Map<String, String> userConfig;

    public UserInfoProcessor(Map<String, Object> user, Set<String> writeMode, Map<String, String> userConfig) {
        this.user = user;
        this.writeMode = writeMode;
        this.userConfig = userConfig;
    }

    public void userToCsv() throws IOException {
        // Define the file path
        String fileDir = System.getProperty("user.dir") + File.separator + "weibo";
        File dir = new File(fileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String filePath = fileDir + File.separator + "users.csv";
        this.lastWeiboId = "";
        this.lastWeiboDate = userConfig.get("since_date");

        // CSV Headers
        List<String> headers = Arrays.asList(
                "用户id", "昵称", "性别", "生日", "所在地", "学习经历", "公司", "注册时间",
                "阳光信用", "微博数", "粉丝数", "关注数", "简介", "主页", "头像", "高清头像",
                "微博等级", "会员等级", "是否认证", "认证类型", "认证信息", "上次记录微博信息"
        );

        // Prepare data row
        List<Object> resultData = new ArrayList<>();
        for (Object value : user.values()) {
            resultData.add(value instanceof String ? value : value.toString());
        }

        // Write to CSV (use a CSV utility or libraries like OpenCSV)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            if (new File(filePath).length() == 0) {
                writer.write(String.join(",", headers));
                writer.newLine();
            }
            writer.write(String.join(",", resultData.toString()));
            writer.newLine();
        }

        // Example of an external function to update CSV if needed
        String lastWeiboMsg = CsvUtil.insertOrUpdateUser(filePath, headers, resultData);
        this.lastWeiboId = lastWeiboMsg.split(" ")[0];
        this.lastWeiboDate = lastWeiboMsg.split(" ")[1];
    }

    public void userToMongoDB() {
        // MongoDB setup
        MongoClient client = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
        MongoDatabase database = client.getDatabase("weibo");
        MongoCollection<Document> collection = database.getCollection("user");

        // Insert user data
        Document userDoc = new Document();
        for (Map.Entry<String, Object> entry : user.entrySet()) {
            userDoc.append(entry.getKey(), entry.getValue());
        }

        collection.insertOne(userDoc);
        System.out.println(user.get("screen_name") + "信息写入MongoDB数据库完毕");

        client.close();
    }

    public void userToMySQL() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/weibo";
        String user = "root";
        String password = "123456";
        Connection connection = DriverManager.getConnection(url, user, password);

        // Create database and table if they don't exist
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS weibo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        String createTableSQL = "CREATE TABLE IF NOT EXISTS user (" +
                "id VARCHAR(20) NOT NULL, " +
                "screen_name VARCHAR(30), " +
                "gender VARCHAR(10), " +
                "statuses_count INT, " +
                "followers_count INT, " +
                "follow_count INT, " +
                "registration_time VARCHAR(20), " +
                "sunshine VARCHAR(20), " +
                "birthday VARCHAR(40), " +
                "location VARCHAR(200), " +
                "education VARCHAR(200), " +
                "company VARCHAR(200), " +
                "description VARCHAR(400), " +
                "profile_url VARCHAR(200), " +
                "profile_image_url VARCHAR(200), " +
                "avatar_hd VARCHAR(200), " +
                "urank INT, " +
                "mbrank INT, " +
                "verified BOOLEAN DEFAULT 0, " +
                "verified_type INT, " +
                "verified_reason VARCHAR(140), " +
                "PRIMARY KEY (id))";
        stmt.executeUpdate(createTableSQL);

        // Insert user data
        String insertSQL = "INSERT INTO user (id, screen_name, gender, statuses_count, followers_count, " +
                "follow_count, registration_time, sunshine, birthday, location, education, company, description, " +
                "profile_url, profile_image_url, avatar_hd, urank, mbrank, verified, verified_type, verified_reason) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pstmt = connection.prepareStatement(insertSQL);
        pstmt.setString(1, (String) user.get("id"));
        pstmt.setString(2, (String) user.get("screen_name"));
        pstmt.setString(3, (String) user.get("gender"));
        pstmt.setInt(4, (Integer) user.get("statuses_count"));
        pstmt.setInt(5, (Integer) user.get("followers_count"));
        pstmt.setInt(6, (Integer) user.get("follow_count"));
        pstmt.setString(7, (String) user.get("registration_time"));
        pstmt.setString(8, (String) user.get("sunshine"));
        pstmt.setString(9, (String) user.get("birthday"));
        pstmt.setString(10, (String) user.get("location"));
        pstmt.setString(11, (String) user.get("education"));
        pstmt.setString(12, (String) user.get("company"));
        pstmt.setString(13, (String) user.get("description"));
        pstmt.setString(14, (String) user.get("profile_url"));
        pstmt.setString(15, (String) user.get("profile_image_url"));
        pstmt.setString(16, (String) user.get("avatar_hd"));
        pstmt.setInt(17, (Integer) user.get("urank"));
        pstmt.setInt(18, (Integer) user.get("mbrank"));
        pstmt.setBoolean(19, (Boolean) user.get("verified"));
        pstmt.setInt(20, (Integer) user.get("verified_type"));
        pstmt.setString(21, (String) user.get("verified_reason"));
        pstmt.executeUpdate();

        System.out.println(user.get("screen_name") + "信息写入MySQL数据库完毕");

        connection.close();
    }

    public void userToDatabase() throws SQLException, IOException {
        // Save to CSV
        userToCsv();

        // Check which databases to write to based on writeMode
        if (writeMode.contains("mysql")) {
            userToMySQL();
        }
        if (writeMode.contains("mongo")) {
            userToMongoDB();
        }
        // Optionally, implement SQLite as needed
        if (writeMode.contains("sqlite")) {
            userToSQLite();
        }
    }

    public void userToSQLite() {
        // Implement SQLite saving logic here (similar to MySQL)
    }
}
