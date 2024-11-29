package getWeiboInfo;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class WeiboParser {

    // 获取微博发布位置
    public String getLocation(Document document) {
        String locationIcon = "timeline_card_small_location_default.png";
        Elements spans = document.select("span");
        String location = "";
        for (int i = 0; i < spans.size(); i++) {
            Element span = spans.get(i);
            Elements imgs = span.select("img");
            if (!imgs.isEmpty() && imgs.attr("src").contains(locationIcon)) {
                location = spans.get(i + 1).text();  // Get the next span's text as location
                break;
            }
        }
        return location;
    }

    // 获取微博中头条文章的url
    public String getArticleUrl(Document document) {
        String articleUrl = "";
        String text = document.text();  // Get the entire text of the document
        if (text.startsWith("发布了头条文章")) {
            Elements links = document.select("a[data-url]");
            for (Element link : links) {
                String url = link.attr("data-url");
                if (url.startsWith("http://t.cn")) {
                    articleUrl = url;
                    break;
                }
            }
        }
        return articleUrl;
    }

    // 获取参与的微博话题
    public String getTopics(Document document) {
        Elements spans = document.select("span.surl-text");
        List<String> topicList = new ArrayList<>();
        for (Element span : spans) {
            String text = span.text();
            if (text.length() > 2 && text.startsWith("#") && text.endsWith("#")) {
                topicList.add(text.substring(1, text.length() - 1));  // Remove '#' from both ends
            }
        }
        return String.join(",", topicList);
    }

    // 获取@用户
    public String getAtUsers(Document document) {
        Elements links = document.select("a");
        List<String> atList = new ArrayList<>();
        for (Element link : links) {
            String href = link.attr("href");
            String text = link.text();
            if (href.startsWith("@") && text.equals(href.substring(1))) {
                atList.add(text.substring(1));  // Remove '@' from the beginning
            }
        }
        return String.join(",", atList);
    }

    public static void main(String[] args) {
        // Example usage
        WeiboParser parser = new WeiboParser();

        // Assuming `htmlContent` is the HTML content of the Weibo post.
        String htmlContent = "<html>Your HTML content here</html>"; // Replace with actual HTML content
        Document document = org.jsoup.Jsoup.parse(htmlContent);

        // Get location
        String location = parser.getLocation(document);
        System.out.println("Location: " + location);

        // Get article URL
        String articleUrl = parser.getArticleUrl(document);
        System.out.println("Article URL: " + articleUrl);

        // Get topics
        String topics = parser.getTopics(document);
        System.out.println("Topics: " + topics);

        // Get @ users
        String atUsers = parser.getAtUsers(document);
        System.out.println("At users: " + atUsers);
    }
}
