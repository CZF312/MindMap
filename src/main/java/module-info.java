module com.example.mindmap {
    requires javafx.controls;
    requires java.desktop;
    requires java.prefs;
    requires java.xml;

    exports com.example.mindmap;
    exports com.example.mindmap.model;
    exports com.example.mindmap.view;
    exports com.example.mindmap.controller;
    exports com.example.mindmap.service;
}
