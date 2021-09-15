package cu.axel.smartdock.db;

import android.provider.BaseColumns;

public class DatabaseContract {
    private DatabaseContract() {
    }
    public static class LaunchModesTable implements BaseColumns {
        public static final String TABLE_NAME = "table_launch_modes";
        public static final String COLUMN_PACKAGE_NAME = "package_name";
        public static final String COLUMN_LAUNCH_MODE = "launch_mode";
    }
}
