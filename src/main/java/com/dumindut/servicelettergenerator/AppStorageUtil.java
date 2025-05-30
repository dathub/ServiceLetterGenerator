package com.dumindut.servicelettergenerator;

import java.io.File;

public class AppStorageUtil {

    private static final String APP_FOLDER_NAME = ".servicelettergenerator";

    /**
     * Returns the base directory path for storing application data in the user's home directory.
     */
    public static String getAppStorageDir() {
        String userHome = System.getProperty("user.home");
        String path = userHome + File.separator + APP_FOLDER_NAME;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs(); // Create directory if it doesn't exist
        }
        return path;
    }

    /**
     * Returns the full path to the application's SQLite DB file.
     */
    public static String getDatabasePath() {
        return getAppStorageDir() + File.separator + "slgdb.db";
    }

    /**
     * Returns the path where log files should be stored.
     */
    public static String getLogDirectoryPath() {
        String path = getAppStorageDir() + File.separator + "logs";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }
}
