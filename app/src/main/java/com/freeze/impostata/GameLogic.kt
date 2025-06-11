package com.freeze.impostata

import android.content.Context
import android.util.Log
import com.freeze.impostata.model.Player
import com.freeze.impostata.model.Role
import com.freeze.impostata.model.WordPair
import kotlin.random.Random

object GameLogic {
    private val wordPairsMaster = mutableListOf<WordPair>()

    private var wordPairs = mutableListOf<WordPair>()
    private val usedWordPairs = mutableListOf<WordPair>()
    private val customWordPairs = mutableListOf<WordPair>()

    var players = mutableListOf<Player>()
    private var selectedPair: WordPair? = null
    private var remainingImpostors = 0
    private var startImpostors = 0
    var gameEnded = false

    private var isCrewGame = false
    private var isImpostorGame = false

    var chanceNoImpostor = 1
    var chanceAllImpostor = 1
    var chanceJester = 1

    fun initWordPairs(context: Context) {
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
    }


    fun setupGame(playerNames: List<String>, undercoverCount: Int, impostorCountOriginal: Int): Boolean {
        isCrewGame = false
        isImpostorGame = false
        var impostorCount = impostorCountOriginal
        if (undercoverCount + impostorCount + 1 >= playerNames.size) return false

        players = playerNames.mapIndexed { index, name ->
            val trimmedName = name.trim()
            Player(if (trimmedName.isNotEmpty()) trimmedName else "Spieler ${index + 1}")
        }.toMutableList()

        if (wordPairs.isEmpty()) {
            wordPairs = (wordPairsMaster + customWordPairs).filterNot { usedWordPairs.contains(it) }.toMutableList()
            if (wordPairs.isEmpty()) {
                usedWordPairs.clear()
                wordPairs = (wordPairsMaster + customWordPairs).toMutableList()
            }
        }

        val chaosRoll = Random.nextInt(100)
        Log.d("GameMode", "Roll=$chaosRoll, NoImpChance=$chanceNoImpostor, AllImpChance=$chanceAllImpostor")

        if (chaosRoll < chanceNoImpostor) {
            impostorCount = 0
            isCrewGame = true
        }

        if (chaosRoll in chanceNoImpostor until (chanceNoImpostor + chanceAllImpostor)) {
            players.forEach { it.role = Role.IMPOSTOR }
            remainingImpostors = players.size
            startImpostors = players.size
            gameEnded = false
            isImpostorGame = true
            return true
        }


        if (!isImpostorGame) {
            selectedPair = wordPairs.removeAt(Random.nextInt(wordPairs.size)).also {
                usedWordPairs.add(it)
            }
        }

        players.forEach { it.role = Role.CREW }
        assignRole(Role.UNDERCOVER, undercoverCount)
        assignRole(Role.IMPOSTOR, impostorCount)
        if (Random.nextInt(100) < chanceJester) {
            assignRole(Role.JESTER, 1)
        }

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

    private fun getRoleForPlayer(index: Int): Role? = players.getOrNull(index)?.role

    fun getWordForPlayer(index: Int): String? {
        val jesterWord = selectedPair?.crewWord
        return when (getRoleForPlayer(index)) {
            Role.CREW -> selectedPair?.crewWord
            Role.JESTER -> "Du bist Jester. Das Wort ist: $jesterWord"
            Role.UNDERCOVER -> selectedPair?.undercoverWord
            Role.IMPOSTOR -> "Du bist der Impostor."
            else -> null
        }
    }

    fun ejectPlayer(index: Int) {
        val ejectedPlayers = players.count { it.isEjected }

        players.getOrNull(index)?.let {
            if (!it.isEjected) {
                if (it.role == Role.JESTER && ejectedPlayers == 0) {
                    gameEnded = true
                    // optionally: show jester win message
                }
                if (it.role == Role.IMPOSTOR) remainingImpostors--
                it.role = Role.EJECTED
                it.isEjected = true
            }
        }
    }

    fun checkImpostorGuessAndEndGame(guess: String): Boolean {
        val correct = guess.trim().lowercase() == selectedPair?.crewWord?.lowercase()
        Log.d("WordPair", selectedPair.toString())
        if (correct) {
            gameEnded = true
        }
        return correct
    }


    fun isGameOver(): Boolean = !isCrewGame && remainingImpostors == 0 || gameEnded

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
