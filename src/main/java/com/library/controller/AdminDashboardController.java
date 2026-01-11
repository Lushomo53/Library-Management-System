package com.library.controller;

import com.library.dao.BookDAO;
import com.library.dao.BorrowedBookDAO;
import com.library.dao.UserDAO;
import com.library.model.Book;
import com.library.model.User;
import com.library.util.EmailService;
import com.library.util.EmailTemplateLoader;
import com.library.util.SceneManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    // Top bar
    @FXML private Label welcomeLabel;
    @FXML private Button logoutButton;
    
    // Sidebar navigation
    @FXML private Button stockTabButton;
    @FXML private Button librariansTabButton;
    @FXML private Button membersTabButton;
    @FXML private Button reportsTabButton;
    
    // Content views
    @FXML private StackPane contentPane;
    @FXML private VBox stockView;
    @FXML private VBox librariansView;
    @FXML private VBox membersView;
    @FXML private VBox reportsView;
    
    // ==================== Stock Management View ====================
    @FXML private Label totalBooksLabel;
    @FXML private Label booksIssuedLabel;
    @FXML private Label booksAvailableLabel;
    @FXML private Label lowStockLabel;
    
    @FXML private TextField stockSearchField;
    @FXML private TableView<Book> stockTable;
    @FXML private TableColumn<Book, String> stockIsbnColumn;
    @FXML private TableColumn<Book, String> stockTitleColumn;
    @FXML private TableColumn<Book, String> stockAuthorColumn;
    @FXML private TableColumn<Book, String> stockCategoryColumn;
    @FXML private TableColumn<Book, Integer> stockTotalColumn;
    @FXML private TableColumn<Book, Integer> stockAvailableColumn;
    @FXML private TableColumn<Book, Void> stockActionColumn;
    
    // ==================== Librarians View ====================
    @FXML private Label totalLibrariansLabel;
    @FXML private Label activeLibrariansLabel;
    
    @FXML private TextField librarianSearchField;
    @FXML private TableView<User> librariansTable;
    @FXML private TableColumn<User, String> libIdColumn;
    @FXML private TableColumn<User, String> libNameColumn;
    @FXML private TableColumn<User, String> libEmailColumn;
    @FXML private TableColumn<User, String> libPhoneColumn;
    @FXML private TableColumn<User, String> libJoinDateColumn;
    @FXML private TableColumn<User, String> libStatusColumn;
    @FXML private TableColumn<User, Void> libActionColumn;
    
    // ==================== Members View ====================
    @FXML private Label totalMembersLabel;
    @FXML private Label activeMembersLabel;
    @FXML private Label pendingApplicationsLabel;
    
    @FXML private TextField memberSearchField;
    @FXML private TableView<User> adminMembersTable;
    @FXML private TableColumn<User, String> adminMemberIdColumn;
    @FXML private TableColumn<User, String> adminMemberNameColumn;
    @FXML private TableColumn<User, String> adminMemberEmailColumn;
    @FXML private TableColumn<User, String> adminMemberPhoneColumn;
    @FXML private TableColumn<User, String> adminMemberJoinDateColumn;
    @FXML private TableColumn<User, Integer> adminMemberBooksColumn;
    @FXML private TableColumn<User, String> adminMemberStatusColumn;
    @FXML private TableColumn<User, Void> adminMemberActionColumn;
    
    // ==================== Reports View ====================
    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private DatePicker reportStartDate;
    @FXML private DatePicker reportEndDate;
    @FXML private Label totalTransactionsLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label systemUptimeLabel;
    @FXML private Label reportStatusLabel;
    @FXML private TableView<ObservableList<String>> reportDataTable;
    @FXML private TableColumn<ObservableList<String>, String> reportCol1;
    @FXML private TableColumn<ObservableList<String>, String> reportCol2;
    @FXML private TableColumn<ObservableList<String>, String> reportCol3;
    @FXML private TableColumn<ObservableList<String>, String> reportCol4;
    @FXML private TableColumn<ObservableList<String>, String> reportCol5;
    
    private User currentUser;
    private BookDAO bookDAO;
    private UserDAO userDAO;
    private BorrowedBookDAO borrowedBookDAO;
    
    private ObservableList<Book> stockList;
    private ObservableList<User> librariansList;
    private ObservableList<User> membersList;
    private ObservableList<ObservableList<String>> reportDataList;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize DAOs
        bookDAO = new BookDAO();
        userDAO = new UserDAO();
        borrowedBookDAO = new BorrowedBookDAO();
        
        // Initialize lists
        stockList = FXCollections.observableArrayList();
        librariansList = FXCollections.observableArrayList();
        membersList = FXCollections.observableArrayList();
        reportDataList = FXCollections.observableArrayList();
        
        // Setup tables
        setupStockTable();
        setupLibrariansTable();
        setupMembersTable();
        setupReportsTable();
        
        // Set default view
        switchToStock();
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
        loadStockData();
        loadLibrariansData();
        loadMembersData();
    }

    // ==================== Navigation Methods ====================
    
    @FXML
    private void switchToStock() {
        setActiveView(stockView);
        setActiveNavButton(stockTabButton);
        loadStockData();
    }
    
    @FXML
    private void switchToLibrarians() {
        setActiveView(librariansView);
        setActiveNavButton(librariansTabButton);
        loadLibrariansData();
    }
    
    @FXML
    private void switchToMembers() {
        setActiveView(membersView);
        setActiveNavButton(membersTabButton);
        loadMembersData();
    }
    
    @FXML
    private void switchToReports() {
        setActiveView(reportsView);
        setActiveNavButton(reportsTabButton);
        loadReportsData();
    }
    
    private void setActiveView(VBox activeView) {
        stockView.setVisible(false);
        librariansView.setVisible(false);
        membersView.setVisible(false);
        reportsView.setVisible(false);
        activeView.setVisible(true);
    }
    
    private void setActiveNavButton(Button activeButton) {
        // Reset all buttons
        stockTabButton.getStyleClass().removeAll("nav-button-active");
        librariansTabButton.getStyleClass().removeAll("nav-button-active");
        membersTabButton.getStyleClass().removeAll("nav-button-active");
        reportsTabButton.getStyleClass().removeAll("nav-button-active");
        
        stockTabButton.getStyleClass().add("nav-button");
        librariansTabButton.getStyleClass().add("nav-button");
        membersTabButton.getStyleClass().add("nav-button");
        reportsTabButton.getStyleClass().add("nav-button");
        
        // Set active button
        activeButton.getStyleClass().removeAll("nav-button");
        activeButton.getStyleClass().add("nav-button-active");
    }

    // ==================== Stock Management Methods ====================
    
    private void setupStockTable() {
        stockIsbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        stockTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        stockAuthorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        stockCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        stockTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalCopies"));
        stockAvailableColumn.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        
        // Highlight low stock
        stockAvailableColumn.setCellFactory(column -> new TableCell<Book, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    if (item < 3) {
                        setStyle("-fx-text-fill: #FF4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        // Add action buttons
        stockActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            
            {
                editButton.setStyle("-fx-background-color: #FF8C00; -fx-text-fill: white; " +
                                  "-fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 5 10; -fx-font-size: 11px;");
                deleteButton.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; " +
                                    "-fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 5 10; -fx-font-size: 11px;");
                
                editButton.setOnAction(event -> {
                    Book book = getTableView().getItems().get(getIndex());
                    handleEditBook(book);
                });
                
                deleteButton.setOnAction(event -> {
                    Book book = getTableView().getItems().get(getIndex());
                    handleDeleteBook(book);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(5, editButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });
        
        stockTable.setItems(stockList);
    }
    
    private void loadStockData() {
        try {
            // Load statistics
            int totalBooks = bookDAO.getTotalBooksCount();
            totalBooksLabel.setText(String.valueOf(totalBooks));
            
            int borrowed = bookDAO.getBorrowedBooksCount();
            booksIssuedLabel.setText(String.valueOf(borrowed));
            
            int available = bookDAO.getAvailableBooksCount();
            booksAvailableLabel.setText(String.valueOf(available));
            
            int lowStock = bookDAO.getLowStockBooks().size();
            lowStockLabel.setText(String.valueOf(lowStock));
            
            // Load books
            List<Book> books = bookDAO.getAllBooks();
            stockList.clear();
            stockList.addAll(books);
            
        } catch (Exception e) {
            System.err.println("Error loading stock data: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to load stock data: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleAddBook() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddBookDialog.fxml"));
            Parent root = loader.load();
            
            AddBookDialogController controller = loader.getController();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Book");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(stockTabButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            
            // Set callback
            controller.setOnBookAdded(() -> {
                loadStockData();
                dialogStage.close();
            });
            
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            System.err.println("Error opening add book dialog: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to open add book dialog: " + e.getMessage());
        }
    }
    
    private void handleEditBook(Book book) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddBookDialog.fxml"));
            Parent root = loader.load();
            
            AddBookDialogController controller = loader.getController();
            controller.setBook(book); // Set book for editing
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Book");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(stockTabButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            
            // Set callback
            controller.setOnBookAdded(() -> {
                loadStockData();
                dialogStage.close();
            });
            
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            System.err.println("Error opening edit book dialog: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to open edit book dialog: " + e.getMessage());
        }
    }
    
    private void handleDeleteBook(Book book) {
        // Check if book is currently borrowed
        int borrowed = book.getTotalCopies() - book.getAvailableCopies();
        if (borrowed > 0) {
            SceneManager.showWarning("Cannot Delete", 
                "This book has " + borrowed + " copies currently borrowed. " +
                "Cannot delete until all copies are returned.");
            return;
        }
        
        boolean confirmed = SceneManager.showConfirmation(
            "Delete Book",
            "Are you sure you want to delete this book?\n\n" +
            "Title: " + book.getTitle() + "\n" +
            "Author: " + book.getAuthor() + "\n" +
            "ISBN: " + book.getIsbn() + "\n\n" +
            "This action cannot be undone."
        );
        
        if (!confirmed) return;
        
        try {
            boolean success = bookDAO.deleteBook(book.getBookId());
            
            if (success) {
                SceneManager.showInfo("Success", "Book deleted successfully!");
                loadStockData();
            } else {
                SceneManager.showError("Error", "Failed to delete book.");
            }
            
        } catch (Exception e) {
            System.err.println("Error deleting book: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to delete book: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleStockSearch() {
        String searchTerm = stockSearchField.getText().trim();
        
        try {
            List<Book> books;
            if (searchTerm.isEmpty()) {
                books = bookDAO.getAllBooks();
            } else {
                books = bookDAO.searchBooks(searchTerm);
            }
            
            stockList.clear();
            stockList.addAll(books);
            
        } catch (Exception e) {
            System.err.println("Error searching stock: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Search failed: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRefreshStock() {
        loadStockData();
        SceneManager.showInfo("Refreshed", "Stock data refreshed successfully!");
    }

    // ==================== Librarians Management Methods ====================
    
    private void setupLibrariansTable() {
        libIdColumn.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        libNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        libEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        libPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        
        libJoinDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(
                    cellData.getValue().getCreatedAt().format(DATE_FORMATTER)
                );
            }
            return new SimpleStringProperty("N/A");
        });
        
        libStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Style status column
        libStatusColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("ACTIVE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #FF4444; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Add action buttons
        libActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deactivateButton = new Button("Deactivate");
            
            {
                editButton.setStyle("-fx-background-color: #FF8C00; -fx-text-fill: white; " +
                                  "-fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 5 10; -fx-font-size: 11px;");
                deactivateButton.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; " +
                                        "-fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 5 10; -fx-font-size: 11px;");
                
                editButton.setOnAction(event -> {
                    User librarian = getTableView().getItems().get(getIndex());
                    handleEditLibrarian(librarian);
                });
                
                deactivateButton.setOnAction(event -> {
                    User librarian = getTableView().getItems().get(getIndex());
                    handleDeactivateLibrarian(librarian);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User librarian = getTableView().getItems().get(getIndex());
                    if ("ACTIVE".equalsIgnoreCase(librarian.getStatus())) {
                        javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(5, editButton, deactivateButton);
                        setGraphic(buttons);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        
        librariansTable.setItems(librariansList);
    }
    
    private void loadLibrariansData() {
        try {
            List<User> librarians = userDAO.findByRole("LIBRARIAN");
            
            // Calculate statistics
            int total = librarians.size();
            int active = (int) librarians.stream()
                .filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus()))
                .count();
            
            totalLibrariansLabel.setText(String.valueOf(total));
            activeLibrariansLabel.setText(String.valueOf(active));
            
            librariansList.clear();
            librariansList.addAll(librarians);
            
        } catch (Exception e) {
            System.err.println("Error loading librarians data: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to load librarians data: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleAddLibrarian() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddLibrarianDialog.fxml"));
            Parent root = loader.load();
            
            AddLibrarianDialogController controller = loader.getController();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Register New Librarian");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(librariansTabButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            
            // Set callback
            controller.setOnLibrarianAdded(() -> {
                loadLibrariansData();
                dialogStage.close();
            });
            
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            System.err.println("Error opening add librarian dialog: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to open add librarian dialog: " + e.getMessage());
        }
    }
    
    private void handleEditLibrarian(User librarian) {
        SceneManager.showInfo("Edit Librarian", 
            "Edit librarian functionality - Opens librarian edit dialog\n" +
            "Librarian: " + librarian.getFullName());
        // TODO: Implement edit librarian dialog
    }
    
    private void handleDeactivateLibrarian(User librarian) {
        boolean confirmed = SceneManager.showConfirmation(
            "Deactivate Librarian",
            "Are you sure you want to deactivate this librarian?\n\n" +
            "Name: " + librarian.getFullName() + "\n" +
            "Employee ID: " + librarian.getEmployeeId()
        );
        
        if (!confirmed) return;
        
        try {
            boolean success = userDAO.updateStatus(librarian.getUserId(), "INACTIVE");
            
            if (success) {
                SceneManager.showInfo("Success", "Librarian deactivated successfully!");
                loadLibrariansData();
            } else {
                SceneManager.showError("Error", "Failed to deactivate librarian.");
            }
            
        } catch (Exception e) {
            System.err.println("Error deactivating librarian: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to deactivate librarian: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleLibrarianSearch() {
        String searchTerm = librarianSearchField.getText().trim();
        
        try {
            List<User> librarians;
            if (searchTerm.isEmpty()) {
                librarians = userDAO.findByRole("LIBRARIAN");
            } else {
                librarians = userDAO.searchUsers(searchTerm, "LIBRARIAN");
            }
            
            librariansList.clear();
            librariansList.addAll(librarians);
            
        } catch (Exception e) {
            System.err.println("Error searching librarians: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Search failed: " + e.getMessage());
        }
    }

    // ==================== Members Management Methods ====================
    
    private void setupMembersTable() {
        adminMemberIdColumn.setCellValueFactory(new PropertyValueFactory<>("memberId"));
        adminMemberNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        adminMemberEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        adminMemberPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        
        adminMemberJoinDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(
                    cellData.getValue().getCreatedAt().format(DATE_FORMATTER)
                );
            }
            return new SimpleStringProperty("N/A");
        });
        
        adminMemberBooksColumn.setCellValueFactory(cellData -> {
            try {
                int count = borrowedBookDAO.getActiveBorrowCount(cellData.getValue().getUserId());
                return new SimpleIntegerProperty(count).asObject();
            } catch (Exception e) {
                return new SimpleIntegerProperty(0).asObject();
            }
        });
        
        adminMemberStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Style status column
        adminMemberStatusColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("ACTIVE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else if ("INACTIVE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #FF4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #FF8C00; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Add action buttons
        adminMemberActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button approveButton = new Button("Approve");
            private final Button revokeButton = new Button("Revoke");
            
            {
                approveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                                     "-fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 5 10; -fx-font-size: 11px;");
                revokeButton.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; " +
                                    "-fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 5 10; -fx-font-size: 11px;");
                
                approveButton.setOnAction(event -> {
                    User member = getTableView().getItems().get(getIndex());
                    handleApproveMember(member);
                });
                
                revokeButton.setOnAction(event -> {
                    User member = getTableView().getItems().get(getIndex());
                    handleRevokeMember(member);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User member = getTableView().getItems().get(getIndex());
                    if ("PENDING".equalsIgnoreCase(member.getStatus())) {
                        setGraphic(approveButton);
                    } else if ("ACTIVE".equalsIgnoreCase(member.getStatus())) {
                        setGraphic(revokeButton);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        
        adminMembersTable.setItems(membersList);
    }
    
    private void loadMembersData() {
        try {
            List<User> members = userDAO.findByRole("MEMBER");
            
            // Calculate statistics
            int total = members.size();
            int active = (int) members.stream()
                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
                .count();
            int pending = (int) members.stream()
                .filter(m -> "PENDING".equalsIgnoreCase(m.getStatus()))
                .count();
            
            totalMembersLabel.setText(String.valueOf(total));
            activeMembersLabel.setText(String.valueOf(active));
            pendingApplicationsLabel.setText(String.valueOf(pending));
            
            membersList.clear();
            membersList.addAll(members);
            
        } catch (Exception e) {
            System.err.println("Error loading members data: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to load members data: " + e.getMessage());
        }
    }
    
    private void handleApproveMember(User member) {
        boolean confirmed = SceneManager.showConfirmation(
            "Approve Membership",
            "Approve membership application for:\n\n" +
            "Name: " + member.getFullName() + "\n" +
            "Email: " + member.getEmail()
        );
        
        if (!confirmed) return;
        
        try {
            boolean success = userDAO.updateStatus(member.getUserId(), "ACTIVE");
            
            if (success) {
                SceneManager.showInfo("Success", "Membership approved successfully!");
                loadMembersData();
                new Thread(() -> {
                    String template = EmailTemplateLoader.loadTemplate("membership-approved.html");
                    String html = EmailTemplateLoader.render(
                            template,
                            Map.of(
                               "FULL_NAME", member.getFullName(),
                               "USERNAME", member.getUsername(),
                               "APPROVAL_DATE", LocalDateTime.now().toString(),
                               "LOGO_URL", ""
                            )
                    );

                    EmailService.sendHtmlEmail(
                            member.getEmail(),
                            "Membership Approval",
                            html
                    );
                }).start();
            } else {
                SceneManager.showError("Error", "Failed to approve membership.");
            }
            
        } catch (Exception e) {
            System.err.println("Error approving member: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to approve member: " + e.getMessage());
        }
    }
    
    private void handleRevokeMember(User member) {
        // Check if member has active borrows
        try {
            int activeBooks = borrowedBookDAO.getActiveBorrowCount(member.getUserId());
            if (activeBooks > 0) {
                SceneManager.showWarning("Cannot Revoke", 
                    "This member has " + activeBooks + " books currently borrowed. " +
                    "Cannot revoke membership until all books are returned.");
                return;
            }
        } catch (Exception e) {
            System.err.println("Error checking member borrows: " + e.getMessage());
        }
        
        boolean confirmed = SceneManager.showConfirmation(
            "Revoke Membership",
            "Are you sure you want to revoke membership for:\n\n" +
            "Name: " + member.getFullName() + "\n" +
            "Member ID: " + member.getMemberId() + "\n\n" +
            "This will set their status to INACTIVE."
        );
        
        if (!confirmed) return;
        
        try {
            boolean success = userDAO.updateStatus(member.getUserId(), "INACTIVE");
            
            if (success) {
                SceneManager.showInfo("Success", "Membership revoked successfully!");
                loadMembersData();
            } else {
                SceneManager.showError("Error", "Failed to revoke membership.");
            }
            
        } catch (Exception e) {
            System.err.println("Error revoking member: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to revoke membership: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleMemberSearch() {
        String searchTerm = memberSearchField.getText().trim();
        
        try {
            List<User> members;
            if (searchTerm.isEmpty()) {
                members = userDAO.findByRole("MEMBER");
            } else {
                members = userDAO.searchUsers(searchTerm, "MEMBER");
            }
            
            membersList.clear();
            membersList.addAll(members);
            
        } catch (Exception e) {
            System.err.println("Error searching members: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Search failed: " + e.getMessage());
        }
    }

    // ==================== Reports Management Methods ====================
    
    private void setupReportsTable() {
        // Dynamic column setup - columns will be configured based on report type
        reportCol1.setCellValueFactory(cellData -> {
            ObservableList<String> row = cellData.getValue();
            return new SimpleStringProperty(row.size() > 0 ? row.get(0) : "");
        });
        
        reportCol2.setCellValueFactory(cellData -> {
            ObservableList<String> row = cellData.getValue();
            return new SimpleStringProperty(row.size() > 1 ? row.get(1) : "");
        });
        
        reportCol3.setCellValueFactory(cellData -> {
            ObservableList<String> row = cellData.getValue();
            return new SimpleStringProperty(row.size() > 2 ? row.get(2) : "");
        });
        
        reportCol4.setCellValueFactory(cellData -> {
            ObservableList<String> row = cellData.getValue();
            return new SimpleStringProperty(row.size() > 3 ? row.get(3) : "");
        });
        
        reportCol5.setCellValueFactory(cellData -> {
            ObservableList<String> row = cellData.getValue();
            return new SimpleStringProperty(row.size() > 4 ? row.get(4) : "");
        });
        
        reportDataTable.setItems(reportDataList);
    }
    
    private void loadReportsData() {
        try {
            // Load statistics
            int totalTransactions = borrowedBookDAO.getActiveBorrowCount(0); // Get all
            totalTransactionsLabel.setText(String.valueOf(totalTransactions));
            
            int activeUsers = (int) userDAO.findByRole("MEMBER").stream()
                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
                .count();
            activeUsersLabel.setText(String.valueOf(activeUsers));
            
            systemUptimeLabel.setText("99.9%");
            
            reportStatusLabel.setText("Select a report type and click Generate");
            
        } catch (Exception e) {
            System.err.println("Error loading reports data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleGenerateReport() {
        String reportType = reportTypeCombo.getValue();
        
        if (reportType == null || reportType.isEmpty()) {
            SceneManager.showWarning("No Report Selected", "Please select a report type first.");
            return;
        }
        
        try {
            reportDataList.clear();
            
            switch (reportType) {
                case "Books Issued Report":
                    generateBooksIssuedReport();
                    break;
                case "Overdue Books Report":
                    generateOverdueBooksReport();
                    break;
                case "Member Activity Report":
                    generateMemberActivityReport();
                    break;
                case "Popular Books Report":
                    generatePopularBooksReport();
                    break;
                case "Inventory Status Report":
                    generateInventoryStatusReport();
                    break;
                case "Revenue Report":
                    generateRevenueReport();
                    break;
                default:
                    SceneManager.showWarning("Unknown Report", "Report type not implemented.");
            }
            
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to generate report: " + e.getMessage());
        }
    }
    
    private void generateBooksIssuedReport() {
        reportStatusLabel.setText("Books Issued Report - Generated");
        
        // Configure columns
        reportCol1.setText("Book Title");
        reportCol2.setText("Author");
        reportCol3.setText("Member");
        reportCol4.setText("Issue Date");
        reportCol5.setText("Due Date");
        
        // Get all borrowed books
        List<com.library.model.BorrowedBook> borrowedBooks = borrowedBookDAO.getAllBorrowedBooks();
        
        for (com.library.model.BorrowedBook bb : borrowedBooks) {
            if ("ISSUED".equalsIgnoreCase(bb.getStatus())) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(bb.getBook() != null ? bb.getBook().getTitle() : "N/A");
                row.add(bb.getBook() != null ? bb.getBook().getAuthor() : "N/A");
                row.add(bb.getMember() != null ? bb.getMember().getFullName() : "N/A");
                row.add(bb.getIssueDate() != null ? bb.getIssueDate().format(DATE_FORMATTER) : "N/A");
                row.add(bb.getDueDate() != null ? bb.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A");
                reportDataList.add(row);
            }
        }
        
        if (reportDataList.isEmpty()) {
            ObservableList<String> emptyRow = FXCollections.observableArrayList();
            emptyRow.add("No books currently issued");
            emptyRow.add("");
            emptyRow.add("");
            emptyRow.add("");
            emptyRow.add("");
            reportDataList.add(emptyRow);
        }
    }
    
    private void generateOverdueBooksReport() {
        reportStatusLabel.setText("Overdue Books Report - Generated");
        
        // Configure columns
        reportCol1.setText("Book Title");
        reportCol2.setText("Member");
        reportCol3.setText("Due Date");
        reportCol4.setText("Days Overdue");
        reportCol5.setText("Fine Amount");
        
        // Get overdue books
        List<com.library.model.BorrowedBook> overdueBooks = borrowedBookDAO.getOverdueBooks();
        
        for (com.library.model.BorrowedBook bb : overdueBooks) {
            ObservableList<String> row = FXCollections.observableArrayList();
            row.add(bb.getBook() != null ? bb.getBook().getTitle() : "N/A");
            row.add(bb.getMember() != null ? bb.getMember().getFullName() : "N/A");
            row.add(bb.getDueDate() != null ? bb.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A");
            row.add(String.valueOf(bb.getDaysOverdue()));
            row.add("$" + bb.getFineAmount().toString());
            reportDataList.add(row);
        }
        
        if (reportDataList.isEmpty()) {
            ObservableList<String> emptyRow = FXCollections.observableArrayList();
            emptyRow.add("No overdue books");
            emptyRow.add("");
            emptyRow.add("");
            emptyRow.add("");
            emptyRow.add("");
            reportDataList.add(emptyRow);
        }
    }
    
    private void generateMemberActivityReport() {
        reportStatusLabel.setText("Member Activity Report - Generated");
        
        // Configure columns
        reportCol1.setText("Member ID");
        reportCol2.setText("Name");
        reportCol3.setText("Email");
        reportCol4.setText("Books Borrowed");
        reportCol5.setText("Status");
        
        // Get all members
        List<User> members = userDAO.findByRole("MEMBER");
        
        for (User member : members) {
            ObservableList<String> row = FXCollections.observableArrayList();
            row.add(member.getMemberId() != null ? member.getMemberId() : "N/A");
            row.add(member.getFullName());
            row.add(member.getEmail());
            
            int borrowCount = borrowedBookDAO.getActiveBorrowCount(member.getUserId());
            row.add(String.valueOf(borrowCount));
            row.add(member.getStatus());
            
            reportDataList.add(row);
        }
        
        if (reportDataList.isEmpty()) {
            ObservableList<String> emptyRow = FXCollections.observableArrayList();
            emptyRow.add("No members found");
            emptyRow.add("");
            emptyRow.add("");
            emptyRow.add("");
            emptyRow.add("");
            reportDataList.add(emptyRow);
        }
    }
    
    private void generatePopularBooksReport() {
        reportStatusLabel.setText("Popular Books Report - Generated");
        
        // Configure columns
        reportCol1.setText("Book Title");
        reportCol2.setText("Author");
        reportCol3.setText("Category");
        reportCol4.setText("Times Borrowed");
        reportCol5.setText("Currently Out");
        
        // Get all books
        List<Book> books = bookDAO.getAllBooks();
        
        for (Book book : books) {
            ObservableList<String> row = FXCollections.observableArrayList();
            row.add(book.getTitle());
            row.add(book.getAuthor());
            row.add(book.getCategory());
            
            int borrowed = book.getTotalCopies() - book.getAvailableCopies();
            row.add(String.valueOf(borrowed));
            row.add(String.valueOf(borrowed));
            
            reportDataList.add(row);
        }
        
        // Sort by times borrowed (descending)
        reportDataList.sort((row1, row2) -> {
            try {
                int val1 = Integer.parseInt(row1.get(3));
                int val2 = Integer.parseInt(row2.get(3));
                return Integer.compare(val2, val1);
            } catch (Exception e) {
                return 0;
            }
        });
        
        if (reportDataList.isEmpty()) {
            ObservableList<String> emptyRow = FXCollections.observableArrayList();
            emptyRow.add("No books found");
            emptyRow.add("");
            emptyRow.add("");
            emptyRow.add("");
            emptyRow.add("");
            reportDataList.add(emptyRow);
        }
    }
    
    private void generateInventoryStatusReport() {
        reportStatusLabel.setText("Inventory Status Report - Generated");
        
        // Configure columns
        reportCol1.setText("Book Title");
        reportCol2.setText("ISBN");
        reportCol3.setText("Total Copies");
        reportCol4.setText("Available");
        reportCol5.setText("Status");
        
        // Get all books
        List<Book> books = bookDAO.getAllBooks();
        
        for (Book book : books) {
            ObservableList<String> row = FXCollections.observableArrayList();
            row.add(book.getTitle());
            row.add(book.getIsbn());
            row.add(String.valueOf(book.getTotalCopies()));
            row.add(String.valueOf(book.getAvailableCopies()));
            
            String status;
            if (book.getAvailableCopies() == 0) {
                status = "OUT OF STOCK";
            } else if (book.isLowStock()) {
                status = "LOW STOCK";
            } else {
                status = "IN STOCK";
            }
            row.add(status);
            
            reportDataList.add(row);
        }
        
        if (reportDataList.isEmpty()) {
            ObservableList<String> emptyRow = FXCollections.observableArrayList();
            emptyRow.add("No books in inventory");
            emptyRow.add("");
            emptyRow.add("");
            emptyRow.add("");
            emptyRow.add("");
            reportDataList.add(emptyRow);
        }
    }
    
    private void generateRevenueReport() {
        reportStatusLabel.setText("Revenue Report - Generated");
        
        // Configure columns
        reportCol1.setText("Source");
        reportCol2.setText("Count");
        reportCol3.setText("Amount");
        reportCol4.setText("Month");
        reportCol5.setText("Status");
        
        // This is a placeholder - actual revenue tracking would need additional tables
        ObservableList<String> row1 = FXCollections.observableArrayList();
        row1.add("Membership Fees");
        row1.add("0");
        row1.add("$0.00");
        row1.add("January 2026");
        row1.add("N/A");
        reportDataList.add(row1);
        
        ObservableList<String> row2 = FXCollections.observableArrayList();
        row2.add("Late Fees");
        row2.add(String.valueOf(borrowedBookDAO.getTotalOverdueCount()));
        row2.add("$0.00");
        row2.add("January 2026");
        row2.add("Pending");
        reportDataList.add(row2);
        
        ObservableList<String> row3 = FXCollections.observableArrayList();
        row3.add("Book Purchases");
        row3.add(String.valueOf(bookDAO.getAllBooks().size()));
        row3.add("N/A");
        row3.add("January 2026");
        row3.add("N/A");
        reportDataList.add(row3);
    }
    
    @FXML
    private void handleExportPDF() {
        SceneManager.showInfo("Export PDF", 
            "Export to PDF functionality - Would export current report to PDF file");
        // TODO: Implement PDF export using iText or similar library
    }

    // ==================== Logout ====================
    
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

    // ==================== Getters/Setters for Testing ====================
    
    public User getCurrentUser() {
        return currentUser;
    }

    public void setBookDAO(BookDAO bookDAO) {
        this.bookDAO = bookDAO;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void setBorrowedBookDAO(BorrowedBookDAO borrowedBookDAO) {
        this.borrowedBookDAO = borrowedBookDAO;
    }
}