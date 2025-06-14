package com.prestamosrapidos.prestamos_app;

import java.sql.*;

public class CheckFlywayMigration {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/prestamos";
        String user = "postgres";
        String password = "deiv2025";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to the database!");
            
            // Check if flyway_schema_history table exists
            checkFlywayTable(conn);
            
            // Check the structure of the prestamos table
            checkPrestamosTable(conn);
            
        } catch (SQLException e) {
            System.err.println("Database connection error:");
            e.printStackTrace();
        }
    }
    
    private static void checkFlywayTable(Connection conn) throws SQLException {
        String sql = "SELECT to_regclass('flyway_schema_history')";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next() && rs.getObject(1) != null) {
                System.out.println("\nFlyway schema history table exists!");
                
                // Show migration history
                String historySql = "SELECT version, description, type, script, installed_on, success, state " +
                                 "FROM flyway_schema_history ORDER BY installed_rank";
                try (ResultSet rs2 = stmt.executeQuery(historySql)) {
                    System.out.println("\nFlyway Migrations:");
                    System.out.println("Version\t| Description\t| Type\t| Success | State\t| Installed On");
                    System.out.println("--------+-----------------+-------+---------+---------+-----------------");
                    
                    while (rs2.next()) {
                        System.out.println(
                            String.format("%-7s", rs2.getString("version")) + " | " +
                            String.format("%-15s", rs2.getString("description")) + " | " +
                            String.format("%-5s", rs2.getString("type")) + " | " +
                            String.format("%-7s", rs2.getBoolean("success")) + " | " +
                            String.format("%-5s", rs2.getString("state")) + " | " +
                            rs2.getTimestamp("installed_on"));
                    }
                }
            } else {
                System.out.println("\nFlyway schema history table does not exist!");
            }
        }
    }
    
    private static void checkPrestamosTable(Connection conn) throws SQLException {
        String sql = "SELECT column_name, data_type, is_nullable, column_default, character_maximum_length " +
                   "FROM information_schema.columns " +
                   "WHERE table_name = 'prestamos' " +
                   "ORDER BY ordinal_position";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("\nTable: prestamos");
            System.out.println("Column Name\t| Data Type\t| Nullable | Default\t| Max Length");
            System.out.println("------------+---------------+----------+---------------+------------");
            
            while (rs.next()) {
                System.out.println(
                    String.format("%-11s", rs.getString("column_name")) + "\t| " +
                    String.format("%-13s", rs.getString("data_type")) + " | " +
                    String.format("%-8s", rs.getString("is_nullable")) + " | " +
                    String.format("%-14s", rs.getString("column_default")) + " | " +
                    rs.getString("character_maximum_length"));
            }
        }
    }
}
