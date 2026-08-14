package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val thumbnailColor: String, // Color style hex/gradient key or Image URL
    val category: String,
    val teacher: String,
    val duration: String,
    val isFavorite: Boolean = false
)

@Entity(tableName = "modules")
data class ModuleEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val title: String,
    val duration: String,
    val videoUrl: String,
    val completed: Boolean = false
)

@Entity(tableName = "user_progress", primaryKeys = ["courseId", "moduleId"])
data class UserProgressEntity(
    val courseId: String,
    val moduleId: String,
    val watched: Boolean = false,
    val completed: Boolean = false,
    val lastPosition: Long = 0L
)

@Dao
interface TheoflixDao {
    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE isFavorite = 1")
    fun getFavoriteCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE id = :id")
    fun getCourseById(id: String): Flow<Course?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>)

    @Query("UPDATE courses SET isFavorite = :isFavorite WHERE id = :courseId")
    suspend fun updateCourseFavorite(courseId: String, isFavorite: Boolean)

    @Query("SELECT * FROM modules WHERE courseId = :courseId")
    fun getModulesForCourse(courseId: String): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM modules WHERE id = :id")
    suspend fun getModuleById(id: String): ModuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModules(modules: List<ModuleEntity>)

    @Query("UPDATE modules SET completed = :completed WHERE id = :moduleId")
    suspend fun updateModuleCompletion(moduleId: String, completed: Boolean)

    @Query("SELECT * FROM user_progress")
    fun getAllUserProgress(): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress WHERE courseId = :courseId")
    fun getProgressForCourse(courseId: String): Flow<List<UserProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: UserProgressEntity)

    @Query("DELETE FROM courses WHERE id LIKE 'course_%'")
    suspend fun deleteLegacyMockCourses()

    @Query("DELETE FROM modules WHERE courseId LIKE 'course_%'")
    suspend fun deleteLegacyMockModules()
}

@Database(
    entities = [Course::class, ModuleEntity::class, UserProgressEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TheoflixDatabase : RoomDatabase() {
    abstract fun theoflixDao(): TheoflixDao

    companion object {
        @Volatile
        private var INSTANCE: TheoflixDatabase? = null

        fun getDatabase(context: Context): TheoflixDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TheoflixDatabase::class.java,
                    "theoflix_database"
                )
                    .addCallback(TheoflixDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class TheoflixDatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database.theoflixDao())
                    }
                }
            }
        }

        suspend fun seedDatabase(dao: TheoflixDao) {
            val courses = listOf(
                Course(
                    id = "imersao",
                    title = "Imersão (Batismo)",
                    description = "Fundamentação doutrinária para o início da caminhada pública com Cristo. Prepare-se para um mergulho profundo na fé e no compromisso com o Reino.",
                    thumbnailColor = "#1D4ED8", // Azul
                    category = "Doutrina",
                    teacher = "Pastoral IBM",
                    duration = "4h 30min",
                    isFavorite = true
                ),
                Course(
                    id = "membros",
                    title = "Curso de Membros",
                    description = "Jornada de integração em 5 etapas fundamentais para quem deseja se tornar parte do organismo da Igreja. Entenda nossa história, visão e como você se encaixa.",
                    thumbnailColor = "#C5A059", // Dourado
                    category = "Integração",
                    teacher = "Liderança IBM",
                    duration = "7h 15min"
                ),
                Course(
                    id = "crescer",
                    title = "Curso Crescer",
                    description = "Desenvolva sua maturidade cristã e entenda os princípios de uma vida frutífera no Reino. Este curso é o segundo passo fundamental na nossa trilha de crescimento espiritual.",
                    thumbnailColor = "#10B981", // Verde
                    category = "Maturidade",
                    teacher = "Corpo Pastoral",
                    duration = "6h 20min"
                )
            )

            val modules = listOf(
                // Imersao (Batismo)
                ModuleEntity(
                    id = "imersao_1",
                    courseId = "imersao",
                    title = "Salvação, Arrependimento e Fé Proporcional",
                    duration = "45 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),
                ModuleEntity(
                    id = "imersao_2",
                    courseId = "imersao",
                    title = "O simbolismo bíblico do Batismo nas Águas",
                    duration = "50 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),
                ModuleEntity(
                    id = "imersao_3",
                    courseId = "imersao",
                    title = "A Ceia do Senhor: Memória e Esperança",
                    duration = "40 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),
                ModuleEntity(
                    id = "imersao_4",
                    courseId = "imersao",
                    title = "Introdução às Disciplinas Espirituais",
                    duration = "55 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),

                // Curso de Membros
                ModuleEntity(
                    id = "membros_1",
                    courseId = "membros",
                    title = "Aula 1: Introdução & História da IBM",
                    duration = "60 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),
                ModuleEntity(
                    id = "membros_2",
                    courseId = "membros",
                    title = "Aula 2: DNA Ministerial & Visão de Células",
                    duration = "75 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),
                ModuleEntity(
                    id = "membros_3",
                    courseId = "membros",
                    title = "Aula 3: Mordomia Cristã & Finanças do Reino",
                    duration = "65 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),
                ModuleEntity(
                    id = "membros_4",
                    courseId = "membros",
                    title = "Aula 4: Governança, Estatuto & Ética",
                    duration = "70 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),
                ModuleEntity(
                    id = "membros_5",
                    courseId = "membros",
                    title = "Aula 5: Comissionamento & Compromisso",
                    duration = "90 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),

                // Curso Crescer
                ModuleEntity(
                    id = "crescer_1",
                    courseId = "crescer",
                    title = "Aula 1: A Base da Maturidade Cristã",
                    duration = "55 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),
                ModuleEntity(
                    id = "crescer_2",
                    courseId = "crescer",
                    title = "Aula 2: Vida no Espírito e Santificação",
                    duration = "60 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),
                ModuleEntity(
                    id = "crescer_3",
                    courseId = "crescer",
                    title = "Aula 3: Caráter Cristão e o Fruto do Espírito",
                    duration = "50 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),
                ModuleEntity(
                    id = "crescer_4",
                    courseId = "crescer",
                    title = "Aula 4: Mordomia dos Dons e Vocação",
                    duration = "65 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),
                ModuleEntity(
                    id = "crescer_5",
                    courseId = "crescer",
                    title = "Aula 5: Vida de Oração e Intimidade",
                    duration = "45 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                ),
                ModuleEntity(
                    id = "crescer_6",
                    courseId = "crescer",
                    title = "Aula 6: Autoridade Espiritual e Submissão",
                    duration = "50 min",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                )
            )

            dao.insertCourses(courses)
            dao.insertModules(modules)
        }
    }
}
