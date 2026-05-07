package com.example.fit.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.fit.FitApp
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Repository-level integration tests against an in-memory Room database.
 * Covers import / switch / delete / log mutation / completion / history / export flows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = FitApp::class)
class ProgrammeRepositoryTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var repo: ProgrammeRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Wipe prefs so each test starts with no active programme.
        ctx.getSharedPreferences("fit_prefs", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()

        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ProgrammeRepository(
            db.exerciseDao(),
            db.exerciseLogDao(),
            db.programmeDao(),
            ctx
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // --- Programme name persistence ---

    @Test
    fun `setProgrammeName then getProgrammeName round-trips`() {
        assertEquals("", repo.getProgrammeName())
        repo.setProgrammeName("my_prog")
        assertEquals("my_prog", repo.getProgrammeName())
    }

    // --- Import: JSON ---

    @Test
    fun `importProgrammeFromJson - inserts exercises and registers programme`() = runTest {
        val result = repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        assertEquals(ImportResult.IMPORTED, result)
        assertEquals("test_prog", repo.getProgrammeName())
        assertTrue(repo.programmeExists("test_prog"))
    }

    @Test
    fun `importProgrammeFromJson - second import with same name returns SWITCHED`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        repo.setProgrammeName("")  // simulate user not actively on it

        val second = repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        assertEquals(ImportResult.SWITCHED, second)
        assertEquals("test_prog", repo.getProgrammeName())
    }

    @Test
    fun `importProgrammeFromJson - exercises preserve order, week and day`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        val all = db.exerciseDao().getAllExercises("test_prog")

        val w1A = all.filter { it.weekNumber == 1 && it.dayName == "Day A" }
            .sortedBy { it.orderIndex }
        assertEquals(listOf("Squat", "Bench"), w1A.map { it.exerciseName })
        assertEquals("8-9", w1A.first { it.exerciseName == "Squat" }.rpe)
    }

    @Test
    fun `importProgrammeFromJson - distinct programmes coexist`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "first")
        repo.importProgrammeFromJson(SAMPLE_JSON_TWO, "second")

        assertTrue(repo.programmeExists("first"))
        assertTrue(repo.programmeExists("second"))
        assertEquals("second", repo.getProgrammeName())

        val firstCount = db.exerciseDao().countByProgramme("first")
        val secondCount = db.exerciseDao().countByProgramme("second")
        assertTrue(firstCount > 0)
        assertTrue(secondCount > 0)
    }

    // --- Delete ---

    @Test
    fun `deleteProgramme removes exercises, logs and registry entry`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        val firstEx = db.exerciseDao().getAllExercises("test_prog").first()
        repo.saveLog(
            ExerciseLog(
                exerciseId = firstEx.id,
                userWeight = "100",
                equipmentType = "Barbell",
                userComments = "ok",
                observedRpe = "8",
                status = "DONE"
            )
        )

        repo.deleteProgramme()

        assertEquals("", repo.getProgrammeName())
        assertFalse(repo.programmeExists("test_prog"))
        assertEquals(0, db.exerciseDao().countByProgramme("test_prog"))
        assertNull(db.exerciseLogDao().getLogSync(firstEx.id))
    }

    @Test
    fun `deleteProgramme with no active programme is a no-op`() = runTest {
        // Should not throw.
        repo.deleteProgramme()
        assertEquals("", repo.getProgrammeName())
    }

    // --- Log save / get ---

    @Test
    fun `saveLog and getLogSync round-trip`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        val ex = db.exerciseDao().getAllExercises("test_prog").first()

        repo.saveLog(
            ExerciseLog(
                exerciseId = ex.id,
                userWeight = "100",
                equipmentType = "Barbell",
                userComments = "wk1",
                observedRpe = "9",
                status = "DONE"
            )
        )

        val log = repo.getLogSync(ex.id)
        assertNotNull(log)
        assertEquals("100", log!!.userWeight)
        assertEquals("DONE", log.status)
    }

    @Test
    fun `saveLog twice replaces existing log via REPLACE strategy`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        val ex = db.exerciseDao().getAllExercises("test_prog").first()

        repo.saveLog(
            ExerciseLog(exerciseId = ex.id, userWeight = "100", userComments = "", observedRpe = "8", status = "DONE")
        )
        val firstId = repo.getLogSync(ex.id)!!.id
        repo.saveLog(
            ExerciseLog(id = firstId, exerciseId = ex.id, userWeight = "110", userComments = "", observedRpe = "9", status = "DONE")
        )

        val log = repo.getLogSync(ex.id)!!
        assertEquals("110", log.userWeight)
        assertEquals("9", log.observedRpe)
    }

    // --- Reactive queries ---

    @Test
    fun `hasProgrammeByName flips true after import`() = runTest {
        assertEquals(false, repo.hasProgrammeByName("test_prog").observeOnce())
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        assertEquals(true, repo.hasProgrammeByName("test_prog").observeOnce())
    }

    @Test
    fun `getDistinctWeeksByName returns sorted weeks`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        assertEquals(listOf(1, 2), repo.getDistinctWeeksByName("test_prog").observeOnce())
    }

    @Test
    fun `getExercises filters by active programme name`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "first")
        repo.importProgrammeFromJson(SAMPLE_JSON_TWO, "second")
        repo.setProgrammeName("first")

        val w1A = repo.getExercises(1, "Day A").observeOnce()!!
        assertEquals(listOf("Squat", "Bench"), w1A.map { it.exerciseName })

        repo.setProgrammeName("second")
        val push = repo.getExercises(1, "Push").observeOnce()!!
        assertEquals(listOf("OHP"), push.map { it.exerciseName })
    }

    // --- Completion logic ---

    @Test
    fun `getCompletedDays - day appears only when every exercise has a log`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        val w1A = db.exerciseDao().getAllExercises("test_prog")
            .filter { it.weekNumber == 1 && it.dayName == "Day A" }

        // No logs yet — empty.
        assertEquals(emptyList<String>(), repo.getCompletedDays(1).observeOnce())

        // Half done — still empty.
        repo.saveLog(ExerciseLog(exerciseId = w1A[0].id, userWeight = "", userComments = "", observedRpe = "", status = "DONE"))
        assertEquals(emptyList<String>(), repo.getCompletedDays(1).observeOnce())

        // All done — appears.
        repo.saveLog(ExerciseLog(exerciseId = w1A[1].id, userWeight = "", userComments = "", observedRpe = "", status = "DONE"))
        assertEquals(listOf("Day A"), repo.getCompletedDays(1).observeOnce())
    }

    @Test
    fun `getCompletedWeeks - week appears only when all days are complete`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        val all = db.exerciseDao().getAllExercises("test_prog").filter { it.weekNumber == 1 }

        for (ex in all.filter { it.dayName == "Day A" }) {
            repo.saveLog(ExerciseLog(exerciseId = ex.id, userWeight = "", userComments = "", observedRpe = "", status = "DONE"))
        }
        // Day B not yet complete
        assertEquals(emptyList<Int>(), repo.getCompletedWeeks().observeOnce())

        for (ex in all.filter { it.dayName == "Day B" }) {
            repo.saveLog(ExerciseLog(exerciseId = ex.id, userWeight = "", userComments = "", observedRpe = "", status = "DONE"))
        }
        assertEquals(listOf(1), repo.getCompletedWeeks().observeOnce())
    }

    @Test
    fun `getFirstIncompleteDay returns earliest unfinished day`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")

        // Initially, Week 1 Day A is the first incomplete.
        val first = repo.getFirstIncompleteDay()
        assertNotNull(first)
        assertEquals(1, first!!.weekNumber)
        assertEquals("Day A", first.dayName)

        // Complete Week 1 entirely; next call should return Week 2 Day A.
        val w1 = db.exerciseDao().getAllExercises("test_prog").filter { it.weekNumber == 1 }
        for (ex in w1) {
            repo.saveLog(ExerciseLog(exerciseId = ex.id, userWeight = "", userComments = "", observedRpe = "", status = "DONE"))
        }
        val next = repo.getFirstIncompleteDay()
        assertEquals(2, next!!.weekNumber)
        assertEquals("Day A", next.dayName)
    }

    @Test
    fun `getFirstIncompleteDay returns null when entire programme complete`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        for (ex in db.exerciseDao().getAllExercises("test_prog")) {
            repo.saveLog(ExerciseLog(exerciseId = ex.id, userWeight = "", userComments = "", observedRpe = "", status = "DONE"))
        }
        assertNull(repo.getFirstIncompleteDay())
    }

    // --- History ---

    @Test
    fun `getHistory returns prior-week logs sorted descending`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        val w1Squat = db.exerciseDao().getAllExercises("test_prog")
            .first { it.weekNumber == 1 && it.exerciseName == "Squat" }
        repo.saveLog(
            ExerciseLog(exerciseId = w1Squat.id, userWeight = "100", userComments = "", observedRpe = "8", status = "DONE")
        )

        val history = repo.getHistory("Squat", currentWeek = 2).observeOnce()!!
        assertEquals(1, history.size)
        assertEquals(1, history.first().weekNumber)
        assertEquals("100", history.first().userWeight)
    }

    @Test
    fun `getHistory empty when current week is 1`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        val w1Squat = db.exerciseDao().getAllExercises("test_prog")
            .first { it.weekNumber == 1 && it.exerciseName == "Squat" }
        repo.saveLog(
            ExerciseLog(exerciseId = w1Squat.id, userWeight = "100", userComments = "", observedRpe = "8", status = "DONE")
        )

        val history = repo.getHistory("Squat", currentWeek = 1).observeOnce()!!
        assertTrue(history.isEmpty())
    }

    // --- Export ---

    @Test
    fun `buildExportJson produces parseable JSON with weeks, days and logs`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        val ex = db.exerciseDao().getAllExercises("test_prog").first()
        repo.saveLog(
            ExerciseLog(exerciseId = ex.id, userWeight = "100", equipmentType = "Barbell",
                userComments = "wk1", observedRpe = "8", status = "DONE")
        )

        val json = repo.buildExportJson("test_prog", "user-id")
        val root = JSONObject(json)
        assertEquals("test_prog", root.getString("programmeName"))
        assertEquals("user-id", root.getString("identifier"))

        val weeks = root.getJSONObject("programme").getJSONArray("weeks")
        assertEquals(2, weeks.length())

        val logs = root.getJSONArray("logs")
        assertEquals(1, logs.length())
        assertEquals("100", logs.getJSONObject(0).getString("userWeight"))
        assertEquals("DONE", logs.getJSONObject(0).getString("status"))
    }

    @Test
    fun `buildExportJson with blank programmeName falls back to active prefs name`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "test_prog")
        val json = repo.buildExportJson("", "user-id")
        val root = JSONObject(json)
        assertEquals("test_prog", root.getString("programmeName"))
    }

    // --- parseProgramme (static) ---

    @Test
    fun `parseProgramme - missing optional fields default to empty or zero`() {
        val minimal = """
            {
              "weeks": [
                { "week": 1, "days": [
                  { "day": "Day A", "exercises": [
                    {"name": "X", "sets": 1, "reps": "5"}
                  ]}
                ]}
              ]
            }
        """.trimIndent()
        val out = ProgrammeRepository.parseProgramme(minimal)
        assertEquals(1, out.size)
        val ex = out.first()
        assertEquals("X", ex.exerciseName)
        assertEquals(1, ex.sets)
        assertEquals("5", ex.reps)
        assertEquals("", ex.rpe)
        assertEquals("", ex.notes)
        assertEquals("0", ex.warmupSets)
        assertEquals("", ex.sub1)
        assertEquals("", ex.videoUrl)
    }

    @Test
    fun `parseProgramme - default order falls back to running index when omitted`() {
        val noOrder = """
            {
              "weeks": [
                { "week": 1, "days": [
                  { "day": "Day A", "exercises": [
                    {"name": "A", "sets": 1, "reps": "5"},
                    {"name": "B", "sets": 1, "reps": "5"}
                  ]}
                ]}
              ]
            }
        """.trimIndent()
        val out = ProgrammeRepository.parseProgramme(noOrder)
        assertEquals(listOf(0, 1), out.map { it.orderIndex })
    }

    // --- BUNDLED_PROGRAMMES sanity ---

    @Test
    fun `BUNDLED_PROGRAMMES has stable normalized names`() {
        val names = ProgrammeRepository.BUNDLED_PROGRAMMES.map { it.first }
        assertTrue(names.contains("essentials_2x"))
        assertTrue(names.contains("essentials_3x"))
        assertTrue(names.contains("essentials_4x"))
        assertTrue(names.contains("essentials_5x"))
    }

    // --- getAvailableProgrammes ---

    @Test
    fun `getAvailableProgrammes lists imported programmes`() = runTest {
        repo.importProgrammeFromJson(SAMPLE_JSON, "first")
        repo.importProgrammeFromJson(SAMPLE_JSON_TWO, "second")

        val names = repo.getAvailableProgrammes().observeOnce()!!.map { it.name }
        assertTrue(names.contains("first"))
        assertTrue(names.contains("second"))
    }

    // --- Helpers ---

    private fun <T> LiveData<T>.observeOnce(): T? {
        var result: T? = null
        val observer = Observer<T> { result = it }
        observeForever(observer)
        try {
            return result
        } finally {
            removeObserver(observer)
        }
    }

    companion object {
        private val SAMPLE_JSON = """
            {
              "weeks": [
                {
                  "week": 1,
                  "days": [
                    {
                      "day": "Day A",
                      "exercises": [
                        {"name": "Squat", "sets": 3, "reps": "5", "rpe": "8-9", "order": 0},
                        {"name": "Bench", "sets": 3, "reps": "8", "rpe": "8", "order": 1}
                      ]
                    },
                    {
                      "day": "Day B",
                      "exercises": [
                        {"name": "Deadlift", "sets": 1, "reps": "5", "rpe": "9", "order": 0}
                      ]
                    }
                  ]
                },
                {
                  "week": 2,
                  "days": [
                    {
                      "day": "Day A",
                      "exercises": [
                        {"name": "Squat", "sets": 3, "reps": "5", "rpe": "9", "order": 0},
                        {"name": "Bench", "sets": 3, "reps": "8", "rpe": "8", "order": 1}
                      ]
                    },
                    {
                      "day": "Day B",
                      "exercises": [
                        {"name": "Deadlift", "sets": 1, "reps": "5", "rpe": "9", "order": 0}
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        private val SAMPLE_JSON_TWO = """
            {
              "weeks": [
                {
                  "week": 1,
                  "days": [
                    {
                      "day": "Push",
                      "exercises": [
                        {"name": "OHP", "sets": 3, "reps": "5", "rpe": "8", "order": 0}
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
    }
}
