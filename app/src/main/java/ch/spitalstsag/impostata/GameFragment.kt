package ch.spitalstsag.impostata

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.Image
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.fragment.app.Fragment
import ch.spitalstsag.impostata.model.Role

class GameFragment : Fragment() {

    private lateinit var gameLogic: GameLogic

    private lateinit var setupLayout: ViewGroup
    private lateinit var gameLayout: ViewGroup
    private lateinit var votingLayout: ViewGroup
    private lateinit var resultLayout: ViewGroup

    private  var selectedPlayerCount = 0
    private lateinit var undercoverCountText: TextView
    private lateinit var ImpostorCountText: TextView

    private lateinit var btnAddNames: Button
    private lateinit var btnStartGame: Button

    private lateinit var nameInputsContainer: LinearLayout

    private lateinit var currentPlayerText: TextView
    private lateinit var btnShowWord: Button
    private lateinit var wordDisplayLayout: ViewGroup
    private lateinit var wordText: TextView
    private lateinit var btnNextPlayer: Button

    private lateinit var votingButtonsContainer: LinearLayout

    private lateinit var btnSelectGroup: Button

    private lateinit var voteResultText: TextView
    private lateinit var ImpostorGuessLayout: ViewGroup
    private lateinit var ImpostorGuessInput: EditText
    private lateinit var btnConfirmGuess: Button
    private lateinit var btnContinueVoting: Button
    private lateinit var btnRestartGame: Button

    private lateinit var importButton: Button

    private lateinit var btnUndercoverPlus: Button
    private lateinit var btnUndercoverMinus: Button
    private lateinit var btnImpostorPlus: Button
    private lateinit var btnImpostorMinus: Button
    private lateinit var civilianCountText: TextView
    private lateinit var groupContainer: LinearLayout


    private var currentPlayerIndex = 0

    private val IMPORT_WORDS_REQUEST_CODE = 1001

    private val REQUEST_CODE_SELECT_GROUP = 2001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)

         groupContainer = view.findViewById<LinearLayout>(R.id.selectedGroupContainer)


        gameLogic = GameLogic

        setupLayout = view.findViewById(R.id.setupLayout)
        gameLayout = view.findViewById(R.id.gameLayout)
        votingLayout = view.findViewById(R.id.votingLayout)
        resultLayout = view.findViewById(R.id.resultLayout)

        val playerCountSlider = view.findViewById<SeekBar>(R.id.playerCountSlider)
        val playerCountLabel = view.findViewById<TextView>(R.id.playerCountLabel)

        playerCountSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 3
                playerCountLabel.text = "$value Spieler"
                selectedPlayerCount = value
                updateCivilianCount()
            }


            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        selectedPlayerCount = playerCountSlider.progress + 3
        undercoverCountText = view.findViewById(R.id.undercoverCountText)
        ImpostorCountText = view.findViewById(R.id.ImpostorCountText)


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
        ImpostorGuessLayout = view.findViewById(R.id.ImpostorGuessLayout)
        ImpostorGuessInput = view.findViewById(R.id.ImpostorGuessInput)
        btnConfirmGuess = view.findViewById(R.id.btnConfirmGuess)
        btnContinueVoting = view.findViewById(R.id.btnContinueVoting)
        btnRestartGame = view.findViewById(R.id.btnRestartGame)

        btnUndercoverPlus = view.findViewById(R.id.btnUndercoverPlus)
        btnUndercoverMinus = view.findViewById(R.id.btnUndercoverMinus)
        btnImpostorPlus = view.findViewById(R.id.btnImpostorPlus)
        btnImpostorMinus = view.findViewById(R.id.btnImpostorMinus)
        civilianCountText = view.findViewById(R.id.civilianCountText)
        btnSelectGroup = view.findViewById(R.id.btnSelectGroup)

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
        btnConfirmGuess.setOnClickListener { confirmImpostorGuess() }
        btnContinueVoting.setOnClickListener { startVoting() }
        btnRestartGame.setOnClickListener { restartGame() }

        btnUndercoverPlus.setOnClickListener { changeRoleCount(undercoverCountText, +1) }
        btnUndercoverMinus.setOnClickListener { changeRoleCount(undercoverCountText, -1) }

        btnImpostorPlus.setOnClickListener { changeRoleCount(ImpostorCountText, +1) }
        btnImpostorMinus.setOnClickListener { changeRoleCount(ImpostorCountText, -1) }

        btnSelectGroup.setOnClickListener {
            val intent = Intent(requireContext(), GroupActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SELECT_GROUP)
        }



        return view
    }



    private fun addNameInputs() {
        nameInputsContainer.removeAllViews()
        for (i in 0 until selectedPlayerCount) {
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

        val undercoverCount = undercoverCountText.text.toString().toIntOrNull() ?: 0
        val ImpostorCount = ImpostorCountText.text.toString().toIntOrNull() ?: 0


        if (!gameLogic.setupGame(playerNames, undercoverCount, ImpostorCount)) {
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

    private fun changeRoleCount(textView: TextView, delta: Int) {
        val current = textView.text.toString().toIntOrNull() ?: 0
        val updated = (current + delta).coerceAtLeast(0)
        textView.text = updated.toString()
        updateCivilianCount()
    }

    private fun updateCivilianCount() {
        val total = selectedPlayerCount
        val undercover = undercoverCountText.text.toString().toIntOrNull() ?: 0
        val Impostor = ImpostorCountText.text.toString().toIntOrNull() ?: 0
        val civilian = total - undercover - Impostor
        civilianCountText.text = "Zivile: ${civilian.coerceAtLeast(0)}"
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

        if (role == Role.IMPOSTOR) {
            ImpostorGuessLayout.visibility = View.VISIBLE
            btnContinueVoting.visibility = View.GONE
            btnConfirmGuess.visibility = View.VISIBLE

        } else {
            ImpostorGuessLayout.visibility = View.GONE
            btnContinueVoting.visibility = View.VISIBLE
        }

        if (gameLogic.isGameOver()) {
            voteResultText.append("\nDas Spiel ist beendet, die Impostor wurden gefunden!")
            btnContinueVoting.visibility = View.GONE
            btnRestartGame.visibility = View.VISIBLE
        }
    }

    private fun confirmImpostorGuess() {
        val guess = ImpostorGuessInput.text.toString()
        val correct = gameLogic.checkImpostorGuess(guess)

        if (correct) {
            voteResultText.text = "Impostor hat richtig geraten! Impostor gewinnen."
        } else {
            voteResultText.text = "Falsches Wort. Das Spiel geht weiter."
            btnContinueVoting.visibility = View.VISIBLE
        }
        ImpostorGuessLayout.visibility = View.GONE
        btnConfirmGuess.visibility = View.GONE
    }

    private fun restartGame() {
        gameLogic.resetGame()
        btnRestartGame.visibility = View.GONE
        setupLayout.visibility = View.VISIBLE
        gameLayout.visibility = View.GONE
        votingLayout.visibility = View.GONE
        resultLayout.visibility = View.GONE

        ImpostorGuessInput.setText("")

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
                Toast.makeText(requireContext(), "Wörter erfolgreich importiert.", Toast.LENGTH_SHORT).show()
            }
        }
        else if (requestCode == REQUEST_CODE_SELECT_GROUP && resultCode == Activity.RESULT_OK) {
            val groupName = data?.getStringExtra("selectedGroupName")
            val playerNames = data?.getStringArrayListExtra("selectedGroupPlayers")

            Log.d("TEST", "Group Name: $groupName, Player Names: $playerNames")
            groupContainer.removeAllViews()

            val inflater = LayoutInflater.from(requireContext())
            val groupView = inflater.inflate(R.layout.item_group, groupContainer, false)


            groupView.findViewById<TextView>(R.id.groupNameText).text = groupName
            groupView.findViewById<TextView>(R.id.playerListText).text = playerNames.toString()
            groupView.findViewById<ImageButton>(R.id.editGroupBtn).visibility = View.GONE


            groupContainer.addView(groupView)
        }
    }
}
