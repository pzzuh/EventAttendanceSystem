
// FIXED CollegesFrame (Auto Dean User)
import java.sql.*;

public class CollegesFrame {
    Connection conn;

    public void saveDean(String f, String m, String l) throws Exception {
        String username = f.toLowerCase() + "." + l.toLowerCase();
        String sql = "INSERT INTO users (first_name,middle_name,last_name,username,password,role,status) VALUES (?,?,?,?,?,'Super Admin','Active')";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1,f);
        pst.setString(2,m);
        pst.setString(3,l);
        pst.setString(4,username);
        pst.setString(5,"123456");
        pst.executeUpdate();
    }
}
