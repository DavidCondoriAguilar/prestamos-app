package com.prestamosrapidos.prestamos_app.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Cuenta;
import com.prestamosrapidos.prestamos_app.entity.Pago;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PDFGeneratorService {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");

    public ByteArrayInputStream generateClientReport(Cliente cliente) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Agregar imagen al PDF
            String imagePath = "src/main/resources/images/image.jpg";
            Image image = Image.getInstance(imagePath);
            image.setAlignment(Element.ALIGN_CENTER);
            image.scaleToFit(200, 200);
            document.add(image);

            // Agregar título
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Reporte Completo del Cliente", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Información del cliente
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Paragraph clientInfo = new Paragraph("Nombre: " + cliente.getNombre(), normalFont);
            clientInfo.setSpacingAfter(10);
            document.add(clientInfo);
            document.add(new Paragraph("Correo: " + cliente.getCorreo(), normalFont));

            // Resumen general
            document.add(Chunk.NEWLINE);
            Paragraph summaryTitle = new Paragraph("Resumen General", boldFont);
            summaryTitle.setSpacingAfter(10);
            document.add(summaryTitle);
            document.add(new Paragraph("Número de cuentas: " + cliente.getCuentas().size(), normalFont));
            document.add(new Paragraph("Número de préstamos: " + cliente.getPrestamos().size(), normalFont));

            // Tabla de cuentas
            if (cliente.getCuentas() != null && !cliente.getCuentas().isEmpty()) {
                document.add(Chunk.NEWLINE);
                Paragraph accountTitle = new Paragraph("Cuentas del Cliente", boldFont);
                accountTitle.setSpacingAfter(10);
                document.add(accountTitle);

                PdfPTable accountTable = new PdfPTable(3);
                accountTable.setWidthPercentage(100);
                addTableHeader(accountTable, "Número de Cuenta", "Saldo", "Cliente ID");

                for (Cuenta cuenta : cliente.getCuentas()) {
                    accountTable.addCell(cuenta.getNumeroCuenta());
                    accountTable.addCell("S/ " + DECIMAL_FORMAT.format(cuenta.getSaldo()));
                    accountTable.addCell(String.valueOf(cuenta.getCliente().getId()));
                }

                document.add(accountTable);
            }

            // Tabla de préstamos
            if (cliente.getPrestamos() != null && !cliente.getPrestamos().isEmpty()) {
                document.add(Chunk.NEWLINE);
                Paragraph loanTitle = new Paragraph("Préstamos del Cliente", boldFont);
                loanTitle.setSpacingAfter(10);
                document.add(loanTitle);

                PdfPTable loanTable = new PdfPTable(4);
                loanTable.setWidthPercentage(100);
                addTableHeader(loanTable, "Monto", "Interés", "Fecha de Creación", "Estado");

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                for (Prestamo prestamo : cliente.getPrestamos()) {
                    loanTable.addCell("S/ " + DECIMAL_FORMAT.format(prestamo.getMonto()));
                    loanTable.addCell(prestamo.getInteres() + " %");
                    loanTable.addCell(prestamo.getFechaCreacion() != null
                            ? prestamo.getFechaCreacion().format(dateFormatter)
                            : "Fecha no disponible");
                    loanTable.addCell(String.valueOf(prestamo.getEstado()));
                }

                document.add(loanTable);
            }

            // Tabla de pagos
            if (cliente.getPrestamos() != null && !cliente.getPrestamos().isEmpty()) {
                document.add(Chunk.NEWLINE);
                Paragraph paymentTitle = new Paragraph("Pagos Realizados", boldFont);
                paymentTitle.setSpacingAfter(10);
                document.add(paymentTitle);

                PdfPTable paymentTable = new PdfPTable(3);
                paymentTable.setWidthPercentage(100);
                addTableHeader(paymentTable, "Monto del Pago", "Fecha", "Préstamo ID");

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                for (Prestamo prestamo : cliente.getPrestamos()) {
                    for (Pago pago : prestamo.getPagos()) {
                        paymentTable.addCell("S/ " + DECIMAL_FORMAT.format(pago.getMonto()));
                        paymentTable.addCell(pago.getFecha() != null
                                ? pago.getFecha().format(dateFormatter)
                                : "Fecha no disponible");
                        paymentTable.addCell(String.valueOf(pago.getPrestamo().getId()));
                    }
                }

                document.add(paymentTable);
            }

            // Cálculo del total restante
            if (cliente.getPrestamos() != null && !cliente.getPrestamos().isEmpty()) {
                BigDecimal totalPrestamos = cliente.getPrestamos().stream()
                        .map(Prestamo::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        
                BigDecimal totalPagos = cliente.getPrestamos().stream()
                        .flatMap(prestamo -> prestamo.getPagos().stream())
                        .map(Pago::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        
                BigDecimal totalRestante = totalPrestamos.subtract(totalPagos);

                document.add(Chunk.NEWLINE);
                Paragraph totalRestanteParagraph = new Paragraph(
                        "Total Restante de los Préstamos: S/ " + DECIMAL_FORMAT.format(totalRestante), boldFont);
                totalRestanteParagraph.setSpacingBefore(15);
                document.add(totalRestanteParagraph);
            }

            document.close();
        } catch (Exception ex) {
            log.error("Error al generar el PDF", ex);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
        PdfPCell cell;
        for (String header : headers) {
            cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(BaseColor.DARK_GRAY);
            table.addCell(cell);
        }
    }
}
