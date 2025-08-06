package com.iit.dp.dp_pos.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class OrderHistoryController {
    private MainController mainController;
    
    @FXML private TableView<OrderItem> orderTable;
    @FXML private TableColumn<OrderItem, String> customerNameColumn;
    @FXML private TableColumn<OrderItem, String> productNameColumn;
    @FXML private TableColumn<OrderItem, Integer> quantityColumn;
    @FXML private TableColumn<OrderItem, Double> priceColumn;
    @FXML private TableColumn<OrderItem, Double> totalColumn;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceColumn.setCellFactory(col -> new javafx.scene.control.TableCell<OrderItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("$" + String.format("%.2f", item));
                }
            }
        });

        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalColumn.setCellFactory(col -> new javafx.scene.control.TableCell<OrderItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("$" + String.format("%.2f", item));
                    setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                }
            }
        });
        orderTable.setItems(getOrderList());
    }

    private ObservableList<OrderItem> getOrderList() {
        ObservableList<OrderItem> orders = FXCollections.observableArrayList();
        String sql = "SELECT c.name AS customerName, p.name AS productName, oi.quantity, p.price, (oi.quantity * p.price) AS total " +
                     "FROM orders o " +
                     "JOIN customers c ON o.customer_id = c.id " +
                     "JOIN order_items oi ON o.id = oi.order_id " +
                     "JOIN products p ON oi.product_id = p.id " +
                     "ORDER BY o.date DESC";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:pos.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                orders.add(new OrderItem(
                    rs.getString("customerName"),
                    rs.getString("productName"),
                    rs.getInt("quantity"),
                    rs.getDouble("price"),
                    rs.getDouble("total")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orders;
    }

    public static class OrderItem {
        private final String customerName;
        private final String productName;
        private final int quantity;
        private final double price;
        private final double total;
        public OrderItem(String customerName, String productName, int quantity, double price, double total) {
            this.customerName = customerName;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
            this.total = total;
        }
        public String getCustomerName() { return customerName; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public double getTotal() { return total; }
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
}
