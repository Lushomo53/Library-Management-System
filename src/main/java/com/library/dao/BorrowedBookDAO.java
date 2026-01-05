package com.library.dao;

import com.library.model.Book;
import com.library.model.BorrowedBook;
import com.library.model.User;
import com.library.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for BorrowedBook operations
 */
public class BorrowedBookDAO {

    private BookDAO bookDAO = new BookDAO();
    private UserDAO userDAO = new UserDAO();

    /**
     * Issue a book (create borrowed book record)
     */
    public boolean issueBook(BorrowedBook borrowedBook) {
        String query = "INSERT INTO borrowed_books (request_id, member_id, book_id, issued_by, " +
                      "issue_date, due_date, status, allow_renewal, notes) " +
                      "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, borrowedBook.getRequestId());
            pstmt.setInt(2, borrowedBook.getMemberId());
            pstmt.setInt(3, borrowedBook.getBookId());
            pstmt.setInt(4, borrowedBook.getIssuedBy());
            pstmt.setDate(5, Date.valueOf(borrowedBook.getDueDate()));
            pstmt.setString(6, borrowedBook.getStatus());
            pstmt.setBoolean(7, borrowedBook.isAllowRenewal());
            pstmt.setString(8, borrowedBook.getNotes());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    borrowedBook.setBorrowId(generatedKeys.getInt(1));
                }
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error issuing book: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Return a book
     */
    public boolean returnBook(int borrowId, int returnedTo) {
        String query = "UPDATE borrowed_books SET status = 'RETURNED', return_date = CURRENT_TIMESTAMP, " +
                      "returned_to = ? WHERE borrow_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, returnedTo);
            pstmt.setInt(2, borrowId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error returning book: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Find borrowed book by ID
     */
    public BorrowedBook findById(int borrowId) {
        String query = "SELECT * FROM borrowed_books WHERE borrow_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, borrowId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractBorrowedBookFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding borrowed book by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Get all borrowed books by member
     */
    public List<BorrowedBook> getBorrowedBooksByMember(int memberId) {
        List<BorrowedBook> borrowedBooks = new ArrayList<>();
        String query = "SELECT bb.*, b.title, b.author, b.isbn, b.category " +
                      "FROM borrowed_books bb " +
                      "JOIN books b ON bb.book_id = b.book_id " +
                      "WHERE bb.member_id = ? " +
                      "ORDER BY bb.issue_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, memberId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                BorrowedBook borrowedBook = extractBorrowedBookFromResultSet(rs);
                
                // Extract book info
                Book book = new Book();
                book.setBookId(borrowedBook.getBookId());
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setIsbn(rs.getString("isbn"));
                book.setCategory(rs.getString("category"));
                borrowedBook.setBook(book);
                
                borrowedBooks.add(borrowedBook);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting borrowed books by member: " + e.getMessage());
            e.printStackTrace();
        }
        
        return borrowedBooks;
    }

    /**
     * Get active borrowed books by member (status = ISSUED)
     */
    public List<BorrowedBook> getActiveBorrowsByMember(int memberId) {
        List<BorrowedBook> borrowedBooks = new ArrayList<>();
        String query = "SELECT bb.*, b.title, b.author, b.isbn " +
                      "FROM borrowed_books bb " +
                      "JOIN books b ON bb.book_id = b.book_id " +
                      "WHERE bb.member_id = ? AND bb.status = 'ISSUED' " +
                      "ORDER BY bb.due_date";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, memberId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                BorrowedBook borrowedBook = extractBorrowedBookFromResultSet(rs);
                
                // Extract book info
                Book book = new Book();
                book.setBookId(borrowedBook.getBookId());
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setIsbn(rs.getString("isbn"));
                borrowedBook.setBook(book);
                
                borrowedBooks.add(borrowedBook);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting active borrows: " + e.getMessage());
            e.printStackTrace();
        }
        
        return borrowedBooks;
    }

    /**
     * Get all borrowed books (for librarian/admin)
     */
    public List<BorrowedBook> getAllBorrowedBooks() {
        List<BorrowedBook> borrowedBooks = new ArrayList<>();
        String query = "SELECT bb.*, b.title, b.author, b.isbn, " +
                      "u.full_name, u.email, u.member_id " +
                      "FROM borrowed_books bb " +
                      "JOIN books b ON bb.book_id = b.book_id " +
                      "JOIN users u ON bb.member_id = u.user_id " +
                      "ORDER BY bb.issue_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                BorrowedBook borrowedBook = extractBorrowedBookFromResultSet(rs);
                
                // Extract book info
                Book book = new Book();
                book.setBookId(borrowedBook.getBookId());
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setIsbn(rs.getString("isbn"));
                borrowedBook.setBook(book);
                
                // Extract member info
                User member = new User();
                member.setUserId(borrowedBook.getMemberId());
                member.setFullName(rs.getString("full_name"));
                member.setEmail(rs.getString("email"));
                member.setMemberId(rs.getString("member_id"));
                borrowedBook.setMember(member);
                
                borrowedBooks.add(borrowedBook);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting all borrowed books: " + e.getMessage());
            e.printStackTrace();
        }
        
        return borrowedBooks;
    }

    /**
     * Get overdue books
     */
    public List<BorrowedBook> getOverdueBooks() {
        List<BorrowedBook> borrowedBooks = new ArrayList<>();
        String query = "SELECT bb.*, b.title, b.author, b.isbn, " +
                      "u.full_name, u.email, u.phone, u.member_id " +
                      "FROM borrowed_books bb " +
                      "JOIN books b ON bb.book_id = b.book_id " +
                      "JOIN users u ON bb.member_id = u.user_id " +
                      "WHERE bb.due_date < CURRENT_DATE AND bb.status = 'ISSUED' " +
                      "ORDER BY bb.due_date";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                BorrowedBook borrowedBook = extractBorrowedBookFromResultSet(rs);
                
                // Extract book info
                Book book = new Book();
                book.setBookId(borrowedBook.getBookId());
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setIsbn(rs.getString("isbn"));
                borrowedBook.setBook(book);
                
                // Extract member info
                User member = new User();
                member.setUserId(borrowedBook.getMemberId());
                member.setFullName(rs.getString("full_name"));
                member.setEmail(rs.getString("email"));
                member.setPhone(rs.getString("phone"));
                member.setMemberId(rs.getString("member_id"));
                borrowedBook.setMember(member);
                
                borrowedBooks.add(borrowedBook);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting overdue books: " + e.getMessage());
            e.printStackTrace();
        }
        
        return borrowedBooks;
    }

    /**
     * Get active borrow count for a member
     */
    public int getActiveBorrowCount(int memberId) {
        String query = "SELECT COUNT(*) FROM borrowed_books WHERE member_id = ? AND status = 'ISSUED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, memberId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting active borrow count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * Get overdue count for a member
     */
    public int getOverdueCount(int memberId) {
        String query = "SELECT COUNT(*) FROM borrowed_books " +
                      "WHERE member_id = ? AND due_date < CURRENT_DATE AND status = 'ISSUED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, memberId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting overdue count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * Get total overdue count (for admin/librarian)
     */
    public int getTotalOverdueCount() {
        String query = "SELECT COUNT(*) FROM borrowed_books " +
                      "WHERE due_date < CURRENT_DATE AND status = 'ISSUED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting total overdue count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * Get books issued today count
     */
    public int getBooksIssuedTodayCount() {
        String query = "SELECT COUNT(*) FROM borrowed_books WHERE DATE(issue_date) = CURRENT_DATE";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting books issued today count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * Update fine amount
     */
    public boolean updateFine(int borrowId, BigDecimal fineAmount) {
        String query = "UPDATE borrowed_books SET fine_amount = ? WHERE borrow_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setBigDecimal(1, fineAmount);
            pstmt.setInt(2, borrowId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating fine: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Update status (for marking as overdue)
     */
    public boolean updateStatus(int borrowId, String status) {
        String query = "UPDATE borrowed_books SET status = ? WHERE borrow_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, status);
            pstmt.setInt(2, borrowId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating status: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Renew a borrowed book (extend due date)
     */
    public boolean renewBook(int borrowId, int additionalDays) {
        String query = "UPDATE borrowed_books SET due_date = DATE_ADD(due_date, INTERVAL ? DAY), " +
                      "renewal_count = renewal_count + 1 WHERE borrow_id = ? AND allow_renewal = TRUE";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, additionalDays);
            pstmt.setInt(2, borrowId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error renewing book: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Extract BorrowedBook object from ResultSet
     */
    private BorrowedBook extractBorrowedBookFromResultSet(ResultSet rs) throws SQLException {
        BorrowedBook borrowedBook = new BorrowedBook();
        borrowedBook.setBorrowId(rs.getInt("borrow_id"));
        borrowedBook.setRequestId(rs.getInt("request_id"));
        borrowedBook.setMemberId(rs.getInt("member_id"));
        borrowedBook.setBookId(rs.getInt("book_id"));
        borrowedBook.setIssuedBy(rs.getInt("issued_by"));
        
        Timestamp issueDate = rs.getTimestamp("issue_date");
        if (issueDate != null) {
            borrowedBook.setIssueDate(issueDate.toLocalDateTime());
        }
        
        Date dueDate = rs.getDate("due_date");
        if (dueDate != null) {
            borrowedBook.setDueDate(dueDate.toLocalDate());
        }
        
        Timestamp returnDate = rs.getTimestamp("return_date");
        if (returnDate != null) {
            borrowedBook.setReturnDate(returnDate.toLocalDateTime());
        }
        
        int returnedTo = rs.getInt("returned_to");
        if (!rs.wasNull()) {
            borrowedBook.setReturnedTo(returnedTo);
        }
        
        borrowedBook.setStatus(rs.getString("status"));
        borrowedBook.setAllowRenewal(rs.getBoolean("allow_renewal"));
        borrowedBook.setRenewalCount(rs.getInt("renewal_count"));
        
        BigDecimal fineAmount = rs.getBigDecimal("fine_amount");
        if (fineAmount != null) {
            borrowedBook.setFineAmount(fineAmount);
        }
        
        borrowedBook.setNotes(rs.getString("notes"));
        
        return borrowedBook;
    }
}