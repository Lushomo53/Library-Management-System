package com.library.dao;

import com.library.model.User;
import com.library.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User operations
 */
public class UserDAO {

    /**
     * Authenticate user with username, password, and role
     * 
     * @param username User's username
     * @param password User's password (should be hashed in production)
     * @param role User's role (MEMBER, LIBRARIAN, ADMIN)
     * @return User object if authentication successful, null otherwise
     */
    public User authenticate(String username, String password, String role) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ? AND role = ? AND status = 'ACTIVE'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password); // In production, hash the password first
            pstmt.setString(3, role);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Find user by ID
     */
    public User findById(int userId) {
        String query = "SELECT * FROM users WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding user by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Find user by username
     */
    public User findByUsername(String username) {
        String query = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding user by username: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Check if username already exists
     */
    public boolean usernameExists(String username) {
        String query = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking username: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Check if email already exists
     */
    public boolean emailExists(String email) {
        String query = "SELECT COUNT(*) FROM users WHERE email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking email: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Create a new user
     */
    public boolean createUser(User user) {
        String query = "INSERT INTO users (username, password, role, full_name, email, phone, address, status, " +
                      "employee_id, member_id, can_approve_requests, can_issue_returns, can_revoke_membership, created_at) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword()); // Hash in production
            pstmt.setString(3, user.getRole());
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getEmail());
            pstmt.setString(6, user.getPhone());
            pstmt.setString(7, user.getAddress());
            pstmt.setString(8, user.getStatus());
            pstmt.setString(9, user.getEmployeeId());
            pstmt.setString(10, user.getMemberId());
            pstmt.setBoolean(11, user.isCanApproveRequests());
            pstmt.setBoolean(12, user.isCanIssueReturns());
            pstmt.setBoolean(13, user.isCanRevokeMembership());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                }
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Update existing user
     */
    public boolean updateUser(User user) {
        String query = "UPDATE users SET full_name = ?, email = ?, phone = ?, address = ?, status = ?, " +
                      "can_approve_requests = ?, can_issue_returns = ?, can_revoke_membership = ?, updated_at = CURRENT_TIMESTAMP " +
                      "WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPhone());
            pstmt.setString(4, user.getAddress());
            pstmt.setString(5, user.getStatus());
            pstmt.setBoolean(6, user.isCanApproveRequests());
            pstmt.setBoolean(7, user.isCanIssueReturns());
            pstmt.setBoolean(8, user.isCanRevokeMembership());
            pstmt.setInt(9, user.getUserId());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Update user password
     */
    public boolean updatePassword(int userId, String newPassword) {
        String query = "UPDATE users SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, newPassword); // Hash in production
            pstmt.setInt(2, userId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Update user status
     */
    public boolean updateStatus(int userId, String status) {
        String query = "UPDATE users SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, status);
            pstmt.setInt(2, userId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating status: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Get all users by role
     */
    public List<User> findByRole(String role) {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users WHERE role = ? ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, role);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding users by role: " + e.getMessage());
            e.printStackTrace();
        }
        
        return users;
    }

    /**
     * Search users by name, email, or username
     */
    public List<User> searchUsers(String searchTerm, String role) {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users WHERE (full_name LIKE ? OR email LIKE ? OR username LIKE ?) " +
                      "AND role = ? ORDER BY full_name";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            String likePattern = "%" + searchTerm + "%";
            pstmt.setString(1, likePattern);
            pstmt.setString(2, likePattern);
            pstmt.setString(3, likePattern);
            pstmt.setString(4, role);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error searching users: " + e.getMessage());
            e.printStackTrace();
        }
        
        return users;
    }

    /**
     * Delete user (soft delete by setting status to INACTIVE)
     */
    public boolean deleteUser(int userId) {
        return updateStatus(userId, "INACTIVE");
    }

    /**
     * Extract User object from ResultSet
     */
    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setAddress(rs.getString("address"));
        user.setStatus(rs.getString("status"));
        user.setEmployeeId(rs.getString("employee_id"));
        user.setMemberId(rs.getString("member_id"));
        user.setCanApproveRequests(rs.getBoolean("can_approve_requests"));
        user.setCanIssueReturns(rs.getBoolean("can_issue_returns"));
        user.setCanRevokeMembership(rs.getBoolean("can_revoke_membership"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return user;
    }
}