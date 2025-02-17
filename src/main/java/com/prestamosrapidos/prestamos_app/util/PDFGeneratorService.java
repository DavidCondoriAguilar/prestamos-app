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

    public ByteArrayInputStream generateClientReport(Cliente cliente) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Agregar imagen
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

            if (cliente.getCuentas() != null && !cliente.getCuentas().isEmpty()) {
                document.add(new Paragraph("Número de cuentas: " + cliente.getCuentas().size(), normalFont));
            }
            if (cliente.getPrestamos() != null) {
                document.add(new Paragraph("Número de préstamos: " + cliente.getPrestamos().size(), normalFont));
            }

            // Tabla de Cuentas
            if (cliente.getCuentas() != null && !cliente.getCuentas().isEmpty()) {
                document.add(Chunk.NEWLINE);
                Paragraph accountTitle = new Paragraph("Cuentas del Cliente", boldFont);
                accountTitle.setSpacingAfter(10);
                document.add(accountTitle);

                PdfPTable accountTable = new PdfPTable(3);
                accountTable.setWidthPercentage(100);
                addTableHeader(accountTable, "Número de Cuenta", "Saldo", "Cliente ID");

                cliente.getCuentas().forEach(cuenta -> {
                    accountTable.addCell(cuenta.getNumeroCuenta());
                    accountTable.addCell("S/ " + DECIMAL_FORMAT.format(cuenta.getSaldo()));
                    accountTable.addCell(String.valueOf(cuenta.getCliente().getId()));
                });
                document.add(accountTable);
            }

            // Tabla de Préstamos
            if (cliente.getPrestamos() != null && !cliente.getPrestamos().isEmpty()) {
                document.add(Chunk.NEWLINE);
                Paragraph loanTitle = new Paragraph("Préstamos del Cliente", boldFont);
                loanTitle.setSpacingAfter(10);
                document.add(loanTitle);

                // Se muestran 8 columnas: Monto, Interés, Interés Moratorio, Fecha Creación, Fecha Vencimiento, Estado, Deuda Restante, Saldo Moratorio
                PdfPTable loanTable = new PdfPTable(8);
                loanTable.setWidthPercentage(100);
                addTableHeader(loanTable, "Monto", "Interés", "Interés Moratorio", "Fecha Creación",
                        "Fecha Vencimiento", "Estado", "Deuda Restante", "Saldo Moratorio");

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                for (Prestamo prestamo : cliente.getPrestamos()) {
                    // Monto y porcentajes
                    loanTable.addCell("S/ " + DECIMAL_FORMAT.format(prestamo.getMonto()));
                    loanTable.addCell(prestamo.getInteres() + " %");
                    loanTable.addCell(prestamo.getInteresMoratorio() + " %");

                    // Fecha de creación (convertimos LocalDateTime a LocalDate)
                    String fechaCreacionStr = (prestamo.getFechaCreacion() != null)
                            ? prestamo.getFechaCreacion().toLocalDate().format(dtf)
                            : "No disponible";
                    loanTable.addCell(fechaCreacionStr);

                    // Fecha de vencimiento
                    String fechaVencimientoStr = (prestamo.getFechaVencimiento() != null)
                            ? prestamo.getFechaVencimiento().format(dtf)
                            : "No disponible";
                    loanTable.addCell(fechaVencimientoStr);

                    // Estado (asumimos que es un enum)
                    loanTable.addCell(prestamo.getEstado().name());

                    // Calcular deuda restante: (monto + (monto * (interés / 100))) - totalPagado
                    BigDecimal totalConInteres = prestamo.getMonto()
                            .multiply(BigDecimal.ONE.add(prestamo.getInteres().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)));
                    BigDecimal totalPagado = prestamo.getPagos().stream()
                            .map(Pago::getMonto)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal deudaRestante = totalConInteres.subtract(totalPagado);
                    loanTable.addCell("S/ " + DECIMAL_FORMAT.format(deudaRestante));

                    // Saldo moratorio: verificación para evitar null
                    if (prestamo.getSaldoMoratorio() != null) {
                        loanTable.addCell("S/ " + DECIMAL_FORMAT.format(prestamo.getSaldoMoratorio()));
                    } else {
                        loanTable.addCell("N/A");
                    }
                }
                document.add(loanTable);
            }


            // Tabla de Pagos
            if (cliente.getPrestamos() != null && !cliente.getPrestamos().isEmpty()) {
                document.add(Chunk.NEWLINE);
                Paragraph paymentTitle = new Paragraph("Pagos Realizados", boldFont);
                paymentTitle.setSpacingAfter(10);
                document.add(paymentTitle);

                PdfPTable paymentTable = new PdfPTable(3);
                paymentTable.setWidthPercentage(100);
                addTableHeader(paymentTable, "Monto del Pago", "Fecha", "Préstamo ID");

                DateTimeFormatter dtfPago = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                for (Prestamo prestamo : cliente.getPrestamos()) {
                    for (Pago pago : prestamo.getPagos()) {
                        paymentTable.addCell("S/ " + DECIMAL_FORMAT.format(pago.getMonto()));
                        String fechaPagoStr = (pago.getFecha() != null)
                                ? pago.getFecha().format(dtfPago)
                                : "No disponible";
                        paymentTable.addCell(fechaPagoStr);
                        paymentTable.addCell(String.valueOf(prestamo.getId()));
                    }
                }
                document.add(paymentTable);
            }

            // Cálculo total de la deuda de todos los préstamos
            if (cliente.getPrestamos() != null && !cliente.getPrestamos().isEmpty()) {
                BigDecimal totalConInteres = cliente.getPrestamos().stream()
                        .map(prestamo -> prestamo.getMonto()
                                .multiply(BigDecimal.ONE.add(prestamo.getInteres().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalPagado = cliente.getPrestamos().stream()
                        .flatMap(prestamo -> prestamo.getPagos().stream())
                        .map(Pago::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalRestante = totalConInteres.subtract(totalPagado);

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
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(BaseColor.DARK_GRAY);
            table.addCell(cell);
        }
    }
}
