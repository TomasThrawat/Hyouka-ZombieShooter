package com.hyouka.zombieshooter
import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
class MainActivity:Activity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);requestWindowFeature(Window.FEATURE_NO_TITLE);window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);setContentView(GameView(this))}
}