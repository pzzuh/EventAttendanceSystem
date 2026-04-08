package com.attendance.modules;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.UITheme;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.util.Base64;

public class StudentsFrame extends JFrame {
    private JComboBox<String> cmbCollege, cmbDept, cmbCourse, cmbYearLevel, cmbFilterDept, cmbFilterCourse;
    private JTextField txtStudentId, txtFirst, txtMiddle, txtLast, txtSearch;
    private JLabel lblPhoto;
    private String photoBase64 = "";
    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedId = -1;
    private Runnable onClose;

    public StudentsFrame() {
        setTitle("AttendX — Students");
        setSize(1060, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        loadColleges();
        loadFilterDepts();
        loadTable();
    }

    private void buildUI() {
        JPanel root = UITheme.moduleRoot();

        // ── Title bar ────────────────────────────────────────────────────
        JLabel title = new JLabel("◈  Students");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UITheme.TEXT_PRIMARY);

        // ── Form card ────────────────────────────────────────────────────
        JPanel form = UITheme.cardPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 7, 4, 7);

        int row = 0;
        // College / Dept / Course / Year Level labels
        g.weightx=0.25;
        g.gridx=0;g.gridy=row;g.gridwidth=1; form.add(UITheme.fieldLabel("COLLEGE *"),g);
        g.gridx=1; form.add(UITheme.fieldLabel("DEPARTMENT *"),g);
        g.gridx=2; form.add(UITheme.fieldLabel("COURSE *"),g);
        g.gridx=3; form.add(UITheme.fieldLabel("YEAR LEVEL *"),g); row++;

        g.gridy=row;
        g.gridx=0; cmbCollege=UITheme.styledCombo(new String[]{"Select College"}); form.add(cmbCollege,g);
        g.gridx=1; cmbDept=UITheme.styledCombo(new String[]{"Select Department"}); form.add(cmbDept,g);
        g.gridx=2; cmbCourse=UITheme.styledCombo(new String[]{"Select Course"}); form.add(cmbCourse,g);
        g.gridx=3; cmbYearLevel=UITheme.styledCombo(new String[]{"1st Year","2nd Year","3rd Year","4th Year"}); form.add(cmbYearLevel,g); row++;

        cmbCollege.addActionListener(e -> loadDeptsForCollege());
        cmbDept.addActionListener(e -> loadCoursesForDept());

        // Student ID / First / Middle / Last
        g.gridx=0;g.gridy=row; form.add(UITheme.fieldLabel("STUDENT ID *"),g);
        g.gridx=1; form.add(UITheme.fieldLabel("FIRST NAME *"),g);
        g.gridx=2; form.add(UITheme.fieldLabel("MIDDLE NAME"),g);
        g.gridx=3; form.add(UITheme.fieldLabel("LAST NAME *"),g); row++;

        g.gridy=row;
        g.gridx=0; txtStudentId=UITheme.styledField(); form.add(txtStudentId,g);
        g.gridx=1; txtFirst=UITheme.styledField(); form.add(txtFirst,g);
        g.gridx=2; txtMiddle=UITheme.styledField(); form.add(txtMiddle,g);
        g.gridx=3; txtLast=UITheme.styledField(); form.add(txtLast,g); row++;

        // Photo
        g.gridx=0;g.gridy=row;g.gridwidth=1; form.add(UITheme.fieldLabel("PHOTO"),g); row++;
        g.gridy=row;g.gridwidth=1;g.gridx=0;
        lblPhoto = new JLabel("No Photo", SwingConstants.CENTER);
        lblPhoto.setPreferredSize(new Dimension(72, 72));
        lblPhoto.setFont(UITheme.FONT_SMALL); lblPhoto.setForeground(UITheme.TEXT_MUTED);
        lblPhoto.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER_COLOR,1,true), BorderFactory.createEmptyBorder(4,4,4,4)));
        lblPhoto.setBackground(new Color(249,250,251)); lblPhoto.setOpaque(true);
        form.add(lblPhoto,g);
        g.gridx=1;
        JButton btnPhoto = UITheme.outlineButton("Upload Photo");
        btnPhoto.addActionListener(e -> uploadPhoto());
        form.add(btnPhoto,g); row++;

        // Action buttons
        g.gridx=0;g.gridy=row;g.gridwidth=4;
        JPanel btnRow=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); btnRow.setOpaque(false);
        JButton btnSave=UITheme.primaryButton("Add Student");
        JButton btnUpdate=UITheme.successButton("Update");
        JButton btnDelete=UITheme.dangerButton("Delete");
        JButton btnClear=UITheme.outlineButton("Clear");
        btnSave.addActionListener(e->doSave()); btnUpdate.addActionListener(e->doUpdate());
        btnDelete.addActionListener(e->doDelete()); btnClear.addActionListener(e->clearForm());
        btnRow.add(btnSave); btnRow.add(btnUpdate); btnRow.add(btnDelete); btnRow.add(btnClear);
        form.add(btnRow,g);

        // ── Filter bar ───────────────────────────────────────────────────
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setBackground(new Color(249,250,251));
        filterBar.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER_COLOR,1,true), BorderFactory.createEmptyBorder(4,8,4,8)));

        txtSearch = UITheme.styledField(); txtSearch.setPreferredSize(new Dimension(200,32));
        cmbFilterDept = UITheme.styledCombo(new String[]{"All Departments"}); cmbFilterDept.setPreferredSize(new Dimension(170,32));
        cmbFilterCourse = UITheme.styledCombo(new String[]{"All Courses"}); cmbFilterCourse.setPreferredSize(new Dimension(150,32));
        JButton btnSearch2 = UITheme.primaryButton("Filter");
        btnSearch2.addActionListener(e->loadTable()); txtSearch.addActionListener(e->loadTable());
        cmbFilterDept.addActionListener(e -> { loadFilterCourses(); loadTable(); });
        cmbFilterCourse.addActionListener(e -> loadTable());

        filterBar.add(new JLabel("Search:") {{ setFont(UITheme.FONT_SMALL); }}); filterBar.add(txtSearch);
        filterBar.add(new JLabel("Dept:") {{ setFont(UITheme.FONT_SMALL); }}); filterBar.add(cmbFilterDept);
        filterBar.add(new JLabel("Course:") {{ setFont(UITheme.FONT_SMALL); }}); filterBar.add(cmbFilterCourse);
        filterBar.add(btnSearch2);

        // ── Table ────────────────────────────────────────────────────────
        String[] cols={"ID","Student ID","First Name","Middle Name","Last Name","Course","Year"};
        tableModel=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        table=new JTable(tableModel);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())loadSelected();});

        JPanel tableCard=new JPanel(new BorderLayout(0,0));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new LineBorder(UITheme.BORDER_COLOR,1,true));
        tableCard.add(filterBar, BorderLayout.NORTH);
        tableCard.add(UITheme.styledScrollPane(table), BorderLayout.CENTER);

        root.add(title, BorderLayout.NORTH);
        JPanel center=new JPanel(new BorderLayout(0,12)); center.setOpaque(false);
        center.add(form, BorderLayout.NORTH);
        center.add(tableCard, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void loadColleges() {
        try(Connection c=DatabaseConnection.getConnection();
            ResultSet rs=c.createStatement().executeQuery("SELECT id,college_name FROM colleges ORDER BY college_name")){
            while(rs.next()) cmbCollege.addItem(rs.getInt(1)+"|"+rs.getString(2));
        } catch(SQLException e){e.printStackTrace();}
    }
    private void loadDeptsForCollege() {
        String item=(String)cmbCollege.getSelectedItem();
        cmbDept.removeAllItems(); cmbDept.addItem("Select Department");
        cmbCourse.removeAllItems(); cmbCourse.addItem("Select Course");
        if(item==null||item.startsWith("Select"))return;
        int id=Integer.parseInt(item.split("\\|")[0]);
        try(Connection c=DatabaseConnection.getConnection();
            PreparedStatement ps=c.prepareStatement("SELECT id,department_name FROM departments WHERE college_id=? ORDER BY department_name")){
            ps.setInt(1,id); ResultSet rs=ps.executeQuery();
            while(rs.next()) cmbDept.addItem(rs.getInt(1)+"|"+rs.getString(2));
        } catch(SQLException e){e.printStackTrace();}
    }
    private void loadCoursesForDept() {
        String item=(String)cmbDept.getSelectedItem();
        cmbCourse.removeAllItems(); cmbCourse.addItem("Select Course");
        if(item==null||item.startsWith("Select"))return;
        int id=Integer.parseInt(item.split("\\|")[0]);
        try(Connection c=DatabaseConnection.getConnection();
            PreparedStatement ps=c.prepareStatement("SELECT id,course_name FROM courses WHERE department_id=? ORDER BY course_name")){
            ps.setInt(1,id); ResultSet rs=ps.executeQuery();
            while(rs.next()) cmbCourse.addItem(rs.getInt(1)+"|"+rs.getString(2));
        } catch(SQLException e){e.printStackTrace();}
    }
    private void loadFilterDepts() {
        try(Connection c=DatabaseConnection.getConnection();
            ResultSet rs=c.createStatement().executeQuery("SELECT id,department_name FROM departments ORDER BY department_name")){
            while(rs.next()) cmbFilterDept.addItem(rs.getInt(1)+"|"+rs.getString(2));
        } catch(SQLException e){e.printStackTrace();}
    }
    private void loadFilterCourses() {
        String item=(String)cmbFilterDept.getSelectedItem();
        cmbFilterCourse.removeAllItems(); cmbFilterCourse.addItem("All Courses");
        if(item==null||item.startsWith("All"))return;
        int id=Integer.parseInt(item.split("\\|")[0]);
        try(Connection c=DatabaseConnection.getConnection();
            PreparedStatement ps=c.prepareStatement("SELECT id,course_name FROM courses WHERE department_id=?")){
            ps.setInt(1,id); ResultSet rs=ps.executeQuery();
            while(rs.next()) cmbFilterCourse.addItem(rs.getInt(1)+"|"+rs.getString(2));
        } catch(SQLException e){e.printStackTrace();}
    }
    private void uploadPhoto() {
        JFileChooser fc=new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Images","jpg","jpeg","png","gif"));
        if(fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        try {
            byte[] bytes=Files.readAllBytes(fc.getSelectedFile().toPath());
            photoBase64=Base64.getEncoder().encodeToString(bytes);
            BufferedImage img=ImageIO.read(fc.getSelectedFile());
            lblPhoto.setIcon(new ImageIcon(img.getScaledInstance(72,72,Image.SCALE_SMOOTH)));
            lblPhoto.setText("");
        } catch(IOException ex){ JOptionPane.showMessageDialog(this,"Cannot read image file."); }
    }
    private void doSave() {
        String sid=txtStudentId.getText().trim(), fn=txtFirst.getText().trim(), ln=txtLast.getText().trim();
        String colItem=(String)cmbCollege.getSelectedItem();
        String dItem=(String)cmbDept.getSelectedItem();
        String cItem=(String)cmbCourse.getSelectedItem();
        if(sid.isEmpty()||fn.isEmpty()||ln.isEmpty()||"Select College".equals(colItem)||
           "Select Department".equals(dItem)||"Select Course".equals(cItem)){
            JOptionPane.showMessageDialog(this,"Fill all required fields."); return; }
        try(Connection c=DatabaseConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement(
                "INSERT INTO students(college_id,department_id,course_id,student_id_number,first_name,middle_name,last_name,photo_base64,year_level) VALUES(?,?,?,?,?,?,?,?,?)");
            ps.setInt(1,Integer.parseInt(colItem.split("\\|")[0]));
            ps.setInt(2,Integer.parseInt(dItem.split("\\|")[0]));
            ps.setInt(3,Integer.parseInt(cItem.split("\\|")[0]));
            ps.setString(4,sid); ps.setString(5,fn); ps.setString(6,txtMiddle.getText().trim());
            ps.setString(7,ln); ps.setString(8,photoBase64.isEmpty()?null:photoBase64);
            ps.setString(9,(String)cmbYearLevel.getSelectedItem());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this,"Student added."); clearForm(); loadTable();
        } catch(SQLException ex){JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
    }
    private void doUpdate() {
        if(selectedId<0){JOptionPane.showMessageDialog(this,"Select a student first.");return;}
        String sid=txtStudentId.getText().trim(), fn=txtFirst.getText().trim(), ln=txtLast.getText().trim();
        if(sid.isEmpty()||fn.isEmpty()||ln.isEmpty()){JOptionPane.showMessageDialog(this,"Fill required fields.");return;}
        try(Connection c=DatabaseConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement(
                "UPDATE students SET student_id_number=?,first_name=?,middle_name=?,last_name=?,year_level=? WHERE id=?");
            ps.setString(1,sid); ps.setString(2,fn); ps.setString(3,txtMiddle.getText().trim());
            ps.setString(4,ln); ps.setString(5,(String)cmbYearLevel.getSelectedItem()); ps.setInt(6,selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this,"Student updated."); clearForm(); loadTable();
        } catch(SQLException ex){JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
    }
    private void doDelete() {
        if(selectedId<0){JOptionPane.showMessageDialog(this,"Select a student.");return;}
        if(JOptionPane.showConfirmDialog(this,"Delete this student?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try(Connection c=DatabaseConnection.getConnection()){
            c.createStatement().executeUpdate("DELETE FROM students WHERE id="+selectedId);
            JOptionPane.showMessageDialog(this,"Deleted."); clearForm(); loadTable();
        } catch(SQLException ex){JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
    }
    private void loadSelected() {
        int row=table.getSelectedRow(); if(row<0)return;
        selectedId=(int)tableModel.getValueAt(row,0);
        txtStudentId.setText((String)tableModel.getValueAt(row,1));
        txtFirst.setText((String)tableModel.getValueAt(row,2));
        txtMiddle.setText(tableModel.getValueAt(row,3)==null?"":(String)tableModel.getValueAt(row,3));
        txtLast.setText((String)tableModel.getValueAt(row,4));
    }
    private void clearForm() {
        selectedId=-1; txtStudentId.setText(""); txtFirst.setText(""); txtMiddle.setText(""); txtLast.setText("");
        photoBase64=""; lblPhoto.setIcon(null); lblPhoto.setText("No Photo");
        cmbCollege.setSelectedIndex(0); cmbDept.removeAllItems(); cmbDept.addItem("Select Department");
        cmbCourse.removeAllItems(); cmbCourse.addItem("Select Course"); table.clearSelection();
    }
    private void loadTable() {
        tableModel.setRowCount(0);
        StringBuilder sql=new StringBuilder(
            "SELECT s.id,s.student_id_number,s.first_name,s.middle_name,s.last_name,c.course_name,s.year_level " +
            "FROM students s LEFT JOIN courses c ON c.id=s.course_id WHERE 1=1");
        String search=txtSearch.getText().trim();
        if(!search.isEmpty()) sql.append(" AND (s.first_name LIKE '%"+search+"%' OR s.last_name LIKE '%"+search+"%' OR s.student_id_number LIKE '%"+search+"%')");
        String di=(String)cmbFilterDept.getSelectedItem();
        if(di!=null&&!di.startsWith("All")) sql.append(" AND s.department_id="+di.split("\\|")[0]);
        String ci=(String)cmbFilterCourse.getSelectedItem();
        if(ci!=null&&!ci.startsWith("All")) sql.append(" AND s.course_id="+ci.split("\\|")[0]);
        sql.append(" ORDER BY s.last_name,s.first_name");
        try(Connection c=DatabaseConnection.getConnection(); ResultSet rs=c.createStatement().executeQuery(sql.toString())){
            while(rs.next()) tableModel.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7)});
        } catch(SQLException e){e.printStackTrace();}
    }
}
