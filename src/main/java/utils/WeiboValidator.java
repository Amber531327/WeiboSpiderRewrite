package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class WeiboValidator {

    public WeiboValidator(String filePath) {
        Map<String, Object> config = WeiboValidator.UseConfigReader(filePath);
        validateConfig(config);
    }
    public static Map<String, Object> UseConfigReader(String filePath){
        ConfigReader configReader = new ConfigReader();
        Map<String, Object> config = configReader.readConfigFromFile(filePath);
        return config;
    }
    private static final List<String> BOOLEAN_ARGUMENTS = List.of(
            "only_crawl_original",
            "original_pic_download",
            "retweet_pic_download",
            "original_video_download",
            "retweet_video_download",
            "download_comment",
            "download_repost"
    );

//    验证user_id_list中的方法,下面用
    private void validateUserIdList(List<String> userIdList) {
        for (String userId : userIdList) {
            if (!userId.matches("\\d+")) {
                System.out.println(("user_id_list中的ID应为数字"));
                System.exit(1);
            }
        }
    }
    // 判断日期格式是否为 %Y-%m-%dT%H:%M:%S，下面验证用
    private boolean isDateTime(Object sinceDate) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            df.parse((String) sinceDate);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    // 判断日期格式是否为 %Y-%m-%d，下面验证用
    private boolean isDate(Object sinceDate) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            df.parse((String) sinceDate);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public void validateConfig(Map<String, Object> config) {
        // 验证 1/0 相关值
        for (String argument : BOOLEAN_ARGUMENTS) {
            if (!config.containsKey(argument)) {
                continue;
            }
            Object value = config.get(argument);
            if (!(value instanceof Integer)) {
                continue;
            }
            int intValue = (int) value;
            if (intValue != 0 && intValue != 1) {
                System.out.println(argument + "值应为0或1，请重新输入");
                System.exit(1);
            }
        }

        // 验证 query_list
        Object queryListObject = config.get("query_list");
        if (!(queryListObject instanceof List) && !(queryListObject instanceof String)) {
            System.out.println("query_list值应为list类型或字符串，请重新输入");
            System.exit(1);
        }
        //验证write_mode
        List<String> validModes = List.of("csv", "json", "mongo", "mysql", "sqlite");
        Object writeModeObj = config.get("write_mode");
        if (!(writeModeObj instanceof List)) {
            System.out.println(("write_mode值应为list类型"));
            System.exit(1);
        }
        List<String> writeMode = (List<String>) writeModeObj;
        for (String mode : writeMode) {
            if (!validModes.contains(mode)) {
                System.out.println((String.format("%s为无效模式，请从csv、json、mongo和mysql中挑选一个或多个作为write_mode", mode)));
                System.exit(1);
            }
        }
        //TODO:这里与原代码不同，原代码使用一个专门存放常量的文件来指定
        String mode = "overwrite"; // 您可以根据需要设置为 "append" 或 "overwrite"
        // 验证运行模式
        if (!writeMode.contains("sqlite") && "append".equals(mode)) {
            System.out.println(("append模式下请将sqlite加入write_mode中"));
            System.exit(1);
        }

//        验证user_id_list
        Object userIdListObj = config.get("user_id_list");
        if (userIdListObj instanceof List) {
            List<String> userIdList = (List<String>) userIdListObj;
            validateUserIdList(userIdList);
        } else if (userIdListObj instanceof String) {
            String userIdListFile = (String) userIdListObj;
            File file = new File(userIdListFile);
            if (!file.exists()) {
                System.out.println("不存在 " + userIdListFile + " 文件");
                System.exit(1);
            } else {
                try {
                    List<String> userIdList = Files.readAllLines(Paths.get(userIdListFile));
                    validateUserIdList(userIdList);
                } catch (IOException e) {
                    System.out.println("无法读取 " + userIdListFile + " 文件");
                    System.exit(1);
                }
            }
        } else {
            System.out.println("user_id_list值应为list类型或txt文件路径");
            System.exit(1);
        }

        // 验证 since_date
        Object sinceDate = config.get("since_date");
        if (!(sinceDate instanceof Integer) && !isDateTime(sinceDate) && !isDate(sinceDate)) {
            System.out.println(("since_date值应为yyyy-mm-dd形式、yyyy-mm-ddTHH:MM:SS形式或整数，请重新输入"));
            System.exit(1);
        }
        // 验证最大评论数
        Object commentMaxCount = config.get("comment_max_download_count");
        if (!(commentMaxCount instanceof Integer)) {
            System.out.println(("最大下载评论数 (comment_max_download_count) 应为整数类型"));
            System.exit(1);
        } else if ((Integer) commentMaxCount < 0) {
            System.out.println(("最大下载评论数 (comment_max_download_count) 应该为正整数"));
            System.exit(1);
        }
        //验证最大转发数
        Object repostMaxCount = config.get("repost_max_download_count");
        if (!(repostMaxCount instanceof Integer)) {
            System.out.println(("最大下载转发数 (repost_max_download_count) 应为整数类型"));
            System.exit(1);
        } else if ((Integer) repostMaxCount < 0) {
            System.out.println(("最大下载转发数 (repost_max_download_count) 应该为正整数"));
            System.exit(1);
        }
        System.out.println("数据全部成功验证通过!!!!");
    }
}
