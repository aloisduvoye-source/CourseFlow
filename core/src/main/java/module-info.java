module com.teacherflow.core {
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens com.teacherflow.model to com.fasterxml.jackson.databind;

    exports com.teacherflow.model;
    exports com.teacherflow.persistence;
    exports com.teacherflow.cli;
    exports com.teacherflow.io;
    exports com.teacherflow.util;
}
