package utils;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CsvUtil {

    // 插入或更新用户 CSV。不存在则插入，存在则返回已抓取最新微博 ID 和日期
    public static String insertOrUpdateUser(Logger logger, String[] headers, List<String[]> resultData, String filePath) {
        boolean firstWrite = !Files.exists(Paths.get(filePath));
        if (!firstWrite) {
            // 文件已存在，查看是否已存在用户数据
            try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line[0].equals(resultData.get(0)[0])) {
                        return line[line.length - 1];
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                logger.warning("Failed to read from CSV file: " + filePath);
            } catch (CsvValidationException e) {
                throw new RuntimeException(e);
            }
        }

        // 如果没有用户数据或者新建文件
        resultData.get(0)[resultData.get(0).length - 1] = "";
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath, true))) {
            if (firstWrite) {
                writer.writeNext(headers);
            }
            writer.writeAll(resultData);
        } catch (IOException e) {
            e.printStackTrace();
            logger.warning("Failed to write to CSV file: " + filePath);
        }

        logger.info(resultData.get(0)[1] + " 信息写入CSV文件完毕，保存路径: " + filePath);
        return "";
    }

    // 更新用户 CSV 中的最新微博 ID
    public static void updateLastWeiboId(String userId, String newLastWeiboMsg, String filePath) {
        List<String[]> lines = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line[0].equals(userId)) {
                    line[line.length - 1] = newLastWeiboMsg;
                }
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            for (String[] line : lines) {
                writer.writeNext(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


