import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqlCorrected {
    public PreparedStatement buildQuery(
            Connection connection, String email)
            throws SQLException {

        String sql =
            "SELECT * FROM users WHERE email = ?";

        PreparedStatement statement =
            connection.prepareStatement(sql);

        statement.setString(1, email);
        return statement;
    }
}