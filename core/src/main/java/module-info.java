module com.courseflow.core {
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens com.courseflow.model to com.fasterxml.jackson.databind;

    exports com.courseflow.model;
    exports com.courseflow.persistence;
    exports com.courseflow.cli;
    exports com.courseflow.io;
    exports com.courseflow.util;
}
