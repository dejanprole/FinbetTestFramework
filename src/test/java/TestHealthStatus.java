import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestHealthStatus {

    /**Test URL status health*/
    @Test
    public void testHealthStatus() throws URISyntaxException, IOException, InterruptedException {
        var config = YamlReader.readConfig("configuration.yaml");
        var healthUri = new URI("http", null, config.host.url, config.host.port, "/" + config.health_status.path, null, null);

        var client = HttpClient.newHttpClient();

        var request = HttpRequest.newBuilder()
                .uri(URI.create(healthUri.toString()))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assert.assertEquals(response.statusCode(), 200, "Expected response is 200");
        Assert.assertEquals(response.body(), "{\"status\":\"healthy\"}\n", "Status should be healthy");
    }
}
