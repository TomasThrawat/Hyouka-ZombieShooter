package com.hyouka.zombieshooter

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private var game: GameView? = null
    private var menu: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        root = FrameLayout(this)
        setContentView(root)
        showMainMenu()
    }

    private fun showMainMenu() {
        game?.onPause()
        game = null
        root.removeAllViews()

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(80, 30, 80, 30)
            setBackgroundColor(Color.rgb(8, 10, 14))
        }

        val title = TextView(this).apply {
            text = "HYOUKA\nZOMBIE SURVIVOR"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }
        panel.addView(title, LinearLayout.LayoutParams(-1, -2))

        panel.addView(menuButton("START GAME") { startGame() })
        panel.addView(menuButton("SETTINGS") {
            showMessage("SETTINGS", "Graphics: OpenGL ES 2\nTouch controls enabled")
        })
        panel.addView(menuButton("CHARACTER") {
            showMessage("CHARACTER", "Default survivor")
        })

        root.addView(panel, FrameLayout.LayoutParams(-1, -1))
        menu = panel
    }

    private fun startGame() {
        root.removeAllViews()
        val gameView = GameView(this)
        game = gameView
        root.addView(gameView, FrameLayout.LayoutParams(-1, -1))

        val controls = TextView(this).apply {
            text = "◉ MOVE                         LOOK ◉\n\n                         ☰"
            textSize = 18f
            setTextColor(Color.argb(180, 255, 255, 255))
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, 35)
            isClickable = false
        }
        root.addView(controls, FrameLayout.LayoutParams(-1, -1))

        gameView.onMenuRequested = { showPauseMenu() }
        gameView.onResume()
    }

    private fun showPauseMenu() {
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(70, 40, 70, 40)
            setBackgroundColor(Color.argb(235, 8, 10, 14))
        }

        val title = TextView(this).apply {
            text = "MENU"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 35)
        }
        overlay.addView(title)
        overlay.addView(menuButton("RESUME") { root.removeView(overlay) })
        overlay.addView(menuButton("MAIN MENU") { showMainMenu() })

        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
    }

    private fun showMessage(titleText: String, body: String) {
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(70, 40, 70, 40)
            setBackgroundColor(Color.rgb(8, 10, 14))
        }
        overlay.addView(TextView(this).apply {
            text = titleText
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        overlay.addView(TextView(this).apply {
            text = body
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 35)
        })
        overlay.addView(menuButton("BACK") { showMainMenu() })
        root.removeAllViews()
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
    }

    private fun menuButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 17f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(35, 40, 48))
            setOnClickListener { action() }
            val lp = LinearLayout.LayoutParams(-1, 58)
            lp.setMargins(0, 10, 0, 10)
            layoutParams = lp
        }

    override fun onResume() {
        super.onResume()
        game?.onResume()
    }

    override fun onPause() {
        game?.onPause()
        super.onPause()
    }
}
