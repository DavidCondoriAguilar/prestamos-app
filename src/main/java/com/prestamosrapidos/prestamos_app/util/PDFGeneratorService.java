package com.prestamosrapidos.prestamos_app.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import com.prestamosrapidos.prestamos_app.entity.enums.EstadoPrestamo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PDFGeneratorService {

    // Colors
    protected static final BaseColor PRIMARY_COLOR = new BaseColor(51, 122, 183);
    protected static final BaseColor SECONDARY_COLOR = new BaseColor(92, 184, 92);
    protected static final BaseColor LIGHT_GRAY = new BaseColor(248, 249, 250);
    protected static final BaseColor BORDER_COLOR = new BaseColor(206, 212, 218);
    protected static final BaseColor TEXT_COLOR = new BaseColor(33, 37, 41);
    
    // Formatting
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    // Fonts
    private Font titleFont;
    private Font subtitleFont;
    private Font headerFont;
    private Font normalFont;
    private Font smallFont;
    private Font boldFont;
    
    // Company Info
    private static final String COMPANY_NAME = "Préstamos Rápidos S.A.";
    private static final String COMPANY_ADDRESS = "Av. Principal 777, Lima, Perú";
    private static final String COMPANY_PHONE = "+51 928 193 119";
    private static final String COMPANY_EMAIL = "info@prestamosrapidos.com";

    public ByteArrayInputStream generateClientReport(Cliente cliente) {
        Document document = new Document(PageSize.A4, 36, 36, 72, 72); // Larger margins for header/footer
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            // Initialize fonts
            initFonts();
            
            // Create PDF writer with page events for header/footer
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PdfPageEventHandler());
            
            document.open();
            
            // Add watermark
            addWatermark(writer);
            
            // Add header
            addDocumentHeader(document);
            
            // Add client info section
            addClientInfoSection(document, cliente);
            
            // Add summary cards
            addSummaryCards(document, cliente);
            
            // Add accounts table if available
            if (cliente.getCuentas() != null && !cliente.getCuentas().isEmpty()) {
                addSectionTitle(document, "Cuentas Bancarias");
                addAccountsTable(document, cliente);
            }
            
            // Add loans table if available
            if (cliente.getPrestamos() != null && !cliente.getPrestamos().isEmpty()) {
                addSectionTitle(document, "Préstamos");
                addLoansTable(document, cliente);
                
                // Add payments table if available
                boolean hasPayments = cliente.getPrestamos().stream()
                    .anyMatch(p -> p.getPagos() != null && !p.getPagos().isEmpty());
                    
                if (hasPayments) {
                    addSectionTitle(document, "Historial de Pagos");
                    addPaymentsTable(document, cliente);
                }
                
                // Add totals summary
                addTotalsSummary(document, cliente);
            }
            
            // Add footer
            addDocumentFooter(document);
            
            document.close();
        } catch (Exception ex) {
            log.error("Error al generar el PDF", ex);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
    
    private void initFonts() throws DocumentException, IOException {
        // Register a custom font if needed, or use standard fonts
        try {
            // Try to load a custom font (optional)
            // BaseFont baseFont = BaseFont.createFont("fonts/arial.ttf", BaseFont.CP1252, BaseFont.EMBEDDED);
            // titleFont = new Font(baseFont, 22, Font.BOLD, PRIMARY_COLOR);
            
            // Fallback to standard fonts
            titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, PRIMARY_COLOR);
            subtitleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, TEXT_COLOR);
            headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
            normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_COLOR);
            smallFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.DARK_GRAY);
            boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, TEXT_COLOR);
        } catch (Exception e) {
            log.warn("Error loading custom fonts, using standard fonts", e);
            // Fallback to standard fonts
            titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, PRIMARY_COLOR);
            subtitleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, TEXT_COLOR);
            headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
            normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_COLOR);
            smallFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.DARK_GRAY);
            boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, TEXT_COLOR);
        }
    }
    
    private void addDocumentHeader(Document document) throws DocumentException {
        // Add company logo (if available)
        try {
            // Uncomment and set path to your logo
            // Image logo = Image.getInstance("path/to/logo.png");
            // logo.scaleToFit(150, 60);
            // logo.setAlignment(Element.ALIGN_CENTER);
            // document.add(logo);
            // document.add(Chunk.NEWLINE);
        } catch (Exception e) {
            log.debug("Logo not found, continuing without it");
        }
        
        // Add company info
        Paragraph companyInfo = new Paragraph(COMPANY_NAME, new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, PRIMARY_COLOR));
        companyInfo.setAlignment(Element.ALIGN_CENTER);
        companyInfo.setSpacingAfter(5);
        document.add(companyInfo);
        
        Paragraph address = new Paragraph(COMPANY_ADDRESS, smallFont);
        address.setAlignment(Element.ALIGN_CENTER);
        document.add(address);
        
        Paragraph contact = new Paragraph("Tel: " + COMPANY_PHONE + " | Email: " + COMPANY_EMAIL, smallFont);
        contact.setAlignment(Element.ALIGN_CENTER);
        contact.setSpacingAfter(15);
        document.add(contact);
        
        // Add report title
        Paragraph title = new Paragraph("REPORTE DE CLIENTE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        document.add(title);
        
        // Add report date
        Paragraph date = new Paragraph("Generado el: " + LocalDateTime.now().format(DATE_TIME_FORMAT), smallFont);
        date.setAlignment(Element.ALIGN_CENTER);
        date.setSpacingAfter(20);
        document.add(date);
    }

    private void addClientInfoSection(Document document, Cliente cliente) throws DocumentException {
        // Add section title
        addSectionTitle(document, "Información del Cliente");
        
        // Create a table for client info
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(20);
        
        // Set column widths
        float[] columnWidths = {2, 5};
        table.setWidths(columnWidths);
        
        // Add client info rows
        addInfoRow(table, "ID del Cliente:", String.valueOf(cliente.getId()));
        addInfoRow(table, "Nombre:", cliente.getNombre());
        addInfoRow(table, "Correo Electrónico:", cliente.getCorreo());
        addInfoRow(table, "Número de Cuentas:", String.valueOf(safeSize(cliente.getCuentas())));

        addInfoRow(table, "Número de Préstamos:", String.valueOf(safeSize(cliente.getPrestamos())));
        
        // Add the table to document
        document.add(table);
    }
    
    private void addSummaryCards(Document document, Cliente cliente) throws DocumentException {
        // Calculate totals
        int totalLoans = safeSize(cliente.getPrestamos());
        int totalAccounts = safeSize(cliente.getCuentas());
        
        BigDecimal totalBorrowed = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;
        
        if (cliente.getPrestamos() != null) {
            for (Prestamo prestamo : cliente.getPrestamos()) {
                BigDecimal loanAmount = prestamo.getMonto();
                BigDecimal interest = prestamo.getMonto()
                    .multiply(prestamo.getInteres())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    
                totalBorrowed = totalBorrowed.add(loanAmount);
                
                BigDecimal paid = prestamo.getPagos().stream()
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                totalPaid = totalPaid.add(paid);
                totalRemaining = totalRemaining.add(loanAmount.add(interest).subtract(paid));
            }
        }
        
        // Create a table for the summary cards
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(20);
        
        // Add cards
        addSummaryCard(table, "Total Préstamos", String.valueOf(totalLoans), "clipboard");
        addSummaryCard(table, "Total Cuentas", String.valueOf(totalAccounts), "credit-card");
        addSummaryCard(table, "Total Pagado", "S/ " + DECIMAL_FORMAT.format(totalPaid), "check-circle");
        addSummaryCard(table, "Total Pendiente", "S/ " + DECIMAL_FORMAT.format(totalRemaining), "dollar");
        
        document.add(table);
    }
    
    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, boldFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(8);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, normalFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(8);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    private void addSummaryCard(PdfPTable table, String title, String value, String icon) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(1);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setPadding(10);
        
        // Add icon (as text for now, could be replaced with actual icons)
        Phrase iconPhrase = new Phrase("• ", new Font(Font.FontFamily.SYMBOL, 14, Font.BOLD, PRIMARY_COLOR));
        
        // Add title
        Phrase titlePhrase = new Phrase(title + "\n", smallFont);
        titlePhrase.setFont(new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.DARK_GRAY));
        
        // Add value
        Phrase valuePhrase = new Phrase(value, new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR));
        
        // Combine all elements
        Paragraph content = new Paragraph();
        content.add(iconPhrase);
        content.add(titlePhrase);
        content.add(valuePhrase);
        
        cell.addElement(content);
        table.addCell(cell);
    }
    
    private void addSectionTitle(Document document, String title) throws DocumentException {
        // Create a table with a single cell for the section title with bottom border
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(20);
        table.setSpacingAfter(10);
        
        // Add title cell with bottom border
        PdfPCell cell = new PdfPCell(new Phrase(title, subtitleFont));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderWidthBottom(1f);
        cell.setBorderColorBottom(PRIMARY_COLOR);
        cell.setPaddingBottom(5);
        table.addCell(cell);
        
        document.add(table);
    }

    private void addAccountsTable(Document document, Cliente cliente) throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(20);
        
        // Table header
        addTableHeader(table, new String[]{"Número de Cuenta", "Saldo Actual", "Cliente ID"});
        
        // Table rows
        cliente.getCuentas().forEach(cuenta -> {
            table.addCell(createCell(cuenta.getNumeroCuenta(), Element.ALIGN_LEFT));
            table.addCell(createCell("S/ " + DECIMAL_FORMAT.format(cuenta.getSaldo()), Element.ALIGN_RIGHT));
            table.addCell(createCell(String.valueOf(cuenta.getCliente().getId()), Element.ALIGN_CENTER));
        });
        
        document.add(table);
    }
    
    private void addLoansTable(Document document, Cliente cliente) throws DocumentException {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(20);
        
        // Set column widths
        float[] columnWidths = {2, 1.5f, 1.5f, 2, 2, 2, 2};
        table.setWidths(columnWidths);
        
        // Table header
        addTableHeader(table, new String[]{"ID Préstamo", "Monto", "Interés", "Fecha Vencimiento", 
                      "Estado", "Deuda Restante", "Mora"});
        
        // Table rows
        for (Prestamo prestamo : cliente.getPrestamos()) {
            BigDecimal totalConInteres = prestamo.getMonto()
                .multiply(BigDecimal.ONE.add(prestamo.getInteres().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)));
                
            BigDecimal totalPagado = prestamo.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            BigDecimal deudaRestante = totalConInteres.subtract(totalPagado);
            
            table.addCell(createCell(String.valueOf(prestamo.getId()), Element.ALIGN_CENTER));
            table.addCell(createCell("S/ " + DECIMAL_FORMAT.format(prestamo.getMonto()), Element.ALIGN_RIGHT));
            table.addCell(createCell(prestamo.getInteres() + "%", Element.ALIGN_CENTER));
            table.addCell(createCell(
                prestamo.getFechaVencimiento() != null ? 
                prestamo.getFechaVencimiento().format(DATE_FORMAT) : "N/A", 
                Element.ALIGN_CENTER));
                
            // Status with color coding
            PdfPCell statusCell = createCell(prestamo.getEstado().name(), Element.ALIGN_CENTER);
            if (prestamo.getEstado() == EstadoPrestamo.EN_MORA) {
                statusCell.setBackgroundColor(new BaseColor(248, 215, 218)); // Light red for overdue
            } else if (prestamo.getEstado() == EstadoPrestamo.PAGADO) {
                statusCell.setBackgroundColor(new BaseColor(212, 237, 218)); // Light green for paid
            } else {
                statusCell.setBackgroundColor(LIGHT_GRAY);
            }
            table.addCell(statusCell);
            
            table.addCell(createCell("S/ " + DECIMAL_FORMAT.format(deudaRestante), Element.ALIGN_RIGHT));
            
            // Mora amount with conditional formatting
            BigDecimal mora = prestamo.getMoraAcumulada() != null ? 
                prestamo.getMoraAcumulada() : BigDecimal.ZERO;
                
            PdfPCell moraCell = createCell(
                mora.compareTo(BigDecimal.ZERO) > 0 ? 
                "S/ " + DECIMAL_FORMAT.format(mora) : "-", 
                Element.ALIGN_RIGHT
            );
            
            if (mora.compareTo(BigDecimal.ZERO) > 0) {
                moraCell.setBackgroundColor(new BaseColor(255, 243, 205)); // Light yellow for mora
            }
            
            table.addCell(moraCell);
        }
        
        document.add(table);
    }
    
    private void addPaymentsTable(Document document, Cliente cliente) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(20);
        
        // Table header
        addTableHeader(table, new String[]{"ID Préstamo", "Monto del Pago", "Fecha de Pago", "Comentario"});
        
        // Table rows
        for (Prestamo prestamo : cliente.getPrestamos()) {
            if (prestamo.getPagos() != null && !prestamo.getPagos().isEmpty()) {
                for (Pago pago : prestamo.getPagos()) {
                    table.addCell(createCell(String.valueOf(prestamo.getId()), Element.ALIGN_CENTER));
                    table.addCell(createCell("S/ " + DECIMAL_FORMAT.format(pago.getMonto()), Element.ALIGN_RIGHT));
                    table.addCell(createCell(
                        pago.getFecha() != null ? 
                        pago.getFecha().format(DATE_FORMAT) : "N/A", 
                        Element.ALIGN_CENTER
                    ));
                    // Add empty cell for comments (can be extended if needed)
                    table.addCell(createCell("-", Element.ALIGN_CENTER));
                }
            }
        }
        
        document.add(table);
    }
    
    private void addTotalsSummary(Document document, Cliente cliente) throws DocumentException {
        BigDecimal totalBorrowed = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalMora = BigDecimal.ZERO;
        
        for (Prestamo prestamo : cliente.getPrestamos()) {
            totalBorrowed = totalBorrowed.add(prestamo.getMonto());
            
            BigDecimal interest = prestamo.getMonto()
                .multiply(prestamo.getInteres())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                
            totalInterest = totalInterest.add(interest);
            
            BigDecimal paid = prestamo.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            totalPaid = totalPaid.add(paid);
            
            if (prestamo.getMoraAcumulada() != null) {
                totalMora = totalMora.add(prestamo.getMoraAcumulada());
            }
        }
        
        BigDecimal totalOwed = totalBorrowed.add(totalInterest).add(totalMora).subtract(totalPaid);
        
        // Create a table for the totals
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingBefore(20);
        
        // Add rows
        addTotalRow(table, "Total Prestado:", "S/ " + DECIMAL_FORMAT.format(totalBorrowed));
        addTotalRow(table, "Total Interés:", "S/ " + DECIMAL_FORMAT.format(totalInterest));
        addTotalRow(table, "Total Mora:", "S/ " + DECIMAL_FORMAT.format(totalMora));
        addTotalRow(table, "Total Pagado:", "S/ " + DECIMAL_FORMAT.format(totalPaid));
        
        // Add total row with different styling
        PdfPCell labelCell = new PdfPCell(new Phrase("TOTAL PENDIENTE:", 
            new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBackgroundColor(PRIMARY_COLOR);
        labelCell.setPadding(8);
        labelCell.setBorderWidthRight(0);
        
        PdfPCell valueCell = new PdfPCell(new Phrase("S/ " + DECIMAL_FORMAT.format(totalOwed),
            new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setBackgroundColor(PRIMARY_COLOR);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(8);
        valueCell.setBorderWidthLeft(0);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
        
        document.add(table);
    }
    
    private void addTotalRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, boldFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, normalFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    /**
     * Safely gets the size of a collection, returning 0 if the collection is null
     */
    private int safeSize(java.util.Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    /**
     * Helper method to create a table header row
     */
    private void addTableHeader(PdfPTable table, String[] headers) {
        for (String header : headers) {
            PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
            headerCell.setBackgroundColor(PRIMARY_COLOR);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setPadding(8);
            table.addCell(headerCell);
        }
    }
    
    /**
     * Helper method to create a styled cell
     */
    private PdfPCell createCell(String content, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(content, normalFont));
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(BORDER_COLOR);
        return cell;
    }
    
    private void addDocumentFooter(Document document) throws DocumentException {
    // Add some space
    document.add(Chunk.NEWLINE);
    
    // Add a thank you message
    Paragraph thanks = new Paragraph("Gracias por confiar en " + COMPANY_NAME, 
        new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.DARK_GRAY));
    thanks.setAlignment(Element.ALIGN_CENTER);
    thanks.setSpacingAfter(10);
    document.add(thanks);
    
    // Add contact information
    Paragraph contact = new Paragraph(
        "Para más información, contáctenos al " + COMPANY_PHONE + " o " + COMPANY_EMAIL,
        smallFont);
    contact.setAlignment(Element.ALIGN_CENTER);
    document.add(contact);
}

private void addWatermark(PdfWriter writer) {
    // Add a watermark to each page
    PdfContentByte canvas = writer.getDirectContentUnder();
    BaseFont baseFont;
    try {
        baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        
        // Set watermark properties
        canvas.beginText();
        canvas.setColorFill(BaseColor.LIGHT_GRAY);
        canvas.setFontAndSize(baseFont, 60);
        
        // Get the page size
        Rectangle pageSize = writer.getPageSize();
        float x = (pageSize.getLeft() + pageSize.getRight()) / 2;
        float y = (pageSize.getTop() + pageSize.getBottom()) / 2;
        
        // Add text at a 45 degree angle
        canvas.showTextAligned(Element.ALIGN_CENTER, COMPANY_NAME, x, y, 45);
        canvas.endText();
    } catch (Exception e) {
        log.debug("Could not add watermark", e);
    }
}

/**
 * Helper class for handling page events (header, footer, etc.)
 */
private static class PdfPageEventHandler extends PdfPageEventHelper {
    private static final float FOOTER_FONT_SIZE = 8f;
    private static final float FOOTER_MARGIN_BOTTOM = 20f;
    private static final float LINE_MARGIN_TOP = 15f;
    private static final float LINE_WIDTH = 0.5f;
    
    private final Font footerFont;
    private final BaseColor lineColor;
    
    public PdfPageEventHandler() {
        this.footerFont = new Font(Font.FontFamily.HELVETICA, FOOTER_FONT_SIZE, 
                                Font.NORMAL, BaseColor.DARK_GRAY);
        this.lineColor = BaseColor.LIGHT_GRAY;
    }
    
    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        if (writer == null || document == null) {
            log.warn("Writer or Document is null in onEndPage");
            return;
        }

        try {
            addPageNumber(writer, document);
            addFooterLine(writer, document);
        } catch (Exception e) {
            log.error("Error adding footer to page {}", writer.getPageNumber(), e);
        }
    }
    
    private void addPageNumber(PdfWriter writer, Document document) throws DocumentException {
        PdfContentByte cb = writer.getDirectContent();
        String pageText = String.format("Página %d de", writer.getPageNumber());
        Phrase footer = new Phrase(pageText, footerFont);
        
        float x = (document.right() - document.left()) / 2 + document.leftMargin();
        float y = document.bottom() - FOOTER_MARGIN_BOTTOM;
        
        ColumnText.showTextAligned(
            cb, Element.ALIGN_CENTER, footer, x, y, 0
        );
    }
    
    private void addFooterLine(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();
        cb.saveState();
        try {
            cb.setLineWidth(LINE_WIDTH);
            cb.setColorStroke(lineColor);
            float y = document.bottom() - LINE_MARGIN_TOP;
            cb.moveTo(document.left(), y);
            cb.lineTo(document.right(), y);
            cb.stroke();
        } finally {
            cb.restoreState();
        }
    }
    
    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        try {
            // Clean up resources if needed
            if (writer != null) {
                writer.flush();
            }
        } catch (Exception e) {
            log.error("Error closing document", e);
        }
    }
}
}
