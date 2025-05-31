package ch.spitalstsag.impostata.model

data class Group(
    val name: String,
    val colorHex: String = "#FF9800", // Default color
    val players: MutableList<Player> = mutableListOf()
)
