// GameLogic.kt (with GameSession abstraction)
// GameLogic.kt (Fix Jester in Grid Mode)
package com.freeze.impostata

import android.content.Context
import android.util.Log
import com.freeze.impostata.model.Player
import com.freeze.impostata.model.Role
import com.freeze.impostata.model.WordPair
import kotlin.random.Random

object GameLogic {
    private val wordRepository = WordRepository()
    private val roleAssigner = RoleAssigner()
    private var session = GameSession()

    var chanceNoImpostor = 1
    var chanceAllImpostor = 1
    var chanceJester = 1

    val players get() = session.players
    val gridAssignedPairs get() = session.gridAssignedPairs

    fun initWordPairs(context: Context) {
        wordRepository.init(context)
    }

    val gameEnded: Boolean
        get() = session.gameEnded

    fun setupGame(playerNames: List<String>, undercoverCount: Int, impostorCountOriginal: Int): Boolean {
        session = GameSession()
        var impostorCount = impostorCountOriginal
        if (undercoverCount + impostorCount >= playerNames.size) return false

        session.players = playerNames.mapIndexed { i, name ->
            Player(name.trim().ifEmpty { "Spieler ${i + 1}" })
        }.toMutableList()

        val chaosRoll = Random.nextInt(100)
        Log.d("GameMode", "Roll=$chaosRoll, NoImpChance=$chanceNoImpostor, AllImpChance=$chanceAllImpostor")

        if (chaosRoll < chanceNoImpostor) {
            impostorCount = 0
            session.isCrewGame = true
        }

        if (chaosRoll in chanceNoImpostor until (chanceNoImpostor + chanceAllImpostor)) {
            session.players.forEach { it.role = Role.IMPOSTOR }
            session.remainingImpostors = session.players.size
            session.startImpostors = session.players.size
            session.isImpostorGame = true
            return true
        }

        session.selectedPair = wordRepository.nextWordPair()

        session.players.forEach { it.role = Role.CREW }
        roleAssigner.assign(session.players, Role.UNDERCOVER, undercoverCount)
        roleAssigner.assign(session.players, Role.IMPOSTOR, impostorCount)
        if (session.players.count { it.role == Role.CREW } > 0 && Random.nextInt(100) < chanceJester) {
            roleAssigner.assign(session.players, Role.JESTER, 1)
        }

        session.players.forEach { it.originalRole = it.role }
        session.remainingImpostors = impostorCount
        session.startImpostors = impostorCount
        return true
    }

    fun prepareRolesForGrid(playerNames: List<String>, undercoverCount: Int, impostorCountOriginal: Int) {
        session = GameSession()
        val playerCount = playerNames.size
        var impostorCount = impostorCountOriginal

        session.players = playerNames.mapIndexed { i, name ->
            Player(name.trim().ifEmpty { "Spieler ${i + 1}" })
        }.toMutableList()

        // Determine chaos mode like setupGame()
        val chaosRoll = Random.nextInt(100)
        Log.d("GridMode", "Roll=$chaosRoll, NoImpChance=$chanceNoImpostor, AllImpChance=$chanceAllImpostor")

        if (chaosRoll < chanceNoImpostor) {
            impostorCount = 0
            session.isCrewGame = true
        }

        if (chaosRoll in chanceNoImpostor until (chanceNoImpostor + chanceAllImpostor)) {
            session.gridAssignedPairs = List(playerCount) {
                Role.IMPOSTOR to null
            }.shuffled().toMutableList()

            session.selectedPair = wordRepository.nextWordPair()
            session.startImpostors = playerCount
            session.remainingImpostors = playerCount
            session.isImpostorGame = true
            return
        }

        // Proceed as usual otherwise
        val selected = wordRepository.nextWordPair()

        val roles = buildList {
            repeat(impostorCount) { add(Role.IMPOSTOR) }
            repeat(undercoverCount) { add(Role.UNDERCOVER) }
            repeat(playerCount - impostorCount - undercoverCount) { add(Role.CREW) }
        }.toMutableList()

        if (roles.count { it == Role.CREW } > 0 && Random.nextInt(100) < chanceJester) {
            val crewIndices = roles.withIndex().filter { it.value == Role.CREW }.map { it.index }
            if (crewIndices.isNotEmpty()) {
                val jesterIndex = crewIndices.random()
                roles[jesterIndex] = Role.JESTER
            }
        }

        session.gridAssignedPairs = roles.shuffled().map { role ->
            val word = when (role) {
                Role.IMPOSTOR -> null
                Role.UNDERCOVER -> selected.undercoverWord
                Role.CREW, Role.JESTER -> selected.crewWord
                else -> null
            }
            role to word
        }.toMutableList()

        session.selectedPair = selected
        session.startImpostors = roles.count { it == Role.IMPOSTOR }
        session.remainingImpostors = session.startImpostors
    }

    fun getWordForPlayer(index: Int): String? {
        val word = session.selectedPair?.crewWord
        return when (session.players.getOrNull(index)?.role) {
            Role.CREW -> word
            Role.JESTER -> "Du bist Jester. Das Wort ist: \n$word"
            Role.UNDERCOVER -> session.selectedPair?.undercoverWord
            Role.IMPOSTOR -> "Du bist der Impostor."
            else -> null
        }
    }

    fun ejectPlayer(index: Int) {
        val ejected = session.players.count { it.isEjected }
        session.players.getOrNull(index)?.let {
            if (!it.isEjected) {
                if (it.role == Role.JESTER && ejected == 0) session.gameEnded = true
                if (it.role == Role.IMPOSTOR) session.remainingImpostors--
                it.role = Role.EJECTED
                it.isEjected = true
            }
        }
    }

    fun checkImpostorGuessAndEndGame(guess: String): Boolean {
        val correct = guess.trim().lowercase() == session.selectedPair?.crewWord?.lowercase()
        if (correct) session.gameEnded = true
        return correct
    }

    fun getRemainingRoleCount(role: Role): Int =
        session.players.count { !it.isEjected && it.role == role }

    fun isGameOver(): Boolean = !session.isCrewGame && session.remainingImpostors == 0 || session.gameEnded

    fun getGameSummary(): String {
        val grouped = session.players.groupBy { it.originalRole }
        return buildString {
            append("Spielzusammenfassung:\n\nDas Wort war: ${session.selectedPair?.crewWord}\n\n")
            Role.entries.forEach { role ->
                grouped[role]?.let {
                    append("🔹 ${role.name}:\n")
                    it.forEach { p -> append("• ${p.name}\n") }
                    append("\n")
                }
            }
        }.trim()
    }

    fun resetGame() {
        session = GameSession()
        wordRepository.reset()
    }

    fun addWordPair(crew: String, undercover: String) {
        wordRepository.addCustomPair(WordPair(crew.trim(), undercover.trim()))
    }
}

class GameSession {
    var players: MutableList<Player> = mutableListOf()
    var selectedPair: WordPair? = null
    var remainingImpostors: Int = 0
    var startImpostors: Int = 0
    var gameEnded: Boolean = false
    var isCrewGame: Boolean = false
    var isImpostorGame: Boolean = false
    var gridAssignedPairs: MutableList<Pair<Role, String?>> = mutableListOf()
}

class WordRepository {
    private val wordPairsMaster = mutableListOf<WordPair>()
    private val usedWordPairs = mutableListOf<WordPair>()
    private val customWordPairs = mutableListOf<WordPair>()
    private var wordPairs = mutableListOf<WordPair>()

    fun init(context: Context) {
        if (wordPairsMaster.isEmpty()) {
            wordPairsMaster += context.assets.open("word_pairs.txt")
                .bufferedReader()
                .lineSequence()
                .mapNotNull { line ->
                    val parts = line.split(',')
                    if (parts.size == 2) WordPair(parts[0].trim(), parts[1].trim()) else null
                }
                .toList()
        }
        reset()
    }

    fun nextWordPair(): WordPair {
        if (wordPairs.isEmpty()) {
            usedWordPairs.clear()
            wordPairs = (wordPairsMaster + customWordPairs).toMutableList()
        }
        return wordPairs.removeAt(Random.nextInt(wordPairs.size)).also { usedWordPairs.add(it) }
    }

    fun reset() {
        wordPairs = (wordPairsMaster + customWordPairs).toMutableList()
    }

    fun addCustomPair(pair: WordPair) {
        customWordPairs.add(pair)
        wordPairs = (wordPairsMaster + customWordPairs).toMutableList()
    }
}

class RoleAssigner {
    fun assign(players: MutableList<Player>, role: Role, count: Int) {
        players.withIndex()
            .filter { it.value.role == Role.CREW }
            .shuffled()
            .take(count)
            .forEach { players[it.index].role = role }
    }
}
