import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class TestUserRegistrationAndLogin {
    /** generate unique username and email as global variables */
    private static final Random random = new Random();
    private static final String usernameRandom = "test"+random.nextInt(1000);
    private static final String emailRandom = usernameRandom+"@mail.com";
    private static final String password = "Password1@";
    private static final String firstName = "John";
    private static final String middleName = "Sarah";
    private static final String lastName = "Connor";
    private static final Config config = YamlReader.readConfig("configuration.yaml");
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static Integer userId;
    private static String accessToken;

    /** Creating test user positive case */
    @Test
    public void creatingNewTestUser() throws URISyntaxException, IOException, InterruptedException {
        var registerUri = new URI("http", null, config.host.url, config.host.port, "/" + config.register.path, null, null);
        var registrationRequest = new RegistrationRequest(
                usernameRandom, password, emailRandom, firstName, lastName, middleName
        );

        var jsonRequest = mapper.writeValueAsString(registrationRequest);
        var request = HttpRequest.newBuilder()
                .uri(URI.create(registerUri.toString()))
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        var jsonNode = mapper.readTree(response.body());
        userId = jsonNode.get("id").asInt(); //Extracting id from response when creating the user

        Assert.assertEquals(response.statusCode(), 200, "Response code should be 200");
        Assert.assertEquals(jsonNode.get("username").asText(), usernameRandom, "Username should be " + usernameRandom);
        Assert.assertEquals(jsonNode.get("email").asText(), emailRandom, "Email should be " + emailRandom);
        Assert.assertEquals(jsonNode.get("firstName").asText(), firstName, "First name should be " + firstName);
        Assert.assertEquals(jsonNode.get("middleName").asText(), middleName, "Middle name should be " + middleName);
        Assert.assertEquals(jsonNode.get("lastName").asText(), lastName, "Last name should be " + lastName);
    }

    /** Creating new user */
    @Test(dataProvider = "creatingNewUser", dependsOnMethods = {"creatingNewTestUser"})
    public void testCreatingNewUserTestCases(String username, String password, String email, String firstName,
                                    String lastName, String middleName, String responseCode, String responseDescription)
            throws URISyntaxException, IOException, InterruptedException {

        var registerUri = new URI("http", null, config.host.url, config.host.port, "/" + config.register.path, null, null);

        var registrationRequest = new RegistrationRequest(
                username, password, email, firstName, lastName, middleName
        );

        var jsonRequest = mapper.writeValueAsString(registrationRequest);
        var request = HttpRequest.newBuilder()
                .uri(URI.create(registerUri.toString()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assert.assertEquals(response.statusCode(), Integer.parseInt(responseCode), "Expected response is 400");
        Assert.assertEquals(response.body(), "{\"error\":\"" + responseDescription + "\"}\n", "" +
                "Error response should be "+ responseDescription + " but it is " + response.body());

    }
    @DataProvider(name = "creatingNewUser")
    public static Object[][] creatingNewUser() {
        return new Object[][] {
                // username / password / email / firstName / lastName / middleName / responseCode / responseDescription
                {usernameRandom, "Password1@", emailRandom, "testFirstName", "testLastName", "testMiddleName", "400", "Username already exists"},
                {usernameRandom+1, "Password1@", emailRandom, "testFirstName", "testLastName", "testMiddleName", "400", "Email already exists"},
                {usernameRandom+1, "Password1@", "123.com", "testFirstName", "testLastName", "testMiddleName", "400", "Invalid email format"},
        };
    }

    /** Creating test user when one of the parameters is missing */
     //TODO: When request is submitted with optional parameter missing: <h1>Bad Request</h1> <p>The browser (or proxy) sent a request that this server could not understand.</p>

    @Test(dataProvider = "ParameterIsMissing")
    public void testCreatingUserWhenMandatoryParameterIsMissing(String parameters, String field) throws URISyntaxException, IOException, InterruptedException {
        var registerUri = new URI("http", null, config.host.url, config.host.port, "/" + config.register.path, null, null);

        var jsonRequest = parameters;

        var request = HttpRequest.newBuilder()
                .uri(URI.create(registerUri.toString()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assert.assertEquals(response.statusCode(), 400, "Expected response is 400");
        Assert.assertEquals(response.body(), "{\"error\":\"Missing required fields: " + field + "\"}\n", "" +
                "Missing field should be "+ field + " but it is " + response.body());
    }

    @DataProvider(name = "ParameterIsMissing")
    public static Object[][] ParameterIsMissing(){
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
//TODO: create test case when first name is missing
//                {"""
//                    {
//                         "username": "testUs10",
//                         "email": "test1@gmail.com",
//                         "password": "Password1@",
//                         "lastName": "test3",
//                         "middleName": "test4"
//                    }
//                    """, "firstName"},

        };
    }


    /**
     * Testing various negative scenarios when user is creating account
     * */
    @Test(dataProvider = "negativeRegisterUserTestCases", dependsOnMethods = {"creatingNewTestUser"})
    public void testNegativeRegisterUserTestCases(String username, String password, String email, String firstName,
                                                  String lastName, String middleName, String errorResponse) throws URISyntaxException, IOException, InterruptedException {
        var registerUri = new URI("http", null, config.host.url, config.host.port, "/" + config.register.path, null, null);

        var registrationRequest = new RegistrationRequest(
                username, password, email, firstName, lastName, middleName
        );

        var jsonRequest = mapper.writeValueAsString(registrationRequest);
        var request = HttpRequest.newBuilder()
                .uri(URI.create(registerUri.toString()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_16))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assert.assertEquals(response.statusCode(), 400, "Expected response is 400");
        Assert.assertEquals(response.body(), "{\"error\":\"" + errorResponse + "\"}\n", "" +
                "Error response should be "+ errorResponse + " but it is " + response.body());
    }

    @DataProvider(name = "negativeRegisterUserTestCases")
    public static Object[][] negativeRegisterUserTestCases() {
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

    /** Test login user */
    @Test(dataProvider = "loginUser", dependsOnMethods = "creatingNewTestUser")
    public void testLoginUser(String username, String password, Integer statusCode, String responseDescription) throws URISyntaxException, IOException, InterruptedException {
        var loginUri = new URI("http", null, config.host.url, config.host.port, "/" + config.login.path, null, null);
        var loginRequest = new LoginRequest(username, password);
        var jsonRequest = mapper.writeValueAsString(loginRequest);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(loginUri.toString()))
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        var jsonNode = mapper.readTree(response.body());

        Assert.assertEquals(response.statusCode(), Integer.parseInt(statusCode.toString()), "Expected response is " + statusCode);

        if (response.statusCode() == 200) {
            Assert.assertEquals(jsonNode.get("message").asText(), "Login successful", "Login was not successful");
            Assert.assertTrue(response.body().contains("access-token"), "Response body does not contains access token");
            accessToken = jsonNode.get("access-token").asText();
        } else {
            Assert.assertEquals(response.body(), "{\"error\":\"" + responseDescription + "\"}\n", "" +
                    "Error response should be "+ responseDescription + " but it is " + response.body());
        }
    }

    @DataProvider(name = "loginUser")
    public static Object[][] loginUser() {
        return new Object[][] {
                {usernameRandom, password, 200, "access-token"},
                {"usr12345", password, 401, "User does not exist"},
                {usernameRandom, "pswd12345", 401, "Invalid password"},
        };
    }

    /** Return user data when userId and Access token are provided */
    @Test(dependsOnMethods = "testLoginUser")
    public void testGetUserById() throws URISyntaxException, IOException, InterruptedException {
        if (userId != null && accessToken != null) {
            var loginUri = new URI("http", null, config.host.url, config.host.port, "/" + config.user.path + "/" + userId, null, null);

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(loginUri.toString()))
                    .header("Authorization", accessToken)
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            var jsonNode = mapper.readTree(response.body());

            Assert.assertEquals(response.statusCode(), 200, "Response code should be 200");
            Assert.assertEquals(jsonNode.get("username").asText(), usernameRandom, "Username should be " + usernameRandom);
            Assert.assertEquals(jsonNode.get("email").asText(), emailRandom, "Email should be " + emailRandom);
            Assert.assertEquals(jsonNode.get("firstName").asText(), firstName, "First name should be " + firstName);
            Assert.assertEquals(jsonNode.get("middleName").asText(), middleName, "Middle name should be " + middleName); //TODO: bug, middle name should not be null
            Assert.assertEquals(jsonNode.get("lastName").asText(), lastName, "Last name should be " + lastName);

        } else {
            throw new IllegalStateException("userId or accessToken is null!");
        }
    }

    @Test(dependsOnMethods = "testLoginUser", dataProvider = "getUserByIdWhenUserIdOrAccessTokenAreIncorrect")
    public void testGetUserByIdWhenUserIdOrAccessTokenAreIncorrect(Integer userId, String accessToken, Integer statusCode,
                                                                   String errorResponse) throws URISyntaxException, IOException, InterruptedException {
        var loginUri = new URI("http", null, config.host.url, config.host.port, "/" + config.user.path + "/" + userId, null, null);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(loginUri.toString()))
                .header("Authorization", accessToken)
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assert.assertEquals(Integer.valueOf(response.statusCode()), Integer.valueOf(statusCode), "Expected response is " + statusCode);
        Assert.assertEquals(response.body(), "{\"error\":\"" + errorResponse + "\"}\n", "" +
                "Error response should be "+ errorResponse + " but it is " + response.body());
    }

    @DataProvider(name = "getUserByIdWhenUserIdOrAccessTokenAreIncorrect")
    public static Object[][] getUserByIdWhenUserIdOrAccessTokenAreIncorrect() {
        return new Object[][] {
                {userId, "eyJhbGciOiJIUzI1", 401, "Invalid token"},
                {123456789, accessToken, 404, "User not found"}
        };
    }

    @AfterClass(alwaysRun = true)
    public void TearDown() {
        System.out.print("Call method for deleting user data");
    }
}
