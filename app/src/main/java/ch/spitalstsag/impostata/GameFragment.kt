package ch.spitalstsag.impostata

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
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
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.appcompat.app.AlertDialog


class GameFragment : Fragment() {
    private enum class GamePhase { SETUP, PLAYING, VOTING, RESULT }
    private enum class Direction { CLOCKWISE, COUNTERCLOCKWISE }

    private lateinit var gameLogic: GameLogic

    private lateinit var setupLayout: ViewGroup
    private lateinit var gameLayout: ViewGroup
    private lateinit var votingLayout: ViewGroup
    private lateinit var resultLayout: ViewGroup

    private lateinit var nameInputsContainer: GridLayout
    private lateinit var currentPlayerText: TextView
    private lateinit var btnShowWord: Button
    private lateinit var wordDisplayLayout: ViewGroup
    private lateinit var wordText: TextView
    private lateinit var btnNextPlayer: Button
    private lateinit var votingButtonsContainer: LinearLayout
    private lateinit var voteResultText: TextView
    private lateinit var ImpostorGuessLayout: ViewGroup
    private lateinit var ImpostorGuessInput: EditText
    private lateinit var btnConfirmGuess: Button
    private lateinit var btnContinueVoting: Button
    private lateinit var btnRestartGame: Button
    private lateinit var playerCountLabel: TextView
    private lateinit var playerCountSlider: SeekBar
    private lateinit var importButton: Button
    private lateinit var btnUndercoverPlus: Button
    private lateinit var btnUndercoverMinus: Button
    private lateinit var btnImpostorPlus: Button
    private lateinit var btnImpostorMinus: Button
    private lateinit var civilianCountText: TextView
    private lateinit var groupContainer: LinearLayout
    private lateinit var btnStartGame: Button
    private lateinit var btnSelectGroup: Button
    private lateinit var undercoverCountText: TextView
    private lateinit var ImpostorCountText: TextView

    private var selectedPlayerCount = 3
    private var currentPlayerIndex = 0

    private val IMPORT_WORDS_REQUEST_CODE = 1001
    private val REQUEST_CODE_SELECT_GROUP = 2001

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)
        initViews(view)
        setupListeners()
        addNameInputs()
        updateCivilianCount()
        updateRoleButtonsVisibility()

        view.setOnTouchListener { _, _ ->
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
            false
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rootView = view.findViewById<View>(R.id.rootLayout)
        val scrollContainer = view.findViewById<LinearLayout>(R.id.nameInputsLinearContainer)

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = rootView.rootView.height - rootView.height
            val isKeyboardOpen = heightDiff > rootView.rootView.height * 0.15

            val existingSpacer = scrollContainer.children.find { it.tag == "keyboardSpacer" }

            if (isKeyboardOpen) {
                Log.d("Spacer","Activated")
                if (existingSpacer == null) {
                    val spacer = View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            350
                        )
                        tag = "keyboardSpacer"
                    }
                    scrollContainer.addView(spacer)

                }
            } else {
                existingSpacer?.let { scrollContainer.removeView(it) }
            }
            val focusedView = view.findFocus()
            if (isKeyboardOpen && focusedView is EditText) {
                val scrollView = view.findViewById<ScrollView>(R.id.nameInputsScrollView)
                scrollView?.post {
                    scrollView.smoothScrollTo(0, focusedView.top - 16)
                }
            }

        }
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
        ImpostorCountText = view.findViewById(R.id.ImpostorCountText)

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
        groupContainer = view.findViewById(R.id.selectedGroupContainer)
        btnSelectGroup = view.findViewById(R.id.btnSelectGroup)
        importButton = view.findViewById(R.id.importWordsButton)
    }

    private fun setGamePhase(phase: GamePhase) {
        setupLayout.visibility = if (phase == GamePhase.SETUP) View.VISIBLE else View.GONE
        gameLayout.visibility = if (phase == GamePhase.PLAYING) View.VISIBLE else View.GONE
        votingLayout.visibility = if (phase == GamePhase.VOTING) View.VISIBLE else View.GONE
        resultLayout.visibility = if (phase == GamePhase.RESULT) View.VISIBLE else View.GONE
    }

    private fun getRoleCounts(): Triple<Int, Int, Int> {
        val undercover = undercoverCountText.text.toString().toIntOrNull() ?: 0
        val impostor = ImpostorCountText.text.toString().toIntOrNull() ?: 0
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

    private fun setupListeners() {
        playerCountSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedPlayerCount = progress + 3
                playerCountLabel.text = "$selectedPlayerCount Spieler"

                val maxRoles = selectedPlayerCount / 2 + 1
                val undercover = undercoverCountText.text.toString().toIntOrNull() ?: 0
                val impostor = ImpostorCountText.text.toString().toIntOrNull() ?: 0

                val correctedUndercover = undercover.coerceAtMost(maxRoles)
                val correctedImpostor = (maxRoles - correctedUndercover).coerceAtMost(impostor)

                undercoverCountText.text = correctedUndercover.toString()
                ImpostorCountText.text = correctedImpostor.toString()

                addNameInputs()
                updateCivilianCount()
                updateRoleButtonsVisibility()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        importButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "text/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, IMPORT_WORDS_REQUEST_CODE)
        }

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

        groupContainer.setOnClickListener { removeGroupView() }
        btnSelectGroup.setOnClickListener {
            val intent = Intent(requireContext(), GroupActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SELECT_GROUP)
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
            onNameConfirmed(input.text.toString())
            dialog.dismiss()
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





    private fun addNameInputs() {
        nameInputsContainer.removeAllViews()

        for (i in 0 until selectedPlayerCount) {
            val playerSlot = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
            }

            val nameText = TextView(requireContext()).apply {
                id = View.generateViewId() // or use a static ID if preferred
                tag = "playerNameText"
                text = "Spieler ${i + 1}"
                ellipsize = TextUtils.TruncateAt.END
                maxLines = 1
                isSingleLine = true
                textSize = 14f
                typeface = ResourcesCompat.getFont(context, R.font.pixel_font)
                setTextColor(ContextCompat.getColor(context, R.color.pixel_text))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val editBtn = Button(requireContext(), null, 0, R.style.PixelButton).apply {
                text = "Edit️"
                typeface = ResourcesCompat.getFont(context, R.font.pixel_font)
                setTextColor(ContextCompat.getColor(context, R.color.pixel_text))
                textSize = 12f
                minWidth = 0
                minHeight = 0
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(150, 55).apply {
                    marginStart = 4
                }
                visibility = View.GONE
            }


            nameText.setOnClickListener {
                editBtn.visibility = if (editBtn.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }

            editBtn.setOnClickListener {
                val currentName = nameText.text.toString()

                showNameEditDialog(currentName) { newName ->
                    nameText.text = newName
                }
            }


            playerSlot.addView(nameText)
            playerSlot.addView(editBtn)
            nameInputsContainer.addView(playerSlot)
        }

        btnStartGame.visibility = View.VISIBLE
    }





    private fun startGame() {
        val playerNames = nameInputsContainer.children
            .mapNotNull { row ->
                row.findViewWithTag<TextView>("playerNameText")?.text?.toString()?.trim()
            }
            .filter { it.isNotEmpty() }
            .toList()

        Log.d("PlayerNames",playerNames.toString())


        val undercoverCount = undercoverCountText.text.toString().toIntOrNull() ?: 0
        val impostorCount = ImpostorCountText.text.toString().toIntOrNull() ?: 0

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
        if (textView == ImpostorCountText) ImpostorCountText.text = newCount.toString()

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
            currentPlayerText.text = "Gerät an: ${player.name}"
            wordDisplayLayout.visibility = View.GONE
        } else {
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

        setGamePhase(GamePhase.VOTING)

        votingButtonsContainer.removeAllViews()

        val activePlayers = gameLogic.players.filter { !it.isEjected }
        val starter = activePlayers.random().name
        val direction = if ((0..1).random() == 0) "↪ (Uhrzeigersinn)" else "↩ (Gegen den Uhrzeigersinn)"
        Toast.makeText(requireContext(), "$starter beginnt. Richtung: $direction", Toast.LENGTH_LONG).show()


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

        if (role == Role.IMPOSTOR) {
            ImpostorGuessLayout.visibility = View.VISIBLE
            btnContinueVoting.visibility = View.GONE
            btnConfirmGuess.visibility = View.VISIBLE
        } else {
            ImpostorGuessLayout.visibility = View.GONE
            btnContinueVoting.visibility = View.VISIBLE
        }

        if (gameLogic.isGameOver() || activePlayers.isEmpty()) {
            voteResultText.append("\n\nDas Spiel ist beendet")
            btnContinueVoting.visibility = View.GONE
            btnRestartGame.visibility = View.VISIBLE
        }
    }

    private fun confirmImpostorGuess() {
        val guess = ImpostorGuessInput.text.toString()
        val correct = gameLogic.checkImpostorGuessAndEndGame(guess)

        voteResultText.text = if (correct) {
            "Impostor hat richtig geraten! Impostor gewinnen."

        } else {
            "Falsches Wort, u suck lol"
        }

        ImpostorGuessInput.text?.clear()

        ImpostorGuessLayout.visibility = View.GONE
        btnConfirmGuess.visibility = View.GONE
        if (!correct && !gameLogic.isGameOver()) btnContinueVoting.visibility = View.VISIBLE else btnRestartGame.visibility = View.VISIBLE
    }

    private fun restartGame() {
        gameLogic.resetGame()
        btnRestartGame.visibility = View.GONE
        setGamePhase(GamePhase.SETUP)
        ImpostorGuessInput.setText("")
    }

    private fun removeGroupView() {
        groupContainer.removeAllViews()
        playerCountLabel.visibility = View.VISIBLE
        playerCountSlider.visibility = View.VISIBLE

        // Reset selectedPlayerCount from nameInputs
        selectedPlayerCount = nameInputsContainer.childCount.takeIf { it > 0 } ?: 3
        playerCountSlider.progress = selectedPlayerCount - 3

        // Clamp roles
        val maxRoles = selectedPlayerCount / 2 + 1
        val undercover = undercoverCountText.text.toString().toIntOrNull() ?: 0
        val impostor = ImpostorCountText.text.toString().toIntOrNull() ?: 0

        val correctedUndercover = undercover.coerceAtMost(maxRoles)
        val correctedImpostor = (maxRoles - correctedUndercover).coerceAtMost(impostor)

        undercoverCountText.text = correctedUndercover.toString()
        ImpostorCountText.text = correctedImpostor.toString()

        addNameInputs()
        updateCivilianCount()
        updateRoleButtonsVisibility()
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

            playerNames.forEachIndexed { i, name ->
                val playerSlot = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setMargins(8, 8, 8, 8)
                    }
                }

                val nameText = TextView(requireContext()).apply {
                    id = View.generateViewId() // or use a static ID if preferred
                    tag = "playerNameText"
                    text = name
                    ellipsize = TextUtils.TruncateAt.END
                    maxLines = 1
                    isSingleLine = true
                    textSize = 14f
                    typeface = ResourcesCompat.getFont(context, R.font.pixel_font)
                    setTextColor(ContextCompat.getColor(context, R.color.pixel_text))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val editBtn = Button(requireContext(), null, 0, R.style.PixelButton).apply {
                    text = "Edit️"
                    typeface = ResourcesCompat.getFont(context, R.font.pixel_font)
                    setTextColor(ContextCompat.getColor(context, R.color.pixel_text))
                    textSize = 12f
                    minWidth = 0
                    minHeight = 0
                    setPadding(0, 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(150, 55).apply {
                        marginStart = 4
                    }
                    visibility = View.GONE
                }


                nameText.setOnClickListener {
                    editBtn.visibility = if (editBtn.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }

                editBtn.setOnClickListener {
                    val currentName = nameText.text.toString()

                    showNameEditDialog(currentName) { newName ->
                        nameText.text = newName
                    }
                }


                playerSlot.addView(nameText)
                playerSlot.addView(editBtn)
                nameInputsContainer.addView(playerSlot)
            }

            val maxRoles = selectedPlayerCount / 2 + 1
            val undercover = undercoverCountText.text.toString().toIntOrNull() ?: 0
            val impostor = ImpostorCountText.text.toString().toIntOrNull() ?: 0

            val correctedUndercover = undercover.coerceAtMost(maxRoles)
            val correctedImpostor = (maxRoles - correctedUndercover).coerceAtMost(impostor)

            undercoverCountText.text = correctedUndercover.toString()
            ImpostorCountText.text = correctedImpostor.toString()

            updateCivilianCount()
            updateRoleButtonsVisibility()
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
