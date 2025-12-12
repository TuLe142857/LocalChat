module LocalChat {
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.google.gson;
    requires jdk.compiler;
    requires org.tinylog.api;

//    requires org.

//    requires com.google.gson;
    opens edu.ptithcm.model to com.google.gson;
    exports edu.ptithcm;
    opens edu.ptithcm.network.packet to com.google.gson;
}