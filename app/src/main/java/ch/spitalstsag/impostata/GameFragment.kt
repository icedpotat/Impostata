package ch.spitalstsag.impostata

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.view.children
import androidx.fragment.app.Fragment
import ch.spitalstsag.impostata.model.Role
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import ch.spitalstsag.impostata.GameLogic.players
import androidx.core.view.isVisible
import ch.spitalstsag.impostata.model.Player


class GameFragment : Fragment() {
    private enum class GamePhase { SETUP, PLAYING, VOTING, RESULT }

    private lateinit var gameLogic: GameLogic

    private lateinit var setupLayout: ViewGroup
    private lateinit var gameLayout: ViewGroup
    private lateinit var votingLayout: ViewGroup
    private lateinit var resultLayout: ViewGroup
    private lateinit var cardFront: View
    private lateinit var cardBack: View

    private lateinit var nameInputsContainer: GridLayout
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
    private lateinit var playerCountLabel: TextView
    private lateinit var playerCountSlider: SeekBar
    private lateinit var btnUndercoverPlus: Button
    private lateinit var btnUndercoverMinus: Button
    private lateinit var btnImpostorPlus: Button
    private lateinit var btnImpostorMinus: Button
    private lateinit var civilianCountText: TextView
    private lateinit var groupContainer: LinearLayout
    private lateinit var btnStartGame: Button
    private lateinit var btnSelectGroup: Button
    private lateinit var undercoverCountText: TextView
    private lateinit var impostorCountText: TextView

    private lateinit var btnSettings: ImageButton

    private var selectedPlayerCount = 3
    private var currentPlayerIndex = 0
    private var flippedOnce = 0

    private val IMPORT_WORDS_REQUEST_CODE = 1001
    private val REQUEST_CODE_SELECT_GROUP = 2001

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)
        initViews(view)
        setupListeners()
        addNameInputs()
        updateCivilianCount()
        updateRoleButtonsVisibility()
        InitFlipAnimation(view)

        val prefs = requireContext().getSharedPreferences("impostata_prefs", Context.MODE_PRIVATE)
        GameLogic.chanceAllImpostor = prefs.getInt("chance_all_impostor", 1)
        GameLogic.chanceNoImpostor = prefs.getInt("chance_no_impostor", 1)
        GameLogic.chanceJester = prefs.getInt("chance_jester", 1)

        return view
    }

    private fun initViews(view: View) {
        gameLogic = GameLogic

        setupLayout = view.findViewById(R.id.setupLayout)
        gameLayout = view.findViewById(R.id.gameLayout)
        votingLayout = view.findViewById(R.id.votingLayout)
        resultLayout = view.findViewById(R.id.resultLayout)

        playerCountSlider = view.findViewById(R.id.playerCountSlider)
        playerCountLabel = view.findViewById(R.id.playerCountLabel)

        undercoverCountText = view.findViewById(R.id.undercoverCountText)
        impostorCountText = view.findViewById(R.id.ImpostorCountText)

        btnStartGame = view.findViewById(R.id.btnStartGame)
        nameInputsContainer = view.findViewById(R.id.nameInputsContainer)
        currentPlayerText = view.findViewById(R.id.currentPlayerText)
        cardFront = view.findViewById(R.id.cardFront)
        cardBack = view.findViewById(R.id.cardBack)
        wordText = view.findViewById(R.id.wordText)
        btnNextPlayer = view.findViewById(R.id.btnNextPlayer)
        votingButtonsContainer = view.findViewById(R.id.votingButtonsLayout)
        voteResultText = view.findViewById(R.id.voteResultText)
        impostorGuessLayout = view.findViewById(R.id.ImpostorGuessLayout)
        impostorGuessInput = view.findViewById(R.id.ImpostorGuessInput)
        btnConfirmGuess = view.findViewById(R.id.btnConfirmGuess)
        btnContinueVoting = view.findViewById(R.id.btnContinueVoting)
        btnRestartGame = view.findViewById(R.id.btnRestartGame)
        btnUndercoverPlus = view.findViewById(R.id.btnUndercoverPlus)
        btnUndercoverMinus = view.findViewById(R.id.btnUndercoverMinus)
        btnImpostorPlus = view.findViewById(R.id.btnImpostorPlus)
        btnImpostorMinus = view.findViewById(R.id.btnImpostorMinus)
        civilianCountText = view.findViewById(R.id.civilianCountText)
        groupContainer = view.findViewById(R.id.selectedGroupContainer)
        btnSelectGroup = view.findViewById(R.id.btnSelectGroup)

        btnSettings = view.findViewById(R.id.settingsButton)
    }

    private fun setGamePhase(phase: GamePhase) {
        setupLayout.visibility = if (phase == GamePhase.SETUP) View.VISIBLE else View.GONE
        gameLayout.visibility = if (phase == GamePhase.PLAYING) View.VISIBLE else View.GONE
        votingLayout.visibility = if (phase == GamePhase.VOTING) View.VISIBLE else View.GONE
        resultLayout.visibility = if (phase == GamePhase.RESULT) View.VISIBLE else View.GONE
    }

    private fun getRoleCounts(): Triple<Int, Int, Int> {
        val undercover = undercoverCountText.text.toString().toIntOrNull() ?: 0
        val impostor = impostorCountText.text.toString().toIntOrNull() ?: 0
        val civilian = (selectedPlayerCount - undercover - impostor).coerceAtLeast(0)
        return Triple(undercover, impostor, civilian)
    }

    private fun updateCivilianCount() {
        val (_, _, civilian) = getRoleCounts()
        civilianCountText.text = "Crew: $civilian"
    }

    private fun updateRoleButtonsVisibility() {
        val (undercover, impostor, _) = getRoleCounts()
        val maxRoles = selectedPlayerCount / 2 + 1

        btnUndercoverPlus.visibility = if (undercover + impostor < maxRoles) View.VISIBLE else View.INVISIBLE
        btnImpostorPlus.visibility = if (undercover + impostor < maxRoles) View.VISIBLE else View.INVISIBLE

        btnUndercoverMinus.visibility = if ((impostor == 0 && undercover == 1) || undercover == 0) View.INVISIBLE else View.VISIBLE
        btnImpostorMinus.visibility = if ((undercover == 0 && impostor == 1) || impostor == 0) View.INVISIBLE else View.VISIBLE
    }

    private fun updateRoleCountsAndClamping() {
        val maxRoles = selectedPlayerCount / 2 + 1

        val (undercover, impostor, _) = getRoleCounts()

        val correctedUndercover = undercover.coerceAtMost(maxRoles)
        val correctedImpostor = (maxRoles - correctedUndercover).coerceAtMost(impostor)

        undercoverCountText.text = correctedUndercover.toString()
        impostorCountText.text = correctedImpostor.toString()

        updateCivilianCount()
        updateRoleButtonsVisibility()
    }

    private fun setupListeners() {
        playerCountSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedPlayerCount = progress + 3
                playerCountLabel.text = "$selectedPlayerCount Spieler"

                updateRoleCountsAndClamping()
                addNameInputs()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnStartGame.setOnClickListener { startGame() }
        btnNextPlayer.setOnClickListener { nextPlayer() }
        btnConfirmGuess.setOnClickListener { confirmImpostorGuess() }
        btnContinueVoting.setOnClickListener { startVoting() }
        btnRestartGame.setOnClickListener { restartGame() }

        btnUndercoverPlus.setOnClickListener { changeRoleCount(undercoverCountText, +1) }
        btnUndercoverMinus.setOnClickListener { changeRoleCount(undercoverCountText, -1) }
        btnImpostorPlus.setOnClickListener { changeRoleCount(impostorCountText, +1) }
        btnImpostorMinus.setOnClickListener { changeRoleCount(impostorCountText, -1) }

        groupContainer.setOnClickListener { removeGroupView() }
        btnSelectGroup.setOnClickListener {
            val intent = Intent(requireContext(), GroupActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SELECT_GROUP)
        }

        btnSettings.setOnClickListener {
            showSettingsDialog()
        }

    }

    private fun showNameEditDialog(initialName: String, onNameConfirmed: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_name, null)
        val input = dialogView.findViewById<EditText>(R.id.editNameInput).apply {
            setText(initialName)
            setSelection(initialName.length)
            requestFocus()
        }


        val dialog = AlertDialog.Builder(requireContext(), R.style.PixelDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                onNameConfirmed(name)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Bitte einen gültigen Namen eingeben.", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }

        dialog.show()
    }


    private fun createPlayerSlot(name: String, onEdit: (String) -> Unit): View {
        val playerSlot = layoutInflater.inflate(R.layout.item_player_slot, nameInputsContainer, false)

        playerSlot.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(8, 8, 8, 8)
        }

        val nameText = playerSlot.findViewById<TextView>(R.id.playerNameText)
        val editBtn = playerSlot.findViewById<ImageButton>(R.id.editBtn)

        nameText.text = name
        nameText.setOnClickListener {
            editBtn.visibility = if (editBtn.isVisible) View.GONE else View.VISIBLE
        }

        editBtn.setOnClickListener {
            showNameEditDialog(nameText.text.toString()) { newName ->
                nameText.text = newName
                onEdit(newName)
            }
        }

        return playerSlot
    }



    private fun addNameInputs() {
        nameInputsContainer.removeAllViews()

        for (i in 0 until selectedPlayerCount) {
            val name = "Spieler ${i + 1}"
            val playerSlot = createPlayerSlot(name) {}
            nameInputsContainer.addView(playerSlot)
        }

        btnStartGame.visibility = View.VISIBLE
    }





    private fun startGame() {
        val playerNames = nameInputsContainer.children
            .mapNotNull { it.findViewById<TextView>(R.id.playerNameText)?.text?.toString()?.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        Log.d("PlayerNames",playerNames.toString())


        val undercoverCount = undercoverCountText.text.toString().toIntOrNull() ?: 0
        val impostorCount = impostorCountText.text.toString().toIntOrNull() ?: 0

        if (!gameLogic.setupGame(playerNames, undercoverCount, impostorCount)) {
            Toast.makeText(requireContext(), "Zu viele Undercover/Impostor für diese Spieleranzahl.", Toast.LENGTH_SHORT).show()
            return
        }

        setGamePhase(GamePhase.PLAYING)

        currentPlayerIndex = 0
        updateCurrentPlayer()
        updateCivilianCount()
        updateRoleButtonsVisibility()

    }

    private fun changeRoleCount(textView: TextView, delta: Int) {
        val current = textView.text.toString().toIntOrNull() ?: 0
        val newCount = (current + delta).coerceAtLeast(0)

        if (textView == undercoverCountText) undercoverCountText.text = newCount.toString()
        if (textView == impostorCountText) impostorCountText.text = newCount.toString()

        val (undercover, impostor, _) = getRoleCounts()
        val maxAllowed = selectedPlayerCount / 2 + 1

        if (undercover + impostor > maxAllowed) {
            Toast.makeText(requireContext(), "Maximal $maxAllowed Sonderrollen erlaubt.", Toast.LENGTH_SHORT).show()
            return
        }

        textView.text = newCount.toString()
        updateCivilianCount()
        updateRoleButtonsVisibility()
    }

    private fun updateCurrentPlayer() {
        if (currentPlayerIndex < gameLogic.players.size) {
            val player = gameLogic.players[currentPlayerIndex]
            currentPlayerText.text = "Gerät an: \n${player.name}"
            //wordDisplayLayout.visibility = View.GONE
        } else {
            startVoting()
        }
    }

    private fun showWord() {
        val word = gameLogic.getWordForPlayer(currentPlayerIndex)
        wordText.text = word ?: "Fehler: Kein Wort gefunden"
        //wordDisplayLayout.visibility = View.VISIBLE
    }

    private fun nextPlayer() {
        if (flippedOnce == 1) {
            flippedOnce = 0
            currentPlayerIndex++

            cardBack.visibility = View.GONE
            cardFront.visibility = View.VISIBLE
            cardFront.rotationY = 0f
            cardBack.rotationY = 0f

            updateCurrentPlayer()
        } else {
            Toast.makeText(requireContext(),"Schaue dir zuerst das Wort an ;)", Toast.LENGTH_LONG).show()
        }
    }
    private fun showGameStartDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_game_start, null)
        val messageView = dialogView.findViewById<TextView>(R.id.dialogMessage)

        val activePlayers = gameLogic.players.filter { !it.isEjected }
        if (activePlayers.isEmpty()) return

        val starter = activePlayers.random().name
        val direction = if ((0..1).random() == 0) "Uhrzeigersinn" else "Gegen den Uhrzeigersinn"
        messageView.text = "$starter beginnt.\n\nDie Runge geht in Richtung: \n$direction"

        val dialog = AlertDialog.Builder(requireContext(), R.style.PixelDialog)
            .setView(dialogView)
            .setCancelable(false)
            .show()

        dialogView.findViewById<Button>(R.id.okButton)?.setOnClickListener {
            dialog.dismiss()
        }
    }


    private fun startVoting() {
        if (gameLogic.gameEnded) return

        setGamePhase(GamePhase.VOTING)

        votingButtonsContainer.removeAllViews()

        showGameStartDialog()



        gameLogic.players.forEachIndexed { index, player ->
            if (!player.isEjected) {
                Button(requireContext()).apply {
                    text = player.name
                    setOnClickListener { resolveVote(index) }
                }.also { votingButtonsContainer.addView(it) }
            }
        }
    }

    private fun resolveVote(index: Int) {
        setGamePhase(GamePhase.RESULT)

        val role = gameLogic.players[index].role
        gameLogic.ejectPlayer(index)

        val activePlayers = gameLogic.players.filter { !it.isEjected }

        val playerName = gameLogic.players[index].name
        voteResultText.text = "$playerName wurde rausgeworfen.\n\nRolle: $role"
        val ejectedPlayers = players.count { !it.isEjected }


        if (role == Role.IMPOSTOR) {
            impostorGuessLayout.visibility = View.VISIBLE
            btnContinueVoting.visibility = View.GONE
            btnConfirmGuess.visibility = View.VISIBLE
        } else if (role == Role.JESTER && ejectedPlayers == 0) {
            voteResultText.append("\n\nDer Jester wurde erfolgreich rausgeworfen! Er gewinnt alleine.")
            btnContinueVoting.visibility = View.GONE
            btnRestartGame.visibility = View.VISIBLE
            return
        } else {
            impostorGuessLayout.visibility = View.GONE
            btnContinueVoting.visibility = View.VISIBLE
        }

        if (gameLogic.isGameOver() || activePlayers.isEmpty()) {
            voteResultText.append("\n\nDas Spiel ist beendet")
            btnContinueVoting.visibility = View.GONE
            btnRestartGame.visibility = View.VISIBLE
        }
    }

    private fun confirmImpostorGuess() {
        val guess = impostorGuessInput.text.toString()
        val correct = gameLogic.checkImpostorGuessAndEndGame(guess)

        voteResultText.text = if (correct) {
            "Impostor hat richtig geraten! Impostor gewinnen."

        } else {
            "Falsches Wort, u suck lol"
        }

        impostorGuessInput.text?.clear()

        impostorGuessLayout.visibility = View.GONE
        btnConfirmGuess.visibility = View.GONE
        if (!correct && !gameLogic.isGameOver()) btnContinueVoting.visibility = View.VISIBLE else btnRestartGame.visibility = View.VISIBLE
    }

    private fun restartGame() {
        gameLogic.resetGame()
        btnRestartGame.visibility = View.GONE
        setGamePhase(GamePhase.SETUP)
        impostorGuessInput.setText("")
    }

    private fun removeGroupView() {
        groupContainer.removeAllViews()
        playerCountLabel.visibility = View.VISIBLE
        playerCountSlider.visibility = View.VISIBLE

        // Reset selectedPlayerCount from nameInputs
        selectedPlayerCount = nameInputsContainer.childCount.takeIf { it > 0 } ?: 3
        playerCountSlider.progress = selectedPlayerCount - 3

        updateRoleCountsAndClamping()
        addNameInputs()
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)

        val chanceAll = dialogView.findViewById<EditText>(R.id.editChanceAllImpostors)
        val chanceNone = dialogView.findViewById<EditText>(R.id.editChanceNoImpostors)
        val chanceJester = dialogView.findViewById<EditText>(R.id.editChanceJester)
        val importBtn = dialogView.findViewById<Button>(R.id.btnImportWords)

        val prefs = requireContext().getSharedPreferences("impostata_prefs", Context.MODE_PRIVATE)
        chanceAll.setText(prefs.getInt("chance_all_impostor", 1).toString())
        chanceNone.setText(prefs.getInt("chance_no_impostor", 1).toString())
        chanceJester.setText(prefs.getInt("chance_jester", 1).toString())

        val dialog = AlertDialog.Builder(requireContext(), R.style.PixelDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnAdd).setOnClickListener {
            prefs.edit {
                putInt("chance_all_impostor", chanceAll.text.toString().toIntOrNull() ?: 0)
                putInt("chance_no_impostor", chanceNone.text.toString().toIntOrNull() ?: 0)
                putInt("chance_jester", chanceJester.text.toString().toIntOrNull() ?: 0)            }

            GameLogic.chanceNoImpostor = prefs.getInt("chance_no_impostor", 1)
            GameLogic.chanceAllImpostor = prefs.getInt("chance_all_impostor", 1)
            GameLogic.chanceJester = prefs.getInt("chance_jester", 1)

            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        importBtn.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "text/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, IMPORT_WORDS_REQUEST_CODE)        }

        dialog.show()
    }


    private fun InitFlipAnimation(view: View) {
        val scale = requireContext().resources.displayMetrics.density
        cardFront.cameraDistance = 8000 * scale
        cardBack.cameraDistance = 8000 * scale

        val flipOut = ObjectAnimator.ofFloat(cardFront, "rotationY", 0f, 90f)
        val flipIn = ObjectAnimator.ofFloat(cardBack, "rotationY", -90f, 0f)
        flipOut.duration = 300
        flipIn.duration = 300

        val flipBackOut = ObjectAnimator.ofFloat(cardBack, "rotationY", 0f, 90f)
        val flipBackIn = ObjectAnimator.ofFloat(cardFront, "rotationY", -90f, 0f)
        flipBackOut.duration = 300
        flipBackIn.duration = 300


        cardFront.setOnClickListener {
            flippedOnce = 1
            val word = GameLogic.getWordForPlayer(currentPlayerIndex)
            wordText.text = word ?: "Fehler: Kein Wort"

            flipOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    cardFront.visibility = View.GONE
                    cardBack.visibility = View.VISIBLE
                    flipIn.start()
                }
            })

            flipOut.start()
        }

        cardBack.setOnClickListener {
            flipBackOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    cardBack.visibility = View.GONE
                    cardFront.visibility = View.VISIBLE
                    flipBackIn.start()
                }
            })

            flipBackOut.start()
        }

    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMPORT_WORDS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.forEachLine { line ->
                    val parts = line.split(",")
                    if (parts.size == 2) {
                        GameLogic.addWordPair(parts[0].trim(), parts[1].trim())
                    }
                }
                Toast.makeText(requireContext(), "Wörter erfolgreich importiert.", Toast.LENGTH_SHORT).show()
            }
        }else if (requestCode == REQUEST_CODE_SELECT_GROUP && resultCode == Activity.RESULT_OK) {
            val groupName = data?.getStringExtra("selectedGroupName")
            val playerNames = data?.getStringArrayListExtra("selectedGroupPlayers") ?: arrayListOf()

            Log.d("TEST", "Group Name: $groupName, Player Names: $playerNames")

            selectedPlayerCount = playerNames.size

            groupContainer.removeAllViews()
            val groupView = LayoutInflater.from(requireContext()).inflate(R.layout.item_group, groupContainer, false)
            groupView.findViewById<TextView>(R.id.groupNameText).text = groupName
            groupView.findViewById<TextView>(R.id.playerListText).text = playerNames.joinToString(", ")
            groupView.findViewById<Button>(R.id.editGroupBtn).visibility = View.GONE
            groupContainer.addView(groupView)

            playerCountLabel.visibility = View.GONE
            playerCountSlider.visibility = View.GONE

            playerCountLabel.text = "$selectedPlayerCount Spieler"
            nameInputsContainer.removeAllViews()

            playerNames.forEach { name ->
                val playerSlot = createPlayerSlot(name) {}
                nameInputsContainer.addView(playerSlot)
            }

            updateRoleCountsAndClamping()
            btnStartGame.visibility = View.VISIBLE
        }

    }
    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If not in setup phase, reset game
                if (setupLayout.visibility != View.VISIBLE) {
                    restartGame()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })
    }

}
