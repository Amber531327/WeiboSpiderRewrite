package getWeiboInfo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class WeiboStandardise {

    // Define date format constants
    private static final DateTimeFormatter DTFORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public int stringToInt(String str) {
        // 字符串转换为整数
        if (str == null) return 0;
        if (str.endsWith("万+")) {
            str = str.substring(0, str.length() - 2) + "0000";
        } else if (str.endsWith("万")) {
            str = Double.parseDouble(str.substring(0, str.length() - 1)) * 10000 + "";
        } else if (str.endsWith("亿")) {
            str = Double.parseDouble(str.substring(0, str.length() - 1)) * 100000000 + "";
        }
        return Integer.parseInt(str);
    }

    public String[] standardizeDate(String createdAt) {
        // 标准化微博发布时间
        LocalDateTime ts = LocalDateTime.now();
        if (createdAt.contains("刚刚")) {
            ts = LocalDateTime.now();
        } else if (createdAt.contains("分钟")) {
            int minute = Integer.parseInt(createdAt.replace("分钟", "").trim());
            ts = ts.minusMinutes(minute);
        } else if (createdAt.contains("小时")) {
            int hour = Integer.parseInt(createdAt.replace("小时", "").trim());
            ts = ts.minusHours(hour);
        } else if (createdAt.contains("昨天")) {
            ts = ts.minus(1, ChronoUnit.DAYS);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
            ts = LocalDateTime.parse(createdAt.replace("+0800 ", ""), formatter);
        }

        String createdAtStr = ts.format(DTFORMAT);
        String fullCreatedAt = ts.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return new String[]{createdAtStr, fullCreatedAt};
    }

    public Map<String, Object> standardizeInfo(Map<String, Object> weibo) {
        // 标准化信息，去除乱码
        weibo.forEach((k, v) -> {
            if (v instanceof String) {
                String value = (String) v;
                weibo.put(k, value.replace("\u200b", ""));
            }
        });
        return weibo;
    }

    public Map<String, Object> parseWeibo(Map<String, Object> weiboInfo) {
        // 解析微博信息并返回标准化后的Map
        Map<String, Object> weibo = new LinkedHashMap<>();
        if (weiboInfo.get("user") != null) {
            Map<String, Object> user = (Map<String, Object>) weiboInfo.get("user");
            weibo.put("user_id", user.get("id"));
            weibo.put("screen_name", user.get("screen_name"));
        } else {
            weibo.put("user_id", "");
            weibo.put("screen_name", "");
        }

        weibo.put("id", Integer.parseInt(weiboInfo.get("id").toString()));
        weibo.put("bid", weiboInfo.get("bid"));
        String textBody = (String) weiboInfo.get("text");

        // Use JSoup for parsing HTML
        Document doc = Jsoup.parse(textBody);
        String modifiedText = doc.text();  // Simplified to just extract the plain text
        weibo.put("text", modifiedText);

        // Get other fields using similar methods
        weibo.put("article_url", getArticleUrl(doc));
        weibo.put("pics", getPics(weiboInfo));
        weibo.put("video_url", getVideoUrl(weiboInfo));
        weibo.put("location", getLocation(doc));
        weibo.put("created_at", weiboInfo.get("created_at"));
        weibo.put("source", weiboInfo.get("source"));
        weibo.put("attitudes_count", stringToInt((String) weiboInfo.getOrDefault("attitudes_count", "0")));
        weibo.put("comments_count", stringToInt((String) weiboInfo.getOrDefault("comments_count", "0")));
        weibo.put("reposts_count", stringToInt((String) weiboInfo.getOrDefault("reposts_count", "0")));
        weibo.put("topics", getTopics(doc));
        weibo.put("at_users", getAtUsers(doc));

        return standardizeInfo(weibo);
    }

    // Methods for extracting data from HTML (implemented similarly to Python)
    private String getArticleUrl(Document doc) {
        Elements links = doc.select("a[data-url]");
        for (Element link : links) {
            String url = link.attr("data-url");
            if (url.startsWith("http://t.cn")) {
                return url;
            }
        }
        return "";
    }

    private String getPics(Map<String, Object> weiboInfo) {
        // This would need to be adapted to your actual data structure.
        return (String) weiboInfo.get("pics");
    }

    private String getVideoUrl(Map<String, Object> weiboInfo) {
        // Similarly, adapt this to your actual data.
        return (String) weiboInfo.get("video_url");
    }

    private String getLocation(Document doc) {
        Elements spans = doc.select("span");
        for (Element span : spans) {
            Elements imgs = span.select("img");
            if (!imgs.isEmpty() && imgs.attr("src").contains("timeline_card_small_location_default.png")) {
                return span.nextElementSibling().text();  // Get next span's text
            }
        }
        return "";
    }

    private String getTopics(Document doc) {
        Elements spans = doc.select("span.surl-text");
        List<String> topics = new ArrayList<>();
        for (Element span : spans) {
            String text = span.text();
            if (text.length() > 2 && text.startsWith("#") && text.endsWith("#")) {
                topics.add(text.substring(1, text.length() - 1));  // Remove '#' from both ends
            }
        }
        return String.join(",", topics);
    }

    private String getAtUsers(Document doc) {
        Elements links = doc.select("a");
        List<String> atUsers = new ArrayList<>();
        for (Element link : links) {
            String href = link.attr("href");
            String text = link.text();
            if (href.startsWith("@") && text.equals(href.substring(1))) {
                atUsers.add(text.substring(1));  // Remove '@' from the beginning
            }
        }
        return String.join(",", atUsers);
    }

    public static void main(String[] args) {
        WeiboParser parser = new WeiboParser();
        // Example usage:
        Map<String, Object> weiboInfo = new HashMap<>();
        // Populate weiboInfo with actual data...
        Map<String, Object> parsedWeibo = parser.parseWeibo(weiboInfo);
        System.out.println(parsedWeibo);
    }
}
