module com.teacherflow.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens com.teacherflow.app to javafx.fxml;
    opens com.teacherflow.model to com.fasterxml.jackson.databind;

    exports com.teacherflow.app;
    exports com.teacherflow.model;
    exports com.teacherflow.persistence;
}
