package ch.spitalstsag.impostata.model

data class Player(
    val name: String,
    var role: Role = Role.CREW,
    var isEjected: Boolean = false
)

enum class Role {
    CREW, UNDERCOVER, IMPOSTOR, JESTER, EJECTED
}
