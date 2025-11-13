import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AppConfig {

    private static final Properties props = new Properties();

    static {
        try {
            Path configPath = Paths.get("config", "app.properties").toAbsolutePath();
            try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
                props.load(fis);
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar config/app.properties", e);
        }
    }

    public static String getOpenAIApiKey() {
        return props.getProperty("openai.api.key", "").trim();
    }

    public static String getOpenAIModel() {
        return props.getProperty("openai.model", "gpt-4.1-mini").trim();
    }
}
