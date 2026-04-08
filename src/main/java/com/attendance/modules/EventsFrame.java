package com.attendance.modules;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class EventsFrame extends JFrame {
    private JComboBox<String> cmbAcadYear, cmbDept, cmbFilterDept;
    private JTextField txtStartDate, txtEndDate, txtStartTime, txtEndTime, txtGrace, txtEventName, txtPenalty, txtSearch;
    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedId = -1;
    private Runnable onClose;

    public EventsFrame() {
        setTitle("AttendX — Events");
        setSize(1060, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        loadDepts();
        loadTable("", "All Departments");
    }

    private void buildUI() {
        JPanel root = UITheme.moduleRoot();

        JLabel title = new JLabel("◈  Events");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UITheme.TEXT_PRIMARY);

        JPanel form = UITheme.cardPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 7, 4, 7);

        int row = 0;
        g.weightx = 0.25;
        // Row 1 labels
        g.gridwidth=1;
        g.gridx=0;g.gridy=row; form.add(UITheme.fieldLabel("ACADEMIC YEAR *"),g);
        g.gridx=1; form.add(UITheme.fieldLabel("DEPARTMENT *"),g);
        g.gridx=2; form.add(UITheme.fieldLabel("EVENT NAME *"),g);
        g.gridx=3; form.add(UITheme.fieldLabel("PENALTY (per day)"),g); row++;

        g.gridy=row;
        g.gridx=0; cmbAcadYear=UITheme.styledCombo(buildYears()); form.add(cmbAcadYear,g);
        g.gridx=1; cmbDept=UITheme.styledCombo(new String[]{"Select Department"}); form.add(cmbDept,g);
        g.gridx=2; txtEventName=UITheme.styledField(); form.add(txtEventName,g);
        g.gridx=3; txtPenalty=UITheme.styledField(); txtPenalty.setText("0"); form.add(txtPenalty,g); row++;

        // Row 2 labels
        g.gridx=0;g.gridy=row; form.add(UITheme.fieldLabel("START DATE (YYYY-MM-DD) *"),g);
        g.gridx=1; form.add(UITheme.fieldLabel("END DATE (YYYY-MM-DD) *"),g);
        g.gridx=2; form.add(UITheme.fieldLabel("START TIME (HH:mm) *"),g);
        g.gridx=3; form.add(UITheme.fieldLabel("END TIME (HH:mm) *"),g); row++;

        String today = LocalDate.now().toString();
        g.gridy=row;
        g.gridx=0; txtStartDate=UITheme.styledField(); txtStartDate.setText(today); form.add(txtStartDate,g);
        g.gridx=1; txtEndDate=UITheme.styledField(); txtEndDate.setText(today); form.add(txtEndDate,g);
        g.gridx=2; txtStartTime=UITheme.styledField(); txtStartTime.setText("08:00"); form.add(txtStartTime,g);
        g.gridx=3; txtEndTime=UITheme.styledField(); txtEndTime.setText("17:00"); form.add(txtEndTime,g); row++;

        g.gridx=0;g.gridy=row;g.gridwidth=2; form.add(UITheme.fieldLabel("GRACE PERIOD (minutes)"),g); g.gridwidth=1; row++;
        g.gridx=0;g.gridy=row;g.gridwidth=2; txtGrace=UITheme.styledField(); txtGrace.setText("15"); form.add(txtGrace,g); g.gridwidth=1; row++;

        g.gridx=0;g.gridy=row;g.gridwidth=4;
        JPanel btnRow=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); btnRow.setOpaque(false);
        JButton btnSave=UITheme.primaryButton("Add Event"); JButton btnUpdate=UITheme.successButton("Update");
        JButton btnDelete=UITheme.dangerButton("Delete"); JButton btnClear=UITheme.outlineButton("Clear");
        btnSave.addActionListener(e->doSave()); btnUpdate.addActionListener(e->doUpdate());
        btnDelete.addActionListener(e->doDelete()); btnClear.addActionListener(e->clearForm());
        btnRow.add(btnSave); btnRow.add(btnUpdate); btnRow.add(btnDelete); btnRow.add(btnClear);
        form.add(btnRow,g);

        // Filter bar
        JPanel filterBar=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        filterBar.setBackground(new Color(249,250,251));
        filterBar.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER_COLOR,1,true),BorderFactory.createEmptyBorder(4,8,4,8)));
        txtSearch=UITheme.styledField(); txtSearch.setPreferredSize(new Dimension(200,32));
        cmbFilterDept=UITheme.styledCombo(new String[]{"All Departments"}); cmbFilterDept.setPreferredSize(new Dimension(170,32));
        JButton btnF=UITheme.primaryButton("Filter"); btnF.addActionListener(e->loadTable(txtSearch.getText().trim(),(String)cmbFilterDept.getSelectedItem()));
        txtSearch.addActionListener(e->loadTable(txtSearch.getText().trim(),(String)cmbFilterDept.getSelectedItem()));
        filterBar.add(new JLabel("Search:") {{ setFont(UITheme.FONT_SMALL); }}); filterBar.add(txtSearch);
        filterBar.add(new JLabel("Dept:") {{ setFont(UITheme.FONT_SMALL); }}); filterBar.add(cmbFilterDept);
        filterBar.add(btnF);

        String[] cols={"ID","Acad Year","Department","Event Name","Start Date","End Date","Start","End","Grace","Penalty"};
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

    private String[] buildYears() {
        int y=LocalDate.now().getYear(); String[] a=new String[6];
        for(int i=0;i<6;i++) a[i]=(y+i)+"-"+(y+i+1); return a;
    }
    private void loadDepts() {
        try(Connection c=DatabaseConnection.getConnection();
            ResultSet rs=c.createStatement().executeQuery("SELECT id,department_name FROM departments ORDER BY department_name")){
            while(rs.next()){ String item=rs.getInt(1)+"|"+rs.getString(2); cmbDept.addItem(item); cmbFilterDept.addItem(item); }
        } catch(SQLException e){e.printStackTrace();}
    }
    private void doSave() {
        String dItem=(String)cmbDept.getSelectedItem();
        String name=txtEventName.getText().trim();
        if("Select Department".equals(dItem)||name.isEmpty()||txtStartDate.getText().trim().isEmpty()||txtEndDate.getText().trim().isEmpty()||txtStartTime.getText().trim().isEmpty()||txtEndTime.getText().trim().isEmpty()){
            JOptionPane.showMessageDialog(this,"Fill all required fields."); return; }
        try(Connection c=DatabaseConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement("INSERT INTO events(academic_year,department_id,start_date,end_date,start_time,end_time,grace_period,event_name,penalty_amount) VALUES(?,?,?,?,?,?,?,?,?)");
            ps.setString(1,(String)cmbAcadYear.getSelectedItem()); ps.setInt(2,Integer.parseInt(dItem.split("\\|")[0]));
            ps.setString(3,txtStartDate.getText().trim()); ps.setString(4,txtEndDate.getText().trim());
            ps.setString(5,txtStartTime.getText().trim()); ps.setString(6,txtEndTime.getText().trim());
            ps.setInt(7,parseInt(txtGrace.getText(),15)); ps.setString(8,name); ps.setDouble(9,parseDouble(txtPenalty.getText(),0));
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this,"Event added."); clearForm(); loadTable("","All Departments");
        } catch(SQLException ex){JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
    }
    private void doUpdate() {
        if(selectedId<0){JOptionPane.showMessageDialog(this,"Select an event.");return;}
        String name=txtEventName.getText().trim(); if(name.isEmpty()){JOptionPane.showMessageDialog(this,"Event name required.");return;}
        try(Connection c=DatabaseConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement("UPDATE events SET event_name=?,start_date=?,end_date=?,start_time=?,end_time=?,grace_period=?,penalty_amount=? WHERE id=?");
            ps.setString(1,name); ps.setString(2,txtStartDate.getText().trim()); ps.setString(3,txtEndDate.getText().trim());
            ps.setString(4,txtStartTime.getText().trim()); ps.setString(5,txtEndTime.getText().trim());
            ps.setInt(6,parseInt(txtGrace.getText(),15)); ps.setDouble(7,parseDouble(txtPenalty.getText(),0)); ps.setInt(8,selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this,"Event updated."); clearForm(); loadTable("","All Departments");
        } catch(SQLException ex){JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
    }
    private void doDelete() {
        if(selectedId<0){JOptionPane.showMessageDialog(this,"Select an event.");return;}
        if(JOptionPane.showConfirmDialog(this,"Delete event?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try(Connection c=DatabaseConnection.getConnection()){
            c.createStatement().executeUpdate("DELETE FROM events WHERE id="+selectedId);
            JOptionPane.showMessageDialog(this,"Deleted."); clearForm(); loadTable("","All Departments");
        } catch(SQLException ex){JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
    }
    private void loadSelected() {
        int r=table.getSelectedRow(); if(r<0)return; selectedId=(int)tableModel.getValueAt(r,0);
        cmbAcadYear.setSelectedItem(tableModel.getValueAt(r,1));
        txtEventName.setText((String)tableModel.getValueAt(r,3));
        txtStartDate.setText((String)tableModel.getValueAt(r,4)); txtEndDate.setText((String)tableModel.getValueAt(r,5));
        txtStartTime.setText((String)tableModel.getValueAt(r,6)); txtEndTime.setText((String)tableModel.getValueAt(r,7));
        txtGrace.setText(String.valueOf(tableModel.getValueAt(r,8))); txtPenalty.setText(String.valueOf(tableModel.getValueAt(r,9)));
    }
    private void clearForm(){ selectedId=-1; txtEventName.setText(""); txtGrace.setText("15"); txtPenalty.setText("0"); table.clearSelection(); }
    private void loadTable(String s, String d){
        tableModel.setRowCount(0);
        StringBuilder sql=new StringBuilder("SELECT e.id,e.academic_year,d.department_name,e.event_name,e.start_date,e.end_date,e.start_time,e.end_time,e.grace_period,e.penalty_amount FROM events e LEFT JOIN departments d ON d.id=e.department_id WHERE 1=1");
        if(!s.isEmpty()) sql.append(" AND e.event_name LIKE '%"+s+"%'");
        if(d!=null&&!d.startsWith("All")) sql.append(" AND e.department_id="+d.split("\\|")[0]);
        sql.append(" ORDER BY e.start_date DESC");
        try(Connection c=DatabaseConnection.getConnection(); ResultSet rs=c.createStatement().executeQuery(sql.toString())){
            while(rs.next()) tableModel.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getInt(9),rs.getDouble(10)});
        } catch(SQLException e){e.printStackTrace();}
    }
    private int parseInt(String s,int def){try{return Integer.parseInt(s.trim());}catch(Exception e){return def;}}
    private double parseDouble(String s,double def){try{return Double.parseDouble(s.trim());}catch(Exception e){return def;}}
}
