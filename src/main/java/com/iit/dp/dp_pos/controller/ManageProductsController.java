package com.iit.dp.dp_pos.controller;

import com.iit.dp.dp_pos.util.ActivityLogger;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import com.iit.dp.dp_pos.model.Product;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

public class ManageProductsController {
    private MainController mainController;
    
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> idColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Integer> quantityColumn;
    @FXML private TableColumn<Product, Void> actionColumn;
    @FXML private TextField newProductNameField;
    @FXML private TextField newProductPriceField;
    @FXML private Label messageLabel;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        // Price column with green color styling
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceColumn.setCellFactory(new Callback<TableColumn<Product, Double>, TableCell<Product, Double>>() {
            @Override
            public TableCell<Product, Double> call(TableColumn<Product, Double> param) {
                return new TableCell<Product, Double>() {
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                        } else {
                            Label price = new Label("$" + String.format("%.2f", item));
                            price.setStyle("-fx-font-weight: bold; -fx-text-fill: #4CAF50;");
                            setGraphic(price);
                        }
                    }
                };
            }
        });
        
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        addActionButtonsToTable();
        loadProducts();
    }

    // load products from database
    private void loadProducts() {
        ObservableList<Product> products = FXCollections.observableArrayList();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:pos.db")) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name, price, quantity FROM products");
            while (rs.next()) {
                products.add(new Product(rs.getInt("id"), rs.getString("name"), rs.getDouble("price"), rs.getInt("quantity")));
            }
        } catch (Exception e) {
            messageLabel.setText("Error loading products: " + e.getMessage());
        }
        productTable.setItems(products);
    }

    // action button with styled increment/decrement buttons
    private void addActionButtonsToTable() {
        actionColumn.setCellFactory(param -> new TableCell<Product, Void>() {
            private final Button incBtn = new Button("➕");
            private final Button decBtn = new Button("➖");
            {
                // Style the buttons with green and red colors
                incBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5px; -fx-min-width: 30px;");
                decBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-background-radius: 5px; -fx-min-width: 30px;");
                
                incBtn.setOnAction(event -> {
                    Product item = getTableView().getItems().get(getIndex());
                    updateQuantity(item, 1);
                });
                decBtn.setOnAction(event -> {
                    Product item = getTableView().getItems().get(getIndex());
                    updateQuantity(item, -1);
                });
            }
            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5, incBtn, decBtn);
                    setGraphic(box);
                }
            }
        });
    }

    // increment and decrement update in database
//    private void updateQuantity(Product item, int delta) {
//        int newQuantity = item.getQuantity() + delta;
//        if (newQuantity < 0) return;
//        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:pos.db")) {
//            PreparedStatement stmt = conn.prepareStatement("UPDATE products SET quantity = ? WHERE id = ?");
//            stmt.setInt(1, newQuantity);
//            stmt.setInt(2, item.getId());
//            stmt.executeUpdate();
//            item.setQuantity(newQuantity);
//            productTable.refresh();
//        } catch (Exception e) {
//            messageLabel.setText("Error updating quantity: " + e.getMessage());
//        }
//    }
    private void updateQuantity(Product item, int delta) {
        int oldQuantity = item.getQuantity();
        int newQuantity = oldQuantity + delta;
        if (newQuantity < 0) return;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:pos.db")) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE products SET quantity = ? WHERE id = ?");
            stmt.setInt(1, newQuantity);
            stmt.setInt(2, item.getId());
            stmt.executeUpdate();

            item.setQuantity(newQuantity);
            productTable.refresh();

            // Get user email using loggedInUserId
            String userEmail = ActivityLogger.getEmailByUserId(AuthController.loggedInUserId);

            // Determine the action type
            String action = (delta > 0) ? "Restock Product" : "Reduce Product Stock";

            // Log the activity
            ActivityLogger.log(userEmail, action,
                    String.format("ID: %d, Name: %s, Old Quantity: %d, New Quantity: %d",
                            item.getId(), item.getName(), oldQuantity, newQuantity));

        } catch (Exception e) {
            messageLabel.setText("Error updating quantity: " + e.getMessage());
        }
    }

    @FXML
//    private void onAddProductClick() {
//        String name = newProductNameField.getText();
//        String priceText = newProductPriceField.getText();
//        if (name == null || name.isEmpty() || priceText == null || priceText.isEmpty()) {
//            messageLabel.setText("Enter product name and price.");
//            return;
//        }
//        try {
//            double price = Double.parseDouble(priceText);
//            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:pos.db")) {
//                PreparedStatement stmt = conn.prepareStatement("INSERT INTO products (name, price) VALUES (?, ?)");
//                stmt.setString(1, name);
//                stmt.setDouble(2, price);
//                stmt.executeUpdate();
//                messageLabel.setText("Product added: " + name);
//                // Log the activity
//                //String userEmail = ActivityLogger.getEmailByUserId(AuthController.loggedInUserId);
//                //ActivityLogger.log(userEmail, "Add Product", "Name: Laptop, Price: 1000");
//                newProductNameField.clear();
//                newProductPriceField.clear();
//                loadProducts();
//
//            }
//        } catch (NumberFormatException e) {
//            messageLabel.setText("Invalid price.");
//        } catch (Exception e) {
//            messageLabel.setText("Error: " + e.getMessage());
//        }
//    }
    private void onAddProductClick() {
        String name = newProductNameField.getText();
        String priceText = newProductPriceField.getText();

        if (name == null || name.isEmpty() || priceText == null || priceText.isEmpty()) {
            messageLabel.setText("Enter product name and price.");
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:pos.db")) {
                // Insert product into DB
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO products (name, price) VALUES (?, ?)");
                stmt.setString(1, name);
                stmt.setDouble(2, price);
                stmt.executeUpdate();

                // Retrieve the newly added product's ID and data
                PreparedStatement selectStmt = conn.prepareStatement("SELECT id, name, price, quantity FROM products ORDER BY id DESC LIMIT 1");
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String dbName = rs.getString("name");
                    double dbPrice = rs.getDouble("price");
                    int dbQty = rs.getInt("quantity");

                    // Get user's email using loggedInUserId
                    String userEmail = ActivityLogger.getEmailByUserId(AuthController.loggedInUserId);

                    // Log actual DB-inserted data
                    ActivityLogger.log(userEmail, "Add Product",
                            String.format("ID: %d, Name: %s, Price: %.2f, Quantity: %d", id, dbName, dbPrice, dbQty));

                    messageLabel.setText("Product added: " + dbName);
                }

                newProductNameField.clear();
                newProductPriceField.clear();
                loadProducts();

            }
        } catch (NumberFormatException e) {
            messageLabel.setText("Invalid price.");
        } catch (Exception e) {
            messageLabel.setText("Error: " + e.getMessage());
        }
    }


    @FXML
    private void onBackClick() {
        mainController.loadView("hello-view.fxml");
    }
} 