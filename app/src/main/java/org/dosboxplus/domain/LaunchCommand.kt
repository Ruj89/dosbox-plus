package org.dosboxplus.domain

object LaunchCommand {
    private val executable = Regex(".+\\.(exe|bat|com)$", RegexOption.IGNORE_CASE)
    fun candidates(names: Iterable<String>): List<String> = names.filter { executable.matches(it) }.sortedBy { it.lowercase() }
    fun command(entry: GameEntry): String = buildString {
        append(entry.drive); append(":\ncd \\\n")
        append(entry.command); if (entry.arguments.isNotBlank()) append(' ').append(entry.arguments)
    }
}
