module org.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.graphics;
    requires javafx.media;


    opens org.example.demo to javafx.fxml;
    exports org.example.demo;
}