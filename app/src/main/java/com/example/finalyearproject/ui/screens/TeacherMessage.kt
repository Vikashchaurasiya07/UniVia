data class TeacherMessage(
    val content: String = "",
    val date: String = "",  // Format: "yyyy-MM-dd"
    val time: String = "",  // Format: "HH:mm"
    val timestamp: Long = 0, // For accurate sorting
    val id: String? = null
) {
    constructor(content: String, date: String, time: String) :
            this(content, date, time, 0, null)
}