package getWeiboInfo;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.CloseableHttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

public class download{

    private static final Logger logger = Logger.getLogger(FileDownloader.class.getName());

    // Simulate Weibo data structure
    private List<Map<String, Object>> weibo = new ArrayList<>();

    public void handleDownload(String fileType, String fileDir, String urls, Map<String, Object> w) {
        String filePrefix = w.get("created_at").toString().substring(0, 10).replace("-", "") + "_" + w.get("id");
        String fileSuffix = "";

        try {
            if ("img".equals(fileType)) {
                if (urls.contains(",")) {
                    String[] urlList = urls.split(",");
                    for (int i = 0; i < urlList.length; i++) {
                        String url = urlList[i];
                        int index = url.lastIndexOf(".");
                        fileSuffix = (url.length() - index >= 5) ? ".jpg" : url.substring(index);
                        String fileName = filePrefix + "_" + (i + 1) + fileSuffix;
                        String filePath = fileDir + File.separator + fileName;
                        downloadOneFile(url, filePath, fileType, w.get("id").toString());
                    }
                } else {
                    int index = urls.lastIndexOf(".");
                    fileSuffix = (urls.length() - index > 5) ? ".jpg" : urls.substring(index);
                    String fileName = filePrefix + fileSuffix;
                    String filePath = fileDir + File.separator + fileName;
                    downloadOneFile(urls, filePath, fileType, w.get("id").toString());
                }
            } else {
                fileSuffix = ".mp4";
                if (urls.contains(";")) {
                    String[] urlList = urls.split(";");
                    if (urlList[0].endsWith(".mov")) {
                        fileSuffix = ".mov";
                    }
                    for (int i = 0; i < urlList.length; i++) {
                        String fileName = filePrefix + "_" + (i + 1) + fileSuffix;
                        String filePath = fileDir + File.separator + fileName;
                        downloadOneFile(urlList[i], filePath, fileType, w.get("id").toString());
                    }
                } else {
                    if (urls.endsWith(".mov")) {
                        fileSuffix = ".mov";
                    }
                    String fileName = filePrefix + fileSuffix;
                    String filePath = fileDir + File.separator + fileName;
                    downloadOneFile(urls, filePath, fileType, w.get("id").toString());
                }
            }
        } catch (Exception e) {
            logger.severe("Error during file download: " + e.getMessage());
        }
    }

    public void downloadFiles(String fileType, String weiboType, int wroteCount) {
        try {
            String describe = "";
            String key = "";

            if ("img".equals(fileType)) {
                describe = "图片";
                key = "pics";
            } else {
                describe = "视频";
                key = "video_url";
            }

            if ("original".equals(weiboType)) {
                describe = "原创微博" + describe;
            } else {
                describe = "转发微博" + describe;
            }

            logger.info("即将进行" + describe + "下载");

            String fileDir = getFilepath(fileType);
            fileDir = fileDir + File.separator + describe;
            Path dirPath = Paths.get(fileDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // Simulating the weibo data and the wroteCount index
            for (Map<String, Object> w : weibo.subList(wroteCount, weibo.size())) {
                if ("retweet".equals(weiboType) && w.containsKey("retweet")) {
                    w = (Map<String, Object>) w.get("retweet");
                }

                if (w.containsKey(key)) {
                    handleDownload(fileType, fileDir, (String) w.get(key), w);
                }
            }

            logger.info(describe + "下载完毕,保存路径:" + fileDir);
        } catch (Exception e) {
            logger.severe("Error during batch file download: " + e.getMessage());
        }
    }

    private void downloadOneFile(String url, String filePath, String fileType, String id) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                byte[] content = EntityUtils.toByteArray(entity);
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(content);
                }
                logger.info("文件下载成功: " + filePath);
            }
        } catch (IOException e) {
            logger.severe("文件下载失败 " + filePath + ": " + e.getMessage());
            throw e;  // Rethrow to handle in calling method
        }
    }

    private String getFilepath(String fileType) {
        // You can modify this method to generate directories based on fileType or other criteria
        return System.getProperty("user.dir") + File.separator + fileType;
    }

    // Example usage of the downloadFiles method
    public static void main(String[] args) {
        FileDownloader downloader = new FileDownloader();

        // Populate Weibo data (this is just for testing purposes)
        Map<String, Object> weiboPost = new HashMap<>();
        weiboPost.put("id", 12345);
        weiboPost.put("created_at", "2024-11-29 12:00:00");
        weiboPost.put("pics", "https://example.com/image1.jpg,https://example.com/image2.jpg");
        downloader.weibo.add(weiboPost);

        downloader.downloadFiles("img", "original", 0);  // Download images for the first post
    }
}

