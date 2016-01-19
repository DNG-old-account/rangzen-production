package org.denovogroup.rangzen.backend;


import android.os.Environment;

import org.apache.log4j.Level;

import de.mindpipe.android.logging.log4j.LogConfigurator;

import java.io.File;

public class ConfigureLog4J {

    public static final File LOG_DIRECTORY = Environment.getExternalStorageDirectory();
    public static final String LOG_FILE = "rangzen.log";
    public static final String LOG_FILENAME = LOG_DIRECTORY + File.separator + LOG_FILE;
    public static final String LOG_LINE_PATTERN = "%d - [%p::%c] - %m%n";
    public static final Level LOG_LEVEL = Level.DEBUG;
    public static final long FILE_SIZE = 6 * 1024 * 1024; //MB

    public static void configure(boolean FreezeLogging) {
        final LogConfigurator logConfigurator = new LogConfigurator();
        logConfigurator.setFilePattern(LOG_LINE_PATTERN);
        logConfigurator.setFileName(LOG_FILENAME);
        logConfigurator.setRootLevel(FreezeLogging ? Level.OFF : LOG_LEVEL);
        logConfigurator.setMaxFileSize(FILE_SIZE);
        // Set log level of a specific logger
        logConfigurator.setLevel("org.apache", Level.ERROR);
        logConfigurator.setResetConfiguration(FreezeLogging);
        logConfigurator.configure();
    }
}