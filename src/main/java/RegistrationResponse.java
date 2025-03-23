import java.util.Objects;

public class RegistrationResponse {
    private Integer id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String middleName;

    public RegistrationResponse() {}

    public RegistrationResponse(Integer id, String username, String email, String firstName, String lastName, String middleName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
    }

    public Integer getUserId() { return id; }
    public void setUserId(Integer id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegistrationResponse that = (RegistrationResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(email, that.email) &&
                Objects.equals(username, that.username) &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(lastName, that.lastName) &&
                Objects.equals(middleName, that.middleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, username, firstName, lastName, middleName);
    }






//    private Integer userId;
//    private String email;
//    private String username;
//    private String firstName;
//    private String lastName;
//    private String middleName;
//    private Integer statusCode;
//
//    public RegistrationResponse(Integer userId, String username, String email, String firstName, String lastName, String middleName,
//                                Integer statusCode) {
//        this.userId = userId;
//        this.username = username;
//        this.email = email;
//        this.firstName = firstName;
//        this.lastName = lastName;
//        this.middleName = middleName;
//        this.statusCode = statusCode;
//    }
//



}
