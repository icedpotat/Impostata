// RevealManager.kt
package com.freeze.impostata

import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.freeze.impostata.model.GamePhase
import com.freeze.impostata.model.RevealMode
import com.freeze.impostata.GameLogic
import com.freeze.impostata.model.Role

class RevealManager(
    private val rootView: View,
    private val revealMode: RevealMode,
    private val getCurrentPlayerIndex: () -> Int,
    private val onWordRevealed: () -> Unit
) {
    fun setup() {
        when (revealMode) {
            RevealMode.CLICK_TO_FLIP -> setupFlip()
            RevealMode.HOLD_TO_REVEAL -> setupHold()
            RevealMode.GRID_SELECTION -> Unit
        }
    }

    private fun setupFlip() {
        val cardFront = rootView.findViewById<View>(R.id.cardFront)
        val cardBack = rootView.findViewById<View>(R.id.cardBack)
        val wordText = rootView.findViewById<TextView>(R.id.wordTextClick)

        cardFront?.visibility = View.VISIBLE
        cardBack?.visibility = View.GONE
        wordText?.text = GameLogic.getWordForPlayer(getCurrentPlayerIndex())

        cardFront?.setOnClickListener {
            cardFront.visibility = View.GONE
            cardBack?.visibility = View.VISIBLE
            onWordRevealed()
        }

        cardBack?.setOnClickListener {
            cardBack.visibility = View.GONE
            cardFront.visibility = View.VISIBLE
        }
    }

    private fun setupHold() {
        val cardFront = rootView.findViewById<View>(R.id.cardTapText)
        val cardBack = rootView.findViewById<View>(R.id.cardBack)
        val wordText = rootView.findViewById<TextView>(R.id.wordTextClick)

        cardFront?.visibility = View.VISIBLE
        cardBack?.visibility = View.GONE

        cardFront?.apply {
            isClickable = true
            isFocusable = true
            visibility = View.VISIBLE
            bringToFront()
        }

        cardFront?.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.performClick()
                    wordText?.text = GameLogic.getWordForPlayer(getCurrentPlayerIndex())
                    cardFront.visibility = View.GONE
                    cardBack?.visibility = View.VISIBLE
                    onWordRevealed()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cardBack?.visibility = View.GONE
                    cardFront.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }
    }
}

