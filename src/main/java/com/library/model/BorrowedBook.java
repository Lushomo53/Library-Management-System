package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Model representing an active or completed book borrowing
 */
public class BorrowedBook {
    
    private int borrowId;
    private int requestId;
    private int memberId;
    private int bookId;
    private int issuedBy; // librarian user_id
    private LocalDateTime issueDate;
    private LocalDate dueDate;
    private LocalDateTime returnDate;
    private Integer returnedTo; // librarian user_id who processed return
    private String status; // ISSUED, RETURNED, OVERDUE
    private boolean allowRenewal;
    private int renewalCount;
    private BigDecimal fineAmount;
    private String notes;
    
    // Navigation properties
    private User member;
    private Book book;
    private User issuer;
    private User returner;

    // Constructors
    public BorrowedBook() {
        this.fineAmount = BigDecimal.ZERO;
        this.renewalCount = 0;
        this.allowRenewal = true;
    }

    public BorrowedBook(int requestId, int memberId, int bookId, int issuedBy, LocalDate dueDate) {
        this();
        this.requestId = requestId;
        this.memberId = memberId;
        this.bookId = bookId;
        this.issuedBy = issuedBy;
        this.issueDate = LocalDateTime.now();
        this.dueDate = dueDate;
        this.status = "ISSUED";
    }

    // Getters and Setters
    public int getBorrowId() {
        return borrowId;
    }

    public void setBorrowId(int borrowId) {
        this.borrowId = borrowId;
    }

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

    public int getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(int issuedBy) {
        this.issuedBy = issuedBy;
    }

    public LocalDateTime getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDateTime issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public Integer getReturnedTo() {
        return returnedTo;
    }

    public void setReturnedTo(Integer returnedTo) {
        this.returnedTo = returnedTo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAllowRenewal() {
        return allowRenewal;
    }

    public void setAllowRenewal(boolean allowRenewal) {
        this.allowRenewal = allowRenewal;
    }

    public int getRenewalCount() {
        return renewalCount;
    }

    public void setRenewalCount(int renewalCount) {
        this.renewalCount = renewalCount;
    }

    public BigDecimal getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(BigDecimal fineAmount) {
        this.fineAmount = fineAmount;
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

    public User getIssuer() {
        return issuer;
    }

    public void setIssuer(User issuer) {
        this.issuer = issuer;
    }

    public User getReturner() {
        return returner;
    }

    public void setReturner(User returner) {
        this.returner = returner;
    }

    // Utility methods
    public boolean isIssued() {
        return "ISSUED".equalsIgnoreCase(status);
    }

    public boolean isReturned() {
        return "RETURNED".equalsIgnoreCase(status);
    }

    public boolean isOverdue() {
        return "OVERDUE".equalsIgnoreCase(status) || 
               (isIssued() && LocalDate.now().isAfter(dueDate));
    }

    public long getDaysOverdue() {
        if (!isOverdue()) return 0;
        return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }

    public long getDaysUntilDue() {
        if (isOverdue()) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    public BigDecimal calculateFine(BigDecimal finePerDay) {
        if (!isOverdue()) return BigDecimal.ZERO;
        long daysOverdue = getDaysOverdue();
        return finePerDay.multiply(BigDecimal.valueOf(daysOverdue));
    }

    @Override
    public String toString() {
        return "BorrowedBook{" +
                "borrowId=" + borrowId +
                ", memberId=" + memberId +
                ", bookId=" + bookId +
                ", status='" + status + '\'' +
                ", issueDate=" + issueDate +
                ", dueDate=" + dueDate +
                ", isOverdue=" + isOverdue() +
                '}';
    }
}