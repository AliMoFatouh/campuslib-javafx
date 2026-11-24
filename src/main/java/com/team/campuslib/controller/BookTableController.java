package com.team.campuslib.controller;

import java.util.List;
import java.util.Optional;

import com.team.campuslib.model.Book;
import com.team.campuslib.observer.DataChangeListener;
import com.team.campuslib.observer.DataChangePublisher;
import com.team.campuslib.observer.DataChangeType;
import com.team.campuslib.observer.FxDataChangeListener;
import com.team.campuslib.service.BookService;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class BookTableController {

    @FXML private VBox root;
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, String> isbnColumn;
    @FXML private TableColumn<Book, String> categoryColumn;
    @FXML private TableColumn<Book, Number> totalColumn;
    @FXML private TableColumn<Book, Number> availableColumn;
    @FXML private ComboBox<String> searchTypeBox;
    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Label messageLabel;

    private final BookService bookService = new BookService();

    private final DataChangeListener booksListener =
            new FxDataChangeListener(DataChangeType.BOOKS, this::reloadBooks);

    @FXML
    private void initialize() {
        titleColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTitle()));
        authorColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getAuthor()));
        isbnColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getIsbn() == null
                        ? "" : c.getValue().getIsbn()));
        categoryColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCategory() == null
                        ? "" : c.getValue().getCategory()));
        totalColumn.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getTotalCopies()));
        availableColumn.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getAvailableCopies()));

        searchTypeBox.setItems(FXCollections.observableArrayList(
                "Title", "Author", "ISBN", "Category"));
        searchTypeBox.getSelectionModel().selectFirst();

        setAdmin(false);
        reloadBooks();

        DataChangePublisher.getInstance().subscribe(booksListener);
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                DataChangePublisher.getInstance().unsubscribe(booksListener);
            }
        });
    }

    public void setAdmin(boolean admin) {
        for (Button b : List.of(addButton, editButton, deleteButton)) {
            b.setVisible(admin);
            b.setManaged(admin);
        }
    }

    private void reloadBooks() {
        try {
            String query = searchField.getText() == null
                    ? "" : searchField.getText().trim();
            List<Book> books = query.isEmpty()
                    ? bookService.getAllBooks()
                    : bookService.searchBooks(
                            searchTypeBox.getValue().toLowerCase(), query);
            bookTable.setItems(FXCollections.observableArrayList(books));
        } catch (RuntimeException e) {
            showMessage(e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        showMessage("");
        reloadBooks();
    }

    @FXML
    private void handleClear() {
        showMessage("");
        searchField.clear();
        reloadBooks();
    }

    @FXML
    private void handleAdd() {
        showMessage("");
        Book book = new Book();
        if (editDialog("Add book", book)) {
            book.setAvailableCopies(book.getTotalCopies());
            try {
                bookService.addBook(book);
            } catch (RuntimeException e) {
                showMessage(e.getMessage());
            }
            reloadBooks();
        }
    }

    @FXML
    private void handleEdit() {
        showMessage("");
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a book first.");
            return;
        }
        if (editDialog("Edit book", selected)) {
            try {
                bookService.updateBook(selected);
            } catch (RuntimeException e) {
                showMessage(e.getMessage());
            }
            reloadBooks();
        }
    }

    @FXML
    private void handleDelete() {
        showMessage("");
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a book first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selected.getTitle() + "\"?",
                ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isPresent() && answer.get() == ButtonType.OK) {
            try {
                bookService.deleteBook(selected.getId());
            } catch (RuntimeException e) {
                showMessage(e.getMessage());
            }
            reloadBooks();
        }
    }

    private boolean editDialog(String title, Book book) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField titleF = new TextField(nz(book.getTitle()));
        TextField authorF = new TextField(nz(book.getAuthor()));
        TextField isbnF = new TextField(nz(book.getIsbn()));
        TextField categoryF = new TextField(nz(book.getCategory()));
        TextField copiesF = new TextField(
                String.valueOf(book.getTotalCopies()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Title"), titleF);
        grid.addRow(1, new Label("Author"), authorF);
        grid.addRow(2, new Label("ISBN"), isbnF);
        grid.addRow(3, new Label("Category"), categoryF);
        grid.addRow(4, new Label("Total copies"), copiesF);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return false;
        }
        try {
            book.setTotalCopies(Integer.parseInt(copiesF.getText().trim()));
        } catch (NumberFormatException e) {
            showMessage("Total copies must be a whole number.");
            return false;
        }
        book.setTitle(titleF.getText());
        book.setAuthor(authorF.getText());
        book.setIsbn(isbnF.getText());
        book.setCategory(categoryF.getText());
        return true;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private void showMessage(String text) {
        boolean has = text != null && !text.isBlank();
        messageLabel.setText(has ? text : "");
        messageLabel.setVisible(has);
        messageLabel.setManaged(has);
    }
}
