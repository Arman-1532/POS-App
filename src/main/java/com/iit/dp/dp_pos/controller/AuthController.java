package com.iit.dp.dp_pos.controller;

import com.iit.dp.dp_pos.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.io.IOException;
import com.iit.dp.dp_pos.util.ActivityLogger;

public class AuthController {
    public static int loggedInUserId = -1;
    private Stage stage;
    private MainController mainController;
    public static String userType = null ;

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label loginMessageLabel;

    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    protected void login(javafx.event.ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            loginMessageLabel.setText("Please enter email and password.");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:pos.db")) {
            String sql = "SELECT id, password, type, email FROM users WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                userType = rs.getString("type");
                int userId = rs.getInt("id");
                String userEmail = rs.getString("email");

                if (org.mindrot.jbcrypt.BCrypt.checkpw(password, hashedPassword)) {
                    loggedInUserId = userId;
                    
                    // Update user info in main controller
                    mainController.updateUserInfo("Logged in as: " + userEmail + " (" + userType + ")");
                    
                    // Load different dashboard based on user type
                    String dashboardFxml = userType.equalsIgnoreCase("admin") ?
                        "hello-view.fxml" :
                        "employee-dashboard-view.fxml";

                    mainController.loadView(dashboardFxml);
                    ActivityLogger.log(userEmail, "login", "User logged in successfully");
                } else {
                    loginMessageLabel.setText("Invalid password.");
                }
            } else {
                loginMessageLabel.setText("User not found.");
            }
        } catch (Exception e) {
            loginMessageLabel.setText("Login error: " + e.getMessage());
            e.printStackTrace();
        }
    }

   /* @FXML
    protected void onGotoAdminDashboard() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/iit/dp/dp_pos/hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        HomeController homeController = fxmlLoader.getController();
        stage.setScene(scene);
        stage.show();
    }*/

  
}
