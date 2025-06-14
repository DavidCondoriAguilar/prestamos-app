package com.prestamosrapidos.prestamos_app;

import java.sql.*;

public class DatabaseSchemaChecker {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/prestamos";
        String user = "postgres";
        String password = "deiv2025";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to the database!");
            
            // Check Flyway schema version
            checkFlywaySchemaVersion(conn);
            
            // Check prestamos table columns
            checkTableColumns(conn, "prestamos");
            
        } catch (SQLException e) {
            System.err.println("Database connection error:");
            e.printStackTrace();
        }
    }
    
    private static void checkFlywaySchemaVersion(Connection conn) throws SQLException {
        String sql = "SELECT version, description, type, script, installed_on, success FROM flyway_schema_history ORDER BY installed_rank";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT to_regclass('flyway_schema_history')")) {
            if (rs.next() && rs.getObject(1) != null) {
                System.out.println("\nFlyway Migrations:");
                System.out.println("Version\t| Description\t| Type\t| Success | Installed On");
                System.out.println("--------+-----------------+-------+---------+-----------------");
                
                try (ResultSet rs2 = stmt.executeQuery(sql)) {
                    while (rs2.next()) {
                        System.out.println(
                            rs2.getString("version") + "\t| " +
                            rs2.getString("description") + "\t| " +
                            rs2.getString("type") + "\t| " +
                            rs2.getBoolean("success") + "\t| " +
                            rs2.getTimestamp("installed_on"));
                    }
                }
            } else {
                System.out.println("\nFlyway schema history table does not exist!");
            }
        }
    }
    
    private static void checkTableColumns(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT column_name, data_type, is_nullable, column_default " +
                   "FROM information_schema.columns " +
                   "WHERE table_name = ? " +
                   "ORDER BY ordinal_position";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            ResultSet rs = pstmt.executeQuery();
            
            System.out.println("\nTable: " + tableName);
            System.out.println("Column Name\t| Data Type\t| Nullable | Default");
            System.out.println("------------+---------------+----------+---------");
            
            while (rs.next()) {
                System.out.println(
                    String.format("%-11s", rs.getString("column_name")) + "\t| " +
                    String.format("%-13s", rs.getString("data_type")) + "\t| " +
                    String.format("%-8s", rs.getString("is_nullable")) + " | " +
                    rs.getString("column_default"));
            }
        }
    }
}
