package pt.isec.andreiagraca.amov_tp.activities

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import pt.isec.andreiagraca.amov_tp.R
import pt.isec.andreiagraca.amov_tp.databinding.ActivityEscolhemodoBinding
import pt.isec.andreiagraca.amov_tp.databinding.ActivityPerfilBinding
import pt.isec.ans.rascunhos.utils.ImageUtils
import pt.isec.ans.rascunhos.utils.ImageUtils.imagePath
import java.io.File


class PerfilActivity : AppCompatActivity() {
    lateinit var b: ActivityPerfilBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val google_web_id="563919512149-u4qn8qeluafphlq4qblqmqsq0u77cntf.apps.googleusercontent.com"

    lateinit var main: MainActivity

    companion object {
        private const val REQ_PERM_CODE = 1234
    }

    var imagePath : String? = null

    private var permissionsGranted = false

    var startActivityForResultFoto = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode != RESULT_OK){
            imagePath = null;
        }
        updatePreview()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = Firebase.auth

        b.imagem.apply {
            text = "Selecionar Imagem"
            setOnClickListener{
                val imageFile = File.createTempFile("img", ".img",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES))
                imagePath = imageFile.absolutePath
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    val fileUri = FileProvider.getUriForFile(
                        this@PerfilActivity,
                        "pt.isec.andreiagraca.amov_tp.android.fileprovider",
                        imageFile)
                    putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                }
                startActivityForResultFoto.launch(intent);
            }
        }

        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(google_web_id)
                .requestEmail()
                .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)

        val signInWithGoogle = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(ContentValues.TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.i(ContentValues.TAG, "onActivityResult - Google authentication: failure")
            }
        }

        if(ImageUtils.imagePath==null){
            Log.i("Image","Image is null")
        }
        else{
            Log.i("Image","Image not null")
            imagePath=ImageUtils.imagePath
        }

        if(ImageUtils.nomeJogador!=null)
        b.nome.hint=ImageUtils.nomeJogador

        setImage()

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE), REQ_PERM_CODE)
        } else
            permissionsGranted = true
    }

    override fun onStart() {
        super.onStart()
        showUser(auth.currentUser)
    }

    override fun onPause() {
        super.onPause()
        Firebase.messaging.unsubscribeFromTopic("weather")
    }

    override fun onResume() {
        super.onResume()
        Firebase.messaging.subscribeToTopic("weather")
            .addOnCompleteListener(this) {
                if (it.isSuccessful)
                    Log.i(ContentValues.TAG, "Weather topic subscribed")
            }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERM_CODE) {
            permissionsGranted =
                (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            b.imagem.isEnabled = permissionsGranted
        }
    }

    fun updatePreview(){

        if(imagePath == null){
            b.profileImage.background = ResourcesCompat.getDrawable(resources,
                R.drawable.avatar, null)
            Log.i("Update Preview","Image Path null")
        }
        else{
            ImageUtils.setPic(b.profileImage, imagePath!!)
            Log.i("Update Preview","Image Path not null: "+imagePath)
        }
    }

    fun setImage(){
        if(imagePath == null){
            b.profileImage.background = ResourcesCompat.getDrawable(resources,
                R.drawable.avatar, null)
            Log.i("Set image","Image Path null")
        }
        else{
            ImageUtils.setBitmap(b.profileImage)
            Log.i("Set image","Image Path not null")
        }
    }

    fun onGuardar(view:android.view.View){
        ImageUtils.imagePath=imagePath
        ImageUtils.nomeJogador=b.nome.text.toString()

        finish()
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener(this) { result ->
                Log.d(ContentValues.TAG, "signInWithCredential:success")
                showUser(auth.currentUser)
            }
            .addOnFailureListener(this) { e ->
                Log.d(ContentValues.TAG, "signInWithCredential:failure ${e.message}")
                showUser(auth.currentUser)
            }
    }

    fun showUser(user : FirebaseUser?) {
        val str = when (user) {
            null -> "No authenticated user"
            else -> "User: ${user.email}"
        }
        b.validationState?.text = str
        Log.i(ContentValues.TAG,str)
    }

    fun createUserWithEmail(email:String,password:String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(this) { result ->
                Log.i(ContentValues.TAG, "createUser: success")
                showUser(auth.currentUser)
            }
            .addOnFailureListener(this) { e ->
                Log.i(ContentValues.TAG, "createUser: failure ${e.message}")
                showUser(null)
            }
    }

    fun signInWithEmail(email:String,password:String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(this) { result ->
                Log.d(ContentValues.TAG, "signInWithEmail: success")
                showUser(auth.currentUser)
            }
            .addOnFailureListener(this) { e->
                Log.d(ContentValues.TAG, "signInWithEmail: failure ${e.message}")
                showUser(null)
            }
    }

    fun onRegistarEmail(view: View) {
        createUserWithEmail(b.edEmail!!.text.toString(),b.edPassword.toString())
    }

    fun onAutenticarEmail(view: View) {
        signInWithEmail(b.edEmail!!.text.toString(),b.edPassword.toString())
    }

    fun onLogoutEmail(view: View) {
        signOut()
    }

    fun signOut() {
        if (auth.currentUser != null) {
            auth.signOut()
        }
        showUser(auth.currentUser)
    }
}