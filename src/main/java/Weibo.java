import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import utils.ConfigReader;
import utils.CsvUtil;
import utils.WeiboValidator;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.sun.org.apache.xalan.internal.xsltc.runtime.BasisLibrary.stringToInt;

@Slf4j
public class Weibo {
    private static final String DTFORMAT = "yyyy-MM-dd";
    private int onlyCrawlOriginal;
    private int removeHtmlTag;
    private String sinceDate;
    private int startPage;
    private List<String> writeMode;
    private int originalPicDownload;
    private int retweetPicDownload;
    private int originalVideoDownload;
    private int retweetVideoDownload;
    private int downloadComment;
    private int commentMaxDownloadCount;
    private int downloadRepost;
    private int repostMaxDownloadCount;
    private int userIdAsFolderName;
    private Map<String, String> headers;
    private Map<String, String> mysqlConfig;
    private String mongodbURI;
    private String userConfigFilePath;
    private List<Map<String, Object>> userConfigList;
    private Map<String, Object> userConfig;
    private String startDate;
    private String query;
    private Map<String, Object> user;
    private int gotCount;
    private List<Map<String, Object>> weibo;
    private List<String> weiboIdList;
    private int longSleepCountBeforeEachUser;
    private String userCsvFilePath;
    private String lastWeiboId;
    private Object lastWeiboDate;

    public Weibo(Properties config) {
        setupLogger();

        WeiboValidator weiboValidator = new WeiboValidator("src/main/resources/config/config.properties");
        // 调用方法并传递 Properties 对象作为参数
        initializeConfig(config);

    }

    private void setupLogger() {
        // 检查日志文件夹是否存在，如果不存在，则创建它
        File logDir = new File("log");
        if (!logDir.exists()) {
            try {
                Files.createDirectories(Paths.get("log"));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // 加载日志配置文件
        String loggingPath = new File(getClass().getClassLoader().getResource("logging.conf").getFile()).getPath();

        // 初始化日志记录器
        LogManager.getLogManager().reset();
        Logger logger = Logger.getLogger("weibo");
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(loggingPath));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println(("日志创建成功!"));
    }


    // 初始化配置方法
    private void initializeConfig(Properties config) {
        // 解析配置信息并赋值给对应的成员变量
        this.onlyCrawlOriginal = Integer.parseInt(config.getProperty("only_crawl_original"));
        this.removeHtmlTag = Integer.parseInt(config.getProperty("remove_html_tag"));
        this.sinceDate = parseSinceDate(config.getProperty("since_date"));
        this.startPage = Integer.parseInt(config.getProperty("start_page"));
        this.writeMode = parseWriteMode(config.getProperty("write_mode"));
        this.originalPicDownload = Integer.parseInt(config.getProperty("original_pic_download"));
        this.retweetPicDownload = Integer.parseInt(config.getProperty("retweet_pic_download"));
        this.originalVideoDownload = Integer.parseInt(config.getProperty("original_video_download"));
        this.retweetVideoDownload = Integer.parseInt(config.getProperty("retweet_video_download"));
        this.downloadComment = Integer.parseInt(config.getProperty("download_comment"));
        this.commentMaxDownloadCount = Integer.parseInt(config.getProperty("comment_max_download_count"));
        this.downloadRepost = Integer.parseInt(config.getProperty("download_repost"));
        this.repostMaxDownloadCount = Integer.parseInt(config.getProperty("repost_max_download_count"));
        this.userIdAsFolderName = Integer.parseInt(config.getProperty("user_id_as_folder_name"));
        this.headers = new HashMap<>();
        this.headers.put("User_Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
        this.headers.put("Cookie", config.getProperty("cookie"));
        this.mysqlConfig = new HashMap<>();
        this.mysqlConfig.put("host", config.getProperty("mysql_config.host"));
        this.mysqlConfig.put("port", config.getProperty("mysql_config.port"));
        this.mysqlConfig.put("username", config.getProperty("mysql_config.username"));
        this.mysqlConfig.put("password", config.getProperty("mysql_config.password"));
        this.mysqlConfig.put("database", config.getProperty("mysql_config.database"));
        this.mongodbURI = config.getProperty("mongodb_URI");
        this.userConfigFilePath = config.getProperty("user_id_list");
        this.userConfigList = parseUserConfigList(config.getProperty("user_id_list"));
        this.userConfig = new HashMap<>();
        this.startDate = "";
        this.query = "";
        this.user = new HashMap<>();
        this.gotCount = 0;
        this.weibo = new ArrayList<>();
        this.weiboIdList = new ArrayList<>();
        this.longSleepCountBeforeEachUser = 0;
    }

    // 解析 since_date 方法
    private String parseSinceDate(String sinceDate) {
        // 根据 since_date 类型不同进行解析
        try {
            int days = Integer.parseInt(sinceDate);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -days);
            return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(cal.getTime());
        } catch (NumberFormatException e) {
            // 如果不是整数，则直接返回
            return sinceDate;
        }
    }

    // 解析 write_mode 方法
    private List<String> parseWriteMode(String writeMode) {
        // 如果 write_mode 是字符串，按逗号分隔转换为列表
        return Arrays.asList(writeMode.split(","));
    }

    // 解析 user_id_list 方法
    private List<Map<String, Object>> parseUserConfigList(String userConfigList) {
        List<Map<String, Object>> result = new ArrayList<>();
        // 如果 user_id_list 是字符串，则解析为列表
        List<String> userIdList = Arrays.asList(userConfigList.split(","));
        System.out.println(userIdList);
        // 遍历用户 ID 列表，构建用户配置列表
        for (String userId : userIdList) {
            Map<String, Object> userConfig = new HashMap<>();
            userConfig.put("user_id", userId);
            userConfig.put("since_date", this.sinceDate);
            userConfig.put("query_list", this.query);
            result.add(userConfig);
        }
        return result;
    }


    // 获取网页中 JSON 数据
    private Map<String, Object> getJson(Map<String, String> params) throws IOException {
        String url = "https://m.weibo.cn/api/container/getIndex?";
        URL apiUrl = new URL(url + buildQueryString(params));
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return parseJson(response.toString());
            }
        } else {
            System.out.println("HTTP GET request failed with response code: " + responseCode);
            return null;
        }
    }

    // 获取网页中微博 JSON 数据
    public Map<String, Object> getWeiboJson(int page) throws IOException {
        Map<String, String> params = new HashMap<>();
        if (this.query != null && !this.query.isEmpty()) {
            params.put("container_ext", "profile_uid:" + this.userConfig.get("user_id"));
            params.put("containerid", "100103type=401&q=" + this.query);
            params.put("page_type", "searchall");
        } else {
            params.put("containerid", "230413" + this.userConfig.get("user_id"));
        }
        params.put("page", String.valueOf(page));
        return getJson(params);
    }

    // 构建查询字符串
    private String buildQueryString(Map<String, String> params) {
        StringBuilder paramString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (paramString.length() != 0) {
                paramString.append("&");
            }
            paramString.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return paramString.toString();
    }

    // 解析 JSON 字符串
    private Map<String, Object> parseJson(String jsonStr) {
        Gson gson = new Gson();
        return gson.fromJson(jsonStr, new TypeToken<Map<String, Object>>() {}.getType());
    }

    // 将爬取到的用户信息写入 CSV 文件
    public void userToCsv() {
        String fileDir = System.getProperty("user.dir") + File.separator + "weibo";
        File dir = new File(fileDir);
        if (!dir.exists()) {
            try {
                Files.createDirectories(Paths.get(fileDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String filePath = fileDir + File.separator + "users.csv";
        this.userCsvFilePath = filePath;
        String[] resultHeaders = {
                "用户id",
                "昵称",
                "性别",
                "生日",
                "所在地",
                "学习经历",
                "公司",
                "注册时间",
                "阳光信用",
                "微博数",
                "粉丝数",
                "关注数",
                "简介",
                "主页",
                "头像",
                "高清头像",
                "微博等级",
                "会员等级",
                "是否认证",
                "认证类型",
                "认证信息",
                "上次记录微博信息"
        };

        // 创建 resultData
        List<String[]> resultData = new ArrayList<>();
        Collection<Object> values = user.values();
        String[] dataArray = new String[values.size()];
        int i = 0;
        for (Object value : values) {
            dataArray[i++] = value != null ? value.toString() : "";
            System.out.println(value);
        }
        resultData.add(dataArray);
        System.out.println(resultData);
        //TODO：这里要改
        utils.Logger logger = new utils.Logger();
        String lastWeiboMsg = CsvUtil.insertOrUpdateUser(logger, resultHeaders, resultData, filePath);
        this.lastWeiboId = lastWeiboMsg.isEmpty() ? "" : lastWeiboMsg.split(" ")[0];
        this.lastWeiboDate = lastWeiboMsg.isEmpty() ? userConfig.get("since_date") : lastWeiboMsg.split(" ")[1];
    }
    //将爬取的信息写入MongoDB数据库
    public void infoToMongoDB(String collectionName,List<Map<String, Object>> infoList) {

        // 创建 MongoDB 客户端
        MongoClient mongoClient = MongoClients.create(mongodbURI);
        // 连接到指定数据库
        MongoDatabase database = mongoClient.getDatabase("weibo");
        // 获取集合
        MongoCollection<Document> collection = database.getCollection(collectionName);

        // 遍历信息列表并写入 MongoDB
        for (Map<String, Object> info : infoList) {
            Document document = new Document(info);
            // 检查集合中是否已存在该信息
            if (collection.find(Filters.eq("id", info.get("id"))).first() == null) {
                // 插入新信息
                collection.insertOne(document);
            } else {
                // 更新已有信息
                collection.updateOne(Filters.eq("id", info.get("id")), new Document("$set", document));
            }
        }
        mongoClient.close();
    }
    //将爬取的用户信息写入MongoDB数据库
    public void user_to_mongodb(){
        List<Map<String, Object>> user_list = Collections.singletonList(user);
        infoToMongoDB("user",user_list);
        log.info("信息写入MongoDB数据库完毕");
    }

    private static final String URL = "jdbc:mysql://localhost:3306";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    public void createDatabase() {
        String sql = "CREATE DATABASE IF NOT EXISTS weibo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("成功创建sql数据库！");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS weibo.user (
                id VARCHAR(20) NOT NULL,
                screen_name VARCHAR(30),
                gender VARCHAR(10),
                statuses_count INT,
                followers_count INT,
                follow_count INT,
                registration_time VARCHAR(20),
                sunshine VARCHAR(20),
                birthday VARCHAR(40),
                location VARCHAR(200),
                education VARCHAR(200),
                company VARCHAR(200),
                description VARCHAR(400),
                profile_url VARCHAR(200),
                profile_image_url VARCHAR(200),
                avatar_hd VARCHAR(200),
                urank INT,
                mbrank INT,
                verified BOOLEAN DEFAULT 0,
                verified_type INT,
                verified_reason VARCHAR(140),
                PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";
        try (Connection conn = DriverManager.getConnection(URL + "/weibo", USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("成功创建sql数据表");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void insertUsers(List<Map<String, Object>> dataList) {
        String sql = """
            INSERT INTO weibo.user (id, screen_name, gender, statuses_count, followers_count, follow_count,
            registration_time, sunshine, birthday, location, education, company, description, profile_url,
            profile_image_url, avatar_hd, urank, mbrank, verified, verified_type, verified_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (Connection conn = DriverManager.getConnection(URL + "/weibo", USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Map<String, Object> userData : dataList) {
                pstmt.setString(1, (String) userData.get("id"));
                pstmt.setString(2, (String) userData.get("screen_name"));
                pstmt.setString(3, (String) userData.get("gender"));
                pstmt.setInt(4, (int) userData.get("statuses_count"));
                pstmt.setInt(5, (int) userData.get("followers_count"));
                pstmt.setInt(6, (int) userData.get("follow_count"));
                pstmt.setString(7, (String) userData.get("registration_time"));
                pstmt.setString(8, (String) userData.get("sunshine"));
                pstmt.setString(9, (String) userData.get("birthday"));
                pstmt.setString(10, (String) userData.get("location"));
                pstmt.setString(11, (String) userData.get("education"));
                pstmt.setString(12, (String) userData.get("company"));
                pstmt.setString(13, (String) userData.get("description"));
                pstmt.setString(14, (String) userData.get("profile_url"));
                pstmt.setString(15, (String) userData.get("profile_image_url"));
                pstmt.setString(16, (String) userData.get("avatar_hd"));
                pstmt.setInt(17, (int) userData.get("urank"));
                pstmt.setInt(18, (int) userData.get("mbrank"));
                pstmt.setBoolean(19, (boolean) userData.get("verified"));
                pstmt.setInt(20, (int) userData.get("verified_type"));
                pstmt.setString(21, (String) userData.get("verified_reason"));

                pstmt.addBatch();
            }

            pstmt.executeBatch();
            log.info("Users inserted successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    //将数据写入MySQL数据库
    public void user_to_mysql(){
        List<Map<String, Object>> user_list = Collections.singletonList(user);
        createDatabase();
        createTable();
        insertUsers(user_list);
        log.info("信息写入Mysql数据库完毕");
    }
    ////将数据写入sqlite数据库
    public void user_to_sqlite() {
        Connection con = getSqliteConnection();
        sqliteInsertUser(con, this.user);
        closeSqliteConnection(con);
    }

    private void sqliteInsertUser(Connection con, Map<String, Object> user) {
        Map<String, Object> sqliteUser = parseSqliteUser(user);
        sqliteInsert(con, sqliteUser, "user");
    }

    private Map<String, Object> parseSqliteUser(Map<String, Object> user) {
        if (user == null || user.isEmpty()) {
            return null;
        }
        Map<String, Object> sqliteUser = new LinkedHashMap<>();
        sqliteUser.put("id", user.get("id"));
        sqliteUser.put("nick_name", user.get("screen_name"));
        sqliteUser.put("gender", user.get("gender"));
        sqliteUser.put("follower_count", user.get("followers_count"));
        sqliteUser.put("follow_count", user.get("follow_count"));
        sqliteUser.put("birthday", user.get("birthday"));
        sqliteUser.put("location", user.get("location"));
        sqliteUser.put("edu", user.get("education"));
        sqliteUser.put("company", user.get("company"));
        sqliteUser.put("reg_date", user.get("registration_time"));
        sqliteUser.put("main_page_url", user.get("profile_url"));
        sqliteUser.put("avatar_url", user.get("avatar_hd"));
        sqliteUser.put("bio", user.get("description"));
        return sqliteUser;
    }

    private void sqliteInsert(Connection con, Map<String, Object> data, String table) {
        if (data == null || data.isEmpty()) {
            return;
        }
        String keys = String.join(",", data.keySet());
        String values = data.keySet().stream().map(k -> "?").collect(Collectors.joining(","));
        String sql = String.format("INSERT OR REPLACE INTO %s(%s) VALUES(%s)", table, keys, values);
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            int index = 1;
            for (Object value : data.values()) {
                pstmt.setObject(index++, value);
            }
            pstmt.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getSqliteConnection() {
        String path = getSqlitePath();
        boolean create = false;
        if (!new File(path).exists()) {
            create = true;
        }

        Connection con = null;
        try {
            con = DriverManager.getConnection("jdbc:sqlite:" + path);
            if (create) {
                createSqliteTable(con);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return con;
    }

    private void createSqliteTable(Connection con) {
        String sql = getSqliteCreateSql();
        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate(sql);
            con.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getSqlitePath() {
        return "./weibo/weibodata.db";
    }

    private String getSqliteCreateSql() {
        return """
                CREATE TABLE IF NOT EXISTS user (
                    id varchar(64) NOT NULL,
                    nick_name varchar(64) NOT NULL,
                    gender varchar(6),
                    follower_count integer,
                    follow_count integer,
                    birthday varchar(10),
                    location varchar(32),
                    edu varchar(32),
                    company varchar(32),
                    reg_date DATETIME,
                    main_page_url text,
                    avatar_url text,
                    bio text,
                    PRIMARY KEY (id)
                );

                CREATE TABLE IF NOT EXISTS weibo (
                    id varchar(20) NOT NULL,
                    bid varchar(12) NOT NULL,
                    user_id varchar(20),
                    screen_name varchar(30),
                    text varchar(2000),
                    article_url varchar(100),
                    topics varchar(200),
                    at_users varchar(1000),
                    pics varchar(3000),
                    video_url varchar(1000),
                    location varchar(100),
                    created_at DATETIME,
                    source varchar(30),
                    attitudes_count INT,
                    comments_count INT,
                    reposts_count INT,
                    retweet_id varchar(20),
                    PRIMARY KEY (id)
                );

                CREATE TABLE IF NOT EXISTS bins (
                    id integer PRIMARY KEY AUTOINCREMENT,
                    ext varchar(10) NOT NULL, /*file extension*/
                    data blob NOT NULL,
                    weibo_id varchar(20),
                    comment_id varchar(20),
                    path text,
                    url text
                );

                CREATE TABLE IF NOT EXISTS comments (
                    id varchar(20) NOT NULL,
                    bid varchar(20) NOT NULL,
                    weibo_id varchar(32) NOT NULL,
                    root_id varchar(20),
                    user_id varchar(20) NOT NULL,
                    created_at varchar(20),
                    user_screen_name varchar(64) NOT NULL,
                    user_avatar_url text,
                    text varchar(1000),
                    pic_url text,
                    like_count integer,
                    PRIMARY KEY (id)
                );

                CREATE TABLE IF NOT EXISTS reposts (
                    id varchar(20) NOT NULL,
                    bid varchar(20) NOT NULL,
                    weibo_id varchar(32) NOT NULL,
                    user_id varchar(20) NOT NULL,
                    created_at varchar(20),
                    user_screen_name varchar(64) NOT NULL,
                    user_avatar_url text,
                    text varchar(1000),
                    like_count integer,
                    PRIMARY KEY (id)
                );
                """;
    }

    private void closeSqliteConnection(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void userToDatabase() {
        userToCsv();
        if (writeMode.contains("mysql")) {
            user_to_mysql();
        }
        if (writeMode.contains("mongo")) {
            user_to_mongodb();
        }
        if (writeMode.contains("sqlite")) {
            user_to_sqlite();
        }
    }
    public int getUserInfo() {
        Map<String, String> params = new HashMap<>();
        params.put("containerid", "100505" + this.userConfig.get("user_id"));

        if (this.longSleepCountBeforeEachUser > 0) {
            int sleepTime = ThreadLocalRandom.current().nextInt(30, 61);
            log.info("短暂sleep " + sleepTime + "秒，避免被ban");
            try {
                Thread.sleep(sleepTime * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("sleep结束");
        }
        this.longSleepCountBeforeEachUser++;

        Map<String, Object> js = getJson(params);
        int statusCode = (int) js.get("status_code");
        if (statusCode != 200) {
            log.info("被ban了，需要等待一段时间");
            System.exit(1);
        }
        if ((boolean) js.get("ok")) {
            Map<String, Object> info = (Map<String, Object>) ((Map<String, Object>) js.get("data")).get("userInfo");
            Map<String, Object> userInfo = new LinkedHashMap<>();
            userInfo.put("id", this.userConfig.get("user_id"));
            userInfo.put("screen_name", info.getOrDefault("screen_name", ""));
            userInfo.put("gender", info.getOrDefault("gender", ""));

            params.put("containerid", "230283" + this.userConfig.get("user_id") + "_-_INFO");
            List<String> zhList = Arrays.asList("生日", "所在地", "小学", "初中", "高中", "大学", "公司", "注册时间", "阳光信用");
            List<String> enList = Arrays.asList(
                    "birthday",
                    "location",
                    "education",
                    "education",
                    "education",
                    "education",
                    "company",
                    "registration_time",
                    "sunshine"
            );
            for (String key : enList) {
                userInfo.put(key, "");
            }

            js = getJson(params);
            if ((boolean) js.get("ok")) {
                List<Map<String, Object>> cards = (List<Map<String, Object>>) ((Map<String, Object>) js.get("data")).get("cards");
                if (cards != null && cards.size() > 1) {
                    List<Map<String, Object>> cardList = new ArrayList<>();
                    cardList.addAll((List<Map<String, Object>>) cards.get(0).get("card_group"));
                    cardList.addAll((List<Map<String, Object>>) cards.get(1).get("card_group"));
                    for (Map<String, Object> card : cardList) {
                        String itemName = (String) card.get("item_name");
                        if (zhList.contains(itemName)) {
                            userInfo.put(enList.get(zhList.indexOf(itemName)), card.getOrDefault("item_content", ""));
                        }
                    }
                }
            }

            userInfo.put("statuses_count", stringToInt(info.get("statuses_count").toString()));
            userInfo.put("followers_count", stringToInt(info.get("followers_count").toString()));
            userInfo.put("follow_count", stringToInt(info.get("follow_count").toString()));
            userInfo.put("description", info.getOrDefault("description", ""));
            userInfo.put("profile_url", info.getOrDefault("profile_url", ""));
            userInfo.put("profile_image_url", info.getOrDefault("profile_image_url", ""));
            userInfo.put("avatar_hd", info.getOrDefault("avatar_hd", ""));
            userInfo.put("urank", info.getOrDefault("urank", 0));
            userInfo.put("mbrank", info.getOrDefault("mbrank", 0));
            userInfo.put("verified", info.getOrDefault("verified", false));
            userInfo.put("verified_type", info.getOrDefault("verified_type", -1));
            userInfo.put("verified_reason", info.getOrDefault("verified_reason", ""));

            this.user = standardizeInfo(userInfo);
            userToDatabase();
            return 0;
        } else {
            logger.info("user_id_list中 " + this.userConfig.get("user_id") + " id出错");
            return -1;
        }
    }

    private Map<String, Object> standardizeInfo(Map<String, Object> userInfo) {
    }
}
