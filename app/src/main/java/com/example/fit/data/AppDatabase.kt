package com.example.fit.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
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
    val orderIndex: Int
)

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE weekNumber = :weekNumber AND dayName = :dayName ORDER BY orderIndex")
    fun getExercises(weekNumber: Int, dayName: String): LiveData<List<Exercise>>

    @Query("SELECT DISTINCT weekNumber FROM exercises ORDER BY weekNumber")
    fun getDistinctWeeks(): LiveData<List<Int>>

    @Query("SELECT dayName FROM exercises WHERE weekNumber = :weekNumber GROUP BY dayName ORDER BY MIN(orderIndex)")
    fun getDistinctDays(weekNumber: Int): LiveData<List<String>>

    @Insert
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()
}

@Database(entities = [Exercise::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fit.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
