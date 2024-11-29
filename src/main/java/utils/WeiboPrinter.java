package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class WeiboPrinter {

    // Create a logger instance
    private static final Logger logger = LoggerFactory.getLogger(WeiboPrinter.class);

    private Map<String, Object> user;

    // Constructor to initialize the user data
    public WeiboPrinter(Map<String, Object> user) {
        this.user = user;
    }

    public void printUserInfo() {
        // 打印用户信息
        logger.info("+" + "-".repeat(100) + "+");
        logger.info("用户信息");
        logger.info("用户id：{}", user.get("id"));
        logger.info("用户昵称：{}", user.get("screen_name"));

        // Gender handling
        String gender = "f".equals(user.get("gender")) ? "女" : "男";
        logger.info("性别：{}", gender);

        // Printing the other user details
        logger.info("生日：{}", user.get("birthday"));
        logger.info("所在地：{}", user.get("location"));
        logger.info("教育经历：{}", user.get("education"));
        logger.info("公司：{}", user.get("company"));
        logger.info("阳光信用：{}", user.get("sunshine"));
        logger.info("注册时间：{}", user.get("registration_time"));
        logger.info("微博数：{}", user.get("statuses_count"));
        logger.info("粉丝数：{}", user.get("followers_count"));
        logger.info("关注数：{}", user.get("follow_count"));
        logger.info("url：https://m.weibo.cn/profile/{}", user.get("id"));

        if (user.containsKey("verified_reason")) {
            logger.info("认证原因：{}", user.get("verified_reason"));
        }
        logger.info("用户简介：{}", user.get("description"));
        logger.info("+" + "-".repeat(100) + "+");
    }

    public void printOneWeibo(Map<String, Object> weibo) {
        // 打印一条微博
        try {
            logger.info("微博id：{}", weibo.get("id"));
            logger.info("微博正文：{}", weibo.get("text"));
            logger.info("原始图片url：{}", weibo.get("pics"));
            logger.info("微博位置：{}", weibo.get("location"));
            logger.info("发布时间：{}", weibo.get("created_at"));
            logger.info("发布工具：{}", weibo.get("source"));
            logger.info("点赞数：{}", weibo.get("attitudes_count"));
            logger.info("评论数：{}", weibo.get("comments_count"));
            logger.info("转发数：{}", weibo.get("reposts_count"));
            logger.info("话题：{}", weibo.get("topics"));
            logger.info("@用户：{}", weibo.get("at_users"));
            logger.info("url：https://m.weibo.cn/detail/{}", weibo.get("id"));
        } catch (Exception e) {
            // Handle possible exceptions (e.g., missing keys in the map)
            logger.error("Error printing weibo info", e);
        }
    }

    // Example usage
    public static void main(String[] args) {
        // Example user and weibo data
        Map<String, Object> user = Map.of(
                "id", 123456,
                "screen_name", "张三",
                "gender", "m",  // 'm' for male, 'f' for female
                "birthday", "1990-01-01",
                "location", "北京",
                "education", "清华大学",
                "company", "某科技公司",
                "sunshine", "A+",
                "registration_time", "2010-01-01",
                "statuses_count", 1000,
                "followers_count", 500,
                "follow_count", 300,
                "verified_reason", "认证理由",
                "description", "这个人很懒，什么也没有写"
        );

        WeiboPrinter printer = new WeiboPrinter(user);
        printer.printUserInfo();

        // Example weibo data
        Map<String, Object> weibo = Map.of(
                "id", 654321,
                "text", "这是微博内容",
                "pics", "http://example.com/img.jpg",
                "location", "北京",
                "created_at", "2024-11-29",
                "source", "微博",
                "attitudes_count", 200,
                "comments_count", 50,
                "reposts_count", 100,
                "topics", "#科技#",
                "at_users", "@李四"
        );

        printer.printOneWeibo(weibo);
    }
}

