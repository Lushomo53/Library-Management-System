package com.library.model;

import java.time.LocalDateTime;

/**
 * Model representing a book borrow request from a member
 */
public class BorrowRequest {
    
    private int requestId;
    private int memberId;
    private int bookId;
    private LocalDateTime requestDate;
    private String status; // PENDING, APPROVED, REJECTED, CANCELLED
    
    // Approved request details
    private Integer approvedBy; // librarian user_id
    private LocalDateTime approvedDate;
    private Integer borrowDurationDays;
    private String notes;
    
    // Navigation properties (populated via joins)
    private User member;
    private Book book;
    private User approver;

    public BorrowRequest() {
    }

    public BorrowRequest(int memberId, int bookId) {
        this.memberId = memberId;
        this.bookId = bookId;
        this.status = "PENDING";
        this.requestDate = LocalDateTime.now();
    }

    // Getters and Setters
    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Integer approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDateTime approvedDate) {
        this.approvedDate = approvedDate;
    }

    public Integer getBorrowDurationDays() {
        return borrowDurationDays;
    }

    public void setBorrowDurationDays(Integer borrowDurationDays) {
        this.borrowDurationDays = borrowDurationDays;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getMember() {
        return member;
    }

    public void setMember(User member) {
        this.member = member;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public User getApprover() {
        return approver;
    }

    public void setApprover(User approver) {
        this.approver = approver;
    }

    // Utility methods
    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "BorrowRequest{" +
                "requestId=" + requestId +
                ", memberId=" + memberId +
                ", bookId=" + bookId +
                ", status='" + status + '\'' +
                ", requestDate=" + requestDate +
                '}';
    }
}