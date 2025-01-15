package com.prestamosrapidos.prestamos_app.util;

import com.prestamosrapidos.prestamos_app.entity.Cliente;
import com.prestamosrapidos.prestamos_app.entity.Cuenta;
import com.prestamosrapidos.prestamos_app.entity.Prestamo;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
@Service
@Slf4j
public class EXCELGeneratorService {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");

    // Método para generar el reporte en Excel
    public ByteArrayInputStream generateClientExcelReport(Cliente cliente) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte del Cliente");

            // Crear estilo para encabezados
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Título
            createTitle(sheet, headerStyle);

            // Información del cliente
            createClientInfo(sheet, cliente);

            // Cuentas
            int rowIndex = createAccountsTable(sheet, cliente, headerStyle);

            // Préstamos
            createLoansTable(sheet, cliente, headerStyle, rowIndex);

            // Ajustar tamaño de columnas
            adjustColumnSize(sheet);

            // Guardar el archivo en un ByteArrayInputStream
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return new ByteArrayInputStream(out.toByteArray());
            }

        } catch (Exception e) {
            log.error("Error al generar el reporte Excel", e);
            return null;
        }
    }

    // Método para crear el estilo de los encabezados
    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        return headerStyle;
    }

    // Método para crear la fila de título
    private void createTitle(Sheet sheet, CellStyle headerStyle) {
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Reporte del Cliente");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3)); // Fusiona celdas
    }

    // Método para crear la información del cliente
    private void createClientInfo(Sheet sheet, Cliente cliente) {
        Row infoRow = sheet.createRow(1);
        infoRow.createCell(0).setCellValue("Nombre: " + cliente.getNombre());
        infoRow.createCell(1).setCellValue("Correo: " + cliente.getCorreo());
    }

    // Método para crear la tabla de cuentas
    private int createAccountsTable(Sheet sheet, Cliente cliente, CellStyle headerStyle) {
        int rowIndex = 3; // Inicia la tabla de cuentas en la fila 3
        if (cliente.getCuentas() != null && !cliente.getCuentas().isEmpty()) {
            Row accountTitleRow = sheet.createRow(rowIndex++);
            accountTitleRow.createCell(0).setCellValue("Cuentas:");
            accountTitleRow.getCell(0).setCellStyle(headerStyle);

            Row accountHeaderRow = sheet.createRow(rowIndex++);
            accountHeaderRow.createCell(0).setCellValue("Número de Cuenta");
            accountHeaderRow.createCell(1).setCellValue("Saldo");
            accountHeaderRow.createCell(2).setCellValue("Cliente ID");

            List<Cuenta> cuentas = cliente.getCuentas();
            for (Cuenta cuenta : cuentas) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(cuenta.getNumeroCuenta());
                row.createCell(1).setCellValue("S/ " + DECIMAL_FORMAT.format(cuenta.getSaldo()));
                row.createCell(2).setCellValue(cuenta.getCliente().getId());
            }
        }
        return rowIndex;
    }

    // Método para crear la tabla de préstamos
    private void createLoansTable(Sheet sheet, Cliente cliente, CellStyle headerStyle, int startRowIndex) {
        int rowIndex = startRowIndex;
        if (cliente.getPrestamos() != null && !cliente.getPrestamos().isEmpty()) {
            Row loanTitleRow = sheet.createRow(rowIndex++);
            loanTitleRow.createCell(0).setCellValue("Préstamos:");
            loanTitleRow.getCell(0).setCellStyle(headerStyle);

            Row loanHeaderRow = sheet.createRow(rowIndex++);
            loanHeaderRow.createCell(0).setCellValue("Monto");
            loanHeaderRow.createCell(1).setCellValue("Interés");
            loanHeaderRow.createCell(2).setCellValue("Fecha de Creación");
            loanHeaderRow.createCell(3).setCellValue("Estado");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            List<Prestamo> prestamos = cliente.getPrestamos();
            for (Prestamo prestamo : prestamos) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue("S/ " + DECIMAL_FORMAT.format(prestamo.getMonto()));
                row.createCell(1).setCellValue(prestamo.getInteres() + " %");
                row.createCell(2).setCellValue(prestamo.getFechaCreacion() != null ?
                        prestamo.getFechaCreacion().format(dateFormatter) : "Fecha no disponible");
                row.createCell(3).setCellValue(String.valueOf(prestamo.getEstado()));
            }
        }
    }

    // Método para ajustar el tamaño de las columnas
    private void adjustColumnSize(Sheet sheet) {
        for (int i = 0; i < sheet.getRow(0).getPhysicalNumberOfCells(); i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
