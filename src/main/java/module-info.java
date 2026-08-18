module com.teacherflow.app {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.teacherflow.app to javafx.fxml;
    exports com.teacherflow.app;
}
