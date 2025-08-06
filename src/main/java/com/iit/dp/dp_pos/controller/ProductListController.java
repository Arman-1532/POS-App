package com.iit.dp.dp_pos.controller;

import com.iit.dp.dp_pos.util.ActivityLogger;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.util.Callback;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;

public class ProductListController {
    private MainController mainController;
    
    @FXML private TableView<ProductItem> productTable;
    @FXML private TableColumn<ProductItem, String> nameColumn;
    @FXML private TableColumn<ProductItem, Integer> quantityColumn;
    @FXML private TableColumn<ProductItem, Double> priceColumn;
    @FXML private TableColumn<ProductItem, String> statusColumn;
    @FXML private TableColumn<ProductItem, Void> actionColumn;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;
    
    @FXML private Label totalProductsLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label outOfStockLabel;
    
    private ObservableList<ProductItem> allProducts;
    private FilteredList<ProductItem> filteredProducts;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupSearchAndFilter();
        setupStatistics();
        loadProducts();
    }
    
    private void setupTableColumns() {
        // Name column with icon
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setCellFactory(new Callback<TableColumn<ProductItem, String>, TableCell<ProductItem, String>>() {
            @Override
            public TableCell<ProductItem, String> call(TableColumn<ProductItem, String> param) {
                return new TableCell<ProductItem, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                        } else {
                            HBox hbox = new HBox(10);
                            hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                            Label icon = new Label("📦");
                            icon.setStyle("-fx-font-size: 16px;");
                            Label name = new Label(item);
                            name.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
                            hbox.getChildren().addAll(icon, name);
                            setGraphic(hbox);
                        }
                    }
                };
            }
        });
        
        // Quantity column with units
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityColumn.setCellFactory(new Callback<TableColumn<ProductItem, Integer>, TableCell<ProductItem, Integer>>() {
            @Override
            public TableCell<ProductItem, Integer> call(TableColumn<ProductItem, Integer> param) {
                return new TableCell<ProductItem, Integer>() {
                    @Override
                    protected void updateItem(Integer item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                        } else {
                            HBox hbox = new HBox(5);
                            hbox.setAlignment(javafx.geometry.Pos.CENTER);
                            Label quantity = new Label(item.toString());
                            quantity.setStyle("-fx-font-weight: bold;");
                            Label units = new Label("units");
                            units.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
                            hbox.getChildren().addAll(quantity, units);
                            setGraphic(hbox);
                        }
                    }
                };
            }
        });
        
        // Price column with currency symbol
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceColumn.setCellFactory(new Callback<TableColumn<ProductItem, Double>, TableCell<ProductItem, Double>>() {
            @Override
            public TableCell<ProductItem, Double> call(TableColumn<ProductItem, Double> param) {
                return new TableCell<ProductItem, Double>() {
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
        
        // Status column
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(new Callback<TableColumn<ProductItem, String>, TableCell<ProductItem, String>>() {
            @Override
            public TableCell<ProductItem, String> call(TableColumn<ProductItem, String> param) {
                return new TableCell<ProductItem, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                        } else {
                            Label status = new Label(item);
                            status.setStyle("-fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 4 8;");
                            
                            switch (item) {
                                case "In Stock":
                                    status.setStyle(status.getStyle() + " -fx-background-color: #4CAF50; -fx-text-fill: white;");
                                    break;
                                case "Low Stock":
                                    status.setStyle(status.getStyle() + " -fx-background-color: #FFC107; -fx-text-fill: white;");
                                    break;
                                case "Out of Stock":
                                    status.setStyle(status.getStyle() + " -fx-background-color: #F44336; -fx-text-fill: white;");
                                    break;
                            }
                            setGraphic(status);
                        }
                    }
                };
            }
        });
        
        // Action column with enhanced buttons
        actionColumn.setCellFactory(new Callback<TableColumn<ProductItem, Void>, TableCell<ProductItem, Void>>() {
            @Override
            public TableCell<ProductItem, Void> call(final TableColumn<ProductItem, Void> param) {
                return new TableCell<ProductItem, Void>() {
                    private final Button incBtn = new Button("➕");
                    private final Button decBtn = new Button("➖");
                    private final Button editBtn = new Button("✏️");
                    
                    {
                        incBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5px; -fx-min-width: 30px;");
                        decBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-background-radius: 5px; -fx-min-width: 30px;");
                        editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 5px; -fx-min-width: 30px;");
                        
                        incBtn.setOnAction(event -> {
                            ProductItem item = getTableView().getItems().get(getIndex());
                            updateQuantity(item, 1);
                        });
                        decBtn.setOnAction(event -> {
                            ProductItem item = getTableView().getItems().get(getIndex());
                            updateQuantity(item, -1);
                        });
                        editBtn.setOnAction(event -> {
                            ProductItem item = getTableView().getItems().get(getIndex());
                            showEditDialog(item);
                        });
                    }
                    
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox box = new HBox(5);
                            box.setAlignment(javafx.geometry.Pos.CENTER);
                            box.getChildren().addAll(incBtn, decBtn, editBtn);
                            setGraphic(box);
                        }
                    }
                };
            }
        });
    }
    
    private void setupSearchAndFilter() {
        // Setup filter options
        filterComboBox.getItems().addAll("All Products", "In Stock", "Low Stock", "Out of Stock");
        filterComboBox.setValue("All Products");
        
        // Setup search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts();
        });
        
        filterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts();
        });
    }
    
    private void setupStatistics() {
        // Statistics will be updated when products are loaded
    }
    
    private void filterProducts() {
        String searchText = searchField.getText().toLowerCase();
        String filterValue = filterComboBox.getValue();
        
        filteredProducts.setPredicate(product -> {
            boolean matchesSearch = product.getName().toLowerCase().contains(searchText);
            boolean matchesFilter = true;
            
            if (filterValue != null) {
                switch (filterValue) {
                    case "In Stock":
                        matchesFilter = product.getQuantity() > 10;
                        break;
                    case "Low Stock":
                        matchesFilter = product.getQuantity() > 0 && product.getQuantity() <= 10;
                        break;
                    case "Out of Stock":
                        matchesFilter = product.getQuantity() == 0;
                        break;
                }
            }
            
            return matchesSearch && matchesFilter;
        });
    }

    private ObservableList<ProductItem> getProductList() {
        ObservableList<ProductItem> products = FXCollections.observableArrayList();
        String sql = "SELECT name, quantity, price FROM products ORDER BY name";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:pos.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(new ProductItem(
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    rs.getDouble("price")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not load products: " + e.getMessage());
        }
        return products;
    }
    
    private void loadProducts() {
        allProducts = getProductList();
        filteredProducts = new FilteredList<>(allProducts, p -> true);
        productTable.setItems(filteredProducts);
        updateStatistics();
    }
    
    private void updateStatistics() {
        int total = allProducts.size();
        int lowStock = (int) allProducts.stream().filter(p -> p.getQuantity() > 0 && p.getQuantity() <= 10).count();
        int outOfStock = (int) allProducts.stream().filter(p -> p.getQuantity() == 0).count();
        
        totalProductsLabel.setText(String.valueOf(total));
        lowStockLabel.setText(String.valueOf(lowStock));
        outOfStockLabel.setText(String.valueOf(outOfStock));
    }
    
//    private void updateQuantity(ProductItem item, int delta) {
//        int newQuantity = item.getQuantity() + delta;
//        if (newQuantity < 0) {
//            showAlert("Error", "Quantity cannot be negative!");
//            return;
//        }
//
//        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:pos.db")) {
//            PreparedStatement stmt = conn.prepareStatement("UPDATE products SET quantity = ? WHERE name = ?");
//            stmt.setInt(1, newQuantity);
//            stmt.setString(2, item.getName());
//            stmt.executeUpdate();
//            item.setQuantity(newQuantity);
//            productTable.refresh();
//            updateStatistics();
//        } catch (Exception e) {
//            e.printStackTrace();
//            showAlert("Error", "Could not update quantity: " + e.getMessage());
//        }
//    }

    private void updateQuantity(ProductItem item, int delta) {
        int oldQuantity = item.getQuantity();
        int newQuantity = oldQuantity + delta;

        if (newQuantity < 0) {
            showAlert("Error", "Quantity cannot be negative!");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:pos.db")) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE products SET quantity = ? WHERE name = ?");
            stmt.setInt(1, newQuantity);
            stmt.setString(2, item.getName());
            stmt.executeUpdate();

            item.setQuantity(newQuantity);
            productTable.refresh();
            updateStatistics();

            // ✅ Logging logic starts here
            String email = ActivityLogger.getEmailByUserId(AuthController.loggedInUserId);
            String action = (delta > 0) ? "Restocked Product" : "Reduced Product Stock";
            String details = String.format("Product: %s, Old Qty: %d, New Qty: %d", item.getName(), oldQuantity, newQuantity);

            ActivityLogger.log(email, action, details);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not update quantity: " + e.getMessage());
        }
    }


    private void showEditDialog(ProductItem item) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(item.getPrice()));
        dialog.setTitle("Edit Product Price");
        dialog.setHeaderText("Edit price for " + item.getName());
        dialog.setContentText("Enter new price:");
        
        dialog.showAndWait().ifPresent(newPrice -> {
            try {
                double price = Double.parseDouble(newPrice);
                if (price < 0) {
                    showAlert("Error", "Price cannot be negative!");
                    return;
                }
                
                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:pos.db")) {
                    PreparedStatement stmt = conn.prepareStatement("UPDATE products SET price = ? WHERE name = ?");
                    stmt.setDouble(1, price);
                    stmt.setString(2, item.getName());
                    stmt.executeUpdate();
                    item.setPrice(price);
                    productTable.refresh();
                    showAlert("Success", "Price updated successfully!");
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error", "Could not update price: " + e.getMessage());
                }
            } catch (NumberFormatException e) {
                showAlert("Error", "Please enter a valid number!");
            }
        });
    }

    public static class ProductItem {
        private final String name;
        private int quantity;
        private double price;
        
        public ProductItem(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
        
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public void setPrice(double price) { this.price = price; }
        
        public String getStatus() {
            if (quantity == 0) return "Out of Stock";
            if (quantity <= 10) return "Low Stock";
            return "In Stock";
        }
    }

    @FXML
    private void onBackClick(javafx.event.ActionEvent event) {
        // Navigate back to appropriate dashboard based on user type
        if (com.iit.dp.dp_pos.util.UserSession.isAdmin()) {
            mainController.loadView("hello-view.fxml");
        } else {
            mainController.loadView("employee-dashboard-view.fxml");
        }
    }

    @FXML
    public void onBackButtonClick(javafx.event.ActionEvent event) {
        mainController.loadView("employee-dashboard-view.fxml");
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
