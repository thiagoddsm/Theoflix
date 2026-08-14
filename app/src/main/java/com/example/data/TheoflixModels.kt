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
    val thumbnailColor: String, // Color style hex/gradient key
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
                    id = "course_1",
                    title = "Escola de Líderes - Mód. 1",
                    description = "Fundamentação teológica e espiritual para todo cristão que deseja exercer o seu chamado com maturidade e excelência na igreja local.",
                    thumbnailColor = "#C5A059", // Dourado
                    category = "Liderança",
                    teacher = "Dra. Helena Santos",
                    duration = "12 horas"
                ),
                Course(
                    id = "course_2",
                    title = "Formação de Líderes do GC",
                    description = "Capacitação completa para pastoreio relacional, dinâmica de reuniões e multiplicação celular eficaz na comunidade.",
                    thumbnailColor = "#1D4ED8", // Azul escuro
                    category = "GC",
                    teacher = "Pr. Tiago Silva",
                    duration = "8 horas"
                ),
                Course(
                    id = "course_3",
                    title = "Paternidade & Casamento Saudável",
                    description = "Estabelecendo os princípios eternos da Palavra de Deus para uma dinâmica familiar equilibrada, amorosa e focada em Cristo.",
                    thumbnailColor = "#10B981", // Verde espiritual
                    category = "Família",
                    teacher = "Pr. Roberto & Aline Souza",
                    duration = "6 horas"
                ),
                Course(
                    id = "course_4",
                    title = "Fundamentos Iniciais da Fé",
                    description = "O ponto de partida essencial para novos convertidos e membros. Compreenda salvação, comunhão, estudo bíblico e discipulado.",
                    thumbnailColor = "#8B5CF6", // Roxo teológico
                    category = "Discipulado",
                    teacher = "Prof. Marcos Oliveira",
                    duration = "5 horas",
                    isFavorite = true // Seed one as favorite
                ),
                Course(
                    id = "course_5",
                    title = "Doutrinas Teológicas Clássicas",
                    description = "Uma jornada profunda e sistemática pelas principais doutrinas cristãs: Teologia Própria, Cristologia, Pneumatologia e Revelação.",
                    thumbnailColor = "#EF4444", // Vermelho teologia
                    category = "Teologia",
                    teacher = "Dr. Carlos Menezes",
                    duration = "10 horas"
                ),
                Course(
                    id = "course_6",
                    title = "Evangelismo de Impacto",
                    description = "Aprenda abordagens dinâmicas, apologética básica e evangelismo relacional prático para cumprir a Grande Comissão no dia a dia.",
                    thumbnailColor = "#F59E0B", // Laranja fogo
                    category = "Evangelismo",
                    teacher = "Ev. Lucas Rocha",
                    duration = "4 horas"
                )
            )

            val modules = listOf(
                // Course 1 Modules
                ModuleEntity(
                    id = "mod_1_1",
                    courseId = "course_1",
                    title = "O Chamado e o Caráter do Líder",
                    duration = "25 min",
                    videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"
                ),
                ModuleEntity(
                    id = "mod_1_2",
                    courseId = "course_1",
                    title = "Gestão Ministerial e Autocuidado",
                    duration = "30 min",
                    videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"
                ),
                ModuleEntity(
                    id = "mod_1_3",
                    courseId = "course_1",
                    title = "Comunicação Eficaz no Ministério",
                    duration = "28 min",
                    videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"
                ),

                // Course 2 Modules
                ModuleEntity(
                    id = "mod_2_1",
                    courseId = "course_2",
                    title = "A Visão do Grupo de Crescimento (GC)",
                    duration = "15 min",
                    videoUrl = "https://www.w3schools.com/html/movie.mp4"
                ),
                ModuleEntity(
                    id = "mod_2_2",
                    courseId = "course_2",
                    title = "Estrutura de Reunião e Dinâmicas",
                    duration = "18 min",
                    videoUrl = "https://www.w3schools.com/html/movie.mp4"
                ),
                ModuleEntity(
                    id = "mod_2_3",
                    courseId = "course_2",
                    title = "Como Formar e Multiplicar o GC",
                    duration = "22 min",
                    videoUrl = "https://www.w3schools.com/html/movie.mp4"
                ),

                // Course 3 Modules
                ModuleEntity(
                    id = "mod_3_1",
                    courseId = "course_3",
                    title = "O Altar Familiar e Práticas Devocionais",
                    duration = "20 min",
                    videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"
                ),
                ModuleEntity(
                    id = "mod_3_2",
                    courseId = "course_3",
                    title = "Resolução de Conflitos no Casamento",
                    duration = "24 min",
                    videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"
                ),
                ModuleEntity(
                    id = "mod_3_3",
                    courseId = "course_3",
                    title = "Criando Filhos no Caminho do Senhor",
                    duration = "27 min",
                    videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"
                ),

                // Course 4 Modules
                ModuleEntity(
                    id = "mod_4_1",
                    courseId = "course_4",
                    title = "Salvação Plena pela Graça",
                    duration = "15 min",
                    videoUrl = "https://www.w3schools.com/html/movie.mp4"
                ),
                ModuleEntity(
                    id = "mod_4_2",
                    courseId = "course_4",
                    title = "Iniciação à Oração Diária",
                    duration = "18 min",
                    videoUrl = "https://www.w3schools.com/html/movie.mp4"
                ),
                ModuleEntity(
                    id = "mod_4_3",
                    courseId = "course_4",
                    title = "A Importância do Corpo de Cristo",
                    duration = "12 min",
                    videoUrl = "https://www.w3schools.com/html/movie.mp4"
                ),

                // Course 5 Modules
                ModuleEntity(
                    id = "mod_5_1",
                    courseId = "course_5",
                    title = "Introdução à Doutrina de Deus",
                    duration = "32 min",
                    videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"
                ),
                ModuleEntity(
                    id = "mod_5_2",
                    courseId = "course_5",
                    title = "Canonicidade e Revelação Escrita",
                    duration = "35 min",
                    videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"
                ),
                ModuleEntity(
                    id = "mod_5_3",
                    courseId = "course_5",
                    title = "Pneumatologia: A Pessoa do Espírito",
                    duration = "30 min",
                    videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"
                ),

                // Course 6 Modules
                ModuleEntity(
                    id = "mod_6_1",
                    courseId = "course_6",
                    title = "Urgência da Grande Comissão",
                    duration = "22 min",
                    videoUrl = "https://www.w3schools.com/html/movie.mp4"
                ),
                ModuleEntity(
                    id = "mod_6_2",
                    courseId = "course_6",
                    title = "Evangelismo Relacional Prático",
                    duration = "25 min",
                    videoUrl = "https://www.w3schools.com/html/movie.mp4"
                )
            )

            dao.insertCourses(courses)
            dao.insertModules(modules)
        }
    }
}
