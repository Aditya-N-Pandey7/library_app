package com.exam;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class App {

    // Default Oracle Database JDBC credentials (used if environment variables are not set)
    private static final String DEFAULT_URL = "jdbc:oracle:thin:@//localhost:1521/XE";
    private static final String DEFAULT_USER = "exam_user";
    private static final String DEFAULT_PASSWORD = "tiger";

    // Embedded fallback URL (Oracle compatibility mode) if Oracle Server is not installed locally
    private static final String FALLBACK_URL = "jdbc:h2:./library_db;MODE=Oracle;AUTO_SERVER=TRUE";
    private static final String IN_MEMORY_FALLBACK_URL = "jdbc:h2:mem:library_db;MODE=Oracle;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) {
        try (Connection conn = connectToDatabase();
             Scanner sc = new Scanner(System.in)) {

            System.out.println("Connected to " + conn.getMetaData().getDatabaseProductName() + " successfully!");
            createTableIfNotExists(conn);

            while (true) {
                System.out.println("\n=== LIBRARY BOOK MANAGEMENT SYSTEM ===");
                System.out.println("1. Add Book");
                System.out.println("2. View All Books");
                System.out.println("3. Search Book by ID");
                System.out.println("4. Update Book Details (Price & Quantity)");
                System.out.println("5. Issue Book");
                System.out.println("6. Return Book");
                System.out.println("7. Delete Book");
                System.out.println("8. Exit");

                int choice = readInt(sc, "Enter your choice: ");

                switch (choice) {
                    case 1 -> addBook(conn, sc);
                    case 2 -> viewAllBooks(conn);
                    case 3 -> searchBook(conn, sc);
                    case 4 -> updateBook(conn, sc);
                    case 5 -> issueBook(conn, sc);
                    case 6 -> returnBook(conn, sc);
                    case 7 -> deleteBook(conn, sc);
                    case 8 -> {
                        System.out.println("Exiting Library Management System...");
                        return;
                    }
                    default -> System.out.println("Invalid choice! Please enter a number between 1 and 8.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Failed to establish database connection: " + e.getMessage());
        }
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }

    private static Connection connectToDatabase() throws SQLException {
        String url = getEnvOrDefault("ORACLE_JDBC_URL", DEFAULT_URL);
        String user = getEnvOrDefault("ORACLE_DB_USER", DEFAULT_USER);
        String password = getEnvOrDefault("ORACLE_DB_PASSWORD", DEFAULT_PASSWORD);

        DriverManager.setLoginTimeout(3);
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            try {
                return DriverManager.getConnection(url, user, password);
            } catch (SQLException firstEx) {
                // Oracle 21c XE on Windows often binds TNS Listener to the PC hostname instead of localhost
                String hostName = java.net.InetAddress.getLocalHost().getHostName();
                String hostUrl = "jdbc:oracle:thin:@//" + hostName + ":1521/XE";
                return DriverManager.getConnection(hostUrl, user, password);
            }
        } catch (Exception e) {
            // If local Oracle Server is not running, use embedded H2 in Oracle compatibility mode
            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException ignored) {
            }
            try {
                return DriverManager.getConnection(FALLBACK_URL, "sa", "");
            } catch (SQLException lockEx) {
                return DriverManager.getConnection(IN_MEMORY_FALLBACK_URL, "sa", "");
            }
        }
    }

    private static int readInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a valid integer.");
            }
        }
    }

    private static double readDouble(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a valid decimal number.");
            }
        }
    }

    private static String readNonEmptyString(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("Input cannot be empty! Please try again.");
        }
    }

    private static void createTableIfNotExists(Connection conn) {
        String createSql = """
                CREATE TABLE library_books (
                    book_id NUMBER PRIMARY KEY,
                    title VARCHAR2(100) NOT NULL,
                    author VARCHAR2(100) NOT NULL,
                    price NUMBER(10, 2),
                    quantity NUMBER DEFAULT 1
                )
                """;
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createSql);
            System.out.println("Table 'library_books' created.");
        } catch (SQLException e) {
            // Ignore if table already exists (ORA-00955 in Oracle / 42101 in H2)
            if (e.getErrorCode() != 955 && e.getErrorCode() != 42101) {
                System.out.println("Note on table creation: " + e.getMessage());
            }
        }
    }

    private static void addBook(Connection conn, Scanner sc) {
        int id = readInt(sc, "Enter Book ID: ");
        String title = readNonEmptyString(sc, "Enter Book Title: ");
        String author = readNonEmptyString(sc, "Enter Author Name: ");
        double price = readDouble(sc, "Enter Price: ");
        int quantity = readInt(sc, "Enter Quantity: ");

        String sql = "INSERT INTO library_books (book_id, title, author, price, quantity) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, title);
            pstmt.setString(3, author);
            pstmt.setDouble(4, price);
            pstmt.setInt(5, quantity);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Book added successfully!");
            }
        } catch (SQLException e) {
            System.out.println("Error adding book (Book ID " + id + " may already exist): " + e.getMessage());
        }
    }

    private static void viewAllBooks(Connection conn) {
        String sql = "SELECT book_id, title, author, price, quantity FROM library_books ORDER BY book_id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.printf("%n%-10s %-30s %-25s %-10s %-10s%n",
                    "Book ID", "Title", "Author", "Price", "Quantity");
            System.out.println("-".repeat(88));

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-10d %-30s %-25s %-10.2f %-10d%n",
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getDouble("price"),
                        rs.getInt("quantity"));
            }
            if (!found) {
                System.out.println("No books available in the library.");
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving books: " + e.getMessage());
        }
    }

    private static void searchBook(Connection conn, Scanner sc) {
        int id = readInt(sc, "Enter Book ID to search: ");

        String sql = "SELECT book_id, title, author, price, quantity FROM library_books WHERE book_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("\n--- Book Details ---");
                    System.out.println("Book ID  : " + rs.getInt("book_id"));
                    System.out.println("Title    : " + rs.getString("title"));
                    System.out.println("Author   : " + rs.getString("author"));
                    System.out.println("Price    : " + rs.getDouble("price"));
                    System.out.println("Quantity : " + rs.getInt("quantity"));
                } else {
                    System.out.println("Book with ID " + id + " not found.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error searching for book: " + e.getMessage());
        }
    }

    private static void updateBook(Connection conn, Scanner sc) {
        int id = readInt(sc, "Enter Book ID to update: ");
        double price = readDouble(sc, "Enter New Price: ");
        int quantity = readInt(sc, "Enter New Quantity: ");

        String sql = "UPDATE library_books SET price = ?, quantity = ? WHERE book_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, price);
            pstmt.setInt(2, quantity);
            pstmt.setInt(3, id);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Book updated successfully!");
            } else {
                System.out.println("Book with ID " + id + " not found.");
            }
        } catch (SQLException e) {
            System.out.println("Error updating book: " + e.getMessage());
        }
    }

    private static void issueBook(Connection conn, Scanner sc) {
        int id = readInt(sc, "Enter Book ID to issue: ");

        String sql = "UPDATE library_books SET quantity = quantity - 1 WHERE book_id = ? AND quantity > 0";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Book issued successfully!");
            } else {
                System.out.println("Book is either out of stock or ID " + id + " does not exist.");
            }
        } catch (SQLException e) {
            System.out.println("Error issuing book: " + e.getMessage());
        }
    }

    private static void returnBook(Connection conn, Scanner sc) {
        int id = readInt(sc, "Enter Book ID to return: ");

        String sql = "UPDATE library_books SET quantity = quantity + 1 WHERE book_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Book returned successfully!");
            } else {
                System.out.println("Book with ID " + id + " not found.");
            }
        } catch (SQLException e) {
            System.out.println("Error returning book: " + e.getMessage());
        }
    }

    private static void deleteBook(Connection conn, Scanner sc) {
        int id = readInt(sc, "Enter Book ID to delete: ");

        String sql = "DELETE FROM library_books WHERE book_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Book deleted successfully!");
            } else {
                System.out.println("Book with ID " + id + " not found.");
            }
        } catch (SQLException e) {
            System.out.println("Error deleting book: " + e.getMessage());
        }
    }
}
