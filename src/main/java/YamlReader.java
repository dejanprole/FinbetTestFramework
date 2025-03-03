import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;

public class YamlReader {
    public static Config readConfig(String fileName) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        try (InputStream inputStream = YamlReader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException("File not found: " + fileName);
            }
            return mapper.readValue(inputStream, Config.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
