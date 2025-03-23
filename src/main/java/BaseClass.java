import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

public class BaseClass {
    protected static final String HEALTH_PATH = "health";
    protected static final String REGISTER_PATH = "register";
    protected static final String USER_PATH = "user/";
    protected static final String LOGIN_PATH= "login";
    protected static final Integer STATUS_CODE_SUCCESSFUL = 200;
    private static final Config config = YamlReader.readConfig("configuration.yaml");
    private static final Logger logger = LoggerFactory.getLogger(BaseClass.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        if (config == null) {
            logger.error("Could not read configuration from config file.");
            throw new IllegalStateException("Configuration is null");
        }
    }

    public HttpRequest createRegistrationRequest(RegistrationRequest registrationRequest, String path) throws JsonProcessingException, URISyntaxException {
        var jsonRequest = mapper.writeValueAsString(registrationRequest);

        return HttpRequest.newBuilder()
                .uri(new URIBuilder()
                        .setScheme("http")
                        .setHost(config.host.url)
                        .setPort(config.host.port)
                        .setPath(path)
                        .build())
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                .build();
    }
}
