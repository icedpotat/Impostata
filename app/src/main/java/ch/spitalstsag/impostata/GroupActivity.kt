package ch.spitalstsag.impostata


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ch.spitalstsag.impostata.model.Group
import androidx.core.graphics.toColorInt
import ch.spitalstsag.impostata.model.Player
import com.google.gson.Gson
import androidx.core.content.edit
import androidx.core.view.children

class GroupActivity : AppCompatActivity() {

    private var groups = mutableListOf<Group>() // Ideally load from local storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group)

        groups = loadGroups(this)
        displayGroups(findViewById(R.id.groupListContainer))

        val container = findViewById<LinearLayout>(R.id.groupListContainer)
        displayGroups(container)

        val addButton = findViewById<Button>(R.id.addGroupButton)
        addButton.setOnClickListener { showAddGroupDialog() }

    }

    private fun displayGroups(container: LinearLayout) {
        container.removeAllViews()
        for ((index, group) in groups.withIndex()) {
            val view = layoutInflater.inflate(R.layout.item_group, container, false)
            view.findViewById<TextView>(R.id.groupNameText).text = group.name
            view.findViewById<TextView>(R.id.playerListText).text = group.players.joinToString(", ") { it.name }
            view.findViewById<LinearLayout>(R.id.groupCard).setBackgroundColor(group.colorHex.toColorInt())

            // Edit button listener
            view.findViewById<Button>(R.id.editGroupBtn).setOnClickListener {
                showEditGroupDialog(index)
            }

            // Group card click - select group and return
            view.findViewById<LinearLayout>(R.id.groupCard).setOnClickListener {
                selectGroup(group)
            }

            container.addView(view)
        }
    }


    private fun showAddGroupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_group, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editGroupName)
        val addPlayerButton = dialogView.findViewById<Button>(R.id.btnAddPlayer)
        val playerContainer = dialogView.findViewById<LinearLayout>(R.id.playerInputsContainer)

        // Add first 3 input fields by default
        repeat(3) { addPlayerInput(playerContainer) }

        addPlayerButton.setOnClickListener {
            addPlayerInput(playerContainer)
        }

        AlertDialog.Builder(this)
            .setTitle("Gruppe hinzufügen")
            .setView(dialogView)
            .setPositiveButton("Hinzufügen") { _, _ ->
                val groupName = nameInput.text.toString().trim()

                val playerNames = playerContainer.children
                    .filterIsInstance<EditText>()
                    .map { it.text.toString().trim() }
                    .filter { it.isNotEmpty() }

                if (groupName.isNotEmpty() && !playerNames.none()) {
                    val newGroup = Group(
                        name = groupName,
                        colorHex = getRandomColorHex(),
                        players = playerNames.map { Player(it) }.toMutableList()
                    )
                    groups.add(newGroup)
                    saveGroups(this, groups)
                    displayGroups(findViewById(R.id.groupListContainer))
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun addPlayerInput(container: LinearLayout) {
        val input = EditText(this).apply {
            hint = "Spielername"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setTextColor(getColor(R.color.pixel_text))
            setHintTextColor(getColor(android.R.color.darker_gray))
            backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.pixel_text))
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
        }
        container.addView(input)
    }

    private fun getRandomColorHex(): String {
        val colors = listOf("#F44336", "#2196F3", "#4CAF50", "#FF9800", "#9C27B0", "#00BCD4", "#FF5722")
        return colors.random()
    }

    fun saveGroups(context: Context, groups: List<Group>) {
        val prefs = context.getSharedPreferences("impostata_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(groups)
        prefs.edit { putString("groups_json", json) }
    }

    fun loadGroups(context: Context): MutableList<Group> {
        val prefs = context.getSharedPreferences("impostata_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("groups_json", null) ?: return mutableListOf()
        val gson = Gson()
        val type = com.google.gson.reflect.TypeToken.getParameterized(MutableList::class.java, Group::class.java).type
        return gson.fromJson(json, type)
    }


    private fun showEditGroupDialog(groupIndex: Int) {
        val group = groups[groupIndex]

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_group, null)
        val groupNameInput = dialogView.findViewById<EditText>(R.id.groupNameInput)
        val playersInput = dialogView.findViewById<EditText>(R.id.playersInput) // Comma separated player names

        groupNameInput.setText(group.name)
        playersInput.setText(group.players.joinToString(", ") { it.name })

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Edit Group")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                // Save changes
                val newName = groupNameInput.text.toString()
                val newPlayers = playersInput.text.toString()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { Player(it) }
                    .toMutableList()

                groups[groupIndex] = Group(newName,group.colorHex, newPlayers)
                saveGroups(this, groups)
                displayGroups(findViewById(R.id.groupListContainer))
            }
            .setNegativeButton("Delete") { _, _ ->
                // Delete group
                groups.removeAt(groupIndex)
                saveGroups(this, groups)
                displayGroups(findViewById(R.id.groupListContainer))
            }
            .setNeutralButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun selectGroup(group: Group) {
        // Pass selected group back to caller activity or wherever needed
        val intent = Intent()
        intent.putExtra("selectedGroupName", group.name)
        intent.putExtra("selectedGroupPlayers", ArrayList(group.players.map { it.name }))
        setResult(RESULT_OK, intent)
        finish()
    }


}
