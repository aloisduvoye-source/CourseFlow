module com.teacherflow.app {
    requires com.teacherflow.core;
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;

    opens com.teacherflow.app to javafx.fxml;

    exports com.teacherflow.app;
}
