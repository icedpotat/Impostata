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

    var chanceOneCrew = 1

    val players get() = session.players
    val gridAssignedPairs get() = session.gridAssignedPairs

    fun initWordPairs(context: Context) {
        wordRepository.init(context)
    }

    val gameEnded: Boolean
        get() = session.gameEnded

    fun setupGame(playerNames: List<String>, undercoverCountOriginal: Int, impostorCountOriginal: Int): Boolean {
        session = GameSession()
        var impostorCount = impostorCountOriginal
        var undercoverCount = undercoverCountOriginal
        var allowJester = true
        if (playerNames.size < 3 || undercoverCount + impostorCount >= playerNames.size) return false

        session.players = playerNames.mapIndexed { i, name ->
            Player(name.trim().ifEmpty { "Spieler ${i + 1}" })
        }.toMutableList()


        val chaosRoll = Random.nextInt(100)
        Log.d("GameMode", "Roll=$chaosRoll")

        when {
            chaosRoll < chanceNoImpostor -> {
                // No impostors
                impostorCount = 0
                session.isCrewGame = true
            }
            chaosRoll < chanceNoImpostor + chanceAllImpostor -> {
                // All impostors
                session.selectedPair = wordRepository.nextWordPair()
                session.players.forEach { it.role = Role.IMPOSTOR; it.originalRole = Role.IMPOSTOR }
                session.remainingImpostors = session.players.size
                session.startImpostors = session.players.size
                session.isImpostorGame = true
                return true
            }
            chaosRoll < chanceNoImpostor + chanceAllImpostor + chanceOneCrew -> {
                // One crew mode: a single crew member, everyone else is an impostor
                impostorCount = playerNames.size - 1
                undercoverCount = 0
                allowJester = false
            }
        }

        session.selectedPair = wordRepository.nextWordPair()

        session.players.forEach { it.role = Role.CREW }
        roleAssigner.assign(session.players, Role.UNDERCOVER, undercoverCount)
        roleAssigner.assign(session.players, Role.IMPOSTOR, impostorCount)
        if (allowJester && session.players.count { it.role == Role.CREW } > 1 && Random.nextInt(100) < chanceJester) {
            roleAssigner.assign(session.players, Role.JESTER, 1)
        }

        session.players.forEach { it.originalRole = it.role }
        session.remainingImpostors = impostorCount
        session.startImpostors = impostorCount
        return true
    }

    fun prepareRolesForGrid(playerNames: List<String>, undercoverCountOriginal: Int, impostorCountOriginal: Int): Boolean {
        session = GameSession()
        val playerCount = playerNames.size
        var impostorCount = impostorCountOriginal
        var undercoverCount = undercoverCountOriginal
        var allowJester = true
        if (playerCount < 3 || undercoverCount + impostorCount >= playerCount) return false

        session.players = playerNames.mapIndexed { i, name ->
            Player(name.trim().ifEmpty { "Spieler ${i + 1}" })
        }.toMutableList()

        // Determine chaos mode like setupGame()
        val chaosRoll = Random.nextInt(100)
        Log.d(
            "GridMode",
            "Roll=$chaosRoll, NoImp=$chanceNoImpostor, AllImp=$chanceAllImpostor, OneCrew=$chanceOneCrew"
        )

        when {
            chaosRoll < chanceNoImpostor -> {
                // No impostors
                impostorCount = 0
                session.isCrewGame = true
            }

            chaosRoll < chanceNoImpostor + chanceAllImpostor -> {
                // All impostors
                session.gridAssignedPairs = List(playerCount) {
                    Role.IMPOSTOR to null
                }.shuffled().toMutableList()

                session.selectedPair = wordRepository.nextWordPair()
                session.startImpostors = playerCount
                session.remainingImpostors = playerCount
                session.isImpostorGame = true
                return true
            }

            chaosRoll < chanceNoImpostor + chanceAllImpostor + chanceOneCrew -> {
                // One crew mode: a single crew member, everyone else is an impostor
                impostorCount = playerCount - 1
                undercoverCount = 0
                allowJester = false
            }
        }


        // Proceed as usual otherwise
        val selected = wordRepository.nextWordPair()

        val roles = buildList {
            repeat(impostorCount) { add(Role.IMPOSTOR) }
            repeat(undercoverCount) { add(Role.UNDERCOVER) }
            repeat(playerCount - impostorCount - undercoverCount) { add(Role.CREW) }
        }.toMutableList()

        if (allowJester && roles.count { it == Role.CREW } > 1 && Random.nextInt(100) < chanceJester) {
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
        return true
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
        val target = session.selectedPair?.crewWord?.trim()?.lowercase() ?: return false
        val correct = guess.trim().lowercase() == target
        if (correct) session.gameEnded = true
        return correct
    }

    fun getRemainingRoleCount(role: Role): Int =
        session.players.count { !it.isEjected && it.role == role }

    fun isGameOver(): Boolean {
        if (session.gameEnded) return true
        val active = session.players.count { !it.isEjected }
        return when {
            // Nobody to find: end once only two players are left
            session.isCrewGame -> active <= 2
            session.startImpostors > 0 -> session.remainingImpostors == 0
            // Undercover-only game: ends when every undercover is out
            else -> getRemainingRoleCount(Role.UNDERCOVER) == 0
        }
    }

    fun wasFirstEjection(): Boolean = session.players.count { it.isEjected } == 1

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
    }

    fun addWordPair(crew: String, undercover: String): Boolean {
        val c = crew.trim(); val u = undercover.trim()
        if (c.isEmpty() || u.isEmpty()) return false
        return wordRepository.addCustomPair(WordPair(c, u))
    }

    /** Parses one "crew,undercover" (or ";"-separated) line. */
    fun parsePairLine(line: String): Pair<String, String>? {
        val parts = line.split(',', ';').map { it.trim().trim('"') }
        return if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) parts[0] to parts[1] else null
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
                .mapNotNull { line -> GameLogic.parsePairLine(line)?.let { WordPair(it.first, it.second) } }
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

    fun addCustomPair(pair: WordPair): Boolean {
        if (pair in customWordPairs || pair in wordPairsMaster) return false
        customWordPairs.add(pair)
        wordPairs.add(pair)
        return true
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
