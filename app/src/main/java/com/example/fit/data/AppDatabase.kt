package com.example.fit.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekNumber: Int,
    val dayName: String,
    val exerciseName: String,
    val sets: Int,
    val reps: String,
    val orderIndex: Int,
    val rpe: String = "",
    val notes: String = "",
    val warmupSets: String = "0"
)

@Entity(tableName = "exercise_logs")
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val userWeight: String,
    val equipmentType: String = "", // comma-separated: "Barbell,Each Side"
    val userComments: String,
    val observedRpe: String,
    val status: String // "PENDING", "DONE", "SKIPPED"
)

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE weekNumber = :weekNumber AND dayName = :dayName ORDER BY orderIndex")
    fun getExercises(weekNumber: Int, dayName: String): LiveData<List<Exercise>>

    @Query("SELECT DISTINCT weekNumber FROM exercises ORDER BY weekNumber")
    fun getDistinctWeeks(): LiveData<List<Int>>

    @Query("SELECT dayName FROM exercises WHERE weekNumber = :weekNumber GROUP BY dayName ORDER BY MIN(id)")
    fun getDistinctDays(weekNumber: Int): LiveData<List<String>>

    @Insert
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM exercises")
    fun countLive(): LiveData<Int>

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()
}

data class ExerciseHistoryEntry(
    val weekNumber: Int,
    val userWeight: String,
    val equipmentType: String,
    val userComments: String,
    val observedRpe: String,
    val status: String
)

@Dao
interface ExerciseLogDao {
    @Query("SELECT * FROM exercise_logs WHERE exerciseId = :exerciseId LIMIT 1")
    fun getLog(exerciseId: Long): LiveData<ExerciseLog?>

    @Query("SELECT * FROM exercise_logs WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun getLogSync(exerciseId: Long): ExerciseLog?

    @Query(
        "SELECT el.* FROM exercise_logs el " +
        "INNER JOIN exercises e ON el.exerciseId = e.id " +
        "WHERE e.weekNumber = :weekNumber AND e.dayName = :dayName"
    )
    fun getLogsForDay(weekNumber: Int, dayName: String): LiveData<List<ExerciseLog>>

    @Query(
        "SELECT e.weekNumber, el.userWeight, el.equipmentType, el.userComments, el.observedRpe, el.status " +
        "FROM exercise_logs el " +
        "INNER JOIN exercises e ON el.exerciseId = e.id " +
        "WHERE e.exerciseName = :exerciseName AND e.weekNumber < :currentWeek " +
        "ORDER BY e.weekNumber DESC"
    )
    fun getHistory(exerciseName: String, currentWeek: Int): LiveData<List<ExerciseHistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: ExerciseLog)

    @Query("DELETE FROM exercise_logs")
    suspend fun deleteAll()
}

@Database(entities = [Exercise::class, ExerciseLog::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseLogDao(): ExerciseLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fit.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
