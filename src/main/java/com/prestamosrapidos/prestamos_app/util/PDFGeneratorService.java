package com.prestamosrapidos.prestamos_app.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Cuenta;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
            Paragraph title = new Paragraph("Reporte del Cliente", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20); // Espaciado después del título
            document.add(title);

            // Información del cliente
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Paragraph clientInfo = new Paragraph("Nombre: " + cliente.getNombre(), normalFont);
            clientInfo.setSpacingAfter(10); // Espaciado después de la información del cliente
            document.add(clientInfo);
            document.add(new Paragraph("Correo: " + cliente.getCorreo(), normalFont));

            // Espaciado
            document.add(Chunk.NEWLINE);

            // Tabla de cuentas
            if (cliente.getCuentas() != null && !cliente.getCuentas().isEmpty()) {
                // Subtítulo para las cuentas
                Font accountTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLUE);
                Paragraph accountTitle = new Paragraph("Cuentas:", accountTitleFont);
                accountTitle.setSpacingAfter(10);
                document.add(accountTitle);

                PdfPTable accountTable = new PdfPTable(3);
                accountTable.setWidthPercentage(100);

                // Encabezados
                addTableHeader(accountTable, "Número de Cuenta", "Saldo", "Cliente ID");

                // Filas de datos
                for (Cuenta cuenta : cliente.getCuentas()) {
                    accountTable.addCell(cuenta.getNumeroCuenta());
                    accountTable.addCell("S/ " + DECIMAL_FORMAT.format(cuenta.getSaldo()));
                    accountTable.addCell(String.valueOf(cuenta.getCliente().getId()));
                }

                document.add(accountTable);
                document.add(Chunk.NEWLINE);
            }

            // Tabla de préstamos
            if (cliente.getPrestamos() != null && !cliente.getPrestamos().isEmpty()) {
                // Subtítulo para los préstamos
                Font loanTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.RED);
                Paragraph loanTitle = new Paragraph("Préstamos:", loanTitleFont);
                loanTitle.setSpacingAfter(10);
                document.add(loanTitle);

                PdfPTable loanTable = new PdfPTable(4); // 4 columnas
                loanTable.setWidthPercentage(100);

                // Encabezados
                addTableHeader(loanTable, "Monto", "Interés", "Fecha de Creación", "Estado");

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                for (Prestamo prestamo : cliente.getPrestamos()) {
                    loanTable.addCell("S/ " + DECIMAL_FORMAT.format(prestamo.getMonto()));
                    loanTable.addCell(prestamo.getInteres() + " %");

                    // Formatear fecha usando DateTimeFormatter
                    if (prestamo.getFechaCreacion() != null) {
                        loanTable.addCell(prestamo.getFechaCreacion().format(dateFormatter));
                    } else {
                        loanTable.addCell("Fecha no disponible");
                    }

                    loanTable.addCell(String.valueOf(prestamo.getEstado()));
                }

                document.add(loanTable);
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
