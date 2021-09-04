module com.lewisk.javafx_learn {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    opens com.lewisk.javafx_learn to javafx.fxml;
    exports com.lewisk.javafx_learn;
}