package getWeiboInfo;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class WeiboService {

    private static final Logger logger = LoggerFactory.getLogger(WeiboService.class);

    private String headers; // Set headers for requests (could be a Map or String)

    // Standardizes the date string into a specific format
    private Map<String, String> standardizeDate(String createdAt) {
        // Implement the standardizeDate method as required
        // This can return a map with keys "created_at" and "full_created_at"
        return Map.of("created_at", createdAt, "full_created_at", createdAt);
    }

    // Parses Weibo information from the given map
    private Map<String, Object> parseWeibo(Map<String, Object> weiboInfo) {
        // Parsing logic for Weibo
        // Extract and return relevant data in a standardized format
        return weiboInfo;
    }

    // Fetches a long Weibo post based on the Weibo ID
    private Map<String, Object> getLongWeibo(long weiboId) {
        // Implement logic to fetch long Weibo posts if needed
        return null; // Placeholder
    }

    // Fetches a single Weibo and its details
    public Map<String, Object> getOneWeibo(Map<String, Object> info) {
        try {
            Map<String, Object> weiboInfo = (Map<String, Object>) info.get("mblog");
            long weiboId = (long) weiboInfo.get("id");
            Map<String, Object> retweetedStatus = (Map<String, Object>) weiboInfo.get("retweeted_status");
            boolean isLong = ((int) weiboInfo.get("pic_num") > 9) || (boolean) weiboInfo.get("isLongText");

            Map<String, Object> weibo;
            if (retweetedStatus != null && retweetedStatus.containsKey("id")) {
                long retweetId = (long) retweetedStatus.get("id");
                boolean isLongRetweet = (boolean) retweetedStatus.get("isLongText");

                if (isLong) {
                    weibo = getLongWeibo(weiboId);
                    if (weibo == null) {
                        weibo = parseWeibo(weiboInfo);
                    }
                } else {
                    weibo = parseWeibo(weiboInfo);
                }

                Map<String, Object> retweet = isLongRetweet ? getLongWeibo(retweetId) : parseWeibo(retweetedStatus);
                if (retweet == null) {
                    retweet = parseWeibo(retweetedStatus);
                }

                Map<String, String> retweetDate = standardizeDate((String) retweetedStatus.get("created_at"));
                retweet.putAll(retweetDate);
                weibo.put("retweet", retweet);

            } else {
                // Handle original Weibo post
                if (isLong) {
                    weibo = getLongWeibo(weiboId);
                    if (weibo == null) {
                        weibo = parseWeibo(weiboInfo);
                    }
                } else {
                    weibo = parseWeibo(weiboInfo);
                }
            }

            Map<String, String> weiboDate = standardizeDate((String) weiboInfo.get("created_at"));
            weibo.putAll(weiboDate);

            return weibo;

        } catch (Exception e) {
            logger.error("Error while fetching Weibo details", e);
        }

        return null;
    }

    // Fetches the comments for a specific Weibo post
    public void getWeiboComments(Map<String, Object> weibo, int maxCount, OnDownloadCompleted onDownloaded) {
        if ((int) weibo.get("comments_count") == 0) {
            return;
        }

        logger.info("Downloading comments for Weibo ID: {}", weibo.get("id"));
        getWeiboCommentsWithCookie(weibo, 0, maxCount, null, onDownloaded);
    }

    // Fetches the reposts for a specific Weibo post
    public void getWeiboReposts(Map<String, Object> weibo, int maxCount, OnDownloadCompleted onDownloaded) {
        if ((int) weibo.get("reposts_count") == 0) {
            return;
        }

        logger.info("Downloading reposts for Weibo ID: {}", weibo.get("id"));
        getWeiboRepostsWithCookie(weibo, 0, maxCount, 1, onDownloaded);
    }

    // Download comments with cookie
    private void getWeiboCommentsWithCookie(Map<String, Object> weibo, int curCount, int maxCount, Integer maxId, OnDownloadCompleted onDownloaded) {
        if (curCount >= maxCount) return;

        long id = (long) weibo.get("id");
        String url = "https://m.weibo.cn/comments/hotflow?max_id_type=0";
        String params = maxId != null ? "mid=" + id + "&max_id=" + maxId : "mid=" + id;

        try {
            JSONObject jsonResponse = sendRequest(url, params);
            if (jsonResponse != null && jsonResponse.has("data")) {
                JSONObject data = jsonResponse.getJSONObject("data");
                if (data.has("data")) {
                    processCommentsData(weibo, curCount, maxCount, data, onDownloaded);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to fetch comments for Weibo ID: {}", weibo.get("id"));
            getWeiboCommentsWithoutCookie(weibo, curCount, maxCount, 1, onDownloaded);
        }
    }

    // Fetch comments without cookies
    private void getWeiboCommentsWithoutCookie(Map<String, Object> weibo, int curCount, int maxCount, int page, OnDownloadCompleted onDownloaded) {
        if (curCount >= maxCount) return;

        long id = (long) weibo.get("id");
        String url = "https://m.weibo.cn/api/comments/show?id=" + id + "&page=" + page;

        try {
            JSONObject jsonResponse = sendRequest(url, null);
            if (jsonResponse != null && jsonResponse.has("data")) {
                JSONObject data = jsonResponse.getJSONObject("data");
                if (data.has("data")) {
                    processCommentsData(weibo, curCount, maxCount, data, onDownloaded);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to fetch comments for Weibo ID: {}", weibo.get("id"));
        }
    }

    // Process comments data (Common logic for both methods)
    private void processCommentsData(Map<String, Object> weibo, int curCount, int maxCount, JSONObject data, OnDownloadCompleted onDownloaded) {
        if (data.has("data")) {
            var comments = data.getJSONArray("data");
            int count = comments.length();
            if (count == 0) {
                return;
            }

            if (onDownloaded != null) {
                onDownloaded.onDownload(weibo, comments);
            }

            curCount += count;
            Integer maxId = data.has("max_id") ? data.getInt("max_id") : null;

            if (maxId != null && maxId > 0) {
                getWeiboCommentsWithCookie(weibo, curCount, maxCount, maxId, onDownloaded);
            }
        }
    }

    // Download reposts with cookie
    private void getWeiboRepostsWithCookie(Map<String, Object> weibo, int curCount, int maxCount, int page, OnDownloadCompleted onDownloaded) {
        if (curCount >= maxCount) return;

        long id = (long) weibo.get("id");
        String url = "https://m.weibo.cn/api/statuses/repostTimeline";
        String params = "id=" + id + "&page=" + page;

        try {
            JSONObject jsonResponse = sendRequest(url, params);
            if (jsonResponse != null && jsonResponse.has("data")) {
                JSONObject data = jsonResponse.getJSONObject("data");
                if (data.has("data")) {
                    processRepostsData(weibo, curCount, maxCount, data, onDownloaded);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to fetch reposts for Weibo ID: {}", weibo.get("id"));
        }
    }

    // Process reposts data
    private void processRepostsData(Map<String, Object> weibo, int curCount, int maxCount, JSONObject data, OnDownloadCompleted onDownloaded) {
        if (data.has("data")) {
            var reposts = data.getJSONArray("data");
            int count = reposts.length();
            if (count == 0) {
                return;
            }

            if (onDownloaded != null) {
                onDownloaded.onDownload(weibo, reposts);
            }

            curCount += count;
            Integer maxPage = data.has("max") ? data.getInt("max") : null;

            if (maxPage != null && maxPage > 0) {
                getWeiboRepostsWithCookie(weibo, curCount, maxCount, maxPage + 1, onDownloaded);
            }
        }
    }

    // Helper method for sending requests
    private JSONObject sendRequest(String url, String params) throws Exception {
        URL requestUrl = new URL(url + (params != null ? "?" + params : ""));
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return new JSONObject(response.toString());
        }
    }

    // Interface for the callback when download is completed
    public interface OnDownloadCompleted {
        void onDownload(Map<String, Object> weibo, Object data);
    }
}
