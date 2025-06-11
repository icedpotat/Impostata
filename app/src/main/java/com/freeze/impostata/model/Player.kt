package com.freeze.impostata.model

data class Player(
    val name: String,
    var role: Role = Role.CREW,
    var isEjected: Boolean = false,
    var originalRole: Role = Role.CREW

)

enum class Role {
    CREW, UNDERCOVER, IMPOSTOR, JESTER, EJECTED
}
