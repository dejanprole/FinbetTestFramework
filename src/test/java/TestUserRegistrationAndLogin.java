import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class TestUserRegistrationAndLogin {
    private static final Logger logger = LoggerFactory.getLogger(TestUserRegistrationAndLogin.class);
    private static final Random random = new Random();
    private static final String usernameRandom = "test" + random.nextInt(1000);
    private static final String emailRandom = usernameRandom + "@mail.com";
    private static final String password = "Password1@";
    private static final String firstName = "John";
    private static final String middleName = "Sarah";
    private static final String lastName = "Connor";
    private static final Config config = YamlReader.readConfig("configuration.yaml");
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static Integer userId;
    private static String accessToken;

    @BeforeClass
    public void creatingUser() throws URISyntaxException, IOException, InterruptedException {
        logger.info("Starting method creatingUser");

        var registrationRequest = new RegistrationRequest(
                usernameRandom, password, emailRandom, firstName, lastName, middleName
        );

        var jsonRequest = mapper.writeValueAsString(registrationRequest);

        if (config == null) {
            logger.error("Could not read configuration from config file.");
            throw new IllegalStateException("Configuration is null");
        }

        var request = HttpRequest.newBuilder()
                .uri(new URIBuilder()
                        .setScheme("http")
                        .setHost(config.host.url)
                        .setPort(config.host.port)
                        .setPath(BaseClass.REGISTER_PATH)
                        .build())
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                .build();

        logger.info("API URL: " + request.uri());

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        var jsonNode = mapper.readTree(response.body());

        logger.info("Validating response");

        Assert.assertEquals(response.statusCode(), 200, "Response code should be 200");
        logger.info("Response status code: " + response.statusCode());
        Assert.assertEquals(jsonNode.get("username").asText(), usernameRandom, "Username should be " + usernameRandom);
        Assert.assertEquals(jsonNode.get("email").asText(), emailRandom, "Email should be " + emailRandom);
        Assert.assertEquals(jsonNode.get("firstName").asText(), firstName, "First name should be " + firstName);
        Assert.assertEquals(jsonNode.get("middleName").asText(), middleName, "Middle name should be " + middleName);
        Assert.assertEquals(jsonNode.get("lastName").asText(), lastName, "Last name should be " + lastName);
        logger.info("Response status body: " + response.body());

        try {
            if (jsonNode.has("id") && !jsonNode.get("id").isNull()) {
                userId = jsonNode.get("id").asInt();
                logger.info("User with ID " + userId + " created");
            } else {
                logger.error("ID not found in response");
            }
        } catch (Exception e) {
            logger.error("Error while extracting user ID: " + e.getMessage());
        }
    }

    /** Creating new user with same email/password/invalid email should not be possible */
    @Test(dataProvider = "usernameAndEmailParameters")
    public void duplicateUsernamePasswordOrInvalidEmail (String username, String password, String email, String firstName,
                                    String lastName, String middleName, String responseCode, String responseDescription)
            throws URISyntaxException, IOException, InterruptedException {

        logger.info("Starting method duplicateUsernamePasswordOrInvalidEmail");

        var registrationRequest = new RegistrationRequest(
                username, password, email, firstName, lastName, middleName
        );

        var jsonRequest = mapper.writeValueAsString(registrationRequest);

        var request = HttpRequest.newBuilder()
                .uri(new URIBuilder()
                        .setScheme("http")
                        .setHost(config.host.url)
                        .setPort(config.host.port)
                        .setPath(BaseClass.REGISTER_PATH)
                        .build())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                .build();

        logger.info("API URL: " + request.uri());

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        logger.info("Validating response");

        Assert.assertEquals(response.statusCode(), Integer.parseInt(responseCode), "Expected response is 400");
        logger.info("Response status code: " + response.statusCode());
        Assert.assertEquals(response.body(), "{\"error\":\"" + responseDescription + "\"}\n", "" +
                "Error response should be "+ responseDescription + " but it is " + response.body());
        logger.info("Response status body: " + response.body());

    }
    @DataProvider(name = "usernameAndEmailParameters")
    public static Object[][] usernameAndEmailParameters() {
        return new Object[][] {
                // username / password / email / firstName / lastName / middleName / responseCode / responseDescription
                {usernameRandom, "Password1@", emailRandom, "testFirstName", "testLastName", "testMiddleName", "400", "Username already exists"},
                {usernameRandom+1, "Password1@", emailRandom, "testFirstName", "testLastName", "testMiddleName", "400", "Email already exists"},
                {usernameRandom+1, "Password1@", "123.com", "testFirstName", "testLastName", "testMiddleName", "400", "Invalid email format"},
        };
    }

    /** Creating test user when one of the parameters is missing */
     //TODO: When request is submitted with optional parameter missing, server should return 400: Bad Request but instead:
     //TODO: <h1>Bad Request</h1> <p>The browser (or proxy) sent a request that this server could not understand.</p>

    @Test(dataProvider = "missingParameters")
    public void mandatoryParameterIsMissing (String parameters, String field) throws URISyntaxException, IOException, InterruptedException {
        logger.info("Starting method mandatoryParameterIsMissing");

        var request = HttpRequest.newBuilder()
                .uri(new URIBuilder()
                        .setScheme("http")
                        .setHost(config.host.url)
                        .setPort(config.host.port)
                        .setPath(BaseClass.REGISTER_PATH)
                        .build())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(parameters, StandardCharsets.UTF_8))
                .build();

        logger.info("API URL: " + request.uri());

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        logger.info("Validating response");

        Assert.assertEquals(response.statusCode(), 400, "Expected response is 400");
        logger.info("Response status code: " + response.statusCode());
        Assert.assertEquals(response.body(), "{\"error\":\"Missing required fields: " + field + "\"}\n", "" +
                "Missing field should be "+ field + " but it is " + response.body());
        logger.info("Response status body: " + response.body());
    }

    @DataProvider(name = "missingParameters")
    public static Object[][] missingParameters(){
        return new Object[][] {
                {"""
                    {
                         "password": "Password1@",
                         "email": "test1@gmail.com",
                         "firstName": "test",
                         "lastName": "test3",
                         "middleName": "test4"
                    }
                    """, "username"},
                {"""
                    {
                         "username": "testUs10",
                         "email": "test1@gmail.com",
                         "firstName": "test",
                         "lastName": "test3",
                         "middleName": "test4"
                    }
                    """, "password"},

                {"""
                    {
                         "username": "testUs10",
                         "password": "Password1@",
                         "firstName": "test",
                         "lastName": "test3",
                         "middleName": "test4"
                    }
                    """, "email"},
        };
    }

    /**
     * Testing various negative scenarios when username, password or email are not in requested format
     * */
    @Test(dataProvider = "usernamePasswordEmailInvalidValues")
    public void usernamePasswordEmailNotInRequestedFormat (String username, String password, String email, String firstName,
                                                  String lastName, String middleName, String errorResponse) throws URISyntaxException, IOException, InterruptedException {
        logger.info("Starting method testNegativeRegisterUserTestCases");

        var registrationRequest = new RegistrationRequest(
                username, password, email, firstName, lastName, middleName
        );

        var jsonRequest = mapper.writeValueAsString(registrationRequest);

        var request = HttpRequest.newBuilder()
                .uri(new URIBuilder()
                        .setScheme("http")
                        .setHost(config.host.url)
                        .setPort(config.host.port)
                        .setPath(BaseClass.REGISTER_PATH)
                        .build())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_16))
                .build();

        logger.info("API URL: " + request.uri());

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        logger.info("Validating response");

        Assert.assertEquals(response.statusCode(), 400, "Expected response is 400");
        logger.info("Response status code: " + response.statusCode());
        Assert.assertEquals(response.body(), "{\"error\":\"" + errorResponse + "\"}\n", "" +
                "Error response should be "+ errorResponse + " but it is " + response.body());
        logger.info("Response status body: " + response.body());
    }

    @DataProvider(name = "usernamePasswordEmailInvalidValues")
    public static Object[][] usernamePasswordEmailInvalidValues() {
        return new Object[][] {
                // username / password / email / firstName / lastName / middleName / errorResponse
                {"", "", "", "", "", "", "Username must be between 5 and 8 characters" },
                {"你", "", "", "", "", "", "Username must be between 5 and 8 characters" },
                {",%^$^&^#$%@$%", "", "", "", "", "", "Username must be between 5 and 8 characters" },
                {"testUser", "pass", "", "", "", "", "Password must be at least 6 characters long" },
                {"testUser", "password", "", "", "", "", "Password must contain at least one uppercase letter" },
                {"testUser", "Password", "", "", "", "", "Password must contain at least one number" },
                {"testUser", "Password1", "", "", "", "", "Password must contain at least one special character" },
                {usernameRandom, "Password1@", emailRandom, "", "", "", "Username already exists" },
                {usernameRandom, "Password1@", "123@com", "", "", "", "Invalid email format" },
        };
    }

    /** Test positive and negative cases when user login */
    @Test(dataProvider = "loginUserParameters")
    public void testLoginUser (String username, String password, Integer statusCode, String responseDescription) throws URISyntaxException, IOException, InterruptedException {
        logger.info("Starting method userLogin");

        var loginRequest = new LoginRequest(username, password);
        var jsonRequest = mapper.writeValueAsString(loginRequest);

        var request = HttpRequest.newBuilder()
                .uri(new URIBuilder()
                        .setScheme("http")
                        .setHost(config.host.url)
                        .setPort(config.host.port)
                        .setPath(BaseClass.LOGIN_PATH)
                        .build())
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                .build();

        logger.info("API URL: " + request.uri());

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        logger.info("Validating response");

        var jsonNode = mapper.readTree(response.body());

        Assert.assertEquals(response.statusCode(), Integer.parseInt(statusCode.toString()), "Expected response is " + statusCode);
        logger.info("Response status code: " + response.statusCode());

        if (response.statusCode() == 200) {
            Assert.assertEquals(jsonNode.get("message").asText(), "Login successful", "Login was not successful");
            logger.info("Response message: " + jsonNode.get("message").asText());
            Assert.assertTrue(response.body().contains("access-token"), "Response body does not contains access token");
            logger.info("Response status body: " + response.body());
            accessToken = jsonNode.get("access-token").asText();
        } else {
            Assert.assertEquals(response.body(), "{\"error\":\"" + responseDescription + "\"}\n", "" +
                    "Error response should be "+ responseDescription + " but it is " + response.body());
            logger.info("Response status body: " + response.body());
        }
    }

    @DataProvider(name = "loginUserParameters")
    public static Object[][] loginUserParameters() {
        return new Object[][] {
                {usernameRandom, password, 200, "access-token"},
                {"usr12345", password, 401, "User does not exist"},
                {usernameRandom, "pswd12345", 401, "Invalid password"},
        };
    }

    @Test(dependsOnMethods = "testLoginUser")
    public void getUserById() throws URISyntaxException, IOException, InterruptedException {
        logger.info("Starting method getUserById");

        if (userId != null && accessToken != null) {
            var request = HttpRequest.newBuilder()
                    .uri(new URIBuilder()
                            .setScheme("http")
                            .setHost(config.host.url)
                            .setPort(config.host.port)
                            .setPath(BaseClass.USER_PATH + userId)
                            .build())
                    .header("Authorization", accessToken)
                    .build();

            logger.info("API URL: " + request.uri());

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            logger.info("Validating response");

            var jsonNode = mapper.readTree(response.body());

            Assert.assertEquals(response.statusCode(), 200, "Response code should be 200");
            logger.info("Response status code: " + response.statusCode());
            Assert.assertEquals(jsonNode.get("username").asText(), usernameRandom, "Username should be " + usernameRandom);
            Assert.assertEquals(jsonNode.get("email").asText(), emailRandom, "Email should be " + emailRandom);
            Assert.assertEquals(jsonNode.get("firstName").asText(), firstName, "First name should be " + firstName);
            Assert.assertEquals(jsonNode.get("middleName").asText(), middleName, "Middle name should be " + middleName); //TODO: bug, middle name should not be null
            Assert.assertEquals(jsonNode.get("lastName").asText(), lastName, "Last name should be " + lastName);
            logger.info("Response status body: " + response.body());

        } else {
            throw new IllegalStateException("userId or accessToken is null!");
        }
    }

    @Test(dependsOnMethods = "testLoginUser", dataProvider = "invalidUserIdToken")
    public void userIdOrAccessTokenIsInvalid(Integer userId, String accessToken, Integer statusCode,
                                                                   String errorResponse) throws URISyntaxException, IOException, InterruptedException {
        logger.info("Starting method getUserById");

        var request = HttpRequest.newBuilder()
                .uri(new URIBuilder()
                        .setScheme("http")
                        .setHost(config.host.url)
                        .setPort(config.host.port)
                        .setPath(BaseClass.USER_PATH + userId.toString())
                        .build())
                .header("Authorization", accessToken)
                .build();

        logger.info("API URL: " + request.uri());

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        logger.info("Validating response");

        Assert.assertEquals(Integer.valueOf(response.statusCode()), statusCode, "Expected response is " + statusCode);
        logger.info("Response status code: " + response.statusCode());
        Assert.assertEquals(response.body(), "{\"error\":\"" + errorResponse + "\"}\n", "" +
                "Error response should be "+ errorResponse + " but it is " + response.body());
        logger.info("Response status body: " + response.body());
    }

    @DataProvider(name = "invalidUserIdToken")
    public static Object[][] invalidUserIdToken() {
        return new Object[][] {
                {userId, "eyJhbGciOiJIUzI1", 401, "Invalid token"},
                {123456789, accessToken, 404, "User not found"}
        };
    }

    @AfterClass(alwaysRun = true)
    public void TearDown() {
        System.out.println("Call method for deleting user data");
    }
}
