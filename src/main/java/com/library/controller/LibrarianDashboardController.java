package com.library.controller;

import com.library.dao.BookDAO;
import com.library.dao.BorrowRequestDAO;
import com.library.dao.BorrowedBookDAO;
import com.library.dao.UserDAO;
import com.library.model.Book;
import com.library.model.BorrowRequest;
import com.library.model.BorrowedBook;
import com.library.model.User;
import com.library.util.SceneManager;
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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class LibrarianDashboardController implements Initializable {

    // Top bar
    @FXML private Label welcomeLabel;
    @FXML private Button logoutButton;
    
    // Sidebar navigation
    @FXML private Button requestsTabButton;
    @FXML private Button inventoryTabButton;
    @FXML private Button membersTabButton;
    
    // Statistics labels
    @FXML private Label pendingRequestsLabel;
    @FXML private Label issuedTodayLabel;
    @FXML private Label overdueReturnsLabel;
    
    // Content pane (StackPane holding all views)
    @FXML private VBox requestsView;
    @FXML private VBox inventoryView;
    @FXML private VBox membersView;
    
    // ==================== Requests View ====================
    @FXML private ComboBox<String> requestFilterCombo;
    @FXML private TableView<BorrowRequest> requestsTable;
    @FXML private TableColumn<BorrowRequest, Integer> reqIdColumn;
    @FXML private TableColumn<BorrowRequest, String> reqMemberColumn;
    @FXML private TableColumn<BorrowRequest, String> reqBookColumn;
    @FXML private TableColumn<BorrowRequest, String> reqDateColumn;
    @FXML private TableColumn<BorrowRequest, String> reqStatusColumn;
    @FXML private TableColumn<BorrowRequest, Void> reqActionColumn;
    
    // ==================== Inventory View ====================
    @FXML private TextField inventorySearchField;
    @FXML private TableView<Book> inventoryTable;
    @FXML private TableColumn<Book, String> invIsbnColumn;
    @FXML private TableColumn<Book, String> invTitleColumn;
    @FXML private TableColumn<Book, String> invAuthorColumn;
    @FXML private TableColumn<Book, String> invCategoryColumn;
    @FXML private TableColumn<Book, Integer> invTotalColumn;
    @FXML private TableColumn<Book, Integer> invAvailableColumn;
    
    // ==================== Members View ====================
    @FXML private TextField memberSearchField;
    @FXML private TableView<User> membersTable;
    @FXML private TableColumn<User, String> memberIdColumn;
    @FXML private TableColumn<User, String> memberNameColumn;
    @FXML private TableColumn<User, String> memberEmailColumn;
    @FXML private TableColumn<User, String> memberJoinDateColumn;
    @FXML private TableColumn<User, Integer> memberBooksColumn;
    @FXML private TableColumn<User, String> memberStatusColumn;
    @FXML private TableColumn<User, Void> memberActionColumn;
    
    private User currentUser;
    private BookDAO bookDAO;
    private BorrowRequestDAO borrowRequestDAO;
    private BorrowedBookDAO borrowedBookDAO;
    private UserDAO userDAO;
    
    private ObservableList<BorrowRequest> requestsList;
    private ObservableList<Book> inventoryList;
    private ObservableList<User> membersList;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize DAOs
        bookDAO = new BookDAO();
        borrowRequestDAO = new BorrowRequestDAO();
        borrowedBookDAO = new BorrowedBookDAO();
        userDAO = new UserDAO();
        
        // Initialize lists
        requestsList = FXCollections.observableArrayList();
        inventoryList = FXCollections.observableArrayList();
        membersList = FXCollections.observableArrayList();
        
        // Setup tables
        setupRequestsTable();
        setupInventoryTable();
        setupMembersTable();
        
        // Setup filter combo
        requestFilterCombo.setValue("All");
        
        // Set default view
        switchToRequests();
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
        loadStatistics();
        loadRequests();
        loadInventory();
        loadMembers();
    }

    /**
     * Load statistics
     */
    private void loadStatistics() {
        try {
            int pendingCount = borrowRequestDAO.getTotalPendingRequestsCount();
            pendingRequestsLabel.setText(String.valueOf(pendingCount));
            
            int issuedToday = borrowedBookDAO.getBooksIssuedTodayCount();
            issuedTodayLabel.setText(String.valueOf(issuedToday));
            
            int overdueCount = borrowedBookDAO.getTotalOverdueCount();
            overdueReturnsLabel.setText(String.valueOf(overdueCount));
            
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== Navigation Methods ====================
    
    @FXML
    private void switchToRequests() {
        setActiveView(requestsView);
        setActiveNavButton(requestsTabButton);
        loadRequests();
    }
    
    @FXML
    private void switchToInventory() {
        setActiveView(inventoryView);
        setActiveNavButton(inventoryTabButton);
        loadInventory();
    }
    
    @FXML
    private void switchToMembers() {
        setActiveView(membersView);
        setActiveNavButton(membersTabButton);
        loadMembers();
    }
    
    private void setActiveView(VBox activeView) {
        requestsView.setVisible(false);
        inventoryView.setVisible(false);
        membersView.setVisible(false);
        activeView.setVisible(true);
    }
    
    private void setActiveNavButton(Button activeButton) {
        // Reset all buttons
        requestsTabButton.getStyleClass().removeAll("nav-button-active");
        inventoryTabButton.getStyleClass().removeAll("nav-button-active");
        membersTabButton.getStyleClass().removeAll("nav-button-active");
        
        requestsTabButton.getStyleClass().add("nav-button");
        inventoryTabButton.getStyleClass().add("nav-button");
        membersTabButton.getStyleClass().add("nav-button");
        
        // Set active button
        activeButton.getStyleClass().removeAll("nav-button");
        activeButton.getStyleClass().add("nav-button-active");
    }

    // ==================== Requests View Methods ====================
    
    private void setupRequestsTable() {
        reqIdColumn.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        
        reqMemberColumn.setCellValueFactory(cellData -> {
            User member = cellData.getValue().getMember();
            return new SimpleStringProperty(member != null ? member.getFullName() : "N/A");
        });
        
        reqBookColumn.setCellValueFactory(cellData -> {
            Book book = cellData.getValue().getBook();
            return new SimpleStringProperty(book != null ? book.getTitle() : "N/A");
        });
        
        reqDateColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRequestDate().format(DATE_FORMATTER)));
        
        reqStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Style status column
        reqStatusColumn.setCellFactory(column -> new TableCell<BorrowRequest, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item.toUpperCase()) {
                        case "PENDING":
                            setStyle("-fx-text-fill: #FF8C00; -fx-font-weight: bold;");
                            break;
                        case "APPROVED":
                            setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                            break;
                        case "REJECTED":
                            setStyle("-fx-text-fill: #FF4444; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        // Add action buttons column
        reqActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button approveButton = new Button("Approve");
            private final Button rejectButton = new Button("Reject");
            
            {
                approveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                                     "-fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 5 10;");
                rejectButton.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; " +
                                    "-fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 5 10;");
                
                approveButton.setOnAction(event -> {
                    BorrowRequest request = getTableView().getItems().get(getIndex());
                    handleApproveRequest(request);
                });
                
                rejectButton.setOnAction(event -> {
                    BorrowRequest request = getTableView().getItems().get(getIndex());
                    handleRejectRequest(request);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    BorrowRequest request = getTableView().getItems().get(getIndex());
                    if ("PENDING".equalsIgnoreCase(request.getStatus())) {
                        javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(5, approveButton, rejectButton);
                        setGraphic(buttons);
                    } else {
                        Label statusLabel = new Label(request.getStatus());
                        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
                        setGraphic(statusLabel);
                    }
                }
            }
        });
        
        requestsTable.setItems(requestsList);
    }
    
    private void loadRequests() {
        try {
            String filter = requestFilterCombo.getValue();
            List<BorrowRequest> requests;
            
            if ("All".equals(filter)) {
                requests = borrowRequestDAO.getAllRequestsWithDetails();
            } else {
                requests = borrowRequestDAO.getRequestsByStatus(filter.toUpperCase());
                // Load details for each request
                for (BorrowRequest req : requests) {
                    req.setMember(userDAO.findById(req.getMemberId()));
                    req.setBook(bookDAO.findById(req.getBookId()));
                }
            }
            
            requestsList.clear();
            requestsList.addAll(requests);
            
        } catch (Exception e) {
            System.err.println("Error loading requests: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to load requests: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRefreshRequests() {
        loadStatistics();
        loadRequests();
        SceneManager.showInfo("Refreshed", "Requests refreshed successfully!");
    }
    
    private void handleApproveRequest(BorrowRequest request) {
        try {
            // Open approve dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ApproveRequestDialog.fxml"));
            Parent root = loader.load();
            
            ApproveRequestDialogController controller = loader.getController();
            controller.setRequest(request);
            controller.setLibrarian(currentUser);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Approve Borrow Request");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(requestsTabButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            
            // Set callback
            controller.setOnApproved(() -> {
                loadStatistics();
                loadRequests();
                dialogStage.close();
            });
            
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            System.err.println("Error opening approve dialog: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to open approval dialog: " + e.getMessage());
        }
    }
    
    private void handleRejectRequest(BorrowRequest request) {
        // Show reason dialog
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Request");
        dialog.setHeaderText("Reject borrow request for: " + request.getBook().getTitle());
        dialog.setContentText("Reason for rejection:");
        
        dialog.showAndWait().ifPresent(reason -> {
            try {
                boolean success = borrowRequestDAO.rejectRequest(request.getRequestId(), reason);
                
                if (success) {
                    SceneManager.showInfo("Success", "Request rejected successfully!");
                    loadStatistics();
                    loadRequests();
                } else {
                    SceneManager.showError("Error", "Failed to reject request.");
                }
                
            } catch (Exception e) {
                System.err.println("Error rejecting request: " + e.getMessage());
                e.printStackTrace();
                SceneManager.showError("Error", "Failed to reject request: " + e.getMessage());
            }
        });
    }

    // ==================== Inventory View Methods ====================
    
    private void setupInventoryTable() {
        invIsbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        invTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        invAuthorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        invCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        invTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalCopies"));
        invAvailableColumn.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        
        // Highlight low stock
        invAvailableColumn.setCellFactory(column -> new TableCell<Book, Integer>() {
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
        
        inventoryTable.setItems(inventoryList);
    }
    
    private void loadInventory() {
        try {
            List<Book> books = bookDAO.getAllBooks();
            inventoryList.clear();
            inventoryList.addAll(books);
        } catch (Exception e) {
            System.err.println("Error loading inventory: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to load inventory: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleInventorySearch() {
        String searchTerm = inventorySearchField.getText().trim();
        
        try {
            List<Book> books;
            if (searchTerm.isEmpty()) {
                books = bookDAO.getAllBooks();
            } else {
                books = bookDAO.searchBooks(searchTerm);
            }
            
            inventoryList.clear();
            inventoryList.addAll(books);
            
        } catch (Exception e) {
            System.err.println("Error searching inventory: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Search failed: " + e.getMessage());
        }
    }

    // ==================== Members View Methods ====================
    
    private void setupMembersTable() {
        memberIdColumn.setCellValueFactory(new PropertyValueFactory<>("memberId"));
        memberNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        memberEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        memberJoinDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(
                    cellData.getValue().getCreatedAt().format(DATE_FORMATTER)
                );
            }
            return new SimpleStringProperty("N/A");
        });
        
        memberBooksColumn.setCellValueFactory(cellData -> {
            try {
                int count = borrowedBookDAO.getActiveBorrowCount(cellData.getValue().getUserId());
                return new javafx.beans.property.SimpleIntegerProperty(count).asObject();
            } catch (Exception e) {
                return new javafx.beans.property.SimpleIntegerProperty(0).asObject();
            }
        });
        
        memberStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Style status column
        memberStatusColumn.setCellFactory(column -> new TableCell<User, String>() {
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
        memberActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button revokeButton = new Button("Revoke");
            
            {
                revokeButton.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; " +
                                    "-fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 5 10;");
                
                revokeButton.setOnAction(event -> {
                    User member = getTableView().getItems().get(getIndex());
                    handleRevokeMembership(member);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User member = getTableView().getItems().get(getIndex());
                    if ("ACTIVE".equalsIgnoreCase(member.getStatus()) && 
                        currentUser != null && currentUser.isCanRevokeMembership()) {
                        setGraphic(revokeButton);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        
        membersTable.setItems(membersList);
    }
    
    private void loadMembers() {
        try {
            List<User> members = userDAO.findByRole("MEMBER");
            membersList.clear();
            membersList.addAll(members);
        } catch (Exception e) {
            System.err.println("Error loading members: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to load members: " + e.getMessage());
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
    
    private void handleRevokeMembership(User member) {
        boolean confirmed = SceneManager.showConfirmation(
            "Revoke Membership",
            "Are you sure you want to revoke membership for:\n\n" +
            member.getFullName() + " (" + member.getMemberId() + ")\n\n" +
            "This action will set their status to INACTIVE."
        );
        
        if (!confirmed) return;
        
        try {
            boolean success = userDAO.updateStatus(member.getUserId(), "INACTIVE");
            
            if (success) {
                SceneManager.showInfo("Success", "Membership revoked successfully!");
                loadMembers();
            } else {
                SceneManager.showError("Error", "Failed to revoke membership.");
            }
            
        } catch (Exception e) {
            System.err.println("Error revoking membership: " + e.getMessage());
            e.printStackTrace();
            SceneManager.showError("Error", "Failed to revoke membership: " + e.getMessage());
        }
    }

    // ==================== Quick Actions ====================
    
    @FXML
    private void handleIssueBook() {
        SceneManager.showInfo("Issue Book", "Issue book functionality - Opens book issuing dialog");
        // TODO: Implement issue book dialog
    }
    
    @FXML
    private void handleReturnBook() {
        SceneManager.showInfo("Return Book", "Return book functionality - Opens book return dialog");
        // TODO: Implement return book dialog
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

    public void setBorrowRequestDAO(BorrowRequestDAO borrowRequestDAO) {
        this.borrowRequestDAO = borrowRequestDAO;
    }

    public void setBorrowedBookDAO(BorrowedBookDAO borrowedBookDAO) {
        this.borrowedBookDAO = borrowedBookDAO;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }
}