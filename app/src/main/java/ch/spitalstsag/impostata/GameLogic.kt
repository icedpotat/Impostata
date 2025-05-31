package ch.spitalstsag.impostata

import ch.spitalstsag.impostata.model.Player
import ch.spitalstsag.impostata.model.Role
import ch.spitalstsag.impostata.model.WordPair
import kotlin.random.Random

object GameLogic {

    private val wordPairsMaster = mutableListOf(
        WordPair("Bibliothek", "Buchhandlung"),
        WordPair("Zahnbürste", "Kamm"),
        WordPair("Fernseher", "Radio"),
        WordPair("Post", "Paketdienst"),
        WordPair("Teller", "Pfanne")
    )
    private var wordPairs = wordPairsMaster.toMutableList()

    var players = mutableListOf<Player>()
    var selectedPair: WordPair? = null
    var remainingImpostors = 0
    var startImpostors = 0
    var gameEnded = false

    fun setupGame(
        playerNames: List<String>,
        undercoverCount: Int,
        ImpostorCount: Int
    ): Boolean {
        val playerCount = playerNames.size
        if (undercoverCount + ImpostorCount >= playerCount) {
            return false
        }

        players.clear()
        players.addAll(playerNames.mapIndexed { index, name ->
            val cleanedName = name.trim()
            Player(if (cleanedName.isNotEmpty()) cleanedName else "Spieler ${index + 1}")
        })


        wordPairs = (wordPairsMaster + customWordPairs).toMutableList()


        val index = Random.nextInt(wordPairs.size)
        selectedPair = wordPairs.removeAt(index)

        // Assign roles
        players.forEach { it.role = Role.CREW }

        assignRole(Role.UNDERCOVER, undercoverCount)
        assignRole(Role.IMPOSTOR, ImpostorCount)
        remainingImpostors = ImpostorCount
        startImpostors = ImpostorCount
        gameEnded = false
        return true
    }

    private fun assignRole(role: Role, count: Int) {
        var assigned = 0
        while (assigned < count) {
            val idx = Random.nextInt(players.size)
            if (players[idx].role == Role.CREW) {
                players[idx].role = role
                assigned++
            }
        }
    }

    fun getRoleForPlayer(index: Int): Role? {
        if (index in players.indices) {
            return players[index].role
        }
        return null
    }

    fun getWordForPlayer(index: Int): String? {
        val role = getRoleForPlayer(index) ?: return null
        return when(role) {
            Role.CREW -> selectedPair?.crewWord
            Role.UNDERCOVER -> selectedPair?.undercoverWord
            Role.IMPOSTOR -> "Du bist der Impostor. Finde heraus, welches Wort die anderen meinen!"
            else -> null
        }
    }

    fun ejectPlayer(index: Int) {
        if (index in players.indices) {
            val role = players[index].role
            players[index].role = Role.EJECTED
            players[index].isEjected = true
            if (role == Role.IMPOSTOR) {
                remainingImpostors--
            }
        }
    }

    fun checkImpostorGuess(guess: String): Boolean {
        val correctWord = selectedPair?.crewWord?.lowercase()
        return guess.lowercase() == correctWord
    }

    fun isGameOver(): Boolean {
        return remainingImpostors <= 1
    }

    fun resetGame() {
        players.clear()
        wordPairs = (wordPairsMaster + customWordPairs).toMutableList()
        selectedPair = null
        remainingImpostors = 0
        gameEnded = false
    }

    val customWordPairs = mutableListOf<WordPair>()

    fun addWordPair(crew: String, undercover: String) {
        customWordPairs.add(WordPair(crew, undercover))
        wordPairs = (wordPairsMaster + customWordPairs).toMutableList()
    }


}
