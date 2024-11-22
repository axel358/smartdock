package cu.axel.smartdock.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cu.axel.smartdock.db.DatabaseContract.LaunchModesTable
import android.provider.BaseColumns

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(p1: SQLiteDatabase) {
        p1.execSQL(SQL_CREATE_MODES_ENTRIES)
    }

    override fun onUpgrade(p1: SQLiteDatabase, p2: Int, p3: Int) {}
    fun saveLaunchMode(app: String, mode: String) {
        try {
            val db = writableDatabase
            val values = ContentValues()
            values.put(LaunchModesTable.COLUMN_PACKAGE_NAME, app)
            values.put(LaunchModesTable.COLUMN_LAUNCH_MODE, mode)
            val selection = LaunchModesTable.COLUMN_PACKAGE_NAME + " = ?"
            val selectionArgs = arrayOf(app)
            val cursor = db.query(LaunchModesTable.TABLE_NAME, null, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                db.update(LaunchModesTable.TABLE_NAME, values, selection, selectionArgs)
            } else {
                db.insert(LaunchModesTable.TABLE_NAME, null, values)
            }
            cursor!!.close()
        } catch (_: Exception) {
        }
    }

    fun getLaunchMode(app: String): String? {
        var mode: String? = null
        try {
            val db = readableDatabase
            val selection = LaunchModesTable.COLUMN_PACKAGE_NAME + " = ?"
            val selectionArgs = arrayOf(app)
            val cursor = db.query(LaunchModesTable.TABLE_NAME, null, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                //TODO: recheck this
                val index = cursor.getColumnIndex(LaunchModesTable.COLUMN_LAUNCH_MODE)
                mode = cursor.getString(index)
                cursor.close()
            }
        } catch (_: Exception) {
        }
        return mode
    }

    fun forgetLaunchModes() {
        writableDatabase.delete(LaunchModesTable.TABLE_NAME, null, null);
    }

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "smartdock.db"
        private const val SQL_CREATE_MODES_ENTRIES = ("CREATE TABLE " + LaunchModesTable.TABLE_NAME + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY NOT NULL," + LaunchModesTable.COLUMN_PACKAGE_NAME
                + " TEXT NOT NULL," + LaunchModesTable.COLUMN_LAUNCH_MODE + " TEXT NOT NULL )")
    }
}
