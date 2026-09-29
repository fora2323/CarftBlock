package org.aurora.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton

import com.aurora.R

class AuroraDialog(context: Context) : Dialog(context, R.style.Base_Theme_AuroraDialog) {

    private lateinit var titleView: TextView
    private lateinit var messageView: TextView
    private lateinit var iconView: ImageView
    private lateinit var buttonContainer: LinearLayout

    private val buttons = mutableListOf<ButtonConfig>()
    
    private var pendingTitle: String? = null
    private var pendingMessage: String? = null
    private var pendingIcon: Int? = null

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

        pendingTitle?.let { titleView.text = it }
        pendingMessage?.let { messageView.text = it }
        pendingIcon?.let { iconView.setImageResource(it) }

        renderButtons()
    }

    fun setTitle(title: String): AuroraDialog {
        if (::titleView.isInitialized) {
            titleView.text = title
        } else {
            pendingTitle = title
        }
        return this
    }

    fun setMessage(message: String): AuroraDialog {
        if (::messageView.isInitialized) {
            messageView.text = message
        } else {
            pendingMessage = message
        }
        return this
    }

    fun setIcon(resId: Int): AuroraDialog {
        if (::iconView.isInitialized) {
            iconView.setImageResource(resId)
        } else {
            pendingIcon = resId
        }
        return this
    }

    fun addButton(text: String, color: Int = context.getColor(R.color.primary), onClick: (AuroraDialog) -> Unit): AuroraDialog {
        buttons.add(ButtonConfig(text, color, onClick))
        return this
    }

    private fun renderButtons() {
        buttonContainer.removeAllViews()

        val density = context.resources.displayMetrics.density
        val largeRadius = 24f * density
        val smallRadius = 4f * density
        val onPrimaryColor = context.getColor(R.color.onPrimary)

        buttons.forEachIndexed { index, config ->
            val button = AppCompatButton(context).apply {
                text = config.text
                isAllCaps = false
                setTextColor(onPrimaryColor)
                textSize = 14f

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (40 * density).toInt()
                )
                if (index > 0) {
                    params.topMargin = (4 * density).toInt()
                }
                layoutParams = params

                val shape = GradientDrawable()
                shape.setColor(config.color)

                when {
                    buttons.size == 1 -> {
                        shape.cornerRadius = largeRadius
                    }
                    index == 0 -> {
                        shape.cornerRadii = floatArrayOf(largeRadius, largeRadius, largeRadius, largeRadius, smallRadius, smallRadius, smallRadius, smallRadius)
                    }
                    index == buttons.size - 1 -> {
                        shape.cornerRadii = floatArrayOf(smallRadius, smallRadius, smallRadius, smallRadius, largeRadius, largeRadius, largeRadius, largeRadius)
                    }
                    else -> {
                        shape.cornerRadius = smallRadius
                    }
                }
                
                val ripple = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null)
                background = ripple
                
                setOnClickListener {
                    config.onClick(this@AuroraDialog)
                }
            }
            buttonContainer.addView(button)
        }
    }

    class Builder(val context: Context) {
        private var title: String = ""
        private var message: String = ""
        private var icon: Int = R.drawable.ic_notifications
        private val buttons = mutableListOf<ButtonConfig>()

        fun setTitle(title: String) = apply {
            this.title = title
        }

        fun setMessage(message: String) = apply {
            this.message = message
        }

        fun setIcon(resId: Int) = apply {
            this.icon = resId
        }

        fun setPositiveButton(text: String, onClick: (AuroraDialog) -> Unit) = apply {
            buttons.add(ButtonConfig(text, context.getColor(R.color.primary), onClick))
        }

        fun setNegativeButton(text: String, onClick: (AuroraDialog) -> Unit) = apply {
            buttons.add(ButtonConfig(text, context.getColor(R.color.primary), onClick))
        }

        fun addButton(text: String, color: Int, onClick: (AuroraDialog) -> Unit) = apply {
            buttons.add(ButtonConfig(text, color, onClick))
        }

        fun show(): AuroraDialog {
            val dialog = AuroraDialog(context)
            dialog.setTitle(title)
            dialog.setMessage(message)
            dialog.setIcon(icon)
            this.buttons.forEach {
                dialog.addButton(it.text, it.color, it.onClick)
            }
            dialog.show()
            return dialog
        }

        fun build(): AuroraDialog {
            val dialog = AuroraDialog(context)
            dialog.setTitle(title)
            dialog.setMessage(message)
            dialog.setIcon(icon)
            this.buttons.forEach {
                dialog.addButton(it.text, it.color, it.onClick)
            }
            return dialog
        }
    }
}