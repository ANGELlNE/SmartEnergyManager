module com.smartenergy {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.smartenergy to javafx.fxml;
    exports com.smartenergy;
}
