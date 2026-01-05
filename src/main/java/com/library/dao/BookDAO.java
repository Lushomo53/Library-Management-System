package com.library.dao;

import com.library.model.Book;
import com.library.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Book operations
 */
public class BookDAO {

    /**
     * Get all books
     */
    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String query = "SELECT * FROM books ORDER BY title";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                books.add(extractBookFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting all books: " + e.getMessage());
            e.printStackTrace();
        }
        
        return books;
    }

    /**
     * Get all available books (with available copies > 0)
     */
    public List<Book> getAllAvailableBooks() {
        List<Book> books = new ArrayList<>();
        String query = "SELECT * FROM books WHERE available_copies > 0 ORDER BY title";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                books.add(extractBookFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting available books: " + e.getMessage());
            e.printStackTrace();
        }
        
        return books;
    }

    /**
     * Find book by ID
     */
    public Book findById(int bookId) {
        String query = "SELECT * FROM books WHERE book_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, bookId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractBookFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding book by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Find book by ISBN
     */
    public Book findByISBN(String isbn) {
        String query = "SELECT * FROM books WHERE isbn = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, isbn);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractBookFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding book by ISBN: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Check if ISBN already exists
     */
    public boolean isbnExists(String isbn) {
        String query = "SELECT COUNT(*) FROM books WHERE isbn = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, isbn);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking ISBN: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Search books by title, author, or ISBN
     */
    public List<Book> searchBooks(String searchTerm) {
        List<Book> books = new ArrayList<>();
        String query = "SELECT * FROM books WHERE title LIKE ? OR author LIKE ? OR isbn LIKE ? ORDER BY title";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            String likePattern = "%" + searchTerm + "%";
            pstmt.setString(1, likePattern);
            pstmt.setString(2, likePattern);
            pstmt.setString(3, likePattern);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                books.add(extractBookFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error searching books: " + e.getMessage());
            e.printStackTrace();
        }
        
        return books;
    }

    /**
     * Get books by category
     */
    public List<Book> getBooksByCategory(String category) {
        List<Book> books = new ArrayList<>();
        String query = "SELECT * FROM books WHERE category = ? ORDER BY title";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                books.add(extractBookFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting books by category: " + e.getMessage());
            e.printStackTrace();
        }
        
        return books;
    }

    /**
     * Search books by term and category
     */
    public List<Book> searchBooksByCategory(String searchTerm, String category) {
        List<Book> books = new ArrayList<>();
        String query = "SELECT * FROM books WHERE (title LIKE ? OR author LIKE ? OR isbn LIKE ?) " +
                      "AND category = ? ORDER BY title";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            String likePattern = "%" + searchTerm + "%";
            pstmt.setString(1, likePattern);
            pstmt.setString(2, likePattern);
            pstmt.setString(3, likePattern);
            pstmt.setString(4, category);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                books.add(extractBookFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error searching books by category: " + e.getMessage());
            e.printStackTrace();
        }
        
        return books;
    }

    /**
     * Create a new book
     */
    public boolean createBook(Book book) {
        String query = "INSERT INTO books (isbn, title, author, publisher, publication_year, edition, " +
                      "category, description, total_copies, available_copies, price, shelf_location, created_at) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, book.getIsbn());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getAuthor());
            pstmt.setString(4, book.getPublisher());
            
            if (book.getPublicationYear() != null) {
                pstmt.setInt(5, book.getPublicationYear());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            
            pstmt.setString(6, book.getEdition());
            pstmt.setString(7, book.getCategory());
            pstmt.setString(8, book.getDescription());
            pstmt.setInt(9, book.getTotalCopies());
            pstmt.setInt(10, book.getAvailableCopies());
            
            if (book.getPrice() != null) {
                pstmt.setBigDecimal(11, book.getPrice());
            } else {
                pstmt.setNull(11, Types.DECIMAL);
            }
            
            pstmt.setString(12, book.getShelfLocation());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    book.setBookId(generatedKeys.getInt(1));
                }
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating book: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Update existing book
     */
    public boolean updateBook(Book book) {
        String query = "UPDATE books SET isbn = ?, title = ?, author = ?, publisher = ?, " +
                      "publication_year = ?, edition = ?, category = ?, description = ?, " +
                      "total_copies = ?, available_copies = ?, price = ?, shelf_location = ?, " +
                      "updated_at = CURRENT_TIMESTAMP WHERE book_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, book.getIsbn());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getAuthor());
            pstmt.setString(4, book.getPublisher());
            
            if (book.getPublicationYear() != null) {
                pstmt.setInt(5, book.getPublicationYear());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            
            pstmt.setString(6, book.getEdition());
            pstmt.setString(7, book.getCategory());
            pstmt.setString(8, book.getDescription());
            pstmt.setInt(9, book.getTotalCopies());
            pstmt.setInt(10, book.getAvailableCopies());
            
            if (book.getPrice() != null) {
                pstmt.setBigDecimal(11, book.getPrice());
            } else {
                pstmt.setNull(11, Types.DECIMAL);
            }
            
            pstmt.setString(12, book.getShelfLocation());
            pstmt.setInt(13, book.getBookId());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating book: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Update book stock (total and available copies)
     */
    public boolean updateStock(int bookId, int totalCopies, int availableCopies) {
        String query = "UPDATE books SET total_copies = ?, available_copies = ?, " +
                      "updated_at = CURRENT_TIMESTAMP WHERE book_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, totalCopies);
            pstmt.setInt(2, availableCopies);
            pstmt.setInt(3, bookId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating stock: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Delete book
     */
    public boolean deleteBook(int bookId) {
        String query = "DELETE FROM books WHERE book_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, bookId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting book: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Get low stock books
     */
    public List<Book> getLowStockBooks() {
        List<Book> books = new ArrayList<>();
        String query = "SELECT * FROM books WHERE available_copies < 3 OR " +
                      "(available_copies * 100.0 / total_copies) < 30 ORDER BY available_copies";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                books.add(extractBookFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting low stock books: " + e.getMessage());
            e.printStackTrace();
        }
        
        return books;
    }

    /**
     * Get total books count
     */
    public int getTotalBooksCount() {
        String query = "SELECT SUM(total_copies) FROM books";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting total books count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * Get available books count
     */
    public int getAvailableBooksCount() {
        String query = "SELECT SUM(available_copies) FROM books";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting available books count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * Get borrowed books count
     */
    public int getBorrowedBooksCount() {
        String query = "SELECT SUM(total_copies - available_copies) FROM books";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting borrowed books count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * Extract Book object from ResultSet
     */
    private Book extractBookFromResultSet(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setBookId(rs.getInt("book_id"));
        book.setIsbn(rs.getString("isbn"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setPublisher(rs.getString("publisher"));
        
        int year = rs.getInt("publication_year");
        if (!rs.wasNull()) {
            book.setPublicationYear(year);
        }
        
        book.setEdition(rs.getString("edition"));
        book.setCategory(rs.getString("category"));
        book.setDescription(rs.getString("description"));
        book.setTotalCopies(rs.getInt("total_copies"));
        book.setAvailableCopies(rs.getInt("available_copies"));
        
        BigDecimal price = rs.getBigDecimal("price");
        if (price != null) {
            book.setPrice(price);
        }
        
        book.setShelfLocation(rs.getString("shelf_location"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            book.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            book.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return book;
    }
}