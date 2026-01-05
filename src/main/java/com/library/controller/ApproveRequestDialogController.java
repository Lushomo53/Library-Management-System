package com.library.controller;

import com.library.dao.BookDAO;
import com.library.dao.BorrowRequestDAO;
import com.library.dao.BorrowedBookDAO;
import com.library.model.Book;
import com.library.model.BorrowRequest;
import com.library.model.BorrowedBook;
import com.library.model.User;
import com.library.util.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ApproveRequestDialogController implements Initializable {

    // Request details (read-only)
    @FXML private Label requestIdLabel;
    @FXML private Label memberNameLabel;
    @FXML private Label bookTitleLabel;
    @FXML private Label requestDateLabel;
    
    // Borrowing parameters
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private TextField returnDateField;
    @FXML private CheckBox allowRenewalCheckbox;
    @FXML private TextArea notesField;
    
    // Error labels
    @FXML private Label durationError;
    @FXML private Label generalMessage;
    
    // Buttons
    @FXML private Button cancelButton;
    @FXML private Button approveButton;
    
    private BorrowRequest currentRequest;
    private User currentLibrarian;
    private BorrowRequestDAO borrowRequestDAO;
    private BorrowedBookDAO borrowedBookDAO;
    private BookDAO bookDAO;
    
    private Runnable onApprovedCallback;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter REQUEST_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final int DEFAULT_DURATION = 14;
    private static final int MIN_DURATION = 1;
    private static final int MAX_DURATION = 90;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize DAOs
        borrowRequestDAO = new BorrowRequestDAO();
        borrowedBookDAO = new BorrowedBookDAO();
        bookDAO = new BookDAO();
        
        // Setup spinner
        setupDurationSpinner();
        
        // Add listener to update return date when duration changes
        durationSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateReturnDate();
            clearError(durationError);
        });
    }

    /**
     * Setup duration spinner with default values
     */
    private void setupDurationSpinner() {
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_DURATION, MAX_DURATION, DEFAULT_DURATION);
        durationSpinner.setValueFactory(valueFactory);
        durationSpinner.setEditable(true);
        
        // Add text formatter to handle manual input
        durationSpinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                try {
                    int value = Integer.parseInt(newVal);
                    if (value >= MIN_DURATION && value <= MAX_DURATION) {
                        updateReturnDate();
                    }
                } catch (NumberFormatException e) {
                    // Invalid input, ignore
                }
            }
        });
    }

    /**
     * Set the borrow request to approve
     */
    public void setRequest(BorrowRequest request) {
        this.currentRequest = request;
        
        if (request != null) {
            populateRequestDetails();
            updateReturnDate();
        }
    }

    /**
     * Set the librarian approving the request
     */
    public void setLibrarian(User librarian) {
        this.currentLibrarian = librarian;
    }

    /**
     * Set callback to execute when request is approved
     */
    public void setOnApproved(Runnable callback) {
        this.onApprovedCallback = callback;
    }

    /**
     * Populate request details (read-only fields)
     */
    private void populateRequestDetails() {
        if (currentRequest == null) return;
        
        requestIdLabel.setText("REQ-" + String.format("%06d", currentRequest.getRequestId()));
        
        if (currentRequest.getMember() != null) {
            memberNameLabel.setText(currentRequest.getMember().getFullName());
        }
        
        if (currentRequest.getBook() != null) {
            bookTitleLabel.setText(currentRequest.getBook().getTitle());
        }
        
        if (currentRequest.getRequestDate() != null) {
            requestDateLabel.setText(currentRequest.getRequestDate().format(REQUEST_DATE_FORMATTER));
        }
    }

    /**
     * Update return date based on current duration
     */
    private void updateReturnDate() {
        try {
            int duration = durationSpinner.getValue();
            LocalDate returnDate = LocalDate.now().plusDays(duration);
            returnDateField.setText(returnDate.format(DATE_FORMATTER));
        } catch (Exception e) {
            returnDateField.setText("Invalid duration");
        }
    }

    /**
     * Handle approve button click
     */
    @FXML
    private void handleApprove() {
        clearAllErrors();
        
        if (currentRequest == null || currentLibrarian == null) {
            showError(generalMessage, "Invalid request or librarian data");
            return;
        }
        
        // Validate duration
        int duration = durationSpinner.getValue();
        if (duration < MIN_DURATION || duration > MAX_DURATION) {
            showError(durationError, "Duration must be between " + MIN_DURATION + " and " + MAX_DURATION + " days");
            return;
        }
        
        // Get parameters
        boolean allowRenewal = allowRenewalCheckbox.isSelected();
        String notes = notesField.getText().trim();
        
        // Confirm approval
        boolean confirmed = SceneManager.showConfirmation(
            "Confirm Approval",
            "Are you sure you want to approve this request?\n\n" +
            "Book: " + currentRequest.getBook().getTitle() + "\n" +
            "Member: " + currentRequest.getMember().getFullName() + "\n" +
            "Duration: " + duration + " days\n" +
            "Return by: " + LocalDate.now().plusDays(duration).format(DATE_FORMATTER)
        );
        
        if (!confirmed) return;
        
        try {
            // Step 1: Check book availability
            Book book = bookDAO.findById(currentRequest.getBookId());
            if (book == null || book.getAvailableCopies() <= 0) {
                showError(generalMessage, "Book is no longer available. Request cannot be approved.");
                return;
            }
            
            // Step 2: Approve the request
            boolean requestApproved = borrowRequestDAO.approveRequest(
                currentRequest.getRequestId(),
                currentLibrarian.getUserId(),
                duration,
                notes
            );
            
            if (!requestApproved) {
                showError(generalMessage, "Failed to approve request. Please try again.");
                return;
            }
            
            // Step 3: Create borrowed book record (issue the book)
            BorrowedBook borrowedBook = new BorrowedBook();
            borrowedBook.setRequestId(currentRequest.getRequestId());
            borrowedBook.setMemberId(currentRequest.getMemberId());
            borrowedBook.setBookId(currentRequest.getBookId());
            borrowedBook.setIssuedBy(currentLibrarian.getUserId());
            borrowedBook.setDueDate(LocalDate.now().plusDays(duration));
            borrowedBook.setStatus("ISSUED");
            borrowedBook.setAllowRenewal(allowRenewal);
            borrowedBook.setNotes(notes);
            
            boolean bookIssued = borrowedBookDAO.issueBook(borrowedBook);
            
            if (!bookIssued) {
                // Rollback: Revert request approval
                borrowRequestDAO.updateStatus(currentRequest.getRequestId(), "PENDING");
                showError(generalMessage, "Failed to issue book. Request not approved.");
                return;
            }
            
            // Success!
            SceneManager.showInfo(
                "Request Approved",
                "Borrow request approved successfully!\n\n" +
                "Book: " + currentRequest.getBook().getTitle() + "\n" +
                "Member: " + currentRequest.getMember().getFullName() + "\n" +
                "Due Date: " + LocalDate.now().plusDays(duration).format(DATE_FORMATTER)
            );
            
            // Trigger callback to refresh parent view
            if (onApprovedCallback != null) {
                onApprovedCallback.run();
            }
            
        } catch (Exception e) {
            System.err.println("Error approving request: " + e.getMessage());
            e.printStackTrace();
            showError(generalMessage, "Error approving request: " + e.getMessage());
        }
    }

    /**
     * Handle cancel button click
     */
    @FXML
    private void handleCancel() {
        // Close the dialog
        cancelButton.getScene().getWindow().hide();
    }

    // ==================== UI Helper Methods ====================
    
    /**
     * Show error message
     */
    private void showError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            errorLabel.setStyle("-fx-text-fill: #FF4444; -fx-font-weight: bold;");
        }
    }

    /**
     * Clear specific error
     */
    private void clearError(Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    /**
     * Clear all errors
     */
    private void clearAllErrors() {
        clearError(durationError);
        clearError(generalMessage);
    }

    // ==================== Getters/Setters for Testing ====================
    
    public BorrowRequest getCurrentRequest() {
        return currentRequest;
    }

    public User getCurrentLibrarian() {
        return currentLibrarian;
    }

    public void setBorrowRequestDAO(BorrowRequestDAO borrowRequestDAO) {
        this.borrowRequestDAO = borrowRequestDAO;
    }

    public void setBorrowedBookDAO(BorrowedBookDAO borrowedBookDAO) {
        this.borrowedBookDAO = borrowedBookDAO;
    }

    public void setBookDAO(BookDAO bookDAO) {
        this.bookDAO = bookDAO;
    }

    /**
     * Get current duration value (for testing)
     */
    public int getDuration() {
        return durationSpinner.getValue();
    }

    /**
     * Get allow renewal status (for testing)
     */
    public boolean isAllowRenewal() {
        return allowRenewalCheckbox.isSelected();
    }

    /**
     * Get notes (for testing)
     */
    public String getNotes() {
        return notesField.getText();
    }
}