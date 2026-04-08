
// FIXED UsersFrame (Role + Status)
import javax.swing.*;
import java.sql.*;

public class UsersFrame {
    JComboBox<String> roleComboBox;
    JComboBox<String> statusComboBox;

    public UsersFrame() {
        String[] roles = {
            "Super Admin","Program Head","Department President",
            "Department Secretary","Department Treasurer"
        };
        roleComboBox = new JComboBox<>(roles);

        String[] status = {"Active","Inactive"};
        statusComboBox = new JComboBox<>(status);
    }
}
