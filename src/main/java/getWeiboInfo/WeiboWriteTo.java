package getWeiboInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class WeiboWriteTo {

    private static final Logger logger = LoggerFactory.getLogger(WeiboService.class);

    private Map<String, Object> user;  // Simulating user data
    private List<Map<String, Object>> weibo;  // List of Weibo data
    private boolean onlyCrawlOriginal;  // Flag for original posts only
    private boolean userIdAsFolderName;  // Flag for using user ID as folder name
    private Map<String, Object> userConfig;  // Configuration for user

    // Constructor
    public WeiboWriteTo(Map<String, Object> user, List<Map<String, Object>> weibo, boolean onlyCrawlOriginal, boolean userIdAsFolderName, Map<String, Object> userConfig) {
        this.user = user;
        this.weibo = weibo;
        this.onlyCrawlOriginal = onlyCrawlOriginal;
        this.userIdAsFolderName = userIdAsFolderName;
        this.userConfig = userConfig;
    }

    // 1. Get the page count
    public int getPageCount() {
        try {
            int weiboCount = (int) user.get("statuses_count");
            return (int) Math.ceil(weiboCount / 10.0);
        } catch (Exception e) {
            logger.error("Error occurred. Possible causes:\n"
                    + "1. Incorrect user_id;\n"
                    + "2. This user's Weibo may require cookies to be crawled.\n"
                    + "Solutions:\n"
                    + "Please refer to https://github.com/dataabc/weibo-crawler#如何获取user_id to get the correct user_id;\n"
                    + "Or refer to https://github.com/dataabc/weibo-crawler#3程序设置 for setting cookies.", e);
            return 0;
        }
    }

    // 2. Get the Weibo information to write
    public List<Map<String, String>> getWriteInfo(int wroteCount) {
        List<Map<String, String>> writeInfo = new ArrayList<>();
        for (int i = wroteCount; i < weibo.size(); i++) {
            Map<String, Object> w = weibo.get(i);
            Map<String, String> wb = new HashMap<>();
            Set<Map.Entry<String, Object>> entries = w.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                String k = entry.getKey();
                Object v = entry.getValue();
                if (!k.equals("user_id") && !k.equals("screen_name") && !k.equals("retweet")) {
                    if (v instanceof String) {
                        v = ((String) v).getBytes();  // UTF-8 Encoding equivalent
                    }
                    if (k.equals("id")) {
                        v = v.toString() + "\t";
                    }
                    wb.put(k, v.toString());
                }
            }

            if (!onlyCrawlOriginal) {
                if (w.containsKey("retweet")) {
                    wb.put("is_original", "false");
                    Map<String, Object> retweet = (Map<String, Object>) w.get("retweet");
                    for (Map.Entry<String, Object> retweetEntry : retweet.entrySet()) {
                        String k2 = retweetEntry.getKey();
                        Object v2 = retweetEntry.getValue();
                        if (v2 instanceof String) {
                            v2 = ((String) v2).getBytes();
                        }
                        if (k2.equals("id")) {
                            v2 = v2.toString() + "\t";
                        }
                        wb.put("retweet_" + k2, v2.toString());
                    }
                } else {
                    wb.put("is_original", "true");
                }
            }
            writeInfo.add(wb);
        }
        return writeInfo;
    }

    // 3. Get the file path for the result
    public String getFilePath(String type) {
        try {
            String dirName = (String) user.get("screen_name");
            if (userIdAsFolderName) {
                dirName = String.valueOf(userConfig.get("user_id"));
            }
            String fileDir = new File(WeiboService.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile()
                    + File.separator + "weibo" + File.separator + dirName;
            if (type.equals("img") || type.equals("video")) {
                fileDir = fileDir + File.separator + type;
            }
            File fileDirObj = new File(fileDir);
            if (!fileDirObj.exists()) {
                fileDirObj.mkdirs();
            }
            if (type.equals("img") || type.equals("video")) {
                return fileDir;
            }
            return fileDir + File.separator + userConfig.get("user_id") + "." + type;
        } catch (Exception e) {
            logger.error("Error while generating file path", e);
            return null;
        }
    }

    // 4. Get the headers for the result file
    public List<String> getResultHeaders() {
        List<String> resultHeaders = new ArrayList<>();
        resultHeaders.add("id");
        resultHeaders.add("bid");
        resultHeaders.add("正文");
        resultHeaders.add("头条文章url");
        resultHeaders.add("原始图片url");
        resultHeaders.add("视频url");
        resultHeaders.add("位置");
        resultHeaders.add("日期");
        resultHeaders.add("工具");
        resultHeaders.add("点赞数");
        resultHeaders.add("评论数");
        resultHeaders.add("转发数");
        resultHeaders.add("话题");
        resultHeaders.add("@用户");
        resultHeaders.add("完整日期");

        if (!onlyCrawlOriginal) {
            List<String> resultHeaders2 = new ArrayList<>();
            resultHeaders2.add("是否原创");
            resultHeaders2.add("源用户id");
            resultHeaders2.add("源用户昵称");
            List<String> resultHeaders3 = new ArrayList<>();
            for (String header : resultHeaders) {
                resultHeaders3.add("源微博" + header);
            }
            resultHeaders.addAll(resultHeaders2);
            resultHeaders.addAll(resultHeaders3);
        }

        return resultHeaders;
    }

    public static void main(String[] args) {
        // Sample usage of the WeiboService class
        Map<String, Object> user = new HashMap<>();
        user.put("statuses_count", 120);
        user.put("screen_name", "sample_user");

        List<Map<String, Object>> weiboData = new ArrayList<>();
        Map<String, Object> weiboPost = new HashMap<>();
        weiboPost.put("id", 12345);
        weiboPost.put("content", "This is a sample Weibo post.");
        weiboData.add(weiboPost);

        Map<String, Object> userConfig = new HashMap<>();
        userConfig.put("user_id", 123);

        WeiboService weiboService = new WeiboService(user, weiboData, false, false, userConfig);

        // Testing methods
        int pageCount = weiboService.getPageCount();
        System.out.println("Page Count: " + pageCount);

        List<Map<String, String>> writeInfo = weiboService.getWriteInfo(0);
        System.out.println("Write Info: " + writeInfo);

        String filePath = weiboService.getFilePath("img");
        System.out.println("File Path: " + filePath);

        List<String> resultHeaders = weiboService.getResultHeaders();
        System.out.println("Result Headers: " + resultHeaders);
    }
}
