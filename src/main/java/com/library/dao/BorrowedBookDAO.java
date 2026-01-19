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

    private final BookDAO bookDAO = new BookDAO();
    private final UserDAO userDAO = new UserDAO();

    /* =====================================================
       ISSUE BOOK (transaction-safe, canonical version)
       ===================================================== */
    public boolean issueBook(BorrowedBook borrowedBook) {
        String insertSql =
                "INSERT INTO borrowed_books (" +
                        "request_id, member_id, book_id, issued_by, " +
                        "issue_date, due_date, status, allow_renewal, notes" +
                        ") VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?)";

        String updateBookSql =
                "UPDATE books SET available_copies = available_copies - 1 " +
                        "WHERE book_id = ? AND available_copies > 0";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement insertStmt =
                         conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

                insertStmt.setInt(1, borrowedBook.getRequestId());
                insertStmt.setInt(2, borrowedBook.getMemberId());
                insertStmt.setInt(3, borrowedBook.getBookId());
                insertStmt.setInt(4, borrowedBook.getIssuedBy());
                insertStmt.setDate(5, Date.valueOf(borrowedBook.getDueDate()));
                insertStmt.setString(6, borrowedBook.getStatus()); // ISSUED
                insertStmt.setBoolean(7, borrowedBook.isAllowRenewal());
                insertStmt.setString(8, borrowedBook.getNotes());

                int rows = insertStmt.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        borrowedBook.setBorrowId(keys.getInt(1));
                    }
                }
            }

            try (PreparedStatement updateBookStmt =
                         conn.prepareStatement(updateBookSql)) {
                updateBookStmt.setInt(1, borrowedBook.getBookId());
                updateBookStmt.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Error issuing book: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /* =====================================================
       RETURN BOOK (schema-safe)
       ===================================================== */
    public boolean returnBook(int borrowId, int returnedTo) {
        String sql =
                "UPDATE borrowed_books SET status = 'RETURNED', " +
                        "return_date = CURRENT_TIMESTAMP, returned_to = ? " +
                        "WHERE borrow_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, returnedTo);
            stmt.setInt(2, borrowId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error returning book: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /* =====================================================
       SEARCH / FILTER FEATURES (adapted safely)
       ===================================================== */

    /** Search active borrows by ISBN or title */
    public List<BorrowedBook> searchActiveBorrowsByBook(String searchTerm) {
        String sql =
                "SELECT bb.* FROM borrowed_books bb " +
                        "JOIN books b ON bb.book_id = b.book_id " +
                        "WHERE bb.status = 'ISSUED' " +
                        "AND (b.isbn LIKE ? OR b.title LIKE ?)";

        return searchBorrowedBooks(sql, searchTerm);
    }

    /** Search active borrows by member info */
    public List<BorrowedBook> searchActiveBorrowsByMember(String searchTerm) {
        String sql =
                "SELECT bb.* FROM borrowed_books bb " +
                        "JOIN users u ON bb.member_id = u.user_id " +
                        "WHERE bb.status = 'ISSUED' " +
                        "AND (u.member_id LIKE ? OR u.full_name LIKE ?)";

        return searchBorrowedBooks(sql, searchTerm);
    }

    private List<BorrowedBook> searchBorrowedBooks(String sql, String searchTerm) {
        List<BorrowedBook> results = new ArrayList<>();
        String pattern = "%" + searchTerm + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pattern);
            stmt.setString(2, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(extractBorrowedBookFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Search error: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Get count of books issued today
     */
    public int getBooksIssuedTodayCount() {
        String query = "SELECT COUNT(*) FROM borrowed_books " +
                "WHERE DATE(issue_date) = CURRENT_DATE";

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
     * Get all borrowed books by member
     */
    public List<BorrowedBook> getBorrowedBooksByMember(int memberId) {
        List<BorrowedBook> borrowedBooks = new ArrayList<>();

        String query =
                "SELECT bb.*, b.title, b.author, b.isbn, b.category " +
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

    /* =====================================================
       OVERDUE & COUNTS
       ===================================================== */

    public List<BorrowedBook> getOverdueBooksByMember(int memberId) {
        List<BorrowedBook> list = new ArrayList<>();
        String sql =
                "SELECT * FROM borrowed_books " +
                        "WHERE member_id = ? AND status = 'ISSUED' " +
                        "AND due_date < CURRENT_DATE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, memberId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(extractBorrowedBookFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching overdue books: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    public int getActiveBorrowCount(int memberId) {
        return getCount(
                "SELECT COUNT(*) FROM borrowed_books WHERE member_id = ? AND status = 'ISSUED'",
                memberId
        );
    }

    public int getOverdueCount(int memberId) {
        return getCount(
                "SELECT COUNT(*) FROM borrowed_books " +
                        "WHERE member_id = ? AND status = 'ISSUED' AND due_date < CURRENT_DATE",
                memberId
        );
    }

    public int getTotalOverdueCount() {
        return getCount(
                "SELECT COUNT(*) FROM borrowed_books " +
                        "WHERE status = 'ISSUED' AND due_date < CURRENT_DATE",
                null
        );
    }

    private int getCount(String sql, Integer param) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (param != null) stmt.setInt(1, param);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<BorrowedBook> getAllBorrowedBooks() {
        List<BorrowedBook> borrowedBooks = new ArrayList<>();

        String sql =
                "SELECT bb.*, b.title, b.author, b.isbn, " +
                        "u.full_name, u.email, u.member_id " +
                        "FROM borrowed_books bb " +
                        "JOIN books b ON bb.book_id = b.book_id " +
                        "JOIN users u ON bb.member_id = u.user_id " +
                        "ORDER BY bb.issue_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                BorrowedBook bb = extractBorrowedBookFromResultSet(rs);

                Book book = new Book();
                book.setBookId(bb.getBookId());
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setIsbn(rs.getString("isbn"));
                bb.setBook(book);

                User member = new User();
                member.setUserId(bb.getMemberId());
                member.setFullName(rs.getString("full_name"));
                member.setEmail(rs.getString("email"));
                member.setMemberId(rs.getString("member_id"));
                bb.setMember(member);

                borrowedBooks.add(bb);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching all borrowed books: " + e.getMessage());
            e.printStackTrace();
        }

        return borrowedBooks;
    }

    public List<BorrowedBook> getOverdueBooks() {
        List<BorrowedBook> borrowedBooks = new ArrayList<>();

        String sql =
                "SELECT bb.*, b.title, b.author, b.isbn, " +
                        "u.full_name, u.email, u.phone, u.member_id " +
                        "FROM borrowed_books bb " +
                        "JOIN books b ON bb.book_id = b.book_id " +
                        "JOIN users u ON bb.member_id = u.user_id " +
                        "WHERE bb.status = 'ISSUED' " +
                        "AND bb.due_date < CURRENT_DATE " +
                        "ORDER BY bb.due_date";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                BorrowedBook bb = extractBorrowedBookFromResultSet(rs);

                Book book = new Book();
                book.setBookId(bb.getBookId());
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setIsbn(rs.getString("isbn"));
                bb.setBook(book);

                User member = new User();
                member.setUserId(bb.getMemberId());
                member.setFullName(rs.getString("full_name"));
                member.setEmail(rs.getString("email"));
                member.setPhone(rs.getString("phone"));
                member.setMemberId(rs.getString("member_id"));
                bb.setMember(member);

                borrowedBooks.add(bb);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return borrowedBooks;
    }

    /* =====================================================
       RENEWAL & FINE
       ===================================================== */

    public boolean renewBook(int borrowId, int additionalDays) {
        String sql =
                "UPDATE borrowed_books SET " +
                        "due_date = DATE_ADD(due_date, INTERVAL ? DAY), " +
                        "renewal_count = renewal_count + 1 " +
                        "WHERE borrow_id = ? AND allow_renewal = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, additionalDays);
            stmt.setInt(2, borrowId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateFine(int borrowId, BigDecimal fineAmount) {
        String sql =
                "UPDATE borrowed_books SET fine_amount = ? WHERE borrow_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, fineAmount);
            stmt.setInt(2, borrowId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /* =====================================================
       RESULTSET MAPPER (canonical)
       ===================================================== */

    private BorrowedBook extractBorrowedBookFromResultSet(ResultSet rs)
            throws SQLException {

        BorrowedBook bb = new BorrowedBook();
        bb.setBorrowId(rs.getInt("borrow_id"));
        bb.setRequestId(rs.getInt("request_id"));
        bb.setMemberId(rs.getInt("member_id"));
        bb.setBookId(rs.getInt("book_id"));
        bb.setIssuedBy(rs.getInt("issued_by"));

        Timestamp issueTs = rs.getTimestamp("issue_date");
        if (issueTs != null) bb.setIssueDate(issueTs.toLocalDateTime());

        Date dueDate = rs.getDate("due_date");
        if (dueDate != null) bb.setDueDate(dueDate.toLocalDate());

        Timestamp returnTs = rs.getTimestamp("return_date");
        if (returnTs != null) bb.setReturnDate(returnTs.toLocalDateTime());

        int returnedTo = rs.getInt("returned_to");
        if (!rs.wasNull()) bb.setReturnedTo(returnedTo);

        bb.setStatus(rs.getString("status"));
        bb.setAllowRenewal(rs.getBoolean("allow_renewal"));
        bb.setRenewalCount(rs.getInt("renewal_count"));
        bb.setFineAmount(rs.getBigDecimal("fine_amount"));
        bb.setNotes(rs.getString("notes"));

        return bb;
    }
}
