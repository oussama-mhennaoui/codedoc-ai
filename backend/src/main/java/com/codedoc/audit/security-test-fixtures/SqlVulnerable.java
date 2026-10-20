public class SqlVulnerable {
    public String buildQuery(String email) {
        return "SELECT * FROM users WHERE email = '"
                + email + "'";
    }
}