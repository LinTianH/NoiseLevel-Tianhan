package com.example.noiselevel;

import android.provider.BaseColumns;

public final class DatabaseContract {

    private DatabaseContract() {
        // Private constructor to prevent instantiation
    }

    public static class HistoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "History";
        public static final String COLUMN_NOISE_LEVEL = "Noise_level";
        public static final String COLUMN_TIMESTAMP = "Timestamp";
    }

    public static class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "User";
        public static final String COLUMN_EMAIL = "Email";
        public static final String COLUMN_PASSWORD = "Password";
        // Add more columns as needed
    }
}
