package org.aurora.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton

class AuroraDialog(context: Context) : Dialog(context, R.style.Base_Theme_AuroraDialog) {

    private lateinit var titleView: TextView
    private lateinit var messageView: TextView
    private lateinit var iconView: ImageView
    private lateinit var buttonContainer: LinearLayout

    private val buttons = mutableListOf<ButtonConfig>()

    data class ButtonConfig(val text: String, val color: Int, val onClick: (AuroraDialog) -> Unit)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        setContentView(R.layout.main_dialog)

        titleView = findViewById(R.id.dialog_title)
        messageView = findViewById(R.id.dialog_message)
        iconView = findViewById(R.id.dialog_icon)
        buttonContainer = findViewById(R.id.button_container)

        renderButtons()
    }

    fun setTitle(title: String): AuroraDialog {
        titleView.text = title
        return this
    }

    fun setMessage(message: String): AuroraDialog {
        messageView.text = message
        return this
    }

    fun setIcon(resId: Int): AuroraDialog {
        iconView.setImageResource(resId)
        return this
    }

    fun addButton(text: String, color: Int = Color.parseColor("#0F7B8E"), onClick: (AuroraDialog) -> Unit): AuroraDialog {
        buttons.add(ButtonConfig(text, color, onClick))
        return this
    }

    private fun renderButtons() {
        buttonContainer.removeAllViews()

        val density = context.resources.displayMetrics.density
        val largeRadius = 24f * density // Radius melengkung penuh
        val smallRadius = 4f * density  // Radius kecil untuk sisi yang bersentuhan

        buttons.forEachIndexed { index, config ->
            val button = AppCompatButton(context).apply {
                text = config.text
                isAllCaps = false
                setTextColor(Color.WHITE)
                textSize = 14f

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (48 * density).toInt()
                )
                if (index > 0) {
                    params.topMargin = (2 * density).toInt()
                }
                layoutParams = params

                // Logika Radius Dinamis
                val shape = GradientDrawable()
                shape.setColor(config.color)

                if (buttons.size == 1) {
                    shape.cornerRadius = largeRadius
                } else if (buttons.size >= 2) {
                    if (index == 0) {
                        shape.cornerRadii = floatArrayOf(largeRadius, largeRadius, largeRadius, largeRadius, smallRadius, smallRadius, smallRadius, smallRadius)
                    } else if (index == buttons.size - 1) {
                        shape.cornerRadii = floatArrayOf(smallRadius, smallRadius, smallRadius, smallRadius, largeRadius, largeRadius, largeRadius, largeRadius)
                    }
                }
                background = shape
                setOnClickListener {
                    config.onClick(this@AuroraDialog)
                }
            }
            buttonContainer.addView(button)
        }
    }
}