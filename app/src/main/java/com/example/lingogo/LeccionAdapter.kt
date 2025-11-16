package com.example.lingogo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.linggo.models.Lesson
import com.example.linggo.models.LessonProgress

// Define un tipo de función para el clic
typealias OnLessonClick = (Lesson, LessonProgress) -> Unit

class LeccionAdapter(private val onLessonClick: OnLessonClick) :
    RecyclerView.Adapter<LeccionAdapter.LeccionViewHolder>() {

    private var lessons: List<Lesson> = emptyList()
    private var progressMap: Map<String, LessonProgress> = emptyMap()

    fun submitList(lessons: List<Lesson>, progressMap: Map<String, LessonProgress>) {
        this.lessons = lessons
        this.progressMap = progressMap
        notifyDataSetChanged() // Notificar al RecyclerView que los datos cambiaron
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeccionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leccion_card, parent, false)
        return LeccionViewHolder(view)
    }

    override fun getItemCount(): Int = lessons.size

    override fun onBindViewHolder(holder: LeccionViewHolder, position: Int) {
        val lesson = lessons[position]
        // Obtener el progreso de esta lección, o usar uno por defecto (locked)
        val progress = progressMap[lesson.id] ?: LessonProgress(
            status = if (position == 0) "unlocked" else "locked", // Desbloquear la primera lección
            stagesCompleted = 0,
            totalStages = lesson.totalStages
        )
        holder.bind(lesson, progress, onLessonClick)
    }


    class LeccionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textViewLessonTitle)
        private val progressText: TextView = itemView.findViewById(R.id.textViewStageProgress)
        private val actionButton: Button = itemView.findViewById(R.id.buttonAction)
        private val lockedIcon: ImageView = itemView.findViewById(R.id.iconLocked)
        private val card: View = itemView.findViewById(R.id.lessonCard)

        fun bind(lesson: Lesson, progress: LessonProgress, onLessonClick: OnLessonClick) {
            title.text = lesson.title

            when (progress.status) {
                "locked" -> {
                    progressText.visibility = View.GONE
                    actionButton.visibility = View.GONE
                    lockedIcon.visibility = View.VISIBLE
                    card.alpha = 0.6f // Atenuar tarjeta
                }
                "unlocked" -> {
                    progressText.visibility = View.VISIBLE
                    progressText.text = "Etapas: ${progress.stagesCompleted} / ${lesson.totalStages}"
                    actionButton.visibility = View.VISIBLE
                    lockedIcon.visibility = View.GONE
                    card.alpha = 1.0f

                    if (progress.stagesCompleted > 0 && progress.stagesCompleted < lesson.totalStages) {
                        actionButton.text = "Continuar"
                    } else if (progress.stagesCompleted == lesson.totalStages) {
                        actionButton.text = "Reintentar"
                    } else {
                        actionButton.text = "Comenzar"
                    }
                }
                "completed" -> {
                    progressText.visibility = View.VISIBLE
                    progressText.text = "¡Completado! ${progress.stagesCompleted} / ${lesson.totalStages}"
                    actionButton.visibility = View.VISIBLE
                    actionButton.text = "Revisar"
                    lockedIcon.visibility = View.GONE
                    card.alpha = 1.0f
                }
            }

            // Configurar el click listener
            itemView.setOnClickListener {
                onLessonClick(lesson, progress)
            }
            actionButton.setOnClickListener {
                onLessonClick(lesson, progress)
            }
        }
    }
}