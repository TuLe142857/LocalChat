module LocalChat {
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.google.gson;
    requires jdk.compiler;
    requires org.tinylog.api;


    // open data packet for gson
    opens edu.ptithcm.model to com.google.gson;
    opens edu.ptithcm.network.packet to com.google.gson;
    opens edu.ptithcm.network.packet.payload to com.google.gson;

    exports edu.ptithcm;
}