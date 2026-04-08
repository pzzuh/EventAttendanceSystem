package com.attendance.util;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import javax.swing.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AttendancePdfExporter.java
 *
 * Handles PDF export for the Attendance Report module.
 * Fixes the missing PDF export requirement (repo only had CSV).
 *
 * File location: src/main/java/com/attendance/AttendancePdfExporter.java
 *
 * Usage in your AttendanceReportFrame:
 *   AttendancePdfExporter.export(parentFrame, tableModel, summaryStats);
 */
public class AttendancePdfExporter {

    /**
     * Opens a save-file dialog and exports the JTable data to PDF.
     *
     * @param parent       Parent JFrame for the file chooser dialog
     * @param table        The JTable displaying attendance records
     * @param totalPresent Count of present students
     * @param totalLate    Count of late students
     * @param totalAbsent  Count of absent students
     * @param totalPenalty Total penalty amount
     * @param filterInfo   String describing active filters (e.g. "College: COE | Dept: IT")
     */
    public static void export(JFrame parent,
                              JTable table,
                              int totalPresent,
                              int totalLate,
                              int totalAbsent,
                              double totalPenalty,
                              String filterInfo) {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Attendance Report as PDF");
        chooser.setSelectedFile(new File("AttendanceReport_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "PDF Files (*.pdf)", "pdf"));

        int result = chooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) return;

        String filePath = chooser.getSelectedFile().getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".pdf")) filePath += ".pdf";

        try {
            PdfWriter writer   = new PdfWriter(filePath);
            PdfDocument pdf    = new PdfDocument(writer);
            Document document  = new Document(pdf, PageSize.A4.rotate()); // landscape for wide table
            document.setMargins(30, 30, 30, 30);

            // ── Title ─────────────────────────────────────────────────────
            document.add(new Paragraph("EVENT ATTENDANCE REPORT")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(16));

            document.add(new Paragraph("Generated: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy  HH:mm")))
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(9));

            if (filterInfo != null && !filterInfo.isBlank()) {
                document.add(new Paragraph("Filters: " + filterInfo)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(9));
            }

            document.add(new Paragraph(" "));

            // ── Summary Statistics ─────────────────────────────────────────
            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .useAllAvailableWidth();

            addStatCell(statsTable, "PRESENT",  String.valueOf(totalPresent),  "#2ecc71");
            addStatCell(statsTable, "LATE",     String.valueOf(totalLate),     "#f39c12");
            addStatCell(statsTable, "ABSENT",   String.valueOf(totalAbsent),   "#e74c3c");
            addStatCell(statsTable, "PENALTY",  "₱" + String.format("%.2f", totalPenalty), "#9b59b6");

            document.add(statsTable);
            document.add(new Paragraph(" "));

            // ── Attendance Table ───────────────────────────────────────────
            int colCount = table.getColumnCount();
            float[] colWidths = new float[colCount];
            for (int i = 0; i < colCount; i++) colWidths[i] = 1;

            Table dataTable = new Table(UnitValue.createPercentArray(colWidths))
                .useAllAvailableWidth();

            // Header row
            for (int col = 0; col < colCount; col++) {
                Cell headerCell = new Cell()
                    .add(new Paragraph(table.getColumnName(col)).setBold().setFontSize(8))
                    .setBackgroundColor(ColorConstants.DARK_GRAY)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER);
                dataTable.addHeaderCell(headerCell);
            }

            // Data rows
            for (int row = 0; row < table.getRowCount(); row++) {
                for (int col = 0; col < colCount; col++) {
                    Object value = table.getValueAt(row, col);
                    String text  = (value == null) ? "" : value.toString();

                    Cell dataCell = new Cell()
                        .add(new Paragraph(text).setFontSize(7))
                        .setTextAlignment(TextAlignment.CENTER);

                    // Color-code remarks column if present
                    if (table.getColumnName(col).equalsIgnoreCase("Remarks") ||
                        table.getColumnName(col).equalsIgnoreCase("Status")) {
                        if ("Late".equalsIgnoreCase(text)) {
                            dataCell.setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(
                                255, 243, 205)); // light yellow
                        } else if ("Absent".equalsIgnoreCase(text)) {
                            dataCell.setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(
                                255, 205, 210)); // light red
                        } else if ("Present".equalsIgnoreCase(text)) {
                            dataCell.setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(
                                200, 245, 218)); // light green
                        }
                    }

                    // Alternate row shading
                    if (row % 2 == 0) {
                        dataCell.setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(
                            245, 245, 245));
                    }

                    dataTable.addCell(dataCell);
                }
            }

            document.add(dataTable);
            document.close();

            JOptionPane.showMessageDialog(parent,
                "PDF exported successfully!\n" + filePath,
                "Export Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent,
                "PDF export failed:\n" + e.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static void addStatCell(Table table, String label, String value, String hexColor) {
        // Parse hex color
        int r = Integer.parseInt(hexColor.substring(1, 3), 16);
        int g = Integer.parseInt(hexColor.substring(3, 5), 16);
        int b = Integer.parseInt(hexColor.substring(5, 7), 16);
        com.itextpdf.kernel.colors.Color bg =
            new com.itextpdf.kernel.colors.DeviceRgb(r, g, b);

        Cell cell = new Cell()
            .add(new Paragraph(label).setFontSize(8).setFontColor(ColorConstants.WHITE).setBold())
            .add(new Paragraph(value).setFontSize(18).setFontColor(ColorConstants.WHITE).setBold())
            .setBackgroundColor(bg)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(8);
        table.addCell(cell);
    }
}
