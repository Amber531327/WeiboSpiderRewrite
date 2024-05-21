package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ConfigReader {
    public Map<String, Object> readConfigFromFile(String filePath) {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, Object> config = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            // 如果属性值是数字，则尝试解析为整数;如果值包含逗号，则尝试解析为列表
            Object parsedValue = parseValue(value);
            config.put(key, parsedValue);
        }
//        // 遍历并输出Map中的键值对
//        for (Map.Entry<String, Object> entry : config.entrySet()) {
//            System.out.println(entry.getKey() + ": " + entry.getValue());
//        }
        return config;
    }

    private Object parseValue(String value) {
        // 如果值包含逗号，则尝试解析为列表
        if (value.contains(",")) {
            List<String> listValue = Arrays.asList(value.split(","));
            return listValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // 如果无法解析为整数，则直接返回字符串值
            return value;
        }
    }
}
