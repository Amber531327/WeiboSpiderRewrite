import utils.ConfigReader;
import utils.WeiboValidator;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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


    //TODO:验证配置是否正确......

    // 获取网页中 JSON 数据
    private Map<String, Object> getJson(Map<String, String> params) throws IOException {
        String url = "https://m.weibo.cn/api/container/getIndex?";
        // 构建 URL 对象
        URL apiUrl = new URL(url + buildQueryString(params));
        // 打开 HTTP 连接
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        // 设置请求头部
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        // 发送 GET 请求
        connection.setRequestMethod("GET");
        // 获取响应状态码
        int responseCode = connection.getResponseCode();
        // 如果请求成功，读取响应体并解析为 JSON
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                // 返回 JSON 数据
                return parseJson(response.toString());
            }
        } else {
            // 处理请求失败的情况
            System.out.println("HTTP GET request failed with response code: " + responseCode);
            return null;
        }
    }

    // 获取网页中微博 JSON 数据
    private Map<String, Object> getWeiboJson(int page) throws IOException {
        Map<String, String> params = buildWeiboParams(page);
        // 发送 HTTP 请求获取微博 JSON 数据
        return getJson(params);
    }

    // 构建查询字符串
    private String buildQueryString(Map<String, String> params) {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            queryString.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        // 删除末尾的 "&" 符号
        queryString.deleteCharAt(queryString.length() - 1);
        return queryString.toString();
    }

    // 解析 JSON 字符串
    private Map<String, Object> parseJson(String jsonStr) {
        // 这里你需要使用 JSON 解析库来解析 JSON 字符串，比如 Gson、Jackson 等
        // 返回解析后的 JSON 数据
        return null;
    }

    // 构建微博请求参数
    public Map<String, String> buildWeiboParams(int page) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));

        if (query != null && !query.isEmpty()) {
            // 如果有查询关键词，构建带有搜索条件的参数
            params.put("container_ext", "profile_uid:" + userConfigList.get(0).get("user_id"));//这里传入列表中第一个id值
            params.put("containerid", "100103type=401&q=" + query);
            params.put("page_type", "searchall");
        } else {
            // 否则，构建普通的参数
            params.put("containerid", "230413" +  userConfigList.get(0).get("user_id"));
        }

        System.out.println(userConfigList.get(0).get("user_id"));
        return params;
    }

}
