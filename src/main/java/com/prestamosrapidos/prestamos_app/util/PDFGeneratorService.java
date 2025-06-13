package com.prestamosrapidos.prestamos_app.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PDFGeneratorService {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final BaseColor HEADER_BACKGROUND = new BaseColor(230, 230, 230);
    private static final BaseColor BORDER_COLOR = new BaseColor(180, 180, 180);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ByteArrayInputStream generateClientReport(Cliente cliente) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Estilos
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.DARK_GRAY);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);

            // Título
            Paragraph title = new Paragraph("Reporte Completo del Cliente", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(25);
            document.add(title);

            // Información del cliente
            addClientInfo(document, cliente, textFont);

            // Resumen general
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Resumen General", sectionTitleFont));
            document.add(new Paragraph("Número de cuentas: " + safeSize(cliente.getCuentas()), textFont));
            document.add(new Paragraph("Número de préstamos: " + safeSize(cliente.getPrestamos()), textFont));

            // Tabla de cuentas
            if (cliente.getCuentas() != null && !cliente.getCuentas().isEmpty()) {
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("Cuentas del Cliente", sectionTitleFont));
                PdfPTable accountTable = new PdfPTable(3);
                accountTable.setWidths(new int[]{4, 3, 3});
                setupTable(accountTable);
                addTableHeader(accountTable, "Número de Cuenta", "Saldo", "Cliente ID");
                cliente.getCuentas().forEach(cuenta -> {
                    accountTable.addCell(newCell(cuenta.getNumeroCuenta()));
                    accountTable.addCell(newCell("S/ " + DECIMAL_FORMAT.format(cuenta.getSaldo())));
                    accountTable.addCell(newCell(String.valueOf(cuenta.getCliente().getId())));
                });
                document.add(accountTable);
            }

            // Tabla de préstamos
            if (cliente.getPrestamos() != null && !cliente.getPrestamos().isEmpty()) {
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("Préstamos del Cliente", sectionTitleFont));
                PdfPTable loanTable = new PdfPTable(8);
                loanTable.setWidths(new float[]{2.5f, 2, 2, 3, 3, 2, 3, 3});
                setupTable(loanTable);
                addTableHeader(loanTable, "Monto", "Interés", "Interés Moratorio", "Fecha Creación",
                        "Fecha Vencimiento", "Estado", "Deuda Restante", "Saldo Moratorio");

                for (Prestamo prestamo : cliente.getPrestamos()) {
                    BigDecimal totalConInteres = prestamo.getMonto()
                            .multiply(BigDecimal.ONE.add(prestamo.getInteres().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)));
                    BigDecimal totalPagado = prestamo.getPagos().stream()
                            .map(Pago::getMonto)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal deudaRestante = totalConInteres.subtract(totalPagado);

                    loanTable.addCell(newCell("S/ " + DECIMAL_FORMAT.format(prestamo.getMonto())));
                    loanTable.addCell(newCell(prestamo.getInteres() + " %"));
                    loanTable.addCell(newCell(prestamo.getInteresMoratorio() + " %"));
                    loanTable.addCell(newCell(prestamo.getFechaCreacion() != null
                            ? prestamo.getFechaCreacion().toLocalDate().format(DATE_FORMAT) : "No disponible"));
                    loanTable.addCell(newCell(prestamo.getFechaVencimiento() != null
                            ? prestamo.getFechaVencimiento().format(DATE_FORMAT) : "No disponible"));
                    loanTable.addCell(newCell(prestamo.getEstado().name()));
                    loanTable.addCell(newCell("S/ " + DECIMAL_FORMAT.format(deudaRestante)));
                    loanTable.addCell(newCell(prestamo.getSaldoMoratorio() != null
                            ? "S/ " + DECIMAL_FORMAT.format(prestamo.getSaldoMoratorio()) : "N/A"));
                }
                document.add(loanTable);
            }

            // Tabla de pagos
            if (cliente.getPrestamos() != null && !cliente.getPrestamos().isEmpty()) {
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("Pagos Realizados", sectionTitleFont));
                PdfPTable paymentTable = new PdfPTable(3);
                paymentTable.setWidths(new float[]{3, 3, 2});
                setupTable(paymentTable);
                addTableHeader(paymentTable, "Monto del Pago", "Fecha", "Préstamo ID");

                for (Prestamo prestamo : cliente.getPrestamos()) {
                    if (prestamo.getPagos() != null) {
                        for (Pago pago : prestamo.getPagos()) {
                            paymentTable.addCell(newCell("S/ " + DECIMAL_FORMAT.format(pago.getMonto())));
                            paymentTable.addCell(newCell(pago.getFecha() != null ? pago.getFecha().format(DATE_FORMAT) : "No disponible"));
                            paymentTable.addCell(newCell(String.valueOf(prestamo.getId())));
                        }
                    }
                }
                document.add(paymentTable);
            }

            // Total restante
            BigDecimal totalConInteres = cliente.getPrestamos().stream()
                    .map(p -> p.getMonto().multiply(BigDecimal.ONE.add(p.getInteres().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPagado = cliente.getPrestamos().stream()
                    .flatMap(p -> p.getPagos().stream())
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalRestante = totalConInteres.subtract(totalPagado);

            document.add(Chunk.NEWLINE);
            Paragraph totalRestanteParagraph = new Paragraph("Total Restante de los Préstamos: S/ " + DECIMAL_FORMAT.format(totalRestante), sectionTitleFont);
            totalRestanteParagraph.setSpacingBefore(15);
            document.add(totalRestanteParagraph);

            document.close();
        } catch (Exception ex) {
            log.error("Error al generar el PDF", ex);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addClientInfo(Document doc, Cliente cliente, Font font) throws DocumentException {
        Paragraph info = new Paragraph();
        info.setSpacingAfter(10);
        info.setFont(font);
        info.add("Nombre: " + cliente.getNombre() + "\n");
        info.add("Correo: " + cliente.getCorreo() + "\n");
        doc.add(info);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(HEADER_BACKGROUND);
            cell.setBorderColor(BORDER_COLOR);
            cell.setPadding(6f);
            table.addCell(cell);
        }
    }

    private void setupTable(PdfPTable table) {
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);
    }

    private PdfPCell newCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 11)));
        cell.setPadding(5f);
        cell.setBorderColor(BORDER_COLOR);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private int safeSize(java.util.List<?> list) {
        return list != null ? list.size() : 0;
    }
}
