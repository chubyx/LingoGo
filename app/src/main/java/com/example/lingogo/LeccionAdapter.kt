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

typealias OnLessonClick = (Lesson, LessonProgress) -> Unit

class LeccionAdapter(private val onLessonClick: OnLessonClick) :
    RecyclerView.Adapter<LeccionAdapter.LeccionViewHolder>() {

    private var lessons: List<Lesson> = emptyList()
    private var progressMap: Map<String, LessonProgress> = emptyMap()

    fun submitList(lessons: List<Lesson>, progressMap: Map<String, LessonProgress>) {
        this.lessons = lessons
        this.progressMap = progressMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeccionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leccion_card, parent, false)
        return LeccionViewHolder(view)
    }

    override fun getItemCount(): Int = lessons.size

    override fun onBindViewHolder(holder: LeccionViewHolder, position: Int) {
        val lesson = lessons[position]
        // Si no existe progreso guardado, el primero está desbloqueado, el resto bloqueado
        val progress = progressMap[lesson.id] ?: LessonProgress(
            status = if (position == 0) "unlocked" else "locked",
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
                    card.alpha = 0.6f
                    card.isEnabled = false
                }
                "unlocked" -> {
                    progressText.visibility = View.VISIBLE
                    progressText.text = "Etapas: ${progress.stagesCompleted} / ${lesson.totalStages}"
                    actionButton.visibility = View.VISIBLE
                    lockedIcon.visibility = View.GONE
                    card.alpha = 1.0f
                    card.isEnabled = true

                    if (progress.stagesCompleted > 0) actionButton.text = "Continuar"
                    else actionButton.text = "Comenzar"
                }
                "completed" -> {
                    progressText.visibility = View.VISIBLE
                    progressText.text = "¡Completado!"
                    actionButton.visibility = View.VISIBLE
                    actionButton.text = "Repasar"
                    lockedIcon.visibility = View.GONE
                    card.alpha = 1.0f
                    card.isEnabled = true
                }
            }

            actionButton.setOnClickListener { onLessonClick(lesson, progress) }
            itemView.setOnClickListener {
                if(progress.status != "locked") onLessonClick(lesson, progress)
            }
        }
    }
}