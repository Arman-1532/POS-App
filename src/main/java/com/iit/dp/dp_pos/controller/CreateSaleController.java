package com.iit.dp.dp_pos.controller;

import com.iit.dp.dp_pos.util.ActivityLogger;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CreateSaleController {
    private MainController mainController;
    
    @FXML private TextField searchField;
    @FXML private TableView<ProductEntry> productTable;
    @FXML private TableView<CartEntry> cartTable;
    @FXML private Label totalLabel;
    @FXML private TextField customerNameField;
    @FXML private TextField customerEmailField;  // Added email field
    @FXML private Label dateTimeLabel;
    @FXML private Button checkoutButton;
    @FXML private Button clearCartButton;
    @FXML private Button backButton;

    private final ObservableList<ProductEntry> products = FXCollections.observableArrayList();
    private final ObservableList<CartEntry> cartItems = FXCollections.observableArrayList();
    private FilteredList<ProductEntry> filteredProducts;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public static class ProductEntry {
        private final SimpleStringProperty name;
        private final SimpleDoubleProperty price;
        private final SimpleIntegerProperty quantity;

        public ProductEntry(String name, double price, int quantity) {
            this.name = new SimpleStringProperty(name);
            this.price = new SimpleDoubleProperty(price);
            this.quantity = new SimpleIntegerProperty(quantity);
        }

        public String getName() { return name.get(); }
        public double getPrice() { return price.get(); }
        public int getQuantity() { return quantity.get(); }
        public void setQuantity(int value) { quantity.set(value); }
    }

    public static class CartEntry {
        private final SimpleStringProperty name;
        private final SimpleDoubleProperty price;
        private final SimpleIntegerProperty quantity;
        private final SimpleDoubleProperty total;

        public CartEntry(String name, double price, int quantity) {
            this.name = new SimpleStringProperty(name);
            this.price = new SimpleDoubleProperty(price);
            this.quantity = new SimpleIntegerProperty(quantity);
            this.total = new SimpleDoubleProperty(price * quantity);

            this.quantity.addListener((obs, oldVal, newVal) ->
                this.total.set(this.price.get() * newVal.intValue()));
        }

        public String getName() { return name.get(); }
        public double getPrice() { return price.get(); }
        public int getQuantity() { return quantity.get(); }
        public void setQuantity(int value) { quantity.set(value); }
        public double getTotal() { return total.get(); }
    }

    @FXML
    public void initialize() {
        setupTables();
        setupSearch();
        loadProducts();
        setupDateTime();
        setupButtons();
        setupCustomerFields();
        cartItems.addListener((javafx.collections.ListChangeListener<CartEntry>) c -> updateTotal());
    }

    private void setupCustomerFields() {
        customerNameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !customerNameField.getText().trim().isEmpty()) {
                lookupCustomerEmail(customerNameField.getText().trim());
            }
        });
        customerEmailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                lookupCustomerName(newVal.trim());
            }
            // Removed clearing of customerNameField when email is empty
        });
    }

    // auto email lookup based on customer name
    private void lookupCustomerEmail(String name) {
        String url = "jdbc:sqlite:pos.db";
        String query = "SELECT email FROM customers WHERE name = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                customerEmailField.setText(rs.getString("email"));
            }
            // Removed clearing of customerEmailField if not found
        } catch (SQLException e) {
            showError("Database Error", "Could not lookup customer: " + e.getMessage());
        }
    }

    // auto name lookup based on customer email
    private void lookupCustomerName(String email) {
        String url = "jdbc:sqlite:pos.db";
        String query = "SELECT name FROM customers WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                customerNameField.setText(rs.getString("name"));
            }
            // Removed clearing of customerNameField if not found
        } catch (SQLException e) {
            showError("Database Error", "Could not lookup customer: " + e.getMessage());
        }
    }

    private void setupTables() {
        // Product table setup
        TableColumn<ProductEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> data.getValue().name);
        nameCol.setPrefWidth(200);

        TableColumn<ProductEntry, Number> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(data -> data.getValue().price);
        priceCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Number price, boolean empty) {
                super.updateItem(price, empty);
                if (empty) setText(null);
                else setText(String.format("$%.2f", price.doubleValue()));
            }
        });
        priceCol.setPrefWidth(100);

        TableColumn<ProductEntry, Number> quantityCol = new TableColumn<>("Available");
        quantityCol.setCellValueFactory(data -> data.getValue().quantity);
        quantityCol.setPrefWidth(100);

        productTable.getColumns().addAll(nameCol, priceCol, quantityCol);
        productTable.setItems(products);

        // Double-click handler for products table
        productTable.setRowFactory(tv -> {
            TableRow<ProductEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ProductEntry product = row.getItem();
                    if (product != null && product.getQuantity() > 0) {
                        addToCart(product);
                    }
                }
            });
            return row;
        });

        // Cart table setup
        TableColumn<CartEntry, String> cartNameCol = new TableColumn<>("Item");
        cartNameCol.setCellValueFactory(data -> data.getValue().name);
        cartNameCol.setPrefWidth(200);

        TableColumn<CartEntry, Number> cartPriceCol = new TableColumn<>("Price");
        cartPriceCol.setCellValueFactory(data -> data.getValue().price);
        cartPriceCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Number price, boolean empty) {
                super.updateItem(price, empty);
                if (empty) setText(null);
                else {
                    setText(String.format("$%.2f", price.doubleValue()));
                    setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                }
            }
        });
        cartPriceCol.setPrefWidth(100);

        TableColumn<CartEntry, Number> cartQuantityCol = new TableColumn<>("Quantity");
        cartQuantityCol.setCellValueFactory(data -> data.getValue().quantity);
        cartQuantityCol.setCellFactory(tc -> new TableCell<>() {
            private final Spinner<Integer> spinner = new Spinner<>(1, 100, 1);

            {
                spinner.setEditable(true);
                spinner.setPrefWidth(80);
            }

            // spinner value change handler
            @Override
            protected void updateItem(Number quantity, boolean empty) {
                super.updateItem(quantity, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    CartEntry entry = getTableView().getItems().get(getIndex());
                    ProductEntry product = products.stream()
                        .filter(p -> p.getName().equals(entry.getName()))
                        .findFirst()
                        .orElse(null);

                    if (product != null) {
                        int maxQuantity = product.getQuantity() + entry.getQuantity();
                        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                            1, maxQuantity, entry.getQuantity()));

                        spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                            int diff = oldVal - newVal;
                            product.setQuantity(product.getQuantity() + diff);
                            entry.setQuantity(newVal);
                            productTable.refresh();
                            updateTotal();
                        });
                    }
                    setGraphic(spinner);
                }
            }
        });
        cartQuantityCol.setPrefWidth(100);

        TableColumn<CartEntry, Number> cartTotalCol = new TableColumn<>("Total");
        cartTotalCol.setCellValueFactory(data -> data.getValue().total);
        cartTotalCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Number total, boolean empty) {
                super.updateItem(total, empty);
                if (empty) setText(null);
                else setText(String.format("$%.2f", total.doubleValue()));
            }
        });
        cartTotalCol.setPrefWidth(100);

        TableColumn<CartEntry, Void> removeCol = new TableColumn<>("Action");
        removeCol.setCellFactory(tc -> new TableCell<>() {
            private final Button removeButton = new Button("Remove");
            {
                removeButton.setOnAction(e -> {
                    CartEntry entry = getTableView().getItems().get(getIndex());
                    removeFromCart(entry);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeButton);
            }
        });
        removeCol.setPrefWidth(80);

        cartTable.getColumns().addAll(cartNameCol, cartPriceCol, cartQuantityCol, cartTotalCol, removeCol);
        cartTable.setItems(cartItems);
    }

    private void setupSearch() {
        filteredProducts = new FilteredList<>(products, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) ->
            filteredProducts.setPredicate(product ->
                newVal == null || newVal.isEmpty() ||
                product.getName().toLowerCase().contains(newVal.toLowerCase())
            )
        );
        productTable.setItems(filteredProducts);
    }

    private void setupDateTime() {
        dateTimeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    private void setupButtons() {
        clearCartButton.setOnAction(e -> clearCart());
        checkoutButton.setOnAction(e -> processCheckout());
        backButton.setOnAction(e -> goBack(e));
    }

    private void loadProducts() {
        String url = "jdbc:sqlite:pos.db";
        String query = "SELECT name, quantity, price FROM products WHERE quantity > 0";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                products.add(new ProductEntry(
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getInt("quantity")
                ));
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not load products: " + e.getMessage());
        }
    }

    private void addToCart(ProductEntry product) {
        if (product.getQuantity() <= 0) {
            showError("Out of Stock", "This product is out of stock.");
            return;
        }

        // Check if product already in cart
        for (CartEntry entry : cartItems) {
            if (entry.getName().equals(product.getName())) {
                if (entry.getQuantity() < product.getQuantity()) {
                    entry.setQuantity(entry.getQuantity() + 1);
                    product.setQuantity(product.getQuantity() - 1);  // Update available quantity
                    cartTable.refresh();
                    productTable.refresh();
                } else {
                    showError("Stock Limit", "Cannot add more of this item - stock limit reached.");
                }
                return;
            }
        }

        // Add new item to cart
        cartItems.add(new CartEntry(product.getName(), product.getPrice(), 1));
        product.setQuantity(product.getQuantity() - 1);  // Update available quantity
        cartTable.refresh();
        productTable.refresh();
        updateTotal();
    }

    private void removeFromCart(CartEntry cartEntry) {
        // Find the corresponding product and restore its quantity
        for (ProductEntry product : products) {
            if (product.getName().equals(cartEntry.getName())) {
                product.setQuantity(product.getQuantity() + cartEntry.getQuantity());
                break;
            }
        }
        cartItems.remove(cartEntry);
        productTable.refresh();
        updateTotal();
    }

    private void clearCart() {
        // Restore all product quantities before clearing cart
        for (CartEntry cartEntry : cartItems) {
            for (ProductEntry product : products) {
                if (product.getName().equals(cartEntry.getName())) {
                    product.setQuantity(product.getQuantity() + cartEntry.getQuantity());
                    break;
                }
            }
        }
        cartItems.clear();
        productTable.refresh();
        updateTotal();
    }

    private void goBack(javafx.event.ActionEvent event) {
        // Navigate back to appropriate dashboard based on user type
        if (com.iit.dp.dp_pos.util.UserSession.isAdmin()) {
            mainController.loadView("hello-view.fxml");
        } else {
            mainController.loadView("employee-dashboard-view.fxml");
        }
    }

    private void processCheckout() {
        if (cartItems.isEmpty()) {
            showError("Empty Cart", "Please add items to cart before checkout.");
            return;
        }

        if (customerNameField.getText().trim().isEmpty()) {
            showError("Missing Information", "Please enter customer name.");
            return;
        }

        if (customerEmailField.getText().trim().isEmpty()) {
            showError("Missing Information", "Please enter customer email.");
            return;
        }

        String url = "jdbc:sqlite:pos.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);
            try {
                // Check if customer exists or create new one
                int customerId = getOrCreateCustomer(conn,
                    customerNameField.getText().trim(),
                    customerEmailField.getText().trim());

                // Update product quantities
                String updateProductSql = "UPDATE products SET quantity = quantity - ? WHERE name = ?";
                PreparedStatement updateProductStmt = conn.prepareStatement(updateProductSql);

                for (CartEntry item : cartItems) {
                    updateProductStmt.setInt(1, item.getQuantity());
                    updateProductStmt.setString(2, item.getName());
                    updateProductStmt.executeUpdate();
                }

                // Create order with customer ID
                String createOrderSql = "INSERT INTO orders (customer_id, status) VALUES (?, 'COMPLETED')";
                PreparedStatement orderStmt = conn.prepareStatement(createOrderSql, Statement.RETURN_GENERATED_KEYS);
                orderStmt.setInt(1, customerId);
                orderStmt.executeUpdate();

                ResultSet rs = orderStmt.getGeneratedKeys();
                int orderId = rs.next() ? rs.getInt(1) : 0;

                // Create order items
                String createOrderItemSql = "INSERT INTO order_items (order_id, product_id, quantity) VALUES (?, ?, ?)";
                PreparedStatement orderItemStmt = conn.prepareStatement(createOrderItemSql);

                for (CartEntry item : cartItems) {
                    orderItemStmt.setInt(1, orderId);
                    // Get product ID by name
                    PreparedStatement productStmt = conn.prepareStatement("SELECT id FROM products WHERE name = ?");
                    productStmt.setString(1, item.getName());
                    ResultSet productRs = productStmt.executeQuery();
                    if (productRs.next()) {
                        orderItemStmt.setInt(2, productRs.getInt("id"));
                        orderItemStmt.setInt(3, item.getQuantity());
                        orderItemStmt.executeUpdate();
                    }
                }

                conn.commit();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Sale Completed");
                alert.setContentText("Sale has been processed successfully!");
                alert.showAndWait();

                // Prepare product list string
                StringBuilder productsPurchased = new StringBuilder();
                for (CartEntry item : cartItems) {
                    productsPurchased.append(item.getName())
                            .append(" (x").append(item.getQuantity()).append("), ");
                }
                if (productsPurchased.length() > 2) {
                    productsPurchased.setLength(productsPurchased.length() - 2); // remove last comma and space
                }

// Calculate total from DB (recommended to use Java side, but here's the DB version if you prefer)
                double total = 0.0;
                String totalQuery = "SELECT SUM(oi.quantity * p.price) AS total " +
                        "FROM order_items oi JOIN products p ON oi.product_id = p.id WHERE oi.order_id = ?";
                PreparedStatement totalStmt = conn.prepareStatement(totalQuery);
                totalStmt.setInt(1, orderId);
                ResultSet totalRs = totalStmt.executeQuery();
                if (totalRs.next()) {
                    total = totalRs.getDouble("total");
                }

// Get current user's email from session
                String userEmail = com.iit.dp.dp_pos.util.UserSession.getCurrentUserEmail();  // You may need to implement this if not done

// Log the order
                ActivityLogger.log(
                        userEmail,
                        "ORDER_PLACED",
                        String.format("Products: [%s], Total: $%.2f", productsPurchased.toString(), total)
                );


                clearCart();
                customerNameField.clear();
                customerEmailField.clear();
                products.clear();
                loadProducts();

            } catch (SQLException e) {
                conn.rollback();
                showError("Transaction Error", "Could not process sale: " + e.getMessage());
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not connect to database: " + e.getMessage());
        }
    }

    private int getOrCreateCustomer(Connection conn, String name, String email) throws SQLException {
        // First try to find existing customer
        String findSql = "SELECT id FROM customers WHERE name = ? AND email = ?";
        PreparedStatement findStmt = conn.prepareStatement(findSql);
        findStmt.setString(1, name);
        findStmt.setString(2, email);
        ResultSet rs = findStmt.executeQuery();

        if (rs.next()) {
            return rs.getInt("id");
        }

        // If not found, create new customer
        String createSql = "INSERT INTO customers (name, email) VALUES (?, ?)";
        PreparedStatement createStmt = conn.prepareStatement(createSql, Statement.RETURN_GENERATED_KEYS);
        createStmt.setString(1, name);
        createStmt.setString(2, email);
        createStmt.executeUpdate();

        rs = createStmt.getGeneratedKeys();
        return rs.next() ? rs.getInt(1) : 0;
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void updateTotal() {
        double total = cartItems.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        totalLabel.setText(String.format("Total: $%.2f", total));
    }
}
