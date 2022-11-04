module main.app {
    requires image.service;
    requires miglayout;
    requires java.desktop;
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    opens com.udacity.catpoint.mainapp.data to com.google.gson;
}