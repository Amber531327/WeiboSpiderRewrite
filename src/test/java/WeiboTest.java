import org.junit.Test;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

public class WeiboTest {
    @Test
    public void testWeibo() {

        // 创建 Properties 对象
        Properties config = new Properties();

        // 读取配置文件
        try {
            FileInputStream input = new FileInputStream("src/main/resources/config/config.properties");
            config.load(input);
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 创建 Weibo 对象并传入 Properties 对象
        Weibo weibo = new Weibo(config);
        weibo.buildWeiboParams(1);
    }
}
