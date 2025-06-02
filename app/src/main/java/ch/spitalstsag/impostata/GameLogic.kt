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
    val customWordPairs = mutableListOf<WordPair>()

    var players = mutableListOf<Player>()
    var selectedPair: WordPair? = null
    var remainingImpostors = 0
    var startImpostors = 0
    var gameEnded = false

    fun setupGame(playerNames: List<String>, undercoverCount: Int, impostorCount: Int): Boolean {
        if (undercoverCount + impostorCount >= playerNames.size) return false

        players = playerNames.mapIndexed { index, name ->
            val trimmedName = name.trim()
            Player(if (trimmedName.isNotEmpty()) trimmedName else "Spieler ${index + 1}")
        }.toMutableList()

        wordPairs = (wordPairsMaster + customWordPairs).toMutableList()
        selectedPair = wordPairs.removeAt(Random.nextInt(wordPairs.size))

        players.forEach { it.role = Role.CREW }
        assignRole(Role.UNDERCOVER, undercoverCount)
        assignRole(Role.IMPOSTOR, impostorCount)

        remainingImpostors = impostorCount
        startImpostors = impostorCount
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

    fun getRoleForPlayer(index: Int): Role? = players.getOrNull(index)?.role

    fun getWordForPlayer(index: Int): String? {
        return when (getRoleForPlayer(index)) {
            Role.CREW -> selectedPair?.crewWord
            Role.UNDERCOVER -> selectedPair?.undercoverWord
            Role.IMPOSTOR -> "Du bist der Impostor. Finde heraus, welches Wort die anderen meinen!"
            else -> null
        }
    }

    fun ejectPlayer(index: Int) {
        players.getOrNull(index)?.let {
            if (!it.isEjected) {
                if (it.role == Role.IMPOSTOR) remainingImpostors--
                it.role = Role.EJECTED
                it.isEjected = true
            }
        }
    }

    fun checkImpostorGuessAndEndGame(guess: String): Boolean {
        val correct = guess.trim().lowercase() == selectedPair?.crewWord?.lowercase()
        if (correct) {
            gameEnded = true
        }
        return correct
    }


    fun isGameOver(): Boolean = remainingImpostors == 0 || gameEnded

    fun resetGame() {
        players.clear()
        wordPairs = (wordPairsMaster + customWordPairs).toMutableList()
        selectedPair = null
        remainingImpostors = 0
        gameEnded = false
    }

    fun addWordPair(crew: String, undercover: String) {
        customWordPairs.add(WordPair(crew.trim(), undercover.trim()))
        wordPairs = (wordPairsMaster + customWordPairs).toMutableList()
    }
}
