package com.library.controller;

import com.library.dao.BookDAO;
import com.library.dao.BorrowRequestDAO;
import com.library.dao.BorrowedBookDAO;
import com.library.dao.UserDAO;
import com.library.model.Book;
import com.library.model.BorrowRequest;
import com.library.model.BorrowedBook;
import com.library.model.User;
import com.library.util.EmailService;
import com.library.util.EmailTemplateLoader;
import com.library.util.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class IssueBookDialogController implements Initializable {

    // Book Search
    @FXML private TextField bookSearchField;
    @FXML private Label bookSearchError;
    @FXML private VBox bookDetailsSection;
    @FXML private Label bookIsbnLabel;
    @FXML private Label bookTitleLabel;
    @FXML private Label bookAuthorLabel;
    @FXML private Label bookAvailableLabel;

    // Member Search
    @FXML private TextField memberSearchField;
    @FXML private Label memberSearchError;
    @FXML private VBox memberDetailsSection;
    @FXML private Label memberIdLabel;
    @FXML private Label memberNameLabel;
    @FXML private Label memberEmailLabel;
    @FXML private Label memberStatusLabel;
    @FXML private HBox overdueWarningBox;
    @FXML private Label overdueWarningLabel;
    @FXML private HBox noOverdueBox;

    // Issue Parameters
    @FXML private VBox issueParametersSection;
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private TextField dueDateField;
    @FXML private TextArea notesField;

    // General
    @FXML private Label generalMessage;
    @FXML private Button cancelButton;
    @FXML private Button issueButton;

    private BookDAO bookDAO;
    private UserDAO userDAO;
    private BorrowedBookDAO borrowedBookDAO;

    private Book selectedBook;
    private User selectedMember;
    private User currentLibrarian;
    private Runnable onIssued;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bookDAO = new BookDAO();
        userDAO = new UserDAO();
        borrowedBookDAO = new BorrowedBookDAO();

        // Initialize spinner
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 90, 14);
        durationSpinner.setValueFactory(valueFactory);
        
        // Add listener to calculate due date when duration changes
        durationSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            calculateDueDate();
        });

        // Initial due date calculation
        calculateDueDate();
    }

    public void setLibrarian(User librarian) {
        this.currentLibrarian = librarian;
    }

    public void setOnIssued(Runnable callback) {
        this.onIssued = callback;
    }

    @FXML
    private void handleSearchBook() {
        String searchTerm = bookSearchField.getText().trim();
        
        if (searchTerm.isEmpty()) {
            showBookError("Please enter ISBN or book title");
            return;
        }

        try {
            // Clear previous selection
            selectedBook = null;
            hideBookDetails();
            clearBookError();

            // Search for book
            List<Book> books = bookDAO.searchBooks(searchTerm);

            if (books.isEmpty()) {
                showBookError("No books found matching: " + searchTerm);
                return;
            }

            // If multiple results, take the first exact ISBN match or first result
            Book foundBook = books.stream()
                .filter(b -> b.getIsbn().equalsIgnoreCase(searchTerm))
                .findFirst()
                .orElse(books.get(0));

            // Check availability
            if (foundBook.getAvailableCopies() <= 0) {
                showBookError("Book is currently unavailable (0 copies available)");
                displayBookDetails(foundBook);
                return;
            }

            selectedBook = foundBook;
            displayBookDetails(foundBook);
            checkCanIssue();

        } catch (Exception e) {
            System.err.println("Error searching book: " + e.getMessage());
            e.printStackTrace();
            showBookError("Error searching book: " + e.getMessage());
        }
    }

    @FXML
    private void handleSearchMember() {
        String searchTerm = memberSearchField.getText().trim();
        
        if (searchTerm.isEmpty()) {
            showMemberError("Please enter Member ID or Username");
            return;
        }

        try {
            // Clear previous selection
            selectedMember = null;
            hideMemberDetails();
            clearMemberError();

            // Search for member
            User member = userDAO.findByMemberId(searchTerm);
            
            if (member == null) {
                member = userDAO.findByUsername(searchTerm);
            }

            if (member == null) {
                showMemberError("No member found with ID/Username: " + searchTerm);
                return;
            }

            // Check if member role is MEMBER
            if (!"MEMBER".equalsIgnoreCase(member.getRole())) {
                showMemberError("User is not a member");
                return;
            }

            // Check if member is active
            if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
                showMemberError("Member account is not active");
                displayMemberDetails(member);
                return;
            }

            selectedMember = member;
            displayMemberDetails(member);
            checkOverdueBooks(member);
            checkCanIssue();

        } catch (Exception e) {
            System.err.println("Error searching member: " + e.getMessage());
            e.printStackTrace();
            showMemberError("Error searching member: " + e.getMessage());
        }
    }

    private void displayBookDetails(Book book) {
        bookIsbnLabel.setText(book.getIsbn());
        bookTitleLabel.setText(book.getTitle());
        bookAuthorLabel.setText(book.getAuthor());
        
        String availableText = book.getAvailableCopies() + " / " + book.getTotalCopies();
        bookAvailableLabel.setText(availableText);
        
        // Color code availability
        if (book.getAvailableCopies() <= 0) {
            bookAvailableLabel.setStyle("-fx-text-fill: #FF4444; -fx-font-weight: bold;");
        } else if (book.getAvailableCopies() < 3) {
            bookAvailableLabel.setStyle("-fx-text-fill: #FF8C00; -fx-font-weight: bold;");
        } else {
            bookAvailableLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        }

        bookDetailsSection.setVisible(true);
        bookDetailsSection.setManaged(true);
    }

    private void hideBookDetails() {
        bookDetailsSection.setVisible(false);
        bookDetailsSection.setManaged(false);
    }

    private void displayMemberDetails(User member) {
        memberIdLabel.setText(member.getMemberId());
        memberNameLabel.setText(member.getFullName());
        memberEmailLabel.setText(member.getEmail());
        memberStatusLabel.setText(member.getStatus());
        
        // Color code status
        if ("ACTIVE".equalsIgnoreCase(member.getStatus())) {
            memberStatusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        } else {
            memberStatusLabel.setStyle("-fx-text-fill: #FF4444; -fx-font-weight: bold;");
        }

        memberDetailsSection.setVisible(true);
        memberDetailsSection.setManaged(true);
    }

    private void hideMemberDetails() {
        memberDetailsSection.setVisible(false);
        memberDetailsSection.setManaged(false);
        overdueWarningBox.setVisible(false);
        overdueWarningBox.setManaged(false);
        noOverdueBox.setVisible(false);
        noOverdueBox.setManaged(false);
    }

    private void checkOverdueBooks(User member) {
        try {
            List<BorrowedBook> overdueBooks = borrowedBookDAO.getOverdueBooksByMember(member.getUserId());

            if (!overdueBooks.isEmpty()) {
                overdueWarningLabel.setText(
                    "Member has " + overdueBooks.size() + " overdue book(s)!"
                );
                overdueWarningBox.setVisible(true);
                overdueWarningBox.setManaged(true);
                noOverdueBox.setVisible(false);
                noOverdueBox.setManaged(false);
            } else {
                overdueWarningBox.setVisible(false);
                overdueWarningBox.setManaged(false);
                noOverdueBox.setVisible(true);
                noOverdueBox.setManaged(true);
            }

        } catch (Exception e) {
            System.err.println("Error checking overdue books: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkCanIssue() {
        boolean canIssue = selectedBook != null && 
                          selectedMember != null && 
                          selectedBook.getAvailableCopies() > 0 &&
                          "ACTIVE".equalsIgnoreCase(selectedMember.getStatus());

        issueButton.setDisable(!canIssue);
        issueParametersSection.setVisible(canIssue);
        issueParametersSection.setManaged(canIssue);
    }

    @FXML
    private void calculateDueDate() {
        int days = durationSpinner.getValue();
        LocalDate dueDate = LocalDate.now().plusDays(days);
        dueDateField.setText(dueDate.format(DATE_FORMATTER));
    }

    @FXML
    private void handleIssueBook() {
        if (selectedBook == null || selectedMember == null) {
            showGeneralError("Please select both book and member");
            return;
        }

        // Validate
        if (selectedBook.getAvailableCopies() <= 0) {
            showGeneralError("Book is not available");
            return;
        }

        if (!"ACTIVE".equalsIgnoreCase(selectedMember.getStatus())) {
            showGeneralError("Member is not active");
            return;
        }

        try {
            int duration = durationSpinner.getValue();
            String notes = notesField.getText().trim();

            // Create a BorrowRequest for in-person issuance
            BorrowRequest request = new BorrowRequest(selectedMember.getUserId(), selectedBook.getBookId());
            request.setStatus("APPROVED"); // automatically approved
            request.setApprovedBy(currentLibrarian != null ? currentLibrarian.getUserId() : null);
            request.setApprovedDate(LocalDateTime.now());
            request.setBorrowDurationDays(duration);
            request.setNotes(notes.isEmpty() ? null : notes);

            // Save request and get generated requestId
            new BorrowRequestDAO().createRequest(request);

            // 2️⃣ Create BorrowedBook and link to request
            BorrowedBook borrowedBook = new BorrowedBook();
            borrowedBook.setMemberId(selectedMember.getUserId());
            borrowedBook.setBookId(selectedBook.getBookId());
            borrowedBook.setRequestId(request.getRequestId());
            borrowedBook.setIssueDate(LocalDateTime.now());
            borrowedBook.setDueDate(LocalDate.now().plusDays(duration));
            borrowedBook.setStatus("ISSUED");
            borrowedBook.setNotes(notes.isEmpty() ? null : notes);
            if (currentLibrarian != null) {
                borrowedBook.setIssuedBy(currentLibrarian.getUserId());
            }

            // Save to database
            boolean success = borrowedBookDAO.issueBook(borrowedBook);

            if (success) {
                showGeneralSuccess("Book issued successfully!");

                // Send email in background
                sendIssueConfirmationEmail(borrowedBook, duration);

                // Callback
                if (onIssued != null) {
                    Platform.runLater(onIssued);
                }

                // Close dialog after short delay
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(800);
                        handleCancel();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

            } else {
                showGeneralError("Failed to issue book. Please try again.");
            }

        } catch (Exception e) {
            System.err.println("Error issuing book: " + e.getMessage());
            e.printStackTrace();
            showGeneralError("Error: " + e.getMessage());
        }
    }

    private void sendIssueConfirmationEmail(BorrowedBook borrowedBook, int duration) {
        new Thread(() -> {
            try {
                // Load template: book-issued.html
                String template = EmailTemplateLoader.loadTemplate("book-issued.html");

                LocalDate issueDate = borrowedBook.getIssueDate().toLocalDate();
                LocalDate dueDate = borrowedBook.getDueDate();

                Map<String, String> values = Map.of(
                    "logoUrl", "https://github.com/Lushomo53/Library-Management-System/blob/master/src/main/resources/images/logo.jpg?raw=true",
                    "memberName", selectedMember.getFullName(),
                    "bookTitle", selectedBook.getTitle(),
                    "author", selectedBook.getAuthor(),
                    "isbn", selectedBook.getIsbn(),
                    "issueDate", issueDate.toString(),
                    "dueDate", dueDate.toString(),
                    "borrowDays", String.valueOf(duration),
                    "notes", borrowedBook.getNotes() != null ? borrowedBook.getNotes() : "None",
                    "libraryName", "Library Management System"
                );

                String html = EmailTemplateLoader.render(template, values);

                EmailService.sendHtmlEmail(
                    selectedMember.getEmail(),
                    "Book Issued – " + selectedBook.getTitle(),
                    html
                );

            } catch (Exception e) {
                System.err.println("Failed to send book issue email");
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        cancelButton.getScene().getWindow().hide();
    }

    // Error/Success handling methods
    private void showBookError(String message) {
        bookSearchError.setText(message);
        bookSearchError.setVisible(true);
        bookSearchError.setManaged(true);
    }

    private void clearBookError() {
        bookSearchError.setVisible(false);
        bookSearchError.setManaged(false);
    }

    private void showMemberError(String message) {
        memberSearchError.setText(message);
        memberSearchError.setVisible(true);
        memberSearchError.setManaged(true);
    }

    private void clearMemberError() {
        memberSearchError.setVisible(false);
        memberSearchError.setManaged(false);
    }

    private void showGeneralError(String message) {
        generalMessage.setText(message);
        generalMessage.setStyle("-fx-text-fill: #FF4444; -fx-background-color: rgba(255, 68, 68, 0.1); -fx-padding: 10; -fx-background-radius: 6;");
        generalMessage.setVisible(true);
        generalMessage.setManaged(true);
    }

    private void showGeneralSuccess(String message) {
        generalMessage.setText(message);
        generalMessage.setStyle("-fx-text-fill: #4CAF50; -fx-background-color: rgba(76, 175, 80, 0.1); -fx-padding: 10; -fx-background-radius: 6;");
        generalMessage.setVisible(true);
        generalMessage.setManaged(true);
    }
}