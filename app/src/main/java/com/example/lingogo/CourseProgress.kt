data class DashboardData(
    val points: Int = 0,
    val streak: Int = 0,
    val dailyProgress: Int = 0,
    val currentLessonId: String = "",
    val lessonsProgress: Map<String, LessonProgressDetail> = emptyMap()
)

data class LessonProgressDetail(
    val stagesCompleted: Int = 0,
    val totalStages: Int = 1
)