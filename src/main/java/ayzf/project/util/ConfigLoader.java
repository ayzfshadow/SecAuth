package ayzf.project.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * @Author 暗影之风
 * @CreateTime 2024-03-31 22:35:56
 * @Description 配置文件
 */
@SuppressWarnings("JavadocDeclaration")
public class ConfigLoader
{
    private final Properties properties;

    public ConfigLoader(String fileName) throws IOException
    {
        this.properties = new Properties();
        try (FileInputStream fis = new FileInputStream(fileName))
        {
            this.properties.load(fis);
        }
    }

    public Properties getObject()
    {
        return this.properties;
    }

    public String getProperty(String key)
    {
        return new String(this.properties.getProperty(key).getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }
}
