module com.bakbak.javafx_proj_1_2 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens com.bakbak.javafx_proj_1_2 to javafx.fxml;
    exports com.bakbak.javafx_proj_1_2;
}