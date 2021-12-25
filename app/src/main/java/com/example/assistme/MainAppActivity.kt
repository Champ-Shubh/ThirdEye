package com.example.assistme

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.assistme.fragment.ExploreFragment
import com.example.assistme.fragment.FaceFragment
import com.example.assistme.fragment.FindFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class MainAppActivity : AppCompatActivity() {

    lateinit var bottomNavigationView: BottomNavigationView
    lateinit var frameLayout: FrameLayout
    lateinit var exploreFragment: ExploreFragment
    lateinit var faceFragment: FaceFragment
    lateinit var findFragment: FindFragment
    private lateinit var textToSpeech: TextToSpeech
    private val VOICE_SWITCH_REQUEST_CODE = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_app)

        frameLayout = findViewById(R.id.frame_layout)
        bottomNavigationView = findViewById(R.id.bottom_navigation_view)

        exploreFragment = ExploreFragment()
        faceFragment = FaceFragment()
        findFragment = FindFragment()

        textToSpeech = TextToSpeech(this){
            if(it != TextToSpeech.ERROR)
                textToSpeech.language = Locale.getDefault()
        }

        //Explore fragment will be opened by default
        openExploreFragment()

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.item_explore -> {
                    //Open Explore fragment
                    openExploreFragment()
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.item_face -> {
                    //Open Face fragment
                    openFaceFragment()
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.item_find -> {
                    //Open Find fragment
                    openFindFragment()
                    return@setOnNavigationItemSelectedListener true
                }
                else -> return@setOnNavigationItemSelectedListener false
            }
        }


    }

    private fun openFindFragment() {
        supportActionBar?.title = "Find Objects"
        bottomNavigationView.menu.findItem(R.id.item_find).isChecked = true
        supportFragmentManager.beginTransaction().replace(R.id.frame_layout, findFragment).commit()
    }

    private fun openFaceFragment() {
        supportActionBar?.title = "Face Recognition"
        bottomNavigationView.menu.findItem(R.id.item_face).isChecked = true
        supportFragmentManager.beginTransaction().replace(R.id.frame_layout, faceFragment).commit()
    }

    private fun openExploreFragment() {
        supportActionBar?.title = "Explore"
        bottomNavigationView.menu.findItem(R.id.item_explore).isChecked = true
        supportFragmentManager.beginTransaction().replace(R.id.frame_layout, exploreFragment).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_mainscreen, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.item_switch -> switchScreenOnVoice()

            R.id.item_camera -> {
                val sharedPreferences = applicationContext.getSharedPreferences("ThirdEyeSharedPref", MODE_PRIVATE)
                val currValOfFrontCam = sharedPreferences.getBoolean("isFrontCamSelected", false)

                if(currValOfFrontCam){
                    sharedPreferences.edit().putBoolean("isFrontCamSelected", false).apply()
                }
                else{
                    sharedPreferences.edit().putBoolean("isFrontCamSelected", true).apply()
                }

                //Notify user to refresh so that new setting is visible
                Toast.makeText(this, "Refresh screen to apply changes", Toast.LENGTH_SHORT).show()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun switchScreenOnVoice(){
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.ENGLISH
        )
        startActivityForResult(intent, VOICE_SWITCH_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == VOICE_SWITCH_REQUEST_CODE && resultCode == RESULT_OK){
            val inputCommand = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

            if (inputCommand != null) {
                if(textToSpeech.isSpeaking)
                    textToSpeech.stop()

                when {
                    inputCommand.contains("face search") || inputCommand.contains("face") -> {
                        textToSpeech.speak("Face Search Screen", TextToSpeech.QUEUE_FLUSH, null, "SWITCH_TO_FACE")
                        textToSpeech.playSilentUtterance(200, TextToSpeech.QUEUE_ADD, null)
                        openFaceFragment()
                    }
                    inputCommand.contains("item search") || inputCommand.contains("find") || inputCommand.contains("item")-> {
                        textToSpeech.speak("Item Find Screen", TextToSpeech.QUEUE_FLUSH, null, "SWITCH_TO_FIND")
                        textToSpeech.playSilentUtterance(200, TextToSpeech.QUEUE_ADD, null)
                        openFindFragment()
                    }
                    inputCommand.contains("explore") || inputCommand.contains("home") -> {
                        textToSpeech.speak("Explore Screen", TextToSpeech.QUEUE_FLUSH, null, "SWITCH_TO_EXPLORE")
                        openExploreFragment()
                    }
                    else -> {
                        textToSpeech.playSilentUtterance(200, TextToSpeech.QUEUE_ADD, null)
                    }
                }
            }
        }
        else
            Log.e("MainAppActivity", "Error in Voice Recognition")
    }

    override fun onBackPressed() {
        showAlertDialog()
    }

    private fun showAlertDialog(){
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Confirm Exit")
        builder.setMessage("Are you sure you want to exit?")

        builder.setPositiveButton("Yes") { dialogInterface: DialogInterface, i: Int ->
            finish()
        }
            .setNegativeButton("No") { dialogInterface: DialogInterface, i: Int ->
                //Do nothing
            }

        builder.create()
        builder.show()
    }

    override fun onStop() {
        super.onStop()
        textToSpeech.shutdown()
    }
}