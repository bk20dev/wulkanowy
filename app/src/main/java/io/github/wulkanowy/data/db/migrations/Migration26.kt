package io.github.wulkanowy.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration26 : Migration(25, 26) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE GradesSummary ADD COLUMN is_predicted_grade_notified INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE GradesSummary ADD COLUMN is_final_grade_notified INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE GradesSummary ADD COLUMN predicted_grade_last_change INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE GradesSummary ADD COLUMN final_grade_last_change INTEGER NOT NULL DEFAULT 0")
    }
}
