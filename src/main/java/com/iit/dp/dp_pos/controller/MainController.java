package com.iit.dp.dp_pos.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import com.iit.dp.dp_pos.util.UserSession;

import java.io.IOException;

public class MainController {
    @FXML private StackPane contentArea;
    @FXML private Label titleLabel;
    @FXML private Label userInfoLabel;
    
    private Stage stage;
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    @FXML
    public void initialize() {
        // Start with login view
        loadView("login-view.fxml");
    }
    
    public void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/iit/dp/dp_pos/" + fxmlFile));
            Parent view = loader.load();
            
            // Set the main controller reference in child controllers
            Object controller = loader.getController();
            if (controller instanceof AuthController) {
                ((AuthController) controller).setMainController(this);
            } else if (controller instanceof HomeController) {
                ((HomeController) controller).setMainController(this);
            } else if (controller instanceof EmployeeDashboardController) {
                ((EmployeeDashboardController) controller).setMainController(this);
            } else if (controller instanceof ProductListController) {
                ((ProductListController) controller).setMainController(this);
            } else if (controller instanceof CustomerListController) {
                ((CustomerListController) controller).setMainController(this);
            } else if (controller instanceof OrderHistoryController) {
                ((OrderHistoryController) controller).setMainController(this);
            } else if (controller instanceof CreateSaleController) {
                ((CreateSaleController) controller).setMainController(this);
            } else if (controller instanceof ManageProductsController) {
                ((ManageProductsController) controller).setMainController(this);
            }
            
            // Clear and set new content
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            
            // Update title based on view
            updateTitle(fxmlFile);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void updateTitle(String fxmlFile) {
        switch (fxmlFile) {
            case "login-view.fxml":
                titleLabel.setText("Login - Supershop POS System");
                break;
            case "hello-view.fxml":
                titleLabel.setText("Admin Dashboard - Supershop POS System");
                break;
            case "employee-dashboard-view.fxml":
                titleLabel.setText("Employee Dashboard - Supershop POS System");
                break;
            case "product-list-view.fxml":
                titleLabel.setText("Product List - Supershop POS System");
                break;
            case "customer-list-view.fxml":
                titleLabel.setText("Customer List - Supershop POS System");
                break;
            case "order-history-view.fxml":
                titleLabel.setText("Order History - Supershop POS System");
                break;
            case "create-sale-view.fxml":
                titleLabel.setText("Create Sale - Supershop POS System");
                break;
            case "manage-products-view.fxml":
                titleLabel.setText("Manage Products - Supershop POS System");
                break;
            default:
                titleLabel.setText("Supershop POS System");
        }
    }
    
    public void updateUserInfo(String userInfo) {
        userInfoLabel.setText(userInfo);
    }
    
    @FXML
    private void onLogoutClick() {
        // Clear user session
        AuthController.loggedInUserId = -1;
        // Load login view
        loadView("login-view.fxml");
        updateUserInfo("Welcome");
    }
} 