module jsch.sftp {
    // JavaFX dependencies
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.dustinredmond.fxtrayicon;

    // Spring Boot dependencies
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.core;

    // JSch dependency
    requires jsch;

    //awt
    requires java.desktop;

    // Open packages to JavaFX
    opens com.example.jsch_sftp.controller to javafx.fxml ,spring.core ,spring.beans, spring.context;
    opens com.example.jsch_sftp.config to spring.core ,spring.beans, spring.context;
    opens com.example.jsch_sftp.service to spring.core, spring.beans, spring.context;
    opens com.example.jsch_sftp to spring.core, spring.beans, spring.context;




    // Export your main package
    exports com.example.jsch_sftp.service;
    exports com.example.jsch_sftp.config;
    exports com.example.jsch_sftp.controller;
    exports com.example.jsch_sftp;
}
