package org.example.demo;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class Util {

    public static TextField createStyledTextField(Pane parentPane, double x, double y, String promptText) {
        TextField textField = new TextField();
        textField.setLayoutX(x);
        textField.setLayoutY(y);
        textField.setPrefSize(200, 40);
        String baseStyle = "-fx-background-color: rgba(50, 50, 50, 0.9);" + "-fx-text-fill: white;" + "-fx-font-size: 14px;" + "-fx-background-radius: 5;" + "-fx-border-color: rgba(255,255,255,0.2);" + "-fx-border-radius: 5;";
        textField.setStyle(baseStyle);
        textField.setPromptText(promptText);
        parentPane.getChildren().add(textField);
        return textField;
    }

    public static PasswordField createStyledPwField(Pane parentPane, double x, double y, String promptText) {
        PasswordField psdField = new PasswordField();
        psdField.setLayoutX(x);
        psdField.setLayoutY(y);
        psdField.setPrefSize(200, 40);
        String baseStyle = "-fx-background-color: rgba(50, 50, 50, 0.9);" + "-fx-text-fill: white;" + "-fx-font-size: 14px;" + "-fx-background-radius: 5;" + "-fx-border-color: rgba(255,255,255,0.2);" + "-fx-border-radius: 5;";
        psdField.setStyle(baseStyle);
        psdField.setPromptText(promptText);
        parentPane.getChildren().add(psdField);
        return psdField;
    }

    public static Button createStyledButton(Pane parentPane, double x, double y, double width, String text) {
        Button button = new Button(text);
        button.setLayoutX(x);
        button.setLayoutY(y);
        button.setPrefSize(width, 40);
        String baseStyle = "-fx-background-color: rgba(50, 50, 50, 0.6);" + "-fx-text-fill: white;" + "-fx-font-size: 14px;" + "-fx-background-radius: 5;" + "-fx-border-color: rgba(255,255,255,0.2);" + "-fx-border-radius: 5;";
        String hoverStyle = "-fx-background-color: rgba(30, 30, 30, 0.75);" + "-fx-text-fill: white;" + "-fx-font-size: 14px;" + "-fx-border-color: rgba(255,255,255,0.4);";
        String darkStyle = "-fx-background-color: rgba(30, 30, 30, 0.9);" + "-fx-text-fill: white;" + "-fx-font-size: 14px;" + "-fx-border-color: rgba(255,255,255,0.4);";
        button.setStyle(baseStyle);
        button.setOnMouseEntered(_ -> button.setStyle(hoverStyle));
        button.setOnMousePressed(_ -> button.setStyle(darkStyle));
        button.setOnMouseClicked(_ -> button.setStyle(hoverStyle));
        button.setOnMouseExited(_ -> button.setStyle(baseStyle));
        parentPane.getChildren().add(button);
        return button;
    }

    public static Button createLobbyButton(HBox box, double width,double height, String text) {
        Button button = new Button(text);
        button.setPrefSize(width, height);
        String baseStyle = "-fx-background-color: rgba(50, 50, 50, 0.6);" + "-fx-text-fill: white;" + "-fx-font-size: 14px;" + "-fx-background-radius: 5;" + "-fx-border-color: rgba(255,255,255,0.2);" + "-fx-border-radius: 5;";
        String hoverStyle = "-fx-background-color: rgba(60, 30, 30, 0.75);" + "-fx-text-fill: white;" + "-fx-font-size: 14px;" + "-fx-border-color: rgba(255,255,255,0.4);";
        String darkStyle = "-fx-background-color: rgba(60, 30, 30, 0.9);" + "-fx-text-fill: white;" + "-fx-font-size: 14px;" + "-fx-border-color: rgba(255,255,255,0.4);";
        button.setStyle(baseStyle);
        button.setOnMouseEntered(_ -> button.setStyle(hoverStyle));
        button.setOnMousePressed(_ -> button.setStyle(darkStyle));
        button.setOnMouseClicked(_ -> button.setStyle(hoverStyle));
        button.setOnMouseExited(_ -> button.setStyle(baseStyle));
        box.getChildren().add(button);
        return button;
    }
}
