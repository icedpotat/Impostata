package ch.spitalstsag.impostata

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.fragment.app.Fragment
import ch.spitalstsag.impostata.model.Role
import ch.spitalstsag.impostata.*
class GameFragment : Fragment() {

    private lateinit var gameLogic: GameLogic

    private lateinit var setupLayout: ViewGroup
    private lateinit var gameLayout: ViewGroup
    private lateinit var votingLayout: ViewGroup
    private lateinit var resultLayout: ViewGroup

    private lateinit var playerCountInput: EditText
    private  var selectedPlayerCount = 0
    private lateinit var undercoverCountInput: EditText
    private lateinit var impostorCountInput: EditText

    private lateinit var btnAddNames: Button
    private lateinit var btnStartGame: Button

    private lateinit var nameInputsContainer: LinearLayout

    private lateinit var currentPlayerText: TextView
    private lateinit var btnShowWord: Button
    private lateinit var wordDisplayLayout: ViewGroup
    private lateinit var wordText: TextView
    private lateinit var btnNextPlayer: Button

    private lateinit var votingButtonsContainer: LinearLayout

    private lateinit var voteResultText: TextView
    private lateinit var impostorGuessLayout: ViewGroup
    private lateinit var impostorGuessInput: EditText
    private lateinit var btnConfirmGuess: Button
    private lateinit var btnContinueVoting: Button
    private lateinit var btnRestartGame: Button

    private lateinit var importButton: Button

    private var currentPlayerIndex = 0

    private val IMPORT_WORDS_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)

        gameLogic = GameLogic

        setupLayout = view.findViewById(R.id.setupLayout)
        gameLayout = view.findViewById(R.id.gameLayout)
        votingLayout = view.findViewById(R.id.votingLayout)
        resultLayout = view.findViewById(R.id.resultLayout)

        val playerCountSlider = view.findViewById<SeekBar>(R.id.playerCountSlider)
        val playerCountLabel = view.findViewById<TextView>(R.id.playerCountLabel)

        playerCountSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 3 // because slider starts at 0 = 3 players
                playerCountLabel.text = value.toString()
                // Store the value somewhere if needed
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        selectedPlayerCount = playerCountSlider.progress + 3

        undercoverCountInput = view.findViewById(R.id.undercoverCountInput)
        impostorCountInput = view.findViewById(R.id.impostorCountInput)

        btnAddNames = view.findViewById(R.id.btnAddNames)
        btnStartGame = view.findViewById(R.id.btnStartGame)

        nameInputsContainer = view.findViewById(R.id.nameInputsContainer)

        currentPlayerText = view.findViewById(R.id.currentPlayerText)
        btnShowWord = view.findViewById(R.id.btnShowWord)
        wordDisplayLayout = view.findViewById(R.id.wordDisplayLayout)
        wordText = view.findViewById(R.id.wordText)
        btnNextPlayer = view.findViewById(R.id.btnNextPlayer)

        votingButtonsContainer = view.findViewById(R.id.votingButtonsLayout)

        voteResultText = view.findViewById(R.id.voteResultText)
        impostorGuessLayout = view.findViewById(R.id.impostorGuessLayout)
        impostorGuessInput = view.findViewById(R.id.impostorGuessInput)
        btnConfirmGuess = view.findViewById(R.id.btnConfirmGuess)
        btnContinueVoting = view.findViewById(R.id.btnContinueVoting)
        btnRestartGame = view.findViewById(R.id.btnRestartGame)

        importButton = view.findViewById(R.id.importWordsButton)

        importButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "text/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, IMPORT_WORDS_REQUEST_CODE)
        }

        btnAddNames.setOnClickListener { addNameInputs() }
        btnStartGame.setOnClickListener { startGame() }
        btnShowWord.setOnClickListener { showWord() }
        btnNextPlayer.setOnClickListener { nextPlayer() }
        btnConfirmGuess.setOnClickListener { confirmimpostorGuess() }
        btnContinueVoting.setOnClickListener { startVoting() }
        btnRestartGame.setOnClickListener { restartGame() }

        return view
    }

    private fun addNameInputs() {
        nameInputsContainer.removeAllViews()

        val count = selectedPlayerCount

        for (i in 0 until count) {
            val et = EditText(requireContext())
            et.hint = "Spieler ${i + 1} Name"
            et.inputType = InputType.TYPE_CLASS_TEXT
            nameInputsContainer.addView(et)
        }

        btnStartGame.visibility = View.VISIBLE
    }

    private fun startGame() {
        val playerNames = nameInputsContainer.children
            .filterIsInstance<EditText>()
            .map { it.text.toString() }
            .toList()
//Arsch?
        val undercoverCount = undercoverCountInput.text.toString().toIntOrNull() ?: 0
        val impostorCount = impostorCountInput.text.toString().toIntOrNull() ?: 0

        if (!gameLogic.setupGame(playerNames, undercoverCount, impostorCount)) {
            Toast.makeText(requireContext(), "Zu viele Undercover/Impostor für diese Spieleranzahl.", Toast.LENGTH_SHORT).show()
            return
        }

        // Switch UI
        setupLayout.visibility = View.GONE
        gameLayout.visibility = View.VISIBLE
        votingLayout.visibility = View.GONE
        resultLayout.visibility = View.GONE

        currentPlayerIndex = 0
        updateCurrentPlayer()
    }

    private fun updateCurrentPlayer() {
        if (currentPlayerIndex < gameLogic.players.size) {
            val player = gameLogic.players[currentPlayerIndex]
            currentPlayerText.text = "Gerät an: ${player.name}"
            wordDisplayLayout.visibility = View.GONE
        } else {
            gameLayout.visibility = View.GONE
            startVoting()
        }
    }

    private fun showWord() {
        val word = gameLogic.getWordForPlayer(currentPlayerIndex)
        wordText.text = word ?: "Fehler: Kein Wort gefunden"
        wordDisplayLayout.visibility = View.VISIBLE
    }

    private fun nextPlayer() {
        currentPlayerIndex++
        updateCurrentPlayer()
    }

    private fun startVoting() {
        if (gameLogic.gameEnded) return

        votingLayout.visibility = View.VISIBLE
        resultLayout.visibility = View.GONE
        votingButtonsContainer.removeAllViews()

        gameLogic.players.forEachIndexed { index, player ->
            if (!player.isEjected) {
                val btn = Button(requireContext()).apply {
                    text = player.name
                    setOnClickListener { resolveVote(index) }
                }
                votingButtonsContainer.addView(btn)
            }
        }
    }

    private fun resolveVote(index: Int) {
        votingLayout.visibility = View.GONE
        resultLayout.visibility = View.VISIBLE

        val role = gameLogic.players[index].role
        gameLogic.ejectPlayer(index)

        val playerName = gameLogic.players[index].name
        voteResultText.text = "$playerName wurde rausgeworfen. Rolle: $role"

        if (role == Role.impostor) {
            impostorGuessLayout.visibility = View.VISIBLE
            btnContinueVoting.visibility = View.GONE
        } else {
            impostorGuessLayout.visibility = View.GONE
            btnContinueVoting.visibility = View.VISIBLE
        }

        if (gameLogic.isGameOver()) {
            voteResultText.append("\nDas Spiel ist beendet, die Impostor wurden gefunden!")
            btnContinueVoting.visibility = View.GONE
            btnRestartGame.visibility = View.VISIBLE
        }
    }

    private fun confirmimpostorGuess() {
        val guess = impostorGuessInput.text.toString()
        val correct = gameLogic.checkimpostorGuess(guess)

        if (correct) {
            voteResultText.text = "Impostor hat richtig geraten! Impostor gewinnen."
        } else {
            voteResultText.text = "Falsches Wort. Das Spiel geht weiter."
            btnContinueVoting.visibility = View.VISIBLE
        }
        impostorGuessLayout.visibility = View.GONE
        btnConfirmGuess.visibility = View.GONE
    }

    private fun restartGame() {
        gameLogic.resetGame()
        btnRestartGame.visibility = View.GONE
        setupLayout.visibility = View.VISIBLE
        gameLayout.visibility = View.GONE
        votingLayout.visibility = View.GONE
        resultLayout.visibility = View.GONE

        playerCountInput.text.clear()
        undercoverCountInput.text.clear()
        impostorCountInput.text.clear()
        nameInputsContainer.removeAllViews()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMPORT_WORDS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                inputStream?.bufferedReader()?.forEachLine { line ->
                    val parts = line.split(",")
                    if (parts.size == 2) {
                        val crew = parts[0].trim()
                        val undercover = parts[1].trim()
                        GameLogic.addWordPair(crew, undercover)  // Assuming static or singleton
                    }
                }
            }
        }
    }

}
