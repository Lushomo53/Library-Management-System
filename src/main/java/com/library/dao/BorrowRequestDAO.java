package com.library.dao;

import com.library.model.Book;
import com.library.model.BorrowRequest;
import com.library.model.User;
import com.library.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for BorrowRequest operations
 */
public class BorrowRequestDAO {

    private UserDAO userDAO = new UserDAO();
    private BookDAO bookDAO = new BookDAO();

    /**
     * Create a new borrow request
     */
    public boolean createRequest(BorrowRequest request) {
        String query = "INSERT INTO borrow_requests (member_id, book_id, request_date, status) " +
                      "VALUES (?, ?, CURRENT_TIMESTAMP, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, request.getMemberId());
            pstmt.setInt(2, request.getBookId());
            pstmt.setString(3, request.getStatus());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    request.setRequestId(generatedKeys.getInt(1));
                }
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating borrow request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Find request by ID
     */
    public BorrowRequest findById(int requestId) {
        String query = "SELECT * FROM borrow_requests WHERE request_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, requestId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractRequestFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding request by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Get all pending requests
     */
    public List<BorrowRequest> getPendingRequests() {
        return getRequestsByStatus("PENDING");
    }

    /**
     * Get requests by status
     */
    public List<BorrowRequest> getRequestsByStatus(String status) {
        List<BorrowRequest> requests = new ArrayList<>();
        String query = "SELECT * FROM borrow_requests WHERE status = ? ORDER BY request_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                requests.add(extractRequestFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting requests by status: " + e.getMessage());
            e.printStackTrace();
        }
        
        return requests;
    }

    /**
     * Get all requests for a member
     */
    public List<BorrowRequest> getRequestsByMember(int memberId) {
        List<BorrowRequest> requests = new ArrayList<>();
        String query = "SELECT * FROM borrow_requests WHERE member_id = ? ORDER BY request_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, memberId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                requests.add(extractRequestFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting requests by member: " + e.getMessage());
            e.printStackTrace();
        }
        
        return requests;
    }

    /**
     * Get all requests with member and book details (for librarian view)
     */
    public List<BorrowRequest> getAllRequestsWithDetails() {
        List<BorrowRequest> requests = new ArrayList<>();
        String query = "SELECT br.*, u.full_name, u.email, u.member_id, " +
                      "b.title, b.author, b.isbn " +
                      "FROM borrow_requests br " +
                      "JOIN users u ON br.member_id = u.user_id " +
                      "JOIN books b ON br.book_id = b.book_id " +
                      "ORDER BY br.request_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                BorrowRequest request = extractRequestFromResultSet(rs);
                
                // Extract member info
                User member = new User();
                member.setUserId(request.getMemberId());
                member.setFullName(rs.getString("full_name"));
                member.setEmail(rs.getString("email"));
                member.setMemberId(rs.getString("member_id"));
                request.setMember(member);
                
                // Extract book info
                Book book = new Book();
                book.setBookId(request.getBookId());
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setIsbn(rs.getString("isbn"));
                request.setBook(book);
                
                requests.add(request);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting requests with details: " + e.getMessage());
            e.printStackTrace();
        }
        
        return requests;
    }

    /**
     * Get pending requests count for a member
     */
    public int getPendingRequestCount(int memberId) {
        String query = "SELECT COUNT(*) FROM borrow_requests WHERE member_id = ? AND status = 'PENDING'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, memberId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting pending request count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * Check if member has pending request for a book
     */
    public boolean hasPendingRequest(int memberId, int bookId) {
        String query = "SELECT COUNT(*) FROM borrow_requests " +
                      "WHERE member_id = ? AND book_id = ? AND status = 'PENDING'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, memberId);
            pstmt.setInt(2, bookId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking pending request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Approve a request
     */
    public boolean approveRequest(int requestId, int approvedBy, int borrowDurationDays, String notes) {
        String query = "UPDATE borrow_requests SET status = 'APPROVED', approved_by = ?, " +
                      "approved_date = CURRENT_TIMESTAMP, borrow_duration_days = ?, notes = ? " +
                      "WHERE request_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, approvedBy);
            pstmt.setInt(2, borrowDurationDays);
            pstmt.setString(3, notes);
            pstmt.setInt(4, requestId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error approving request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Reject a request
     */
    public boolean rejectRequest(int requestId, String notes) {
        String query = "UPDATE borrow_requests SET status = 'REJECTED', notes = ? WHERE request_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, notes);
            pstmt.setInt(2, requestId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error rejecting request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Cancel a request (by member)
     */
    public boolean cancelRequest(int requestId) {
        String query = "UPDATE borrow_requests SET status = 'CANCELLED' WHERE request_id = ? AND status = 'PENDING'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, requestId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error cancelling request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Update request status
     */
    public boolean updateStatus(int requestId, String status) {
        String query = "UPDATE borrow_requests SET status = ? WHERE request_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, status);
            pstmt.setInt(2, requestId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating request status: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Delete a request
     */
    public boolean deleteRequest(int requestId) {
        String query = "DELETE FROM borrow_requests WHERE request_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, requestId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Get total pending requests count (for admin/librarian)
     */
    public int getTotalPendingRequestsCount() {
        String query = "SELECT COUNT(*) FROM borrow_requests WHERE status = 'PENDING'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting total pending requests count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * Extract BorrowRequest object from ResultSet
     */
    private BorrowRequest extractRequestFromResultSet(ResultSet rs) throws SQLException {
        BorrowRequest request = new BorrowRequest();
        request.setRequestId(rs.getInt("request_id"));
        request.setMemberId(rs.getInt("member_id"));
        request.setBookId(rs.getInt("book_id"));
        
        Timestamp requestDate = rs.getTimestamp("request_date");
        if (requestDate != null) {
            request.setRequestDate(requestDate.toLocalDateTime());
        }
        
        request.setStatus(rs.getString("status"));
        
        int approvedBy = rs.getInt("approved_by");
        if (!rs.wasNull()) {
            request.setApprovedBy(approvedBy);
        }
        
        Timestamp approvedDate = rs.getTimestamp("approved_date");
        if (approvedDate != null) {
            request.setApprovedDate(approvedDate.toLocalDateTime());
        }
        
        int borrowDuration = rs.getInt("borrow_duration_days");
        if (!rs.wasNull()) {
            request.setBorrowDurationDays(borrowDuration);
        }
        
        request.setNotes(rs.getString("notes"));
        
        return request;
    }
}