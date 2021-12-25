package com.example.assistme

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import java.util.*
import java.util.jar.Manifest

class WelcomeActivity : AppCompatActivity() {

    private val VOICE_COMMAND_REQUEST_CODE = 1
    private var textToSpeech: TextToSpeech? = null
    lateinit var animSpeak: LottieAnimationView
    lateinit var vibrator: Vibrator
    private var isMicPermissionGranted = false
    private var isCameraPermissionGranted = false
    private var permissionGranted = false
    private val WELCOME_COMMAND_ID = "10"
    private val REQUEST_PERMISSIONS_CODE = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        if (checkAndRequestPermissions()) {
            permissionGranted = true
        }
            animSpeak = findViewById(R.id.anim_speak)
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            val btnDebug = findViewById<Button>(R.id.btn_debug)
            btnDebug.setOnClickListener {
                val intent = Intent(this, MainAppActivity::class.java)
                startActivity(intent)
            }

            //Initialize TextToSpeech object
            textToSpeech = TextToSpeech(applicationContext) {
                if (it != TextToSpeech.ERROR) {
                    textToSpeech!!.language = Locale.getDefault()
                }

                if(permissionGranted)
                    speakCommand()
            }

            animSpeak.setOnClickListener {
                //Handle click on the mic animation
                if (!textToSpeech!!.isSpeaking)
                    textToSpeech!!.shutdown()

                startVoiceRecognitionActivity()
            }
        }

    private fun startVoiceRecognitionActivity() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.ENGLISH
        )
        startActivityForResult(intent, VOICE_COMMAND_REQUEST_CODE)
    }

    private fun speakCommand(){
        val welcomeCommand = getString(R.string.welcome_command)
        //Play silent for 0.5 sec before speaking out command
        textToSpeech!!.playSilentUtterance(300, TextToSpeech.QUEUE_ADD, null)

        //Speak out the command
        textToSpeech!!.speak(welcomeCommand, TextToSpeech.QUEUE_ADD, null, WELCOME_COMMAND_ID)

        //Start voice recognition once welcome statement is spoken
        val speechListener = object : UtteranceProgressListener(){
            override fun onStart(p0: String?) {
                //Do nothing
            }

            override fun onDone(p0: String?) {
                if(p0.equals(WELCOME_COMMAND_ID, ignoreCase = true)) {
                    //Create vibration haptic to inform user
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val vibrationEffect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                        vibrator.cancel()
                        vibrator.vibrate(vibrationEffect)
                    }

                    textToSpeech!!.shutdown()
                    startVoiceRecognitionActivity()
                }
            }

            override fun onError(p0: String?) {
                Toast.makeText(this@WelcomeActivity, "Some error occurred in TTS", Toast.LENGTH_SHORT).show()
            }

        }
        textToSpeech!!.setOnUtteranceProgressListener(speechListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == VOICE_COMMAND_REQUEST_CODE && resultCode == RESULT_OK){
            val listOfWords = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

            if( listOfWords!= null && listOfWords.contains("start")) {
                Toast.makeText(this, "Initializing", Toast.LENGTH_SHORT).show()
                textToSpeech!!.shutdown()

                val mainIntent = Intent(this@WelcomeActivity, MainAppActivity::class.java)
                startActivity(mainIntent)
            }
            else{
                Toast.makeText(this, "START not detected", Toast.LENGTH_LONG).show()
            }
            textToSpeech!!.shutdown()
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissions: MutableList<String> = ArrayList()

        //Check CAMERA permission
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            permissions.add(android.Manifest.permission.CAMERA)
        }
        else
            isCameraPermissionGranted = true

        //Check MICROPHONE permission
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            permissions.add(android.Manifest.permission.RECORD_AUDIO)
        }
        else
            isMicPermissionGranted = true

        //Close application if permission not granted
        if(!isMicPermissionGranted || !isCameraPermissionGranted) {
            Log.e("WelcomeActivity", "Mic: $isMicPermissionGranted, Camera: $isCameraPermissionGranted")
            requestPermissions(permissions.toTypedArray(), REQUEST_PERMISSIONS_CODE)
            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        var checkIfDenied = false

        when (requestCode) {
            REQUEST_PERMISSIONS_CODE -> {
                if(grantResults.isNotEmpty()){
                    for(x in grantResults)
                        if(x != PackageManager.PERMISSION_GRANTED)
                            checkIfDenied = true
                }
            }
        }

        if(checkIfDenied){
            val dialog = AlertDialog.Builder(this)
            dialog.setMessage("Mic and Camera permissions are necessary for the functioning of the app")

            dialog.setPositiveButton("Ok"){_,_ ->
                //Toast.makeText(this, "Go to app settings and grant permissions", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions()
            }
            dialog.setNegativeButton("Cancel"){_,_ ->
                //Permission denied
                ActivityCompat.finishAffinity(this)
            }

            dialog.create()
            dialog.show()
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onStop() {
        super.onStop()

        if(textToSpeech!=null)
            textToSpeech!!.shutdown()

        ActivityCompat.finishAffinity(this)
    }
}