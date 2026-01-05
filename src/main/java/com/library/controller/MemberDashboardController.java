package com.library.controller;

import com.library.dao.BookDAO;
import com.library.dao.BorrowRequestDAO;
import com.library.dao.BorrowedBookDAO;
import com.library.model.Book;
import com.library.model.BorrowRequest;
import com.library.model.BorrowedBook;
import com.library.model.User;
import com.library.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class MemberDashboardController implements Initializable {

    // Top bar
    @FXML private Label welcomeLabel;
    @FXML private Button logoutButton;
    
    // Search section
    @FXML private TextField searchField;
    @FXML private ComboBox<String> searchCategoryCombo;
    @FXML private Button searchButton;
    
    // Statistics
    @FXML private Label borrowedCountLabel;
    @FXML private Label pendingRequestsLabel;
    @FXML private Label overdueCountLabel;
    
    // Available books table
    @FXML private TableView<Book> booksTable;
    @FXML private TableColumn<Book, String> isbnColumn;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, String> categoryColumn;
    @FXML private TableColumn<Book, Integer> availabilityColumn;
    @FXML private TableColumn<Book, Void> actionColumn;
    
    // Borrowed books table
    @FXML private TableView<BorrowedBook> borrowedBooksTable;
    @FXML private TableColumn<BorrowedBook, String> borrowedTitleColumn;
    @FXML private TableColumn<BorrowedBook, String> borrowedDateColumn;
    @FXML private TableColumn<BorrowedBook, String> dueDateColumn;
    @FXML private TableColumn<BorrowedBook, String> statusColumn;
    
    @FXML private Button refreshButton;
    
    private User currentUser;
    private BookDAO bookDAO;
    private BorrowRequestDAO borrowRequestDAO;
    private BorrowedBookDAO borrowedBookDAO;
    
    private ObservableList<Book> booksList;
    private ObservableList<BorrowedBook> borrowedBooksList;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize DAOs
        bookDAO = new BookDAO();
        borrowRequestDAO = new BorrowRequestDAO();
        borrowedBookDAO = new BorrowedBookDAO();
        
        // Initialize lists
        booksList = FXCollections.observableArrayList();
        borrowedBooksList = FXCollections.observableArrayList();
        
        // Setup tables
        setupBooksTable();
        setupBorrowedBooksTable();
        
        // Setup category combo
        searchCategoryCombo.setValue("All");
    }

    /**
     * Set the current user (called by SceneManager)
     */
    public void setUser(User user) {
        this.currentUser = user;
        
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getFullName() + "!");
            loadDashboardData();
        }
    }

    /**
     * Load all dashboard data
     */
    private void loadDashboardData() {
        if (currentUser == null) return;
        
        loadStatistics();
        loadAvailableBooks();
        loadBorrowedBooks();
    }

    /**
     * Load statistics
     */
    private void loadStatistics() {
        try {
            // Get borrowed books count (currently issued)
            int borrowedCount = borrowedBookDAO.getActiveBorrowCount(currentUser.getUserId());
            borrowedCountLabel.setText(String.valueOf(borrowedCount));
            
            // Get pending requests count
            int pendingCount = borrowRequestDAO.getPendingRequestCount(currentUser.getUserId());
            pendingRequestsLabel.setText(String.valueOf(pendingCount));
            
            // Get overdue books count
            int overdueCount = borrowedBookDAO.getOverdueCount(currentUser.getUserId());
            overdueCountLabel.setText(String.valueOf(overdueCount));
            
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Setup available books table
     */
    private void setupBooksTable() {
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        availabilityColumn.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        
        // Add action button column
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button requestButton = new Button("Request");
            
            {
                requestButton.setStyle("-fx-background-color: #FF8C00; -fx-text-fill: white; " +
                                     "-fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
                requestButton.setOnAction(event -> {
                    Book book = getTableView().getItems().get(getIndex());
                    handleRequestBook(book);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Book book = getTableView().getItems().get(getIndex());
                    if (book.getAvailableCopies() > 0) {
                        requestButton.setDisable(false);
                        setGraphic(requestButton);
                    } else {
                        requestButton.setDisable(true);
                        requestButton.setText("N/A");
                        setGraphic(requestButton);
                    }
                }
            }
        });
        
        booksTable.setItems(booksList);
    }

    /**
     * Setup borrowed books table
     */
    private void setupBorrowedBooksTable() {
        borrowedTitleColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getBook().getTitle()));
        
        borrowedDateColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getIssueDate().format(DATE_FORMATTER)));
        
        dueDateColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));
        
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Style status column
        statusColumn.setCellFactory(column -> new TableCell<BorrowedBook, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("OVERDUE")) {
                        setStyle("-fx-text-fill: #FF4444; -fx-font-weight: bold;");
                    } else if (item.equalsIgnoreCase("ISSUED")) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        borrowedBooksTable.setItems(borrowedBooksList);
    }

    /**
     * Load available books
     */
    private void loadAvailableBooks() {
        try {
            List<Book> books = bookDAO.getAllAvailableBooks();
            booksList.clear();
            booksList.addAll(books);
        } catch (Exception e) {
            System.err.println("Error loading books: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to load books: " + e.getMessage());
        }
    }

    /**
     * Load borrowed books for current user
     */
    private void loadBorrowedBooks() {
        if (currentUser == null) return;
        
        try {
            List<BorrowedBook> borrowed = borrowedBookDAO.getBorrowedBooksByMember(currentUser.getUserId());
            borrowedBooksList.clear();
            borrowedBooksList.addAll(borrowed);
        } catch (Exception e) {
            System.err.println("Error loading borrowed books: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to load borrowed books: " + e.getMessage());
        }
    }

    /**
     * Handle search button click
     */
    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim();
        String category = searchCategoryCombo.getValue();
        
        try {
            List<Book> books;
            
            if (searchTerm.isEmpty() && "All".equals(category)) {
                // Load all books
                books = bookDAO.getAllAvailableBooks();
            } else if (!searchTerm.isEmpty() && "All".equals(category)) {
                // Search by term only
                books = bookDAO.searchBooks(searchTerm);
            } else if (searchTerm.isEmpty() && !"All".equals(category)) {
                // Filter by category only
                books = bookDAO.getBooksByCategory(category);
            } else {
                // Search by term and category
                books = bookDAO.searchBooksByCategory(searchTerm, category);
            }
            
            booksList.clear();
            booksList.addAll(books);
            
        } catch (Exception e) {
            System.err.println("Error searching books: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Search failed: " + e.getMessage());
        }
    }

    /**
     * Handle book request
     */
    private void handleRequestBook(Book book) {
        if (currentUser == null || book == null) return;
        
        // Confirm request
        boolean confirmed = SceneManager.showConfirmation(
            "Request Book",
            "Do you want to request to borrow:\n\n" +
            "Title: " + book.getTitle() + "\n" +
            "Author: " + book.getAuthor() + "\n\n" +
            "Your request will be reviewed by a librarian."
        );
        
        if (!confirmed) return;
        
        try {
            // Check if user already has a pending request for this book
            if (borrowRequestDAO.hasPendingRequest(currentUser.getUserId(), book.getBookId())) {
                SceneManager.showWarning("Request Exists", 
                    "You already have a pending request for this book.");
                return;
            }
            
            // Create borrow request
            BorrowRequest request = new BorrowRequest();
            request.setMemberId(currentUser.getUserId());
            request.setBookId(book.getBookId());
            request.setStatus("PENDING");
            
            boolean success = borrowRequestDAO.createRequest(request);
            
            if (success) {
                SceneManager.showInfo("Success", 
                    "Book request submitted successfully!\n\n" +
                    "You will be notified once a librarian reviews your request.");
                
                // Refresh statistics
                loadStatistics();
            } else {
                SceneManager.showError("Error", "Failed to submit request. Please try again.");
            }
            
        } catch (Exception e) {
            System.err.println("Error creating request: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to create request: " + e.getMessage());
        }
    }

    /**
     * Handle refresh button click
     */
    @FXML
    private void handleRefresh() {
        loadDashboardData();
        SceneManager.showInfo("Refreshed", "Dashboard data refreshed successfully!");
    }

    /**
     * Handle logout button click
     */
    @FXML
    private void handleLogout() {
        boolean confirmed = SceneManager.showConfirmation(
            "Logout",
            "Are you sure you want to logout?"
        );
        
        if (confirmed) {
            SceneManager.logout();
        }
    }

    // ==================== Getters for Testing ====================
    
    public User getCurrentUser() {
        return currentUser;
    }

    public BookDAO getBookDAO() {
        return bookDAO;
    }

    public void setBookDAO(BookDAO bookDAO) {
        this.bookDAO = bookDAO;
    }

    public BorrowRequestDAO getBorrowRequestDAO() {
        return borrowRequestDAO;
    }

    public void setBorrowRequestDAO(BorrowRequestDAO borrowRequestDAO) {
        this.borrowRequestDAO = borrowRequestDAO;
    }

    public BorrowedBookDAO getBorrowedBookDAO() {
        return borrowedBookDAO;
    }

    public void setBorrowedBookDAO(BorrowedBookDAO borrowedBookDAO) {
        this.borrowedBookDAO = borrowedBookDAO;
    }
}