package com.library.model;

import java.time.LocalDateTime;

/**
 * User model representing all types of users in the system
 * (Members, Librarians, and Admins)
 */
public class User {
    
    private int userId;
    private String username;
    private String password;
    private String role; // MEMBER, LIBRARIAN, ADMIN
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String status; // ACTIVE, INACTIVE, PENDING
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional fields for specific roles
    private String employeeId; // For Librarians
    private String memberId;   // For Members
    
    // Permissions (mainly for Librarians)
    private boolean canApproveRequests;
    private boolean canIssueReturns;
    private boolean canRevokeMembership;

    // Constructors
    public User() {
    }

    public User(int userId, String username, String role, String fullName, String email) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.status = "ACTIVE";
    }

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public boolean isCanApproveRequests() {
        return canApproveRequests;
    }

    public void setCanApproveRequests(boolean canApproveRequests) {
        this.canApproveRequests = canApproveRequests;
    }

    public boolean isCanIssueReturns() {
        return canIssueReturns;
    }

    public void setCanIssueReturns(boolean canIssueReturns) {
        this.canIssueReturns = canIssueReturns;
    }

    public boolean isCanRevokeMembership() {
        return canRevokeMembership;
    }

    public void setCanRevokeMembership(boolean canRevokeMembership) {
        this.canRevokeMembership = canRevokeMembership;
    }

    // Utility methods
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }

    public boolean isMember() {
        return "MEMBER".equalsIgnoreCase(this.role);
    }

    public boolean isLibrarian() {
        return "LIBRARIAN".equalsIgnoreCase(this.role);
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.role);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}