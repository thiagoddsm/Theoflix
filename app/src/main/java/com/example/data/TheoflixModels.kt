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
    val thumbnailColor: String, // Color style hex or image URL
    val category: String,
    val teacher: String,
    val duration: String,
    val level: Int = 1,
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
    @Query("SELECT * FROM courses ORDER BY level ASC")
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

    @Query("DELETE FROM courses")
    suspend fun clearAllCourses()

    @Query("DELETE FROM modules")
    suspend fun clearAllModules()

    @Query("DELETE FROM courses WHERE id LIKE 'course_%' OR id = 'imersao' OR id = 'membros'")
    suspend fun deleteLegacyMockCourses()

    @Query("DELETE FROM modules WHERE courseId LIKE 'course_%' OR courseId = 'imersao' OR courseId = 'membros'")
    suspend fun deleteLegacyMockModules()
}

@Database(
    entities = [Course::class, ModuleEntity::class, UserProgressEntity::class],
    version = 2,
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
                    .fallbackToDestructiveMigration()
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
                // Nível 1: Fundamentos
                Course(
                    id = "batismo",
                    title = "Batismo",
                    description = "Fundamentação doutrinária para o início da caminhada pública com Cristo. Prepare-se para um mergulho profundo na fé e no compromisso com o Reino.",
                    thumbnailColor = "#1D4ED8", // Azul
                    category = "Obrigatório",
                    teacher = "Pastoral IBM",
                    duration = "3h 10min",
                    level = 1,
                    isFavorite = true
                ),
                Course(
                    id = "pertencer",
                    title = "Pertencer",
                    description = "Jornada de integração em 5 etapas fundamentais para quem deseja se tornar parte do organismo da Igreja Batista da Manhã.",
                    thumbnailColor = "#2563EB", // Azul Royal
                    category = "Obrigatório",
                    teacher = "Liderança IBM",
                    duration = "1h 36min",
                    level = 1
                ),

                // Nível 2: Consolidação
                Course(
                    id = "crescer",
                    title = "Crescer",
                    description = "Desenvolva sua maturidade cristã e entenda os princípios de uma vida frutífera no Reino. Trilha essencial de crescimento espiritual.",
                    thumbnailColor = "#E11D48", // Rose / Vinho
                    category = "Maturidade",
                    teacher = "Corpo Pastoral",
                    duration = "6h 20min",
                    level = 2
                ),
                Course(
                    id = "cuidar",
                    title = "Cuidar",
                    description = "Capacitação para o cuidado relacional, discipulado de novos convertidos e pastoreio mútuo nas células da comunidade.",
                    thumbnailColor = "#BE123C", // Rose Escuro
                    category = "Pastoreio",
                    teacher = "Liderança de Células",
                    duration = "4h 40min",
                    level = 2
                ),

                // Nível 3: Liderança & Multiplicação
                Course(
                    id = "discipular",
                    title = "Discipular",
                    description = "Escola de líderes e discipuladores para formação ministerial, pastoreio de líderes e multiplicação celular.",
                    thumbnailColor = "#D97706", // Âmbar / Dourado
                    category = "Liderança",
                    teacher = "Pr. Sênior IBM",
                    duration = "5h 15min",
                    level = 3
                ),

                // Nível 4: Alta Gestão & Supervisão
                Course(
                    id = "papo_da_manha",
                    title = "Papo da manhã",
                    description = "Encontros de alinhamento, supervisão estratégica e mentoria ministerial com a alta liderança da igreja.",
                    thumbnailColor = "#7C3AED", // Roxo
                    category = "Alta Gestão",
                    teacher = "Conselho Ministerial",
                    duration = "3h 45min",
                    level = 4
                )
            )

            val modules = listOf(
                // Batismo (Nível 1)
                ModuleEntity(
                    id = "batismo_1",
                    courseId = "batismo",
                    title = "Aula 1: Salvação, Arrependimento e Fé Proporcional",
                    duration = "45 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "batismo_2",
                    courseId = "batismo",
                    title = "Aula 2: O simbolismo bíblico do Batismo nas Águas",
                    duration = "50 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "batismo_3",
                    courseId = "batismo",
                    title = "Aula 3: A Ceia do Senhor: Memória e Esperança",
                    duration = "40 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "batismo_4",
                    courseId = "batismo",
                    title = "Aula 4: Introdução às Disciplinas Espirituais",
                    duration = "55 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),

                // Pertencer (Nível 1)
                ModuleEntity(
                    id = "pertencer_1",
                    courseId = "pertencer",
                    title = "Aula 1: Introdução & História da IBM",
                    duration = "20 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "pertencer_2",
                    courseId = "pertencer",
                    title = "Aula 2: DNA Ministerial & Visão de Células",
                    duration = "25 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "pertencer_3",
                    courseId = "pertencer",
                    title = "Aula 3: Mordomia Cristã & Finanças do Reino",
                    duration = "18 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "pertencer_4",
                    courseId = "pertencer",
                    title = "Aula 4: Governança, Estatuto & Ética",
                    duration = "20 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "pertencer_5",
                    courseId = "pertencer",
                    title = "Aula 5: Comissionamento & Compromisso",
                    duration = "23 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),

                // Crescer (Nível 2)
                ModuleEntity(
                    id = "crescer_1",
                    courseId = "crescer",
                    title = "Aula 1: A Base da Maturidade Cristã",
                    duration = "55 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "crescer_2",
                    courseId = "crescer",
                    title = "Aula 2: Vida no Espírito e Santificação",
                    duration = "60 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "crescer_3",
                    courseId = "crescer",
                    title = "Aula 3: Caráter Cristão e o Fruto do Espírito",
                    duration = "50 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "crescer_4",
                    courseId = "crescer",
                    title = "Aula 4: Mordomia dos Dons e Vocação",
                    duration = "65 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "crescer_5",
                    courseId = "crescer",
                    title = "Aula 5: Vida de Oração e Intimidade",
                    duration = "45 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "crescer_6",
                    courseId = "crescer",
                    title = "Aula 6: Autoridade Espiritual e Submissão",
                    duration = "50 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),

                // Cuidar (Nível 2)
                ModuleEntity(
                    id = "cuidar_1",
                    courseId = "cuidar",
                    title = "Aula 1: O Coração do Cuidador",
                    duration = "45 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "cuidar_2",
                    courseId = "cuidar",
                    title = "Aula 2: Escuta Empática e Aconselhamento Bíblico",
                    duration = "50 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "cuidar_3",
                    courseId = "cuidar",
                    title = "Aula 3: Acompanhamento de Novos Decididos",
                    duration = "45 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "cuidar_4",
                    courseId = "cuidar",
                    title = "Aula 4: Intercessão e Batalha Espiritual",
                    duration = "50 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),

                // Discipular (Nível 3)
                ModuleEntity(
                    id = "discipular_1",
                    courseId = "discipular",
                    title = "Aula 1: O Modelo de Jesus para o Discipulado",
                    duration = "50 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "discipular_2",
                    courseId = "discipular",
                    title = "Aula 2: Formando Discípulos Multiplicadores",
                    duration = "55 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "discipular_3",
                    courseId = "discipular",
                    title = "Aula 3: Gestão e Dinâmica do Grupo de Crescimento",
                    duration = "50 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "discipular_4",
                    courseId = "discipular",
                    title = "Aula 4: Enviando e Comissionando Novos Líderes",
                    duration = "60 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),

                // Papo da manhã (Nível 4)
                ModuleEntity(
                    id = "papo_1",
                    courseId = "papo_da_manha",
                    title = "Episódio 1: Visão Estratégica e Alinhamento Ministerial",
                    duration = "45 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "papo_2",
                    courseId = "papo_da_manha",
                    title = "Episódio 2: Cultura de Honra e Excelência",
                    duration = "40 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "papo_3",
                    courseId = "papo_da_manha",
                    title = "Episódio 3: Mentoria Pastoral e Pastoreio de Pastores",
                    duration = "50 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                ),
                ModuleEntity(
                    id = "papo_4",
                    courseId = "papo_da_manha",
                    title = "Episódio 4: Liderança Sob Pressão e Resiliência",
                    duration = "45 min",
                    videoUrl = "https://www.youtube.com/watch?v=7wfYIMvS_9g"
                )
            )

            dao.insertCourses(courses)
            dao.insertModules(modules)
        }
    }
}
