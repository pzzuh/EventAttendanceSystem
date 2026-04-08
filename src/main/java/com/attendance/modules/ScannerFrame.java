
// FIXED Scanner Logic
import java.sql.*;
import java.time.LocalTime;
import javax.swing.*;

public class ScannerFrame {
    Connection conn;

    public void scan(String barcode) throws Exception {
        PreparedStatement pst = conn.prepareStatement(
            "SELECT * FROM students WHERE student_id=?");
        pst.setString(1, barcode);
        ResultSet rs = pst.executeQuery();

        if (!rs.next()) {
            JOptionPane.showMessageDialog(null,"Student not found");
            return;
        }

        PreparedStatement ev = conn.prepareStatement(
            "SELECT * FROM events WHERE CURDATE() BETWEEN start_date AND end_date");
        ResultSet re = ev.executeQuery();

        if (!re.next()) {
            JOptionPane.showMessageDialog(null,"No event");
            return;
        }

        PreparedStatement chk = conn.prepareStatement(
            "SELECT * FROM attendance WHERE student_id=? AND event_id=?");
        chk.setString(1, barcode);
        chk.setInt(2, re.getInt("id"));
        ResultSet rc = chk.executeQuery();

        if (rc.next()) {
            JOptionPane.showMessageDialog(null,"Duplicate");
            return;
        }

        LocalTime now = LocalTime.now();
        LocalTime start = re.getTime("start_time").toLocalTime();
        int grace = re.getInt("grace_period");

        String remark = now.isAfter(start.plusMinutes(grace)) ? "Late" : "Present";

        PreparedStatement ins = conn.prepareStatement(
            "INSERT INTO attendance(student_id,event_id,time_in,remark) VALUES(?,?,NOW(),?)");
        ins.setString(1, barcode);
        ins.setInt(2, re.getInt("id"));
        ins.setString(3, remark);
        ins.executeUpdate();

        JOptionPane.showMessageDialog(null,"Recorded: "+remark);
    }
}
