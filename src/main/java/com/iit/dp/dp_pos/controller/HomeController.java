package com.iit.dp.dp_pos.controller;

import javafx.fxml.FXML;

public class HomeController {
    private MainController mainController;
    
    // Admin Dashboard logic will be implemented here.

    /*@FXML
    public void onAddEmployeeClick() {
        System.out.println("Add Employee button clicked");
    }*/
    
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void onAddProductClick(javafx.event.ActionEvent event) {
        mainController.loadView("manage-products-view.fxml");
    }

    @FXML
    public void onProductListClick(javafx.event.ActionEvent event) {
        mainController.loadView("product-list-view.fxml");
    }

    @FXML
    public void onCustomerListClick(javafx.event.ActionEvent event) {
        mainController.loadView("customer-list-view.fxml");
    }

    @FXML
    public void onOrderListClick(javafx.event.ActionEvent event) {
        mainController.loadView("order-history-view.fxml");
    }

    @FXML
    public void onCreateSaleClick(javafx.event.ActionEvent event) {
        mainController.loadView("create-sale-view.fxml");
    }

    @FXML
    public void onBackToLoginClick(javafx.event.ActionEvent event) {
        mainController.loadView("login-view.fxml");
    }
}
