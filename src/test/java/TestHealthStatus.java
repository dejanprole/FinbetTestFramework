import org.apache.http.client.utils.URIBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestHealthStatus extends BaseClass {
    private static final Logger logger = LoggerFactory.getLogger(TestHealthStatus.class);

    /** Test health status */
    @Test
    public void healthStatus() throws URISyntaxException, IOException, InterruptedException {
        logger.info("Starting method healthStatus");

        var config = YamlReader.readConfig("configuration.yaml");
        var client = HttpClient.newHttpClient();

        if (config == null) {
            logger.error("Could not read configuration from config file.");
            throw new IllegalStateException("Configuration is null");
        }

        logger.info("API is running at " + config.host.url + ":" + config.host.port);

        var request = HttpRequest.newBuilder()
                .uri(new URIBuilder()
                        .setScheme("http")
                        .setHost(config.host.url)
                        .setPort(config.host.port)
                        .setPath(BaseClass.HEALTH_PATH)
                        .build())
                .build();

        logger.info("API URL: " + request.uri());

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        logger.info("Validating response");

        Assert.assertEquals(response.statusCode(), 200, "Expected response is 200");
        logger.info("Response status code: " + response.statusCode());
        Assert.assertEquals(response.body(), "{\"status\":\"healthy\"}\n", "Status should be healthy");
        logger.info("Response status code: " + response.body());
    }
}
