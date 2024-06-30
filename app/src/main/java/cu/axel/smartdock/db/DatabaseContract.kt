package cu.axel.smartdock.db

import android.provider.BaseColumns

class DatabaseContract private constructor() {
    object LaunchModesTable : BaseColumns {
        const val TABLE_NAME = "table_launch_modes"
        const val COLUMN_PACKAGE_NAME = "package_name"
        const val COLUMN_LAUNCH_MODE = "launch_mode"
    }
}
