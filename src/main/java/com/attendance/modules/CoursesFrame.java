package com.attendance.modules;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class CoursesFrame extends JFrame {
    private JComboBox<String> cmbDept;
    private JTextField txtCourseName, txtSearch;
    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedId = -1;

    public CoursesFrame() {
        setTitle("AttendX — Courses");
        setSize(820, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI(); loadDepts(); loadTable("");
    }

    private void buildUI() {
        JPanel root = UITheme.moduleRoot();
        JLabel title = new JLabel("◈  Courses");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UITheme.TEXT_PRIMARY);

        JPanel form = UITheme.cardPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill=GridBagConstraints.HORIZONTAL; g.insets=new Insets(5,8,5,8); g.weightx=0.5;

        g.gridx=0;g.gridy=0;g.gridwidth=1; form.add(UITheme.fieldLabel("DEPARTMENT *"),g);
        g.gridx=1; form.add(UITheme.fieldLabel("COURSE NAME *"),g);

        g.gridy=1;
        g.gridx=0; cmbDept=UITheme.styledCombo(new String[]{"Select Department"}); form.add(cmbDept,g);
        g.gridx=1; txtCourseName=UITheme.styledField(); form.add(txtCourseName,g);

        g.gridx=0;g.gridy=2;g.gridwidth=2;
        JPanel btnRow=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); btnRow.setOpaque(false);
        JButton btnSave=UITheme.primaryButton("Add Course");
        JButton btnUpdate=UITheme.successButton("Update");
        JButton btnDelete=UITheme.dangerButton("Delete");
        JButton btnClear=UITheme.outlineButton("Clear");
        btnSave.addActionListener(e->doSave()); btnUpdate.addActionListener(e->doUpdate());
        btnDelete.addActionListener(e->doDelete()); btnClear.addActionListener(e->clearForm());
        btnRow.add(btnSave); btnRow.add(btnUpdate); btnRow.add(btnDelete); btnRow.add(btnClear);
        form.add(btnRow,g);

        JPanel filterBar=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        filterBar.setBackground(new Color(249,250,251));
        filterBar.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER_COLOR,1,true),BorderFactory.createEmptyBorder(4,8,4,8)));
        txtSearch=UITheme.styledField(); txtSearch.setPreferredSize(new Dimension(220,32));
        JButton btnSearch=UITheme.primaryButton("Search"); btnSearch.addActionListener(e->loadTable(txtSearch.getText().trim()));
        txtSearch.addActionListener(e->loadTable(txtSearch.getText().trim()));
        filterBar.add(new JLabel("Search:") {{ setFont(UITheme.FONT_SMALL); }}); filterBar.add(txtSearch); filterBar.add(btnSearch);

        String[] cols={"ID","Department","Course Name"};
        tableModel=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        table=new JTable(tableModel);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())loadSelected();});

        JPanel tableCard=new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new LineBorder(UITheme.BORDER_COLOR,1,true));
        tableCard.add(filterBar,BorderLayout.NORTH);
        tableCard.add(UITheme.styledScrollPane(table),BorderLayout.CENTER);

        root.add(title,BorderLayout.NORTH);
        JPanel center=new JPanel(new BorderLayout(0,12)); center.setOpaque(false);
        center.add(form,BorderLayout.NORTH); center.add(tableCard,BorderLayout.CENTER);
        root.add(center,BorderLayout.CENTER);
        setContentPane(root);
    }

    private void loadDepts(){
        try(Connection c=DatabaseConnection.getConnection();
            ResultSet rs=c.createStatement().executeQuery("SELECT id,department_name FROM departments ORDER BY department_name")){
            while(rs.next()) cmbDept.addItem(rs.getInt(1)+"|"+rs.getString(2));
        }catch(SQLException e){e.printStackTrace();}
    }
    private void doSave(){
        String item=(String)cmbDept.getSelectedItem(); String name=txtCourseName.getText().trim();
        if("Select Department".equals(item)||name.isEmpty()){JOptionPane.showMessageDialog(this,"Fill required fields.");return;}
        try(Connection c=DatabaseConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement("INSERT INTO courses(department_id,course_name) VALUES(?,?)");
            ps.setInt(1,Integer.parseInt(item.split("\\|")[0])); ps.setString(2,name); ps.executeUpdate();
            JOptionPane.showMessageDialog(this,"Course added."); clearForm(); loadTable("");
        }catch(SQLException ex){JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
    }
    private void doUpdate(){
        if(selectedId<0){JOptionPane.showMessageDialog(this,"Select a course.");return;}
        String name=txtCourseName.getText().trim(); if(name.isEmpty()){JOptionPane.showMessageDialog(this,"Name required.");return;}
        try(Connection c=DatabaseConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement("UPDATE courses SET course_name=? WHERE id=?");
            ps.setString(1,name); ps.setInt(2,selectedId); ps.executeUpdate();
            JOptionPane.showMessageDialog(this,"Updated."); clearForm(); loadTable("");
        }catch(SQLException ex){JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
    }
    private void doDelete(){
        if(selectedId<0){JOptionPane.showMessageDialog(this,"Select a course.");return;}
        if(JOptionPane.showConfirmDialog(this,"Delete this course?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try(Connection c=DatabaseConnection.getConnection()){
            c.createStatement().executeUpdate("DELETE FROM courses WHERE id="+selectedId);
            JOptionPane.showMessageDialog(this,"Deleted."); clearForm(); loadTable("");
        }catch(SQLException ex){JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
    }
    private void loadSelected(){
        int row=table.getSelectedRow(); if(row<0)return; selectedId=(int)tableModel.getValueAt(row,0);
        txtCourseName.setText((String)tableModel.getValueAt(row,2));
    }
    private void clearForm(){ selectedId=-1; txtCourseName.setText(""); cmbDept.setSelectedIndex(0); table.clearSelection(); }
    private void loadTable(String s){
        tableModel.setRowCount(0);
        String sql="SELECT c.id,d.department_name,c.course_name FROM courses c JOIN departments d ON d.id=c.department_id" +
            (s.isEmpty()?"":" WHERE c.course_name LIKE '%"+s+"%'")+" ORDER BY c.course_name";
        try(Connection c=DatabaseConnection.getConnection(); ResultSet rs=c.createStatement().executeQuery(sql)){
            while(rs.next()) tableModel.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3)});
        }catch(SQLException e){e.printStackTrace();}
    }
}
