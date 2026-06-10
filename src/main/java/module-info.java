module com.bakbak.javafx_proj_1_2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.media;

    requires org.controlsfx.controls;

    opens com.bakbak.javafx_proj_1_2 to javafx.fxml;
    opens com.bakbak.javafx_proj_1_2.controller to javafx.fxml;
    exports com.bakbak.javafx_proj_1_2;
    exports com.bakbak.javafx_proj_1_2.controller;

}
