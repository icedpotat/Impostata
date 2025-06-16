package  com.freeze.impostata
import android.animation.ObjectAnimator
import android.view.MotionEvent
import android.view.View

object RoleRevealAnimator {

    /**
     * Slider reveal: hold to slide, release to hide. Calls performClick for accessibility.
     */

    private val pixelStep = android.view.animation.PathInterpolator(0f, 0f, 1f, 1f)

    fun setSliderReveal(cover: View, onClicked: (() -> Unit)? = null) {
        cover.setOnTouchListener { v, event ->
            val slideUp = ObjectAnimator.ofFloat(cover, "translationY", -cover.height.toFloat()).apply {
                duration = 300
                interpolator = pixelStep
            }
            val slideDown = ObjectAnimator.ofFloat(cover, "translationY", 0f).apply {
                duration = 300
                interpolator = pixelStep
            }


            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    slideUp.start()
                }
                MotionEvent.ACTION_UP -> {
                    slideDown.start()
                    v.performClick() // ✅ Accessibility
                    onClicked?.invoke() // ✅ Triggers optional click action
                }
                MotionEvent.ACTION_CANCEL -> slideDown.start()
            }
            true
        }
    }



}
