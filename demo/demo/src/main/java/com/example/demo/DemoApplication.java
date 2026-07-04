package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

@SpringBootApplication
@EnableAsync
public class DemoApplication {

	private final DataSource dataSource;

	public DemoApplication(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@jakarta.annotation.PostConstruct
	public void migrateDatabase() {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			ResultSet columns = metaData.getColumns(null, null, "notifications", "read");
			
			if (columns.next()) {
				// Column exists with old name, rename it
				try {
					connection.createStatement().executeUpdate(
						"ALTER TABLE notifications CHANGE COLUMN read is_read BIT NOT NULL DEFAULT 0"
					);
					System.out.println("Successfully renamed 'read' column to 'is_read' in notifications table");
				} catch (SQLException e) {
					System.err.println("Failed to rename column: " + e.getMessage());
				}
			} else {
				// Check if is_read column exists
				ResultSet isReadColumns = metaData.getColumns(null, null, "notifications", "is_read");
				if (isReadColumns.next()) {
					System.out.println("Column 'is_read' already exists - migration already applied");
				} else {
					System.err.println("Neither 'read' nor 'is_read' column found in notifications table");
				}
			}
		} catch (SQLException e) {
			System.err.println("Failed to check database schema: " + e.getMessage());
		}
	}
}