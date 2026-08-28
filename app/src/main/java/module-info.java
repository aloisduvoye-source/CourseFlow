module com.courseflow.app {
    requires com.courseflow.core;
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;

    opens com.courseflow.app to javafx.fxml;

    exports com.courseflow.app;
}
