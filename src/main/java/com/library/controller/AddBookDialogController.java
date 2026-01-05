package com.library.controller;

import com.library.dao.BookDAO;
import com.library.model.Book;
import com.library.util.SceneManager;
import com.library.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class AddBookDialogController implements Initializable {

    // Form fields
    @FXML private TextField isbnField;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField publisherField;
    @FXML private Spinner<Integer> yearSpinner;
    @FXML private TextField editionField;
    @FXML private Spinner<Integer> copiesSpinner;
    @FXML private TextField priceField;
    @FXML private TextArea descriptionField;
    @FXML private TextField shelfLocationField;
    
    // Error labels
    @FXML private Label isbnError;
    @FXML private Label titleError;
    @FXML private Label authorError;
    @FXML private Label categoryError;
    @FXML private Label copiesError;
    @FXML private Label generalMessage;
    
    // Buttons
    @FXML private Button cancelButton;
    @FXML private Button saveButton;
    
    private BookDAO bookDAO;
    private Book currentBook; // For editing
    private boolean isEditMode = false;
    private Runnable onBookAddedCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bookDAO = new BookDAO();
        
        // Setup spinners
        setupYearSpinner();
        setupCopiesSpinner();
        
        // Setup listeners
        setupListeners();
    }

    /**
     * Setup year spinner
     */
    private void setupYearSpinner() {
        int currentYear = java.time.Year.now().getValue();
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1800, currentYear, currentYear);
        yearSpinner.setValueFactory(valueFactory);
        yearSpinner.setEditable(true);
    }

    /**
     * Setup copies spinner
     */
    private void setupCopiesSpinner() {
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 1);
        copiesSpinner.setValueFactory(valueFactory);
        copiesSpinner.setEditable(true);
    }

    /**
     * Setup input field listeners to clear errors on typing
     */
    private void setupListeners() {
        isbnField.textProperty().addListener((obs, oldVal, newVal) -> clearFieldError(isbnField, isbnError));
        titleField.textProperty().addListener((obs, oldVal, newVal) -> clearFieldError(titleField, titleError));
        authorField.textProperty().addListener((obs, oldVal, newVal) -> clearFieldError(authorField, authorError));
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> clearError(categoryError));
    }

    /**
     * Set book for editing (edit mode)
     */
    public void setBook(Book book) {
        this.currentBook = book;
        this.isEditMode = true;
        
        if (book != null) {
            populateFields(book);
            saveButton.setText("Update Book");
            isbnField.setDisable(true); // Can't change ISBN
        }
    }

    /**
     * Populate fields with book data
     */
    private void populateFields(Book book) {
        isbnField.setText(book.getIsbn());
        titleField.setText(book.getTitle());
        authorField.setText(book.getAuthor());
        categoryComboBox.setValue(book.getCategory());
        publisherField.setText(book.getPublisher());
        
        if (book.getPublicationYear() != null) {
            yearSpinner.getValueFactory().setValue(book.getPublicationYear());
        }
        
        editionField.setText(book.getEdition());
        copiesSpinner.getValueFactory().setValue(book.getTotalCopies());
        
        if (book.getPrice() != null) {
            priceField.setText(book.getPrice().toString());
        }
        
        descriptionField.setText(book.getDescription());
        shelfLocationField.setText(book.getShelfLocation());
    }

    /**
     * Set callback to execute when book is added/updated
     */
    public void setOnBookAdded(Runnable callback) {
        this.onBookAddedCallback = callback;
    }

    /**
     * Handle save button click
     */
    @FXML
    private void handleSave() {
        clearAllErrors();
        
        // Get input values
        String isbn = isbnField.getText().trim();
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String category = categoryComboBox.getValue();
        String publisher = publisherField.getText().trim();
        Integer year = yearSpinner.getValue();
        String edition = editionField.getText().trim();
        int totalCopies = copiesSpinner.getValue();
        String priceStr = priceField.getText().trim();
        String description = descriptionField.getText().trim();
        String shelfLocation = shelfLocationField.getText().trim();
        
        // Validate inputs
        if (!validateInputs(isbn, title, author, category, totalCopies, priceStr)) {
            return;
        }
        
        // Check ISBN uniqueness (only for new books)
        if (!isEditMode && bookDAO.isbnExists(isbn)) {
            showError(isbnError, "ISBN already exists in the system");
            highlightError(isbnField);
            return;
        }
        
        // Parse price
        BigDecimal price = null;
        if (!priceStr.isEmpty()) {
            try {
                price = new BigDecimal(priceStr);
                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    showError(generalMessage, "Price cannot be negative");
                    return;
                }
            } catch (NumberFormatException e) {
                showError(generalMessage, "Invalid price format");
                return;
            }
        }
        
        try {
            Book book;
            boolean success;
            
            if (isEditMode) {
                // Update existing book
                book = currentBook;
                book.setTitle(title);
                book.setAuthor(author);
                book.setCategory(category);
                book.setPublisher(publisher.isEmpty() ? null : publisher);
                book.setPublicationYear(year);
                book.setEdition(edition.isEmpty() ? null : edition);
                
                // Update stock (maintain borrowed count)
                int borrowed = book.getTotalCopies() - book.getAvailableCopies();
                int newAvailable = totalCopies - borrowed;
                
                if (newAvailable < 0) {
                    showError(copiesError, "Total copies cannot be less than currently borrowed (" + borrowed + ")");
                    highlightError(copiesSpinner);
                    return;
                }
                
                book.setTotalCopies(totalCopies);
                book.setAvailableCopies(newAvailable);
                book.setPrice(price);
                book.setDescription(description.isEmpty() ? null : description);
                book.setShelfLocation(shelfLocation.isEmpty() ? null : shelfLocation);
                
                success = bookDAO.updateBook(book);
                
            } else {
                // Create new book
                book = new Book();
                book.setIsbn(isbn);
                book.setTitle(title);
                book.setAuthor(author);
                book.setCategory(category);
                book.setPublisher(publisher.isEmpty() ? null : publisher);
                book.setPublicationYear(year);
                book.setEdition(edition.isEmpty() ? null : edition);
                book.setTotalCopies(totalCopies);
                book.setAvailableCopies(totalCopies); // All available initially
                book.setPrice(price);
                book.setDescription(description.isEmpty() ? null : description);
                book.setShelfLocation(shelfLocation.isEmpty() ? null : shelfLocation);
                
                success = bookDAO.createBook(book);
            }
            
            if (success) {
                String message = isEditMode ? 
                    "Book updated successfully!\n\nTitle: " + title :
                    "Book added successfully!\n\nTitle: " + title + "\nISBN: " + isbn;
                    
                SceneManager.showInfo(isEditMode ? "Book Updated" : "Book Added", message);
                
                // Trigger callback
                if (onBookAddedCallback != null) {
                    onBookAddedCallback.run();
                }
                
            } else {
                showError(generalMessage, "Failed to " + (isEditMode ? "update" : "add") + " book. Please try again.");
            }
            
        } catch (Exception e) {
            System.err.println("Error saving book: " + e.getMessage());
            e.printStackTrace();
            showError(generalMessage, "Error: " + e.getMessage());
        }
    }

    /**
     * Validate all form inputs
     */
    private boolean validateInputs(String isbn, String title, String author, 
                                   String category, int totalCopies, String priceStr) {
        boolean isValid = true;
        
        // Validate ISBN (only for new books)
        if (!isEditMode) {
            if (ValidationUtil.isEmpty(isbn)) {
                showError(isbnError, "ISBN is required");
                highlightError(isbnField);
                isValid = false;
            } else if (!ValidationUtil.isValidISBN(isbn)) {
                showError(isbnError, "Invalid ISBN format");
                highlightError(isbnField);
                isValid = false;
            }
        }
        
        // Validate title
        if (ValidationUtil.isEmpty(title)) {
            showError(titleError, "Title is required");
            highlightError(titleField);
            isValid = false;
        } else if (!ValidationUtil.isValidLength(title, 1, 255)) {
            showError(titleError, "Title must be between 1 and 255 characters");
            highlightError(titleField);
            isValid = false;
        }
        
        // Validate author
        if (ValidationUtil.isEmpty(author)) {
            showError(authorError, "Author is required");
            highlightError(authorField);
            isValid = false;
        } else if (!ValidationUtil.isValidLength(author, 1, 100)) {
            showError(authorError, "Author name must be between 1 and 100 characters");
            highlightError(authorField);
            isValid = false;
        }
        
        // Validate category
        if (category == null || category.isEmpty()) {
            showError(categoryError, "Category is required");
            highlightError(categoryComboBox);
            isValid = false;
        }
        
        // Validate total copies
        if (totalCopies < 1) {
            showError(copiesError, "Total copies must be at least 1");
            highlightError(copiesSpinner);
            isValid = false;
        }
        
        // Validate price (if provided)
        if (!priceStr.isEmpty() && !ValidationUtil.isNumeric(priceStr)) {
            showError(generalMessage, "Invalid price format. Please enter a valid number.");
            isValid = false;
        }
        
        return isValid;
    }

    /**
     * Handle cancel button click
     */
    @FXML
    private void handleCancel() {
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
     * Clear field error
     */
    private void clearFieldError(Control field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().remove("error-field");
        }
        clearError(errorLabel);
    }

    /**
     * Clear all errors
     */
    private void clearAllErrors() {
        clearError(isbnError);
        clearError(titleError);
        clearError(authorError);
        clearError(categoryError);
        clearError(copiesError);
        clearError(generalMessage);
        
        isbnField.getStyleClass().remove("error-field");
        titleField.getStyleClass().remove("error-field");
        authorField.getStyleClass().remove("error-field");
        categoryComboBox.getStyleClass().remove("error-field");
    }

    /**
     * Highlight field with error styling
     */
    private void highlightError(Control control) {
        if (control != null && !control.getStyleClass().contains("error-field")) {
            control.getStyleClass().add("error-field");
        }
    }

    // ==================== Getters/Setters for Testing ====================
    
    public Book getCurrentBook() {
        return currentBook;
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public void setBookDAO(BookDAO bookDAO) {
        this.bookDAO = bookDAO;
    }

    public BookDAO getBookDAO() {
        return bookDAO;
    }
}