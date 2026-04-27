module com.smartenergy {
    requires transitive javafx.controls;
    requires javafx.fxml;

    opens com.smartenergy to javafx.fxml;
    exports com.smartenergy;
    exports com.smartenergy.enumeration;
    exports com.smartenergy.model;
}
