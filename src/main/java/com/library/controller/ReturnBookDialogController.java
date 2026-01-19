package com.library.controller;

import com.library.dao.BookDAO;
import com.library.dao.BorrowedBookDAO;
import com.library.dao.UserDAO;
import com.library.model.Book;
import com.library.model.BorrowedBook;
import com.library.model.User;
import com.library.util.EmailService;
import com.library.util.EmailTemplateLoader;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ReturnBookDialogController implements Initializable {

    // Search
    @FXML private TextField searchField;
    @FXML private Label searchError;
    @FXML private VBox searchResultsSection;
    @FXML private TableView<BorrowedBook> borrowedBooksTable;
    @FXML private TableColumn<BorrowedBook, String> colBookTitle;
    @FXML private TableColumn<BorrowedBook, String> colMemberName;
    @FXML private TableColumn<BorrowedBook, String> colIssueDate;
    @FXML private TableColumn<BorrowedBook, String> colDueDate;
    @FXML private TableColumn<BorrowedBook, String> colStatus;

    // Borrow Details
    @FXML private Separator detailsSeparator;
    @FXML private VBox borrowDetailsSection;
    @FXML private Label detailBookLabel;
    @FXML private Label detailMemberLabel;
    @FXML private Label detailIssueDateLabel;
    @FXML private Label detailDueDateLabel;
    @FXML private Label detailDaysElapsedLabel;
    @FXML private HBox overdueWarningBox;
    @FXML private Label overdueWarningLabel;

    // Return Parameters
    @FXML private Separator returnParamsSeparator;
    @FXML private VBox returnParametersSection;
    @FXML private TextField returnDateField;
    @FXML private ComboBox<String> conditionComboBox;
    @FXML private VBox lateFeeSection;
    @FXML private TextField lateFeeField;
    @FXML private Label lateFeeHint;
    @FXML private VBox damageFeeSection;
    @FXML private TextField damageFeeField;
    @FXML private VBox totalFeeSection;
    @FXML private TextField totalFeeField;
    @FXML private TextArea returnNotesField;

    // General
    @FXML private Label generalMessage;
    @FXML private Button cancelButton;
    @FXML private Button returnButton;

    private BookDAO bookDAO;
    private UserDAO userDAO;
    private BorrowedBookDAO borrowedBookDAO;

    private BorrowedBook selectedBorrowedBook;
    private Book selectedBook;
    private User selectedMember;
    private User currentLibrarian;
    private Runnable onReturned;

    private ObservableList<BorrowedBook> borrowedBooksList;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final double LATE_FEE_PER_DAY = 1.0; // $1 per day

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bookDAO = new BookDAO();
        userDAO = new UserDAO();
        borrowedBookDAO = new BorrowedBookDAO();

        borrowedBooksList = FXCollections.observableArrayList();

        setupTable();

        // Set default return date to today
        returnDateField.setText(LocalDate.now().format(DATE_FORMATTER));

        // Set default condition
        conditionComboBox.setValue("Good");

        // Add listener to condition combo
        conditionComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            handleConditionChange(newVal);
        });

        // Add listeners to fee fields for total calculation
        lateFeeField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotalFees());
        damageFeeField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotalFees());
    }

    private void setupTable() {
        colBookTitle.setCellValueFactory(cellData -> {
            if (cellData.getValue().getBook() != null) {
                return new SimpleStringProperty(cellData.getValue().getBook().getTitle());
            }
            return new SimpleStringProperty("N/A");
        });

        colMemberName.setCellValueFactory(cellData -> {
            if (cellData.getValue().getMember() != null) {
                return new SimpleStringProperty(cellData.getValue().getMember().getFullName());
            }
            return new SimpleStringProperty("N/A");
        });

        colIssueDate.setCellValueFactory(cellData -> {
            LocalDateTime issueDate = cellData.getValue().getIssueDate();
            if (issueDate != null) {
                return new SimpleStringProperty(issueDate.toLocalDate().format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("N/A");
        });

        colDueDate.setCellValueFactory(cellData -> {
            LocalDate dueDate = cellData.getValue().getDueDate();
            if (dueDate != null) {
                return new SimpleStringProperty(dueDate.format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("N/A");
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Style overdue books
        colDueDate.setCellFactory(column -> new TableCell<BorrowedBook, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    BorrowedBook book = getTableView().getItems().get(getIndex());
                    if (book.getDueDate().isBefore(LocalDate.now())) {
                        setStyle("-fx-text-fill: #FF4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        borrowedBooksTable.setItems(borrowedBooksList);

        // Add selection listener
        borrowedBooksTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    handleBorrowedBookSelected(newSelection);
                }
            }
        );
    }

    public void setLibrarian(User librarian) {
        this.currentLibrarian = librarian;
    }

    public void setOnReturned(Runnable callback) {
        this.onReturned = callback;
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim();

        if (searchTerm.isEmpty()) {
            showSearchError("Please enter search criteria");
            return;
        }

        try {
            clearSearchError();
            borrowedBooksList.clear();
            hideDetails();

            // Try searching by book details first
            List<BorrowedBook> results = borrowedBookDAO.searchActiveBorrowsByBook(searchTerm);

            // If no results, try searching by member
            if (results.isEmpty()) {
                results = borrowedBookDAO.searchActiveBorrowsByMember(searchTerm);
            }

            if (results.isEmpty()) {
                showSearchError("No borrowed books found matching: " + searchTerm);
                return;
            }

            // Load book and member details for each result
            for (BorrowedBook bb : results) {
                bb.setBook(bookDAO.findById(bb.getBookId()));
                bb.setMember(userDAO.findById(bb.getMemberId()));
            }

            borrowedBooksList.addAll(results);
            searchResultsSection.setVisible(true);
            searchResultsSection.setManaged(true);

        } catch (Exception e) {
            System.err.println("Error searching borrowed books: " + e.getMessage());
            e.printStackTrace();
            showSearchError("Error searching: " + e.getMessage());
        }
    }

    private void handleBorrowedBookSelected(BorrowedBook borrowedBook) {
        try {
            selectedBorrowedBook = borrowedBook;
            selectedBook = borrowedBook.getBook();
            selectedMember = borrowedBook.getMember();

            displayBorrowDetails(borrowedBook);
            showReturnParameters();
            calculateLateFee();

            returnButton.setDisable(false);

        } catch (Exception e) {
            System.err.println("Error handling selection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayBorrowDetails(BorrowedBook borrowedBook) {
        detailBookLabel.setText(selectedBook.getTitle() + " by " + selectedBook.getAuthor());
        detailMemberLabel.setText(selectedMember.getFullName() + " (" + selectedMember.getMemberId() + ")");
        detailIssueDateLabel.setText(borrowedBook.getIssueDate().toLocalDate().format(DATE_FORMATTER));
        detailDueDateLabel.setText(borrowedBook.getDueDate().format(DATE_FORMATTER));

        // Calculate days elapsed
        long daysElapsed = ChronoUnit.DAYS.between(
            borrowedBook.getIssueDate().toLocalDate(),
            LocalDate.now()
        );
        detailDaysElapsedLabel.setText(daysElapsed + " days");

        // Check if overdue
        if (borrowedBook.getDueDate().isBefore(LocalDate.now())) {
            long daysOverdue = ChronoUnit.DAYS.between(borrowedBook.getDueDate(), LocalDate.now());
            overdueWarningLabel.setText("Book is " + daysOverdue + " day(s) overdue!");
            overdueWarningBox.setVisible(true);
            overdueWarningBox.setManaged(true);
        } else {
            overdueWarningBox.setVisible(false);
            overdueWarningBox.setManaged(false);
        }

        detailsSeparator.setVisible(true);
        detailsSeparator.setManaged(true);
        borrowDetailsSection.setVisible(true);
        borrowDetailsSection.setManaged(true);
    }

    private void showReturnParameters() {
        returnParamsSeparator.setVisible(true);
        returnParamsSeparator.setManaged(true);
        returnParametersSection.setVisible(true);
        returnParametersSection.setManaged(true);
    }

    private void hideDetails() {
        detailsSeparator.setVisible(false);
        detailsSeparator.setManaged(false);
        borrowDetailsSection.setVisible(false);
        borrowDetailsSection.setManaged(false);
        returnParamsSeparator.setVisible(false);
        returnParamsSeparator.setManaged(false);
        returnParametersSection.setVisible(false);
        returnParametersSection.setManaged(false);
        searchResultsSection.setVisible(false);
        searchResultsSection.setManaged(false);
    }

    private void calculateLateFee() {
        if (selectedBorrowedBook == null) return;

        LocalDate dueDate = selectedBorrowedBook.getDueDate();
        LocalDate today = LocalDate.now();

        if (today.isAfter(dueDate)) {
            long daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
            double lateFee = daysOverdue * LATE_FEE_PER_DAY;

            lateFeeField.setText(String.format("%.2f", lateFee));
            lateFeeHint.setText("💡 " + daysOverdue + " day(s) overdue × $" + LATE_FEE_PER_DAY + "/day");
            
            lateFeeSection.setVisible(true);
            lateFeeSection.setManaged(true);
            totalFeeSection.setVisible(true);
            totalFeeSection.setManaged(true);
        } else {
            lateFeeField.setText("0.00");
            lateFeeSection.setVisible(false);
            lateFeeSection.setManaged(false);
        }

        calculateTotalFees();
    }

    private void handleConditionChange(String condition) {
        if ("Damaged".equals(condition) || "Lost".equals(condition)) {
            damageFeeSection.setVisible(true);
            damageFeeSection.setManaged(true);
            totalFeeSection.setVisible(true);
            totalFeeSection.setManaged(true);
            
            // Suggest replacement fee based on book price (if available)
            if (selectedBook != null && selectedBook.getPrice() != null) {
                damageFeeField.setText(String.format("%.2f", selectedBook.getPrice()));
            } else {
                damageFeeField.setText("0.00");
            }
        } else {
            damageFeeField.setText("0.00");
            damageFeeSection.setVisible(false);
            damageFeeSection.setManaged(false);
        }

        calculateTotalFees();
    }

    private void calculateTotalFees() {
        try {
            double lateFee = parseDouble(lateFeeField.getText());
            double damageFee = parseDouble(damageFeeField.getText());
            double total = lateFee + damageFee;

            totalFeeField.setText(String.format("$%.2f", total));

            if (total > 0) {
                totalFeeSection.setVisible(true);
                totalFeeSection.setManaged(true);
            } else {
                totalFeeSection.setVisible(false);
                totalFeeSection.setManaged(false);
            }

        } catch (Exception e) {
            totalFeeField.setText("$0.00");
        }
    }

    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text.trim().replace("$", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    @FXML
    private void handleReturnBook() {
        if (selectedBorrowedBook == null) {
            showGeneralError("Please select a book to return");
            return;
        }

        try {
            String condition = conditionComboBox.getValue();
            double lateFee = parseDouble(lateFeeField.getText());
            double damageFee = parseDouble(damageFeeField.getText());
            double totalFees = lateFee + damageFee;
            String notes = returnNotesField.getText().trim();

            // Update borrowed book record
            boolean success = borrowedBookDAO.returnBook(
                    selectedBorrowedBook.getBorrowId(),
                    currentLibrarian != null ? currentLibrarian.getUserId() : 0
            );

            if (success) {
                showGeneralSuccess("Book returned successfully!");

                // Send email in background
                sendReturnConfirmationEmail(condition, totalFees);

                // Callback
                if (onReturned != null) {
                    Platform.runLater(onReturned);
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
                showGeneralError("Failed to process return. Please try again.");
            }

        } catch (Exception e) {
            System.err.println("Error processing return: " + e.getMessage());
            e.printStackTrace();
            showGeneralError("Error: " + e.getMessage());
        }
    }

    private void sendReturnConfirmationEmail(String condition, double totalFees) {
        new Thread(() -> {
            try {
                // Load template: book-returned.html
                String template = EmailTemplateLoader.loadTemplate("book-returned.html");

                LocalDate returnDate = LocalDate.now();
                LocalDate dueDate = selectedBorrowedBook.getDueDate();
                long daysOverdue = dueDate.isBefore(returnDate) ? 
                    ChronoUnit.DAYS.between(dueDate, returnDate) : 0;

                Map<String, String> values = Map.ofEntries(
                        Map.entry("logoUrl", "https://github.com/Lushomo53/Library-Management-System/blob/master/src/main/resources/images/logo.jpg?raw=true"),
                        Map.entry("memberName", selectedMember.getFullName()),
                        Map.entry("bookTitle", selectedBook.getTitle()),
                        Map.entry("author", selectedBook.getAuthor()),
                        Map.entry("isbn", selectedBook.getIsbn()),
                        Map.entry("returnDate", returnDate.toString()),
                        Map.entry("dueDate", dueDate.toString()),
                        Map.entry("condition", condition),
                        Map.entry("daysOverdue", String.valueOf(daysOverdue)),
                        Map.entry("totalFees", String.format("%.2f", totalFees)),
                        Map.entry("libraryName", "Library Management System")
                );

                String html = EmailTemplateLoader.render(template, values);

                EmailService.sendHtmlEmail(
                    selectedMember.getEmail(),
                    "Book Return Confirmation – " + selectedBook.getTitle(),
                    html
                );

            } catch (Exception e) {
                System.err.println("Failed to send return confirmation email");
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        cancelButton.getScene().getWindow().hide();
    }

    private void showSearchError(String message) {
        searchError.setText(message);
        searchError.setVisible(true);
        searchError.setManaged(true);
    }

    private void clearSearchError() {
        searchError.setVisible(false);
        searchError.setManaged(false);
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