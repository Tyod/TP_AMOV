package pt.isec.andreiagraca.amov_tp.activities

import android.app.AlertDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import pt.isec.andreiagraca.amov_tp.R
import pt.isec.andreiagraca.amov_tp.databinding.ActivityModo3Binding
import pt.isec.ans.rascunhos.utils.ImageUtils
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.random.Random


class Modo3Activity : AppCompatActivity() {
    lateinit var b: ActivityModo3Binding
    private val servidor: ServidorModo3Model by viewModels()

    val pecaJog0 = "\uD83D\uDD35" //Blue
    val pecaJog1 = "\uD83D\uDD34" //Red
    val pecaJog2 = "\uD83D\uDFE2" //Green
    var pecaJogAtual : String = ""
    var minhaPeca:String=""
    var pecaAdv1:String?=null
    var pecaAdv2:String?=null

    private var dlg: AlertDialog? = null

    val state = MutableLiveData(Modo2Activity.State.STARTING)
    val connectionState = MutableLiveData(Modo2Activity.ConnectionState.SETTING_PARAMETERS)

    var tabuleiro = Array(10) {Array(10) {""} }
    var tabPosicoesFinais = Array(10) { Array(2) {0} }

    var CentCim : Boolean = true     //Cima
    var CentCimDir : Boolean = true  //Cima à direita
    var CentDir : Boolean = true     //Direita
    var CentBaiDir : Boolean = true  //Direita em baixo
    var CentBai : Boolean = true     //Baixo
    var CentBaiEsq : Boolean = true  //Baixo à esquerda
    var CentEsq : Boolean = true     //Esquerda
    var CentCimaEsq : Boolean = true //Cima à esquerda

    var bombaSelec:Boolean=false
    var troca:Boolean=false

    var jog0bomba:Boolean=false
    var jog1bomba:Boolean=false
    var jog2bomba:Boolean=false
    var jogBomba:Boolean=false

    var jog0troca:Boolean=false
    var jog1troca:Boolean=false
    var jog2troca:Boolean=false
    var jogTroca:Boolean=false

    var pecasSelecionadas:Int=0
    var pecas=Array(3){Array(2){0} }
    var btn: Button? =null
    var selectedBtns=Array(3){btn}
    var nAjudas:Int=0
    var ajudaBtns=Array(64){btn}
    var ajudas=Array(64){Array(2){0} }

    var str:Int=0
    var jogoAcabou=false

    var myMove:Boolean=false


    private var socketS: Socket? = null
    private val socketIS: InputStream?
        get() = socketS?.getInputStream()
    private val socketOS: OutputStream?
        get() = socketS?.getOutputStream()


    private var threadCommS: Thread? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b= ActivityModo3Binding.inflate(layoutInflater)
        setContentView(b.root)

        connectionState.observe(this) { state ->
            Log.i("CONNECTION OBSERVABLE",connectionState.value.toString())
            if ( state != Modo2Activity.ConnectionState.SETTING_PARAMETERS &&
                state != Modo2Activity.ConnectionState.SERVER_CONNECTING && dlg?.isShowing == true && servidor.clientes==3) {
                dlg?.dismiss()
                dlg = null
                servidor.preparaJogo()
            }

            if (state == Modo2Activity.ConnectionState.CONNECTION_ERROR) {
                finish()
            }
            if (state == Modo2Activity.ConnectionState.CONNECTION_ENDED)
                finish()
        }

            if (connectionState.value != Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED) {
                when (intent.getIntExtra("mode", SERVER_MODE)) {
                    SERVER_MODE -> startAsServer()
                    CLIENT_MODE -> startAsClient()
                }
            }
    /*    sorteiaJogador()
        acionaListeners()
        preparaTabuleiro()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        b.numPecasJog0.text = str.toString()
        b.numPecasJog1.text = str.toString()
        b.numPecasJog2.text=str.toString()*/
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.opcoes,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.ajudaJogada->{
                Toast.makeText(this,R.string.ajuda_selecionada,Toast.LENGTH_SHORT).show()
                if(nAjudas==0) {
                    enviaJogada(socketOS, AJUDA_JOGADA,0,0)
                }
                else
                    tiraAjuda()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun startAsServer() {
        servidor.b=b
        servidor.cliente=this

        val wifiManager = applicationContext.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress // Deprecated in API Level 31. Suggestion NetworkCallback
        val strIPAddress = String.format("%d.%d.%d.%d",
            ip and 0xff,
            (ip shr 8) and 0xff,
            (ip shr 16) and 0xff,
            (ip shr 24) and 0xff
        )

        val ll = LinearLayout(this).apply {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            this.setPadding(50, 50, 50, 50)
            layoutParams = params
            setBackgroundColor(Color.rgb(240, 224, 208))
            orientation = LinearLayout.HORIZONTAL
            addView(ProgressBar(context).apply {
                isIndeterminate = true
                val paramsPB = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                paramsPB.gravity = Gravity.CENTER_VERTICAL
                layoutParams = paramsPB
                indeterminateTintList = ColorStateList.valueOf(Color.rgb(96, 96, 32))
            })
            addView(TextView(context).apply {
                val paramsTV = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                layoutParams = paramsTV
                text = String.format(getString(R.string.msg_ip_address),strIPAddress)
                textSize = 20f
                setTextColor(Color.rgb(96, 96, 32))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            })
        }

        dlg = AlertDialog.Builder(this).run {
            setTitle(getString(R.string.server_mode))
            setView(ll)
            setOnCancelListener {
                servidor.stopServer()
                finish()
            }
            create()
        }
        thread {
            servidor.startServer()
        }
        dlg?.show()

        Thread.sleep(500)
        startClient("10.0.2.2", SERVER_PORT-1)
    }

    private fun startAsClient() {
        val edtBox = EditText(this).apply {
            maxLines = 1
            filters = arrayOf(object : InputFilter {
                override fun filter(
                    source: CharSequence?,
                    start: Int,
                    end: Int,
                    dest: Spanned?,
                    dstart: Int,
                    dend: Int
                ): CharSequence? {
                    source?.run {
                        var ret = ""
                        forEach {
                            if (it.isDigit() || it.equals('.'))
                                ret += it
                        }
                        return ret
                    }
                    return null
                }

            })
        }
        val dlg = AlertDialog.Builder(this).run {
            setTitle(getString(R.string.client_mode))
            setMessage(getString(R.string.ask_ip))
            setPositiveButton(getString(R.string.button_connect)) { _: DialogInterface, _: Int ->
                val strIP = edtBox.text.toString()
                if (strIP.isEmpty() || !Patterns.IP_ADDRESS.matcher(strIP).matches()) {
                    Toast.makeText(this@Modo3Activity, getString(R.string.error_address), Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    startClient(edtBox.text.toString())
                }
            }
            setNeutralButton(getString(R.string.btn_emulator)) { _: DialogInterface, _: Int ->
                startClient("10.0.2.2", SERVER_PORT-1)
                // Configure port redirect on the Server Emulator:
                // telnet localhost <5554|5556|5558|...>
                // auth <key>
                // redir add tcp:9998:9999
            }
            setNegativeButton(getString(R.string.button_cancel)) { _: DialogInterface, _: Int ->
                finish()
            }
            setCancelable(false)
            setView(edtBox)
            create()
        }
        dlg.show()
    }

    fun startClient(serverIP: String,serverPort: Int = SERVER_PORT) {

        if (connectionState.value != Modo2Activity.ConnectionState.SETTING_PARAMETERS)
            return

        connectionState.postValue(Modo2Activity.ConnectionState.CLIENT_CONNECTING)
        Log.i("START CLIENT",connectionState.value.toString())

        thread {
            //connectionState.postValue(ConnectionState.CLIENT_CONNECTING)
            try {
                //val newsocket = Socket(serverIP, serverPort)
                val newsocket = Socket()
                newsocket.connect(InetSocketAddress(serverIP,serverPort),5000)
                connectionState.postValue(Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED)

                runOnUiThread(Runnable {
                    preparaNovoJogo()
                    acionaListeners()
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                })

                atendeServidor(newsocket)

                Log.i("TAG","Connection succeed")
            } catch (e : Exception) {
                Log.i("Exception",""+e)
                connectionState.postValue(Modo2Activity.ConnectionState.CONNECTION_ERROR)
               // stopGame()
            }
        }
    }

    fun onCriarDados() {
        val db = Firebase.firestore
        val dados = hashMapOf(
            "Jogador1" to 0,
            "Jogador2" to 0
        )
        db.collection("Modo2").document("Modo").set(dados)

        db.collection("Modo2").get().addOnSuccessListener(this) { query ->
            for (doc in query.documents)
                Log.i(ContentValues.TAG, "onCriarDados: ${doc.id}")
        }
    }

    private fun decodeToBitmap(image:String):Bitmap{
        val imageBytes = Base64.decode(image, 0)
        return  BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun atendeServidor(newSocket: Socket) {
        if (threadCommS != null)
            return

        socketS = newSocket

        threadCommS = thread {
            try {
                if (socketIS == null)
                    return@thread

                connectionState.postValue(Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED)
                val bufI = socketIS!!.bufferedReader()
                Log.i("TAG","Atende Servidor")


                while (state.value != Modo2Activity.State.GAME_OVER) {
                    Log.i("TAG","Cliente em espera")
                    var op:Int=0
                    var pecaAtual:String
                    var message:String

                    //var myObj=bufI.read
                    message = bufI.readLine()
                    var obj= Json.decodeFromString<Jogada>(message)

                    op=obj.op

                    Log.i("TAG","Mensagem recebida: "+op)

                    when(op){
                        SORTEIA_JOGADOR -> {
                            pecaAtual=obj.pecaJogAtual!!
                            minhaPeca= obj.minhaPeca!!
                            myMove=obj.vez!!

                            runOnUiThread(Runnable {

                                Log.i("SORTEIA_JOGADOR",""+myMove+" | "+pecaAtual)

                                if (pecaAtual == pecaJog1) {
                                    vezJogador1()
                                } else {
                                    if(pecaAtual==pecaJog0){
                                        vezJogador0()
                                    }
                                    else
                                        vezJogador2()
                                }

                            })

                            thread {
                                enviaDados(socketOS)
                            }

                        }
                        DADOS->{
                            var nomeJogAdv1=obj.nome!!
                            var imagemJogAdv1=obj.imagem!!
                            var pecaAdv1=obj.minhaPeca!!
                            var nomeJogAdv2=obj.nome2!!
                            var imagemJogAdv2=obj.imagem2!!
                            var pecaAdv2=obj.peca2!!

                            var imageAdv1=decodeToBitmap(imagemJogAdv1)
                            var imageAdv2=decodeToBitmap(imagemJogAdv2)

                            runOnUiThread(Runnable {
                                if(pecaAdv1==pecaJog0){
                                    ImageUtils.setBitmap(b.profileImageJog0, imageAdv1)
                                    b.nomeJog0.text = nomeJogAdv1
                                }
                                else{
                                    if(pecaAdv1==pecaJog1) {
                                        ImageUtils.setBitmap(b.profileImageJog1, imageAdv1)
                                        b.nomeJog1.text = nomeJogAdv1
                                    }
                                    else{
                                        ImageUtils.setBitmap(b.profileImageJog2, imageAdv1)
                                        b.nomeJog2.text = nomeJogAdv1
                                    }
                                }

                                if(pecaAdv2==pecaJog0){
                                    ImageUtils.setBitmap(b.profileImageJog0, imageAdv2)
                                    b.nomeJog0.text = nomeJogAdv2
                                }
                                else{
                                    if(pecaAdv2==pecaJog1) {
                                        ImageUtils.setBitmap(b.profileImageJog1, imageAdv2)
                                        b.nomeJog1.text = nomeJogAdv2
                                    }
                                    else{
                                        ImageUtils.setBitmap(b.profileImageJog2, imageAdv2)
                                        b.nomeJog2.text = nomeJogAdv2
                                    }
                                }

                                    if((pecaAdv1==pecaJog0 || pecaAdv1==pecaJog1) && (pecaAdv2==pecaJog0 || pecaAdv2==pecaJog1)){
                                        ImageUtils.setBitmap(b.profileImageJog2)
                                        b.nomeJog2.text = ImageUtils.nomeJogador
                                    }
                                    else{
                                        if((pecaAdv1==pecaJog0 || pecaAdv1==pecaJog2) && (pecaAdv2==pecaJog0 || pecaAdv2==pecaJog2)){
                                            ImageUtils.setBitmap(b.profileImageJog1)
                                            b.nomeJog1.text = ImageUtils.nomeJogador
                                        }
                                        else{
                                                ImageUtils.setBitmap(b.profileImageJog0)
                                                b.nomeJog0.text = ImageUtils.nomeJogador
                                        }
                                    }

                            })
                        }
                        VEZ_JOGADOR ->{
                            runOnUiThread(Runnable {
                                pecaAtual = obj.pecaJogAtual!!
                                myMove = obj.vez!!

                                Log.i("VEZ_JOGADOR", "" + myMove + " | " + pecaAtual)

                                if (pecaAtual == pecaJog1) {
                                    vezJogador1()
                                } else {
                                    if(pecaAtual==pecaJog0){
                                        vezJogador0()
                                    }
                                    else
                                        vezJogador2()
                                }


                                bombaSelec = false
                                b.btnPassaVez.isEnabled = false

                                if(obj.troca!!)
                                    b.btnPassaVez.isEnabled = true
                            })
                        }

                        TABULEIRO ->{
                            tabuleiro = obj.tabuleiro!!
                            val minhaJogada = obj.jogadaEspecial!!
                            Log.i("TABULEIRO", "Mensagem recebida do Servidor")
                            runOnUiThread(Runnable {

                                limpaBtns()
                                tiraAjuda()
                                atualizaVista()
                                atualizaContador()
                            })
                                if(minhaJogada)
                                    jogBomba=true

                        }

                        TROCA_PECAS->{
                            Log.i("TROCA_PECAS","TROCA_PECAS")
                            tabuleiro=obj.tabuleiro!!
                            val minhaJogada=obj.jogadaEspecial!!

                            runOnUiThread(Runnable {
                                limpaBtns()
                                tiraAjuda()
                                atualizaVista()
                                atualizaContador()
                            })

                            if(minhaJogada)
                                jogTroca=true
                        }
                        AJUDA_JOGADA->{
                            nAjudas=0
                            ajudas=obj.ajudas!!
                            val n=obj.nAjudas!!

                            for(i in 0 until n){
                                botaoAjuda(ajudas[i][0],ajudas[i][1])
                                ajudaBtns[nAjudas]?.isActivated = true
                                nAjudas++
                            }
                        }

                        EMPATE->{
                            Log.i("TAG","Recebida mensagem que jogo terminou")
                            runOnUiThread(Runnable {
                                jogoTerminou(op)
                            })
                        }
                        EMPATE_VERMELHO_AZUL->{
                            Log.i("TAG","Recebida mensagem que jogo terminou")
                            runOnUiThread(Runnable {
                                jogoTerminou(op)
                            })
                        }
                        EMPATE_AZUL_VERDE->{
                            Log.i("TAG","Recebida mensagem que jogo terminou")
                            runOnUiThread(Runnable {
                                jogoTerminou(op)
                            })
                        }
                        EMPATE_VERMELHO_VERDE->{
                            Log.i("TAG","Recebida mensagem que jogo terminou")
                            runOnUiThread(Runnable {
                                jogoTerminou(op)
                            })
                        }
                        VENCEDOR_PECAS_34->{
                            Log.i("TAG","Recebida mensagem que jogo terminou")
                            runOnUiThread(Runnable {
                                jogoTerminou(op)
                            })
                        }
                        VENCEDOR_PECAS_35->{
                            Log.i("TAG","Recebida mensagem que jogo terminou")
                            runOnUiThread(Runnable {
                                jogoTerminou(op)
                            })
                        }
                        VENCEDOR_PECAS_36->{
                            Log.i("TAG","Recebida mensagem que jogo terminou")
                            runOnUiThread(Runnable {
                                jogoTerminou(op)
                            })
                        }
                    }

                }
            } catch (e : Exception) {
                Log.i("Exception: ","Exception atende servidor: "+e)
            } finally {
                Log.i("FINALLY","Atende Servidor")

                //  stopGame()
            }
        }
    }

    private fun tiraAjuda(){
        for(i in 0 until nAjudas){
            ajudaBtns[i]?.isActivated=false
        }

        nAjudas=0
    }



    private fun botaoAjuda(linha: Int,coluna:Int){
        if(linha==0 && coluna==0) { ajudaBtns[nAjudas]=b.btn00;return }
        if(linha==0 && coluna==1) { ajudaBtns[nAjudas]=b.btn01;return }
        if(linha==0 && coluna==2) { ajudaBtns[nAjudas]=b.btn02;return }
        if(linha==0 && coluna==3) { ajudaBtns[nAjudas]=b.btn03;return }
        if(linha==0 && coluna==4) { ajudaBtns[nAjudas]=b.btn04;return }
        if(linha==0 && coluna==5) { ajudaBtns[nAjudas]=b.btn05;return }
        if(linha==0 && coluna==6) { ajudaBtns[nAjudas]=b.btn06;return }
        if(linha==0 && coluna==7) { ajudaBtns[nAjudas]=b.btn07;return }
        if(linha==0 && coluna==8) { ajudaBtns[nAjudas]=b.btn08;return }
        if(linha==0 && coluna==9) { ajudaBtns[nAjudas]=b.btn09;return }

        if(linha==1 && coluna==0) {ajudaBtns[nAjudas]=b.btn10;  return }
        if(linha==1 && coluna==1) { ajudaBtns[nAjudas]=b.btn11; return }
        if(linha==1 && coluna==2) { ajudaBtns[nAjudas]=b.btn12; return }
        if(linha==1 && coluna==3) { ajudaBtns[nAjudas]=b.btn13; return }
        if(linha==1 && coluna==4) { ajudaBtns[nAjudas]=b.btn14; return }
        if(linha==1 && coluna==5) { ajudaBtns[nAjudas]=b.btn15; return }
        if(linha==1 && coluna==6) { ajudaBtns[nAjudas]=b.btn16; return }
        if(linha==1 && coluna==7) { ajudaBtns[nAjudas]=b.btn17; return }
        if(linha==1 && coluna==8) { ajudaBtns[nAjudas]=b.btn18; return }
        if(linha==1 && coluna==9) { ajudaBtns[nAjudas]=b.btn19; return }

        if(linha==2 && coluna==0) { ajudaBtns[nAjudas]=b.btn20; return }
        if(linha==2 && coluna==1) { ajudaBtns[nAjudas]=b.btn21; return }
        if(linha==2 && coluna==2) { ajudaBtns[nAjudas]=b.btn22; return }
        if(linha==2 && coluna==3) { ajudaBtns[nAjudas]=b.btn23; return }
        if(linha==2 && coluna==4) { ajudaBtns[nAjudas]=b.btn24; return }
        if(linha==2 && coluna==5) { ajudaBtns[nAjudas]=b.btn25; return }
        if(linha==2 && coluna==6) { ajudaBtns[nAjudas]=b.btn26; return }
        if(linha==2 && coluna==7) { ajudaBtns[nAjudas]=b.btn27; return }
        if(linha==2 && coluna==8) { ajudaBtns[nAjudas]=b.btn28; return }
        if(linha==2 && coluna==9) { ajudaBtns[nAjudas]=b.btn29; return }

        if(linha==3 && coluna==0) { ajudaBtns[nAjudas]=b.btn30; return }
        if(linha==3 && coluna==1) { ajudaBtns[nAjudas]=b.btn31; return }
        if(linha==3 && coluna==2) { ajudaBtns[nAjudas]=b.btn32; return }
        if(linha==3 && coluna==3) { ajudaBtns[nAjudas]=b.btn33; return }
        if(linha==3 && coluna==4) { ajudaBtns[nAjudas]=b.btn34; return }
        if(linha==3 && coluna==5) { ajudaBtns[nAjudas]=b.btn35; return }
        if(linha==3 && coluna==6) { ajudaBtns[nAjudas]=b.btn36; return }
        if(linha==3 && coluna==7) { ajudaBtns[nAjudas]=b.btn37; return }
        if(linha==3 && coluna==8) { ajudaBtns[nAjudas]=b.btn38; return }
        if(linha==3 && coluna==9) { ajudaBtns[nAjudas]=b.btn39; return }

        if(linha==4 && coluna==0) { ajudaBtns[nAjudas]=b.btn40; return }
        if(linha==4 && coluna==1) { ajudaBtns[nAjudas]=b.btn41; return }
        if(linha==4 && coluna==2) { ajudaBtns[nAjudas]=b.btn42; return }
        if(linha==4 && coluna==3) { ajudaBtns[nAjudas]=b.btn43; return }
        if(linha==4 && coluna==4) { ajudaBtns[nAjudas]=b.btn44; return }
        if(linha==4 && coluna==5) { ajudaBtns[nAjudas]=b.btn45; return }
        if(linha==4 && coluna==6) { ajudaBtns[nAjudas]=b.btn46; return }
        if(linha==4 && coluna==7) { ajudaBtns[nAjudas]=b.btn47; return }
        if(linha==4 && coluna==8) { ajudaBtns[nAjudas]=b.btn48;return }
        if(linha==4 && coluna==9) { ajudaBtns[nAjudas]=b.btn49;return }

        if(linha==5 && coluna==0) { ajudaBtns[nAjudas]=b.btn50; return }
        if(linha==5 && coluna==1) { ajudaBtns[nAjudas]=b.btn51; return }
        if(linha==5 && coluna==2) { ajudaBtns[nAjudas]=b.btn52; return }
        if(linha==5 && coluna==3) { ajudaBtns[nAjudas]=b.btn53; return }
        if(linha==5 && coluna==4) { ajudaBtns[nAjudas]=b.btn54; return }
        if(linha==5 && coluna==5) { ajudaBtns[nAjudas]=b.btn55; return }
        if(linha==5 && coluna==6) { ajudaBtns[nAjudas]=b.btn56; return }
        if(linha==5 && coluna==7) { ajudaBtns[nAjudas]=b.btn57; return }
        if(linha==5 && coluna==8) { ajudaBtns[nAjudas]=b.btn58;return }
        if(linha==5 && coluna==9) { ajudaBtns[nAjudas]=b.btn59;return }

        if(linha==6 && coluna==0) { ajudaBtns[nAjudas]=b.btn60; return }
        if(linha==6 && coluna==1) { ajudaBtns[nAjudas]=b.btn61; return }
        if(linha==6 && coluna==2) { ajudaBtns[nAjudas]=b.btn62; return }
        if(linha==6 && coluna==3) { ajudaBtns[nAjudas]=b.btn63; return }
        if(linha==6 && coluna==4) { ajudaBtns[nAjudas]=b.btn64; return }
        if(linha==6 && coluna==5) { ajudaBtns[nAjudas]=b.btn65; return }
        if(linha==6 && coluna==6) { ajudaBtns[nAjudas]=b.btn66; return }
        if(linha==6 && coluna==7) { ajudaBtns[nAjudas]=b.btn67; return }
        if(linha==6 && coluna==8) { ajudaBtns[nAjudas]=b.btn68;return }
        if(linha==6 && coluna==9) { ajudaBtns[nAjudas]=b.btn69;return }

        if(linha==7 && coluna==0) { ajudaBtns[nAjudas]=b.btn70; return }
        if(linha==7 && coluna==1) { ajudaBtns[nAjudas]=b.btn71; return }
        if(linha==7 && coluna==2) { ajudaBtns[nAjudas]=b.btn72; return }
        if(linha==7 && coluna==3) { ajudaBtns[nAjudas]=b.btn73; return }
        if(linha==7 && coluna==4) { ajudaBtns[nAjudas]=b.btn74; return }
        if(linha==7 && coluna==5) { ajudaBtns[nAjudas]=b.btn75; return }
        if(linha==7 && coluna==6) { ajudaBtns[nAjudas]=b.btn76; return }
        if(linha==7 && coluna==7) { ajudaBtns[nAjudas]=b.btn77; return }
        if(linha==7 && coluna==8) { ajudaBtns[nAjudas]=b.btn78;return }
        if(linha==7 && coluna==9) { ajudaBtns[nAjudas]=b.btn79;return }

        if(linha==8 && coluna==0) { ajudaBtns[nAjudas]=b.btn80; return }
        if(linha==8 && coluna==1) { ajudaBtns[nAjudas]=b.btn81; return }
        if(linha==8 && coluna==2) { ajudaBtns[nAjudas]=b.btn82; return }
        if(linha==8 && coluna==3) { ajudaBtns[nAjudas]=b.btn83; return }
        if(linha==8 && coluna==4) { ajudaBtns[nAjudas]=b.btn84; return }
        if(linha==8 && coluna==5) { ajudaBtns[nAjudas]=b.btn85; return }
        if(linha==8 && coluna==6) { ajudaBtns[nAjudas]=b.btn86; return }
        if(linha==8 && coluna==7) { ajudaBtns[nAjudas]=b.btn87; return }
        if(linha==8 && coluna==8) { ajudaBtns[nAjudas]=b.btn88;return }
        if(linha==8 && coluna==9) { ajudaBtns[nAjudas]=b.btn89;return }

        if(linha==9 && coluna==0) { ajudaBtns[nAjudas]=b.btn90; return }
        if(linha==9 && coluna==1) { ajudaBtns[nAjudas]=b.btn91; return }
        if(linha==9 && coluna==2) { ajudaBtns[nAjudas]=b.btn92; return }
        if(linha==9 && coluna==3) { ajudaBtns[nAjudas]=b.btn93; return }
        if(linha==9 && coluna==4) { ajudaBtns[nAjudas]=b.btn94; return }
        if(linha==9 && coluna==5) { ajudaBtns[nAjudas]=b.btn95; return }
        if(linha==9 && coluna==6) { ajudaBtns[nAjudas]=b.btn96; return }
        if(linha==9 && coluna==7) { ajudaBtns[nAjudas]=b.btn97; return }
        if(linha==9 && coluna==8) { ajudaBtns[nAjudas]=b.btn98;return }
        if(linha==9 && coluna==9) { ajudaBtns[nAjudas]=b.btn99;return }
    }

    private fun ajudaCantoSupEsq(linha:Int, coluna:Int,jogAdv1:String, jogAdv2:String){
        if(linha-2<0)
            return
        if(coluna-2<0)
            return


        if(tabuleiro[linha-1][coluna-1]==jogAdv1 || tabuleiro[linha-1][coluna-1]==jogAdv2) {
            var i=linha-2
            var j=coluna-2
            while(i>=0 && j>=0){
                if (tabuleiro[i][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][j]==""){
                    botaoAjuda(i,j)
                    ajudaBtns[nAjudas]?.isActivated = true
                    nAjudas++
                    break
                }
                i--
                j--
            }
        }
    }

    private fun ajudaEsq(linha:Int, coluna:Int,jogAdv1:String, jogAdv2:String){
        if(coluna-2<0)
            return

        if(tabuleiro[linha][coluna-1]==jogAdv1 || tabuleiro[linha][coluna-1]==jogAdv2) {
            for (j in (coluna - 2) downTo 0) {
                if (tabuleiro[linha][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[linha][j]==""){
                    botaoAjuda(linha,j)
                    ajudaBtns[nAjudas]?.isActivated = true
                    nAjudas++
                    return
                }
            }
        }
    }

    private fun ajudaInfEsq(linha:Int, coluna:Int,jogAdv1:String, jogAdv2:String){
        if(linha+2>9)
            return
        if(coluna-2<0)
            return


        if(tabuleiro[linha+1][coluna-1]==jogAdv1 || tabuleiro[linha+1][coluna-1]==jogAdv2) {
            var i=linha+2
            var j=coluna-2
            while(i<10 && j>=0){
                if (tabuleiro[i][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][j]==""){
                    botaoAjuda(i,j)
                    ajudaBtns[nAjudas]?.isActivated = true
                    nAjudas++
                    break
                }
                i++
                j--
            }
        }
    }

    private fun ajudaAbaixo(linha:Int, coluna:Int,jogAdv1:String, jogAdv2:String){
        if(linha+2>9)
            return

        if(tabuleiro[linha+1][coluna]==jogAdv1 || tabuleiro[linha+1][coluna]==jogAdv2) {
            for(i in (linha+2) until 10){
                if (tabuleiro[i][coluna] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][coluna]==""){
                    botaoAjuda(i,coluna)
                    ajudaBtns[nAjudas]?.isActivated = true
                    nAjudas++
                    return
                }
            }
        }
    }

    private fun ajudaInfDir(linha:Int, coluna:Int,jogAdv1:String, jogAdv2:String){
        if(linha+2>9)
            return
        if(coluna+2>9)
            return

        if(tabuleiro[linha+1][coluna+1]==jogAdv1 || tabuleiro[linha+1][coluna+1]==jogAdv2) {
            var i=linha+2
            var j=coluna+2
            while(i<10 && j<10){
                if (tabuleiro[i][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][j]==""){
                    botaoAjuda(i,j)
                    ajudaBtns[nAjudas]?.isActivated = true
                    nAjudas++
                    return
                }
                i++
                j++
            }
        }
    }

    private fun ajudaDir(linha:Int, coluna:Int,jogAdv1:String, jogAdv2:String){
        if(coluna+2>9)
            return

        if(tabuleiro[linha][coluna+1]==jogAdv1 || tabuleiro[linha][coluna+1]==jogAdv2) {
            for (j in (coluna + 2) until 10) {
                if (tabuleiro[linha][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[linha][j]==""){
                    botaoAjuda(linha,j)
                    ajudaBtns[nAjudas]?.isActivated = true
                    nAjudas++
                    return
                }
            }
        }
    }

    private fun ajudaSupDir(linha:Int, coluna:Int,jogAdv1:String, jogAdv2:String){
        if(linha-2<0)
            return
        if(coluna+2>9)
            return


        if(tabuleiro[linha-1][coluna+1]==jogAdv1 || tabuleiro[linha-1][coluna+1]==jogAdv2) {
            var i=linha-2
            var j=coluna+2
            while(i>=0 && j<10){
                if (tabuleiro[i][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][j]==""){
                    botaoAjuda(i,j)
                    ajudaBtns[nAjudas]?.isActivated = true
                    nAjudas++
                    break
                }
                i--
                j++
            }
        }
    }

    private fun ajudaAcima(linha:Int, coluna:Int,jogAdv1:String, jogAdv2:String){
        if(linha-2<0)
            return

        if(tabuleiro[linha-1][coluna]==jogAdv1 || tabuleiro[linha-1][coluna]==jogAdv2 ) {
            for(i in (linha-2) downTo  0){
                if (tabuleiro[i][coluna] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][coluna]==""){
                    botaoAjuda(i,coluna)
                    ajudaBtns[nAjudas]?.isActivated = true
                    nAjudas++
                    return
                }
            }
        }
    }

    private fun ajudaJogada(){
        var jogAdv1:String?=null
        var jogAdv2:String?=null

        if(pecaJogAtual==pecaJog0) {
            jogAdv1 = pecaJog1
            jogAdv2 = pecaJog2
        }
        else{
            if(pecaJogAtual==pecaJog1){
                jogAdv1=pecaJog0
                jogAdv2=pecaJog2
            }
            else{
                jogAdv1=pecaJog0
                jogAdv2=pecaJog1
            }
        }


        for(i in 0 until 10){
            for(j in 0 until 10){
                if(tabuleiro[i][j]==pecaJogAtual){
                    ajudaCantoSupEsq(i,j,jogAdv1,jogAdv2)
                    ajudaEsq(i,j,jogAdv1,jogAdv2)
                    ajudaInfEsq(i,j,jogAdv1,jogAdv2)
                    ajudaAbaixo(i,j,jogAdv1,jogAdv2)
                    ajudaInfDir(i,j,jogAdv1,jogAdv2)
                    ajudaDir(i,j,jogAdv1,jogAdv2)
                    ajudaSupDir(i,j,jogAdv1,jogAdv2)
                    ajudaAcima(i,j,jogAdv1,jogAdv2)
                }
            }
        }
    }


    private fun preparaTabuleiro() {

        //1º Grupo Superior
        tabuleiro[2][4] = pecaJog0;tabuleiro[2][5] = pecaJog1
        tabuleiro[3][4] = pecaJog1;tabuleiro[3][5] = pecaJog0

        //2º Grupo Inferior Esquerdo
        tabuleiro[6][2] = pecaJog2;tabuleiro[6][3] = pecaJog0
        tabuleiro[7][2] = pecaJog0;tabuleiro[7][3] = pecaJog2

        //3ª Grupo Inferior Direito
        tabuleiro[6][6] = pecaJog1;tabuleiro[6][7]=pecaJog2
        tabuleiro[7][6] = pecaJog2;tabuleiro[7][7]=pecaJog1

        atualizaVista()
    }

    private fun troca(){
        if(pecaJogAtual==pecaJog0){
            vezJogador1()
        }
        else {
            if (pecaJogAtual == pecaJog1) {
                vezJogador2()
            } else {
                vezJogador0()
            }
        }
    }


    private fun preparaNovoJogo() {
        for(i in 0 until 10){
            for(j in 0 until 10){
                tabuleiro[i][j]=""
            }
        }

        sorteiaJogador()
        preparaTabuleiro()
        //verificaVencedor()
        b.numPecasJog0.text = str.toString()
        b.numPecasJog1.text = str.toString()
        b.numPecasJog2.text = str.toString()

        b.btnTrocaPecas.isEnabled = true
        b.btnPecaBomba.isEnabled = true
        b.btnPassaVez.isEnabled = false

        bombaSelec=false
        troca=false
        jog0bomba=false
        jog1bomba=false
        jog0troca=false
        jog1troca=false
        pecasSelecionadas=0
        jogoAcabou=false
    }

    private fun vezJogador0(){
        pecaJogAtual = pecaJog0
        b.profileImageJog0.borderColor = Color.BLUE
        b.profileImageJog1.borderColor = Color.argb(255,253,253,150)
        b.profileImageJog2.borderColor = Color.argb(255,253,253,150)
    }

    private fun vezJogador1(){
        pecaJogAtual = pecaJog1
        b.profileImageJog1.borderColor = Color.RED
        b.profileImageJog0.borderColor = Color.argb(255,253,253,150)
        b.profileImageJog2.borderColor = Color.argb(255,253,253,150)
    }

    private fun vezJogador2(){
        pecaJogAtual = pecaJog2
        b.profileImageJog2.borderColor = Color.YELLOW
        b.profileImageJog0.borderColor = Color.argb(255,253,253,150)
        b.profileImageJog1.borderColor = Color.argb(255,253,253,150)
    }

    private fun sorteiaJogador() {
        var temp = Random.nextInt(0,3)
        if(temp==0) {
            vezJogador0()
        }
        if(temp==1){
            vezJogador1()
        }
        if(temp==2){
            vezJogador2()
        }
    }

    private fun selecionaBotao(btn: Button, linha : Int, coluna : Int){
        btn.isSelected = true
        pecas[pecasSelecionadas][0] = linha
        pecas[pecasSelecionadas][1] = coluna
        selectedBtns[pecasSelecionadas] = btn
        pecasSelecionadas++
    }


    private fun selecionaPeca(btn: Button, linha : Int, coluna : Int) {

        if (btn.isSelected == true) {
                btn.isSelected = !btn.isSelected
                pecasSelecionadas--
                return
        }

        if(myMove){
            if (pecasSelecionadas < 2) {
                if (jogTroca == false) {
                    if (btn.text == pecaJogAtual && !btn.isSelected) {
                        selecionaBotao(btn, linha, coluna)
                    }
                    else{
                        Toast.makeText(
                            applicationContext,
                            R.string.posicao_invalida,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        applicationContext,
                        R.string.limite_troca,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } else {
                if (pecaJogAtual == pecaJog0) {
                    if ((btn.text == pecaJog1 || btn.text == pecaJog2) && pecasSelecionadas == 2 && !btn.isSelected) {
                        selecionaBotao(btn, linha, coluna)
                    }
                } else {
                    if (pecaJogAtual == pecaJog1) {
                        if ((btn.text == pecaJog0 || btn.text == pecaJog2) && pecasSelecionadas == 2 && !btn.isSelected) {
                            selecionaBotao(btn, linha, coluna)
                        }
                    } else {
                        if ((btn.text == pecaJog0 || btn.text == pecaJog1) && pecasSelecionadas == 2 && !btn.isSelected) {
                            selecionaBotao(btn, linha, coluna)
                        }
                    }
                }
            }
        }

    }


    private fun limpaTabuleiroPosicoesFinais(){
        tabPosicoesFinais[0][0]=-1;tabPosicoesFinais[0][1]=-1;tabPosicoesFinais[1][0]=-1;tabPosicoesFinais[1][1]=-1;tabPosicoesFinais[2][0]=-1
        tabPosicoesFinais[2][1]=-1;tabPosicoesFinais[3][0]=-1;tabPosicoesFinais[3][1]=-1;tabPosicoesFinais[4][0]=-1;tabPosicoesFinais[4][1]=-1
        tabPosicoesFinais[5][0]=-1;tabPosicoesFinais[5][1]=-1;tabPosicoesFinais[6][0]=-1;tabPosicoesFinais[6][1]=-1;tabPosicoesFinais[7][0]=-1;
        tabPosicoesFinais[7][1]=-1;tabPosicoesFinais[8][0]=-1;tabPosicoesFinais[8][1]=-1;tabPosicoesFinais[9][0]=-1;tabPosicoesFinais[9][1]=-1;
    }

    private fun limpaBtns(){
        for(i in 0 until 3){
            selectedBtns[i]?.isSelected=false
        }
        pecasSelecionadas=0
    }

    fun jogoTerminou(op:Int){
        when(op){
            EMPATE->{
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.empate)
                builder.setMessage(R.string.empateee)


                builder.setPositiveButton(R.string.novJog) { dialog, which ->
                    // preparaNovoJogo()
                    state.postValue(Modo2Activity.State.GAME_OVER)
                    //  changeMyMove(socketOS, NOVO_JOGO,0,0)
                    finish()
                }

                builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                    b.btnTrocaPecas.isEnabled = false
                    b.btnPecaBomba.isEnabled = false
                    b.btnPassaVez.isEnabled = false
                    //b.txtIndicadorDeJogador.text = R.string.empate.toString()
                }

                builder.show()
            }
            EMPATE_VERMELHO_VERDE->{
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.empate)
                builder.setMessage(R.string.empate_vermelho_verde)


                builder.setPositiveButton(R.string.novJog) { dialog, which ->
                    // preparaNovoJogo()
                    state.postValue(Modo2Activity.State.GAME_OVER)
                    //  changeMyMove(socketOS, NOVO_JOGO,0,0)
                    finish()
                }

                builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                    b.btnTrocaPecas.isEnabled = false
                    b.btnPecaBomba.isEnabled = false
                    b.btnPassaVez.isEnabled = false
                    //b.txtIndicadorDeJogador.text = R.string.empate.toString()
                }

                builder.show()
            }
            EMPATE_AZUL_VERDE->{
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.empate)
                builder.setMessage(R.string.empate_azul_verde)


                builder.setPositiveButton(R.string.novJog) { dialog, which ->
                    // preparaNovoJogo()
                    state.postValue(Modo2Activity.State.GAME_OVER)
                    //  changeMyMove(socketOS, NOVO_JOGO,0,0)
                    finish()
                }

                builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                    b.btnTrocaPecas.isEnabled = false
                    b.btnPecaBomba.isEnabled = false
                    b.btnPassaVez.isEnabled = false
                    //b.txtIndicadorDeJogador.text = R.string.empate.toString()
                }

                builder.show()
            }
            EMPATE_VERMELHO_AZUL->{
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.empate)
                builder.setMessage(R.string.empate_vermelho_azul)


                builder.setPositiveButton(R.string.novJog) { dialog, which ->
                    // preparaNovoJogo()
                    state.postValue(Modo2Activity.State.GAME_OVER)
                    //  changeMyMove(socketOS, NOVO_JOGO,0,0)
                    finish()
                }

                builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                    b.btnTrocaPecas.isEnabled = false
                    b.btnPecaBomba.isEnabled = false
                    b.btnPassaVez.isEnabled = false
                    //b.txtIndicadorDeJogador.text = R.string.empate.toString()
                }

                builder.show()
            }
            VENCEDOR_PECAS_34->{
                vezJogador1()

                if(myMove){
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.vitoria)
                    builder.setMessage(R.string.vencedor_pecas_34)


                    builder.setPositiveButton(R.string.novJog) { dialog, which ->
                        // preparaNovoJogo()
                        state.postValue(Modo2Activity.State.GAME_OVER)
                        finish()
                        //changeMyMove(socketOS, NOVO_JOGO,0,0)
                    }

                    builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                        b.btnTrocaPecas.isEnabled = false
                        b.btnPecaBomba.isEnabled = false
                        b.btnPassaVez.isEnabled = false
                        //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                        b.profileImageJog1.borderColor = Color.RED
                        b.profileImageJog0.borderColor = Color.argb(255,253,253,150)
                    }

                    builder.show()
                }
                else{
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.derrota)
                    builder.setMessage(R.string.vencedor_pecas_34)


                    builder.setPositiveButton(R.string.novJog) { dialog, which ->
                        // preparaNovoJogo()
                        state.postValue(Modo2Activity.State.GAME_OVER)
                        //changeMyMove(socketOS, NOVO_JOGO,0,0)
                        finish()
                    }

                    builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                        b.btnTrocaPecas.isEnabled = false
                        b.btnPecaBomba.isEnabled = false
                        b.btnPassaVez.isEnabled = false
                        //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                        b.profileImageJog1.borderColor = Color.RED
                        b.profileImageJog0.borderColor = Color.argb(255,253,253,150)
                    }

                    builder.show()
                }
            }

            VENCEDOR_PECAS_35->{
                vezJogador0()

                if(myMove){
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.vitoria)
                    builder.setMessage(R.string.vencedor_pecas_35)


                    builder.setPositiveButton(R.string.novJog) { dialog, which ->
                        // preparaNovoJogo()
                        state.postValue(Modo2Activity.State.GAME_OVER)
                        //changeMyMove(socketOS, NOVO_JOGO,0,0)
                        finish()
                    }

                    builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                        b.btnTrocaPecas.isEnabled = false
                        b.btnPecaBomba.isEnabled = false
                        b.btnPassaVez.isEnabled = false
                        //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                        b.profileImageJog1.borderColor = Color.RED
                        b.profileImageJog0.borderColor = Color.argb(255,253,253,150)
                    }

                    builder.show()
                }
                else{
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.derrota)
                    builder.setMessage(R.string.vencedor_pecas_35)


                    builder.setPositiveButton(R.string.novJog) { dialog, which ->
                        //preparaNovoJogo()
                        state.postValue(Modo2Activity.State.GAME_OVER)
                        //changeMyMove(socketOS, NOVO_JOGO,0,0)
                        finish()
                    }

                    builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                        b.btnTrocaPecas.isEnabled = false
                        b.btnPecaBomba.isEnabled = false
                        b.btnPassaVez.isEnabled = false
                        //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                        b.profileImageJog1.borderColor = Color.RED
                        b.profileImageJog0.borderColor = Color.argb(255,253,253,150)
                    }

                    builder.show()
                }
            }

            VENCEDOR_PECAS_36->{
                vezJogador2()

                if(myMove){
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.vitoria)
                    builder.setMessage(R.string.vencedor_pecas_36)


                    builder.setPositiveButton(R.string.novJog) { dialog, which ->
                        // preparaNovoJogo()
                        state.postValue(Modo2Activity.State.GAME_OVER)
                        //changeMyMove(socketOS, NOVO_JOGO,0,0)
                        finish()
                    }

                    builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                        b.btnTrocaPecas.isEnabled = false
                        b.btnPecaBomba.isEnabled = false
                        b.btnPassaVez.isEnabled = false
                        //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                        b.profileImageJog1.borderColor = Color.RED
                        b.profileImageJog0.borderColor = Color.argb(255,253,253,150)
                    }

                    builder.show()
                }
                else{
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.derrota)
                    builder.setMessage(R.string.vencedor_pecas_36)


                    builder.setPositiveButton(R.string.novJog) { dialog, which ->
                        //preparaNovoJogo()
                        state.postValue(Modo2Activity.State.GAME_OVER)
                        //changeMyMove(socketOS, NOVO_JOGO,0,0)
                        finish()
                    }

                    builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                        b.btnTrocaPecas.isEnabled = false
                        b.btnPecaBomba.isEnabled = false
                        b.btnPassaVez.isEnabled = false
                        //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                        b.profileImageJog1.borderColor = Color.RED
                        b.profileImageJog0.borderColor = Color.argb(255,253,253,150)
                    }

                    builder.show()
                }
            }
        }

    }

    private fun verificaVencedor() {
        var pjog0=0
        var pjog1=0
        var pjog2=0
        var parcelVazias = false

        for(i in 0 until 10){
            for(j in 0 until 10){

                if(tabuleiro[i][j]=="")
                    parcelVazias=true

                if(tabuleiro[i][j]==pecaJog0)
                    pjog0++

                if(tabuleiro[i][j]==pecaJog1){
                    pjog1++
                }

                if(tabuleiro[i][j]==pecaJog2)
                    pjog2++

            }
        }

        b.numPecasJog0.text = pjog0.toString()
        b.numPecasJog1.text = pjog1.toString()
        b.numPecasJog2.text = pjog2.toString()

        if(pjog0==pjog1 && pjog1==pjog2 && jogoAcabou){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.empate)
            builder.setMessage(R.string.empateee)
            state.postValue(Modo2Activity.State.GAME_OVER)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
                //preparaNovoJogo()
                finish()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                //b.txtIndicadorDeJogador.text = R.string.empate.toString()
            }

            builder.show()
        }

        if(pjog0 == 0 && pjog2==0 && pjog1>0){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.fim_jogo)
            builder.setMessage(R.string.vencedor_pecas_34)
            state.postValue(Modo2Activity.State.GAME_OVER)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
               // preparaNovoJogo()
                finish()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                vezJogador1()
            }

            builder.show()
        }

        if(pjog1==0 && pjog2==0 && pjog0>0){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.fim_jogo)
            builder.setMessage(R.string.vencedor_pecas_35)
            state.postValue(Modo2Activity.State.GAME_OVER)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
                //preparaNovoJogo()
                finish()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                vezJogador0()
            }

            builder.show()
        }

        if(pjog0==0 && pjog1==0 && pjog2>0){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.fim_jogo)
            builder.setMessage(R.string.vencedor_pecas_36)
            state.postValue(Modo2Activity.State.GAME_OVER)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
               // preparaNovoJogo()
                finish()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                vezJogador2()
            }

            builder.show()
        }


        if(!parcelVazias && pjog0>pjog1 && pjog0>pjog2){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.fim_jogo)
            builder.setMessage(R.string.vencedor_pecas_35)
            state.postValue(Modo2Activity.State.GAME_OVER)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
               // preparaNovoJogo()
                finish()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                // b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                vezJogador0()
            }

            builder.show()
        }

        if(!parcelVazias && pjog1>pjog0 && pjog1>pjog2){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.fim_jogo)
            builder.setMessage(R.string.vencedor_pecas_34)
            state.postValue(Modo2Activity.State.GAME_OVER)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
               // preparaNovoJogo()
                finish()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                // b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                vezJogador1()
            }

            builder.show()
        }

        if(!parcelVazias && pjog2>pjog0 && pjog2>pjog1){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.fim_jogo)
            builder.setMessage(R.string.vencedor_pecas_36)
            state.postValue(Modo2Activity.State.GAME_OVER)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
               // preparaNovoJogo()
                finish()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                // b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                vezJogador2()
            }

            builder.show()
        }

        if(parcelVazias && pjog0>pjog1 && pjog0>pjog2 && jogoAcabou){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.fim_jogo)
            builder.setMessage(R.string.vencedor_pecas_35)
            state.postValue(Modo2Activity.State.GAME_OVER)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
              //  preparaNovoJogo()
                finish()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                // b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                vezJogador0()
            }

            builder.show()
        }

        if(parcelVazias && pjog1>pjog0 && pjog1>pjog2 && jogoAcabou){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.fim_jogo)
            builder.setMessage(R.string.vencedor_pecas_34)
            state.postValue(Modo2Activity.State.GAME_OVER)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
                //preparaNovoJogo()
                finish()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                vezJogador1()
            }

            builder.show()
        }

        if(parcelVazias && pjog2>pjog0 && pjog2>pjog1 && jogoAcabou){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.fim_jogo)
            builder.setMessage(R.string.vencedor_pecas_36)
            state.postValue(Modo2Activity.State.GAME_OVER)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
                //preparaNovoJogo()
                finish()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                vezJogador2()
            }

            builder.show()
        }

        if(pjog0==pjog1 && pjog0>pjog2 && jogoAcabou){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.empate)
            builder.setMessage(R.string.empate_vermelho_azul)
            state.postValue(Modo2Activity.State.GAME_OVER)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
                //preparaNovoJogo()
                finish()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
            }

            builder.show()
        }

        if(pjog0==pjog2 && pjog0>pjog1 && jogoAcabou){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.empate)
            builder.setMessage(R.string.empate_azul_verde)
            state.postValue(Modo2Activity.State.GAME_OVER)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
                //preparaNovoJogo()
                finish()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
            }

            builder.show()
        }

        if(pjog1==pjog2 && pjog1>pjog0 && jogoAcabou){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.empate)
            builder.setMessage(R.string.empate_vermelho_verde)
            state.postValue(Modo2Activity.State.GAME_OVER)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
                //preparaNovoJogo()
                finish()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
            }

            builder.show()
        }


    }


    private fun atualizaVista() {
        b.btn00.text = tabuleiro[0][0];b.btn01.text = tabuleiro[0][1]; b.btn02.text = tabuleiro[0][2]; b.btn03.text = tabuleiro[0][3]; b.btn04.text = tabuleiro[0][4]; b.btn05.text = tabuleiro[0][5]; b.btn06.text = tabuleiro[0][6]; b.btn07.text = tabuleiro[0][7];b.btn08.text=tabuleiro[0][8];b.btn09.text=tabuleiro[0][9];
        b.btn10.text = tabuleiro[1][0]; b.btn11.text = tabuleiro[1][1]; b.btn12.text = tabuleiro[1][2]; b.btn13.text = tabuleiro[1][3]; b.btn14.text = tabuleiro[1][4]; b.btn15.text = tabuleiro[1][5]; b.btn16.text = tabuleiro[1][6]; b.btn17.text = tabuleiro[1][7];b.btn18.text=tabuleiro[1][8];b.btn19.text=tabuleiro[1][9];
        b.btn20.text = tabuleiro[2][0]; b.btn21.text = tabuleiro[2][1]; b.btn22.text = tabuleiro[2][2]; b.btn23.text = tabuleiro[2][3]; b.btn24.text = tabuleiro[2][4]; b.btn25.text = tabuleiro[2][5]; b.btn26.text = tabuleiro[2][6]; b.btn27.text = tabuleiro[2][7];b.btn28.text=tabuleiro[2][8];b.btn29.text=tabuleiro[2][9];
        b.btn30.text = tabuleiro[3][0]; b.btn31.text = tabuleiro[3][1]; b.btn32.text = tabuleiro[3][2]; b.btn33.text = tabuleiro[3][3]; b.btn34.text = tabuleiro[3][4]; b.btn35.text = tabuleiro[3][5]; b.btn36.text = tabuleiro[3][6]; b.btn37.text = tabuleiro[3][7];b.btn38.text=tabuleiro[3][8];b.btn39.text=tabuleiro[3][9];
        b.btn40.text = tabuleiro[4][0]; b.btn41.text = tabuleiro[4][1]; b.btn42.text = tabuleiro[4][2]; b.btn43.text = tabuleiro[4][3]; b.btn44.text = tabuleiro[4][4]; b.btn45.text = tabuleiro[4][5]; b.btn46.text = tabuleiro[4][6]; b.btn47.text = tabuleiro[4][7];b.btn48.text=tabuleiro[4][8];b.btn49.text=tabuleiro[4][9];
        b.btn50.text = tabuleiro[5][0]; b.btn51.text = tabuleiro[5][1]; b.btn52.text = tabuleiro[5][2]; b.btn53.text = tabuleiro[5][3]; b.btn54.text = tabuleiro[5][4]; b.btn55.text = tabuleiro[5][5]; b.btn56.text = tabuleiro[5][6]; b.btn57.text = tabuleiro[5][7];b.btn58.text=tabuleiro[5][8];b.btn59.text=tabuleiro[5][9];
        b.btn60.text = tabuleiro[6][0]; b.btn61.text = tabuleiro[6][1]; b.btn62.text = tabuleiro[6][2]; b.btn63.text = tabuleiro[6][3]; b.btn64.text = tabuleiro[6][4]; b.btn65.text = tabuleiro[6][5]; b.btn66.text = tabuleiro[6][6]; b.btn67.text = tabuleiro[6][7];b.btn68.text=tabuleiro[6][8];b.btn69.text=tabuleiro[6][9];
        b.btn70.text = tabuleiro[7][0]; b.btn71.text = tabuleiro[7][1]; b.btn72.text = tabuleiro[7][2]; b.btn73.text = tabuleiro[7][3]; b.btn74.text = tabuleiro[7][4]; b.btn75.text = tabuleiro[7][5]; b.btn76.text = tabuleiro[7][6]; b.btn77.text = tabuleiro[7][7];b.btn78.text=tabuleiro[7][8];b.btn79.text=tabuleiro[7][9];
        b.btn80.text = tabuleiro[8][0]; b.btn81.text = tabuleiro[8][1]; b.btn82.text = tabuleiro[8][2]; b.btn83.text = tabuleiro[8][3]; b.btn84.text = tabuleiro[8][4]; b.btn85.text = tabuleiro[8][5]; b.btn86.text = tabuleiro[8][6]; b.btn87.text = tabuleiro[8][7];b.btn88.text=tabuleiro[8][8];b.btn89.text=tabuleiro[8][9];
        b.btn90.text = tabuleiro[9][0]; b.btn91.text = tabuleiro[9][1]; b.btn92.text = tabuleiro[9][2]; b.btn93.text = tabuleiro[9][3]; b.btn94.text = tabuleiro[9][4]; b.btn95.text = tabuleiro[9][5]; b.btn96.text = tabuleiro[9][6]; b.btn97.text = tabuleiro[9][7];b.btn98.text=tabuleiro[9][8];b.btn99.text=tabuleiro[9][9];
    }



    //Operacao do cliente
    fun enviaJogada(socketO: OutputStream?,operacao:Int,linha: Int,coluna: Int) {
        if (connectionState.value != Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED)
            return

        Log.i("TAG","Change my move")

        socketO?.run {
            thread {
                try {
                    var data=Jogada(operacao)
                    if(operacao== JOGADA) {
                        data.linha = linha
                        data.coluna = coluna
                        data.jogadaEspecial = bombaSelec
                    }

                    Log.i("TAG","@8")
                    var str= Json.encodeToString(data)
                    Log.i("TAG","@9")
                    val printStream = PrintStream(this)
                    printStream.println(str)
                    Log.i("TAG","@10")
                    printStream.flush()
                } catch (e: Exception) {
                    Log.i("Exception",e.toString())
                    //stopGame()
                }
            }
        }
        state.postValue(Modo2Activity.State.PLAYING_OTHER)
        //checkIfSomeoneWins()
    }

    fun enviaTrocaPecas(socketO: OutputStream?){
        if (connectionState.value != Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED)
            return

        Log.i("TAG","Troca peças")

        socketO?.run {
            thread {
                try {
                    var data=Jogada(TROCA_PECAS)
                    data.pecas=pecas

                    Log.i("TAG","@18")
                    var str= Json.encodeToString(data)
                    Log.i("TAG","@19")
                    val printStream = PrintStream(this)
                    printStream.println(str)
                    Log.i("TAG","@20")
                    printStream.flush()
                    Log.i("TAG","Peças selecionadas enviadas")
                } catch (e: Exception) {
                    Log.i("Exception", e.toString())
                    //stopGame()
                }
            }
        }

    }

    fun BitMapToString(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    fun enviaDados(socketO: OutputStream?){
        if (connectionState.value != Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED)
            return

        Log.i("TAG","Troca peças")

        socketO?.run {
            thread {
                try {
                    var data=Jogada(DADOS)
                    data.nome= ImageUtils.nomeJogador
                    data.imagem=BitMapToString(ImageUtils.bitm!!)

                    Log.i("TAG","@18")
                    var str= Json.encodeToString(data)
                    Log.i("TAG","@19")
                    val printStream = PrintStream(this)
                    printStream.println(str)
                    Log.i("TAG","@20")
                    printStream.flush()
                    Log.i("TAG","Peças selecionadas enviadas")
                } catch (e: Exception) {
                    Log.i("Exception", e.toString())
                    //stopGame()
                }
            }
        }

    }

    private fun jogada(linha:Int, coluna:Int){
        if(myMove)
            enviaJogada(socketOS,JOGADA,linha,coluna)
    }

    private fun atualizaContador() {
        var pjog0 = 0
        var pjog1 = 0
        var pjog2=0

        for (i in 0 until 10) {
            for (j in 0 until 10) {

                if (tabuleiro[i][j] == pecaJog0)
                    pjog0++

                if (tabuleiro[i][j] == pecaJog1) {
                    pjog1++
                }

                if (tabuleiro[i][j] == pecaJog2) {
                    pjog2++
                }

            }
        }

        b.numPecasJog0.text = pjog0.toString()
        b.numPecasJog1.text = pjog1.toString()
        b.numPecasJog2.text = pjog2.toString()
    }

    fun onTroca(view:android.view.View){
        if(myMove && jogTroca){
            Snackbar.make(view,
                R.string.limite_troca,
                Snackbar.LENGTH_SHORT).show()
            return
        }
        if(myMove && pecasSelecionadas<2){
            Snackbar.make(view,
                R.string.selecione_peca,
                Snackbar.LENGTH_SHORT).show()
            return
        }
        if(myMove && pecasSelecionadas==2){
            Snackbar.make(view,
                R.string.selecione_adversaria,
                Snackbar.LENGTH_SHORT).show()
            return
        }
        if(myMove && pecasSelecionadas==3){
            Snackbar.make(view,
                R.string.confirmar_troca,
                Snackbar.LENGTH_SHORT)
                .setAction(R.string.sim){
                    enviaTrocaPecas(socketOS)
                }.show()
        }
    }

    fun onBomba(view:android.view.View){
        if(myMove) {
            if (!bombaSelec) {
                if (jogBomba) {
                    Snackbar.make(
                        view,
                        R.string.bomba_ja_jogada,
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(
                        view,
                        R.string.bomba_selecionada,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    bombaSelec = true
                    b.btnPecaBomba.setBackgroundResource(R.drawable.button_shadow)
                }
            } else {
                if (bombaSelec) {
                    Snackbar.make(
                        view,
                        R.string.bomba_desselecionada,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    bombaSelec = false
                    b.btnTrocaPecas.setBackgroundResource(R.drawable.round_btn)
                }
            }
        }
    }


    private fun acionaListeners(){
        //BOTÕES ESPECIAIS

        b.btnPassaVez.setOnClickListener{
            enviaJogada(socketOS, PASSA_VEZ,0,0)
        }


        //0ª FILA DE PEÇAS
        b.btn00.setOnClickListener{
            if((b.btn00.text.isEmpty() || b.btn00.text.isBlank())) {
                jogada(0,0)
            }
            else{
                selecionaPeca(b.btn00,0,0)
            }

        }

        b.btn01.setOnClickListener{
            if((b.btn01.text.isEmpty() || b.btn01.text.isBlank())) {
                jogada(0,1)
            }
            else{
                selecionaPeca(b.btn01,0,1)
            }
        }

        b.btn02.setOnClickListener{
            if((b.btn02.text.isEmpty() || b.btn02.text.isBlank())) {
                jogada(0,2)
            }
            else{
                selecionaPeca(b.btn02,0,2)
            }
        }

        b.btn03.setOnClickListener{
            if((b.btn03.text.isEmpty() || b.btn03.text.isBlank())) {
                jogada(0,3)
            }
            else{
                selecionaPeca(b.btn03,0,3)
            }
        }

        b.btn04.setOnClickListener{
            if((b.btn04.text.isEmpty() || b.btn04.text.isBlank())) {
                jogada(0,4)
            }
            else{
                selecionaPeca(b.btn04,0,4)
            }
        }

        b.btn05.setOnClickListener{
            if((b.btn05.text.isEmpty() || b.btn05.text.isBlank())) {
                jogada(0,5)
            }
            else{
                selecionaPeca(b.btn05,0,5)
            }
        }

        b.btn06.setOnClickListener{
            if((b.btn06.text.isEmpty() || b.btn06.text.isBlank())) {
                jogada(0,6)
            }
            else{
                selecionaPeca(b.btn06,0,6)
            }
        }

        b.btn07.setOnClickListener{
            if((b.btn07.text.isEmpty() || b.btn07.text.isBlank())) {
                jogada(0,7)
            }
            else{
                selecionaPeca(b.btn07,0,7)
            }
        }

        b.btn08.setOnClickListener{
            if((b.btn08.text.isEmpty() || b.btn08.text.isBlank())) {
                jogada(0,8)
            }
            else{
                selecionaPeca(b.btn08,0,8)
            }
        }

        b.btn09.setOnClickListener{
            if((b.btn09.text.isEmpty() || b.btn09.text.isBlank())) {
                jogada(0,9)
            }
            else{
                selecionaPeca(b.btn09,0,9)
            }
        }






        //1ª FILA DE PEÇAS
        b.btn10.setOnClickListener{
            if((b.btn10.text.isEmpty() || b.btn10.text.isBlank())) {
                jogada(1,0)
            }
            else{
                selecionaPeca(b.btn10,1,0)
            }
        }

        b.btn11.setOnClickListener{
            if((b.btn11.text.isEmpty() || b.btn11.text.isBlank())) {
                jogada(1,1)
            }
            else{
                selecionaPeca(b.btn11,1,1)
            }
        }

        b.btn12.setOnClickListener{
            if((b.btn12.text.isEmpty() || b.btn12.text.isBlank())) {
                jogada(1,2)
            }
            else{
                selecionaPeca(b.btn12,1,2)
            }
        }

        b.btn13.setOnClickListener{
            if((b.btn13.text.isEmpty() || b.btn13.text.isBlank())) {
                jogada(1,3)
            }
            else{
                selecionaPeca(b.btn13,1,3)
            }
        }

        b.btn14.setOnClickListener{
            if((b.btn14.text.isEmpty() || b.btn14.text.isBlank())) {
                jogada(1,4)
            }
            else{
                selecionaPeca(b.btn14,1,4)
            }
        }

        b.btn15.setOnClickListener{
            if((b.btn15.text.isEmpty() || b.btn15.text.isBlank())) {
                jogada(1,5)
            }
            else{
                selecionaPeca(b.btn15,1,5)
            }
        }

        b.btn16.setOnClickListener{
            if((b.btn16.text.isEmpty() || b.btn16.text.isBlank())) {
                jogada(1,6)
            }
            else{
                selecionaPeca(b.btn16,1,6)
            }
        }

        b.btn17.setOnClickListener{
            if((b.btn17.text.isEmpty() || b.btn17.text.isBlank())) {
                jogada(1,7)
            }
            else{
                selecionaPeca(b.btn17,1,7)
            }
        }

        b.btn18.setOnClickListener{
            if((b.btn18.text.isEmpty() || b.btn18.text.isBlank())) {
                jogada(1,8)
            }
            else{
                selecionaPeca(b.btn18,1,8)
            }
        }

        b.btn19.setOnClickListener{
            if((b.btn19.text.isEmpty() || b.btn19.text.isBlank())) {
                jogada(1,9)
            }
            else{
                selecionaPeca(b.btn19,1,9)
            }
        }







        //2ª FILA DE PEÇAS
        b.btn20.setOnClickListener{
            if((b.btn20.text.isEmpty() || b.btn20.text.isBlank())) {
                jogada(2,0)
            }
            else{
                selecionaPeca(b.btn20,2,0)
            }
        }

        b.btn21.setOnClickListener{
            if((b.btn21.text.isEmpty() || b.btn21.text.isBlank())) {
                jogada(2,1)
            }
            else{
                selecionaPeca(b.btn21,2,1)
            }
        }

        b.btn22.setOnClickListener{
            if((b.btn22.text.isEmpty() || b.btn22.text.isBlank())) {
                jogada(2,2)
            }
            else{
                selecionaPeca(b.btn22,2,2)
            }
        }

        b.btn23.setOnClickListener{
            if((b.btn23.text.isEmpty() || b.btn23.text.isBlank())) {
                jogada(2,3)
            }
            else{
                selecionaPeca(b.btn23,2,3)
            }
        }

        b.btn24.setOnClickListener{
            if((b.btn24.text.isEmpty() || b.btn24.text.isBlank())) {
                jogada(2,4)
            }
            else{
                selecionaPeca(b.btn24,2,4)
            }
        }

        b.btn25.setOnClickListener{
            if((b.btn25.text.isEmpty() || b.btn25.text.isBlank())) {
                jogada(2,5)
            }
            else{
                selecionaPeca(b.btn25,2,5)
            }
        }

        b.btn26.setOnClickListener{
            if((b.btn26.text.isEmpty() || b.btn26.text.isBlank())) {
                jogada(2,6)
            }
            else{
                selecionaPeca(b.btn26,2,6)
            }
        }

        b.btn27.setOnClickListener{
            if((b.btn27.text.isEmpty() || b.btn27.text.isBlank())) {
                jogada(2,7)
            }
            else{
                selecionaPeca(b.btn27,2,7)
            }
        }

        b.btn28.setOnClickListener{
            if((b.btn28.text.isEmpty() || b.btn28.text.isBlank())) {
                jogada(2,8)
            }
            else{
                selecionaPeca(b.btn28,2,8)
            }
        }

        b.btn29.setOnClickListener{
            if((b.btn29.text.isEmpty() || b.btn29.text.isBlank())) {
                jogada(2,9)
            }
            else{
                selecionaPeca(b.btn29,2,9)
            }
        }







        //3ª FILA DE PEÇAS
        b.btn30.setOnClickListener{
            if((b.btn30.text.isEmpty() || b.btn30.text.isBlank())) {
                jogada(3,0)
            }
            else{
                selecionaPeca(b.btn30,3,0)
            }
        }

        b.btn31.setOnClickListener{
            if((b.btn31.text.isEmpty() || b.btn31.text.isBlank())) {
                jogada(3,1)
            }
            else{
                selecionaPeca(b.btn31,3,1)
            }
        }

        b.btn32.setOnClickListener{
            if((b.btn32.text.isEmpty() || b.btn32.text.isBlank())) {
                jogada(3,2)
            }
            else{
                selecionaPeca(b.btn32,3,2)
            }
        }

        b.btn33.setOnClickListener{
            if((b.btn33.text.isEmpty() || b.btn33.text.isBlank())) {
                jogada(3,3)
            }
            else{
                selecionaPeca(b.btn33,3,3)
            }
        }

        b.btn34.setOnClickListener{
            if((b.btn34.text.isEmpty() || b.btn34.text.isBlank())) {
                jogada(3,4)
            }
            else{
                selecionaPeca(b.btn34,3,4)
            }
        }

        b.btn35.setOnClickListener{
            if((b.btn35.text.isEmpty() || b.btn35.text.isBlank())) {
                jogada(3,5)
            }
            else{
                selecionaPeca(b.btn35,3,5)
            }
        }

        b.btn36.setOnClickListener{
            if((b.btn36.text.isEmpty() || b.btn36.text.isBlank())) {
                jogada(3,6)
            }
            else{
                selecionaPeca(b.btn36,3,6)
            }
        }

        b.btn37.setOnClickListener{
            if((b.btn37.text.isEmpty() || b.btn37.text.isBlank())) {
                jogada(3,7)
            }
            else{
                selecionaPeca(b.btn37,3,7)
            }
        }

        b.btn38.setOnClickListener{
            if((b.btn38.text.isEmpty() || b.btn38.text.isBlank())) {
                jogada(3,8)
            }
            else{
                selecionaPeca(b.btn38,3,8)
            }
        }

        b.btn39.setOnClickListener{
            if((b.btn39.text.isEmpty() || b.btn39.text.isBlank())) {
                jogada(3,9)
            }
            else{
                selecionaPeca(b.btn39,3,9)
            }
        }









        //4º FILA DE PEÇAS
        b.btn40.setOnClickListener{
            if((b.btn40.text.isEmpty() || b.btn40.text.isBlank())) {
                jogada(4,0)
            }
            else{
                selecionaPeca(b.btn40,4,0)
            }
        }

        b.btn41.setOnClickListener{
            if((b.btn41.text.isEmpty() || b.btn41.text.isBlank())) {
                jogada(4,1)
            }
            else{
                selecionaPeca(b.btn41,4,1)
            }
        }

        b.btn42.setOnClickListener{
            if((b.btn42.text.isEmpty() || b.btn42.text.isBlank())) {
                jogada(4,2)
            }
            else{
                selecionaPeca(b.btn42,4,2)
            }
        }

        b.btn43.setOnClickListener{
            if((b.btn43.text.isEmpty() || b.btn43.text.isBlank())) {
                jogada(4,3)
            }
            else{
                selecionaPeca(b.btn43,4,3)
            }
        }

        b.btn44.setOnClickListener{
            if((b.btn44.text.isEmpty() || b.btn44.text.isBlank())) {
                jogada(4,4)
            }
            else{
                selecionaPeca(b.btn44,4,4)
            }
        }

        b.btn45.setOnClickListener{
            if((b.btn45.text.isEmpty() || b.btn45.text.isBlank())) {
                jogada(4,5)
            }
            else{
                selecionaPeca(b.btn45,4,5)
            }
        }

        b.btn46.setOnClickListener{
            if((b.btn46.text.isEmpty() || b.btn46.text.isBlank())) {
                jogada(4,6)
            }
            else{
                selecionaPeca(b.btn46,4,6)
            }
        }

        b.btn47.setOnClickListener{
            if((b.btn47.text.isEmpty() || b.btn47.text.isBlank())) {
                jogada(4,7)
            }
            else{
                selecionaPeca(b.btn47,4,7)
            }
        }

        b.btn48.setOnClickListener{
            if((b.btn48.text.isEmpty() || b.btn48.text.isBlank())) {
                jogada(4,8)
            }
            else{
                selecionaPeca(b.btn48,4,8)
            }
        }

        b.btn49.setOnClickListener{
            if((b.btn49.text.isEmpty() || b.btn49.text.isBlank())) {
                jogada(4,9)
            }
            else{
                selecionaPeca(b.btn49,4,9)
            }
        }









        //5ª FILA DE PEÇAS
        b.btn50.setOnClickListener{
            if((b.btn50.text.isEmpty() || b.btn50.text.isBlank())) {
                jogada(5,0)
            }
            else{
                selecionaPeca(b.btn50,5,0)
            }
        }

        b.btn51.setOnClickListener{
            if((b.btn51.text.isEmpty() || b.btn51.text.isBlank())) {
                jogada(5,1)
            }
            else{
                selecionaPeca(b.btn51,5,1)
            }
        }

        b.btn52.setOnClickListener{
            if((b.btn52.text.isEmpty() || b.btn52.text.isBlank())) {
                jogada(5,2)
            }
            else{
                selecionaPeca(b.btn52,5,2)
            }
        }

        b.btn53.setOnClickListener{
            if((b.btn53.text.isEmpty() || b.btn53.text.isBlank())) {
                jogada(5,3)
            }
            else{
                selecionaPeca(b.btn53,5,3)
            }
        }

        b.btn54.setOnClickListener{
            if((b.btn54.text.isEmpty() || b.btn54.text.isBlank())) {
                jogada(5,4)
            }
            else{
                selecionaPeca(b.btn54,5,4)
            }
        }

        b.btn55.setOnClickListener{
            if((b.btn55.text.isEmpty() || b.btn55.text.isBlank())) {
                jogada(5,5)
            }
            else{
                selecionaPeca(b.btn55,5,5)
            }
        }

        b.btn56.setOnClickListener{
            if((b.btn56.text.isEmpty() || b.btn56.text.isBlank())) {
                jogada(5,6)
            }
            else{
                selecionaPeca(b.btn56,5,6)
            }
        }

        b.btn57.setOnClickListener{
            if((b.btn57.text.isEmpty() || b.btn57.text.isBlank())) {
                jogada(5,7)
            }
            else{
                selecionaPeca(b.btn57,5,7)
            }
        }

        b.btn58.setOnClickListener{
            if((b.btn58.text.isEmpty() || b.btn58.text.isBlank())) {
                jogada(5,8)
            }
            else{
                selecionaPeca(b.btn58,5,8)
            }
        }

        b.btn59.setOnClickListener{
            if((b.btn59.text.isEmpty() || b.btn59.text.isBlank())) {
                jogada(5,9)
            }
            else{
                selecionaPeca(b.btn59,5,9)
            }
        }








        //6ª FILA DE PEÇAS
        b.btn60.setOnClickListener{
            if((b.btn60.text.isEmpty() || b.btn60.text.isBlank())) {
                jogada(6,0)
            }
            else{
                selecionaPeca(b.btn60,6,0)
            }
        }

        b.btn61.setOnClickListener{
            if((b.btn61.text.isEmpty() || b.btn61.text.isBlank())) {
                jogada(6,1)
            }
            else{
                selecionaPeca(b.btn61,6,1)
            }
        }

        b.btn62.setOnClickListener{
            if((b.btn62.text.isEmpty() || b.btn62.text.isBlank())) {
                jogada(6,2)
            }
            else{
                selecionaPeca(b.btn62,6,2)
            }
        }

        b.btn63.setOnClickListener{
            if((b.btn63.text.isEmpty() || b.btn63.text.isBlank())) {
                jogada(6,3)
            }
            else{
                selecionaPeca(b.btn63,6,3)
            }
        }

        b.btn64.setOnClickListener{
            if((b.btn64.text.isEmpty() || b.btn64.text.isBlank())) {
                jogada(6,4)
            }
            else{
                selecionaPeca(b.btn64,6,4)
            }
        }

        b.btn65.setOnClickListener{
            if((b.btn65.text.isEmpty() || b.btn65.text.isBlank())) {
                jogada(6,5)
            }
            else{
                selecionaPeca(b.btn65,6,5)
            }
        }

        b.btn66.setOnClickListener{
            if((b.btn66.text.isEmpty() || b.btn66.text.isBlank())) {
                jogada(6,6)
            }
            else{
                selecionaPeca(b.btn66,6,6)
            }
        }

        b.btn67.setOnClickListener{
            if((b.btn67.text.isEmpty() || b.btn67.text.isBlank())) {
                jogada(6,7)
            }
            else{
                selecionaPeca(b.btn67,6,7)
            }
        }

        b.btn68.setOnClickListener{
            if((b.btn68.text.isEmpty() || b.btn68.text.isBlank())) {
                jogada(6,8)
            }
            else{
                selecionaPeca(b.btn68,6,8)
            }
        }

        b.btn69.setOnClickListener{
            if((b.btn69.text.isEmpty() || b.btn69.text.isBlank())) {
                jogada(6,9)
            }
            else{
                selecionaPeca(b.btn69,6,9)
            }
        }






        //7ª FILA DE PEÇAS
        b.btn70.setOnClickListener{
            if((b.btn70.text.isEmpty() || b.btn70.text.isBlank())) {
                jogada(7,0)
            }
            else{
                selecionaPeca(b.btn70,7,0)
            }
        }

        b.btn71.setOnClickListener{
            if((b.btn71.text.isEmpty() || b.btn71.text.isBlank())) {
                jogada(7,1)
            }
            else{
                selecionaPeca(b.btn71,7,1)
            }
        }

        b.btn72.setOnClickListener{
            if((b.btn72.text.isEmpty() || b.btn72.text.isBlank())) {
                jogada(7,2)
            }
            else{
                selecionaPeca(b.btn72,7,2)
            }
        }

        b.btn73.setOnClickListener{
            if((b.btn73.text.isEmpty() || b.btn73.text.isBlank())) {
                jogada(7,3)
            }
            else{
                selecionaPeca(b.btn73,7,3)
            }
        }

        b.btn74.setOnClickListener{
            if((b.btn74.text.isEmpty() || b.btn74.text.isBlank())) {
                jogada(7,4)
            }
            else{
                selecionaPeca(b.btn74,7,4)
            }
        }

        b.btn75.setOnClickListener{
            if((b.btn75.text.isEmpty() || b.btn75.text.isBlank())) {
                jogada(7,5)
            }
            else{
                selecionaPeca(b.btn75,7,5)
            }
        }

        b.btn76.setOnClickListener{
            if((b.btn76.text.isEmpty() || b.btn76.text.isBlank())) {
                jogada(7,6)
            }
            else{
                selecionaPeca(b.btn76,7,6)
            }
        }

        b.btn77.setOnClickListener{
            if((b.btn77.text.isEmpty() || b.btn77.text.isBlank())) {
                jogada(7,7)
            }
            else{
                selecionaPeca(b.btn77,7,7)
            }
        }

        b.btn78.setOnClickListener{
            if((b.btn78.text.isEmpty() || b.btn78.text.isBlank())) {
                jogada(7,8)
            }
            else{
                selecionaPeca(b.btn78,7,8)
            }
        }

        b.btn79.setOnClickListener{
            if((b.btn79.text.isEmpty() || b.btn79.text.isBlank())) {
                jogada(7,9)
            }
            else{
                selecionaPeca(b.btn79,7,9)
            }
        }



        //8ª FILA DE PEÇAS
        b.btn80.setOnClickListener{
            if((b.btn80.text.isEmpty() || b.btn80.text.isBlank())) {
                jogada(8,0)
            }
            else{
                selecionaPeca(b.btn80,8,0)
            }
        }

        b.btn81.setOnClickListener{
            if((b.btn81.text.isEmpty() || b.btn81.text.isBlank())) {
                jogada(8,1)
            }
            else{
                selecionaPeca(b.btn81,8,1)
            }
        }

        b.btn82.setOnClickListener{
            if((b.btn82.text.isEmpty() || b.btn82.text.isBlank())) {
                jogada(8,2)
            }
            else{
                selecionaPeca(b.btn82,8,2)
            }
        }

        b.btn83.setOnClickListener{
            if((b.btn83.text.isEmpty() || b.btn83.text.isBlank())) {
                jogada(8,3)
            }
            else{
                selecionaPeca(b.btn83,8,3)
            }
        }

        b.btn84.setOnClickListener{
            if((b.btn84.text.isEmpty() || b.btn84.text.isBlank())) {
                jogada(8,4)
            }
            else{
                selecionaPeca(b.btn84,8,4)
            }
        }

        b.btn85.setOnClickListener{
            if((b.btn85.text.isEmpty() || b.btn85.text.isBlank())) {
                jogada(8,5)
            }
            else{
                selecionaPeca(b.btn85,8,5)
            }
        }

        b.btn86.setOnClickListener{
            if((b.btn86.text.isEmpty() || b.btn86.text.isBlank())) {
                jogada(8,6)
            }
            else{
                selecionaPeca(b.btn86,8,6)
            }
        }

        b.btn87.setOnClickListener{
            if((b.btn87.text.isEmpty() || b.btn87.text.isBlank())) {
                jogada(8,7)
            }
            else{
                selecionaPeca(b.btn87,8,7)
            }
        }

        b.btn88.setOnClickListener{
            if((b.btn88.text.isEmpty() || b.btn88.text.isBlank())) {
                jogada(8,8)
            }
            else{
                selecionaPeca(b.btn88,8,8)
            }
        }

        b.btn89.setOnClickListener{
            if((b.btn89.text.isEmpty() || b.btn89.text.isBlank())) {
                jogada(8,9)
            }
            else{
                selecionaPeca(b.btn89,8,9)
            }
        }




        //9ª FILA DE PEÇAS
        b.btn90.setOnClickListener{
            if((b.btn90.text.isEmpty() || b.btn90.text.isBlank())) {
                jogada(9,0)
            }
            else{
                selecionaPeca(b.btn90,9,0)
            }
        }

        b.btn91.setOnClickListener{
            if((b.btn91.text.isEmpty() || b.btn91.text.isBlank())) {
                jogada(9,1)
            }
            else{
                selecionaPeca(b.btn91,9,1)
            }
        }

        b.btn92.setOnClickListener{
            if((b.btn92.text.isEmpty() || b.btn92.text.isBlank())) {
                jogada(9,2)
            }
            else{
                selecionaPeca(b.btn92,9,2)
            }
        }

        b.btn93.setOnClickListener{
            if((b.btn93.text.isEmpty() || b.btn93.text.isBlank())) {
                jogada(9,3)
            }
            else{
                selecionaPeca(b.btn93,9,3)
            }
        }

        b.btn94.setOnClickListener{
            if((b.btn94.text.isEmpty() || b.btn94.text.isBlank())) {
                jogada(9,4)
            }
            else{
                selecionaPeca(b.btn94,9,4)
            }
        }

        b.btn95.setOnClickListener{
            if((b.btn95.text.isEmpty() || b.btn95.text.isBlank())) {
                jogada(9,5)
            }
            else{
                selecionaPeca(b.btn95,9,5)
            }
        }

        b.btn96.setOnClickListener{
            if((b.btn96.text.isEmpty() || b.btn96.text.isBlank())) {
                jogada(9,6)
            }
            else{
                selecionaPeca(b.btn96,9,6)
            }
        }

        b.btn97.setOnClickListener{
            if((b.btn97.text.isEmpty() || b.btn97.text.isBlank())) {
                jogada(9,7)
            }
            else{
                selecionaPeca(b.btn97,9,7)
            }
        }

        b.btn98.setOnClickListener{
            if((b.btn98.text.isEmpty() || b.btn98.text.isBlank())) {
                jogada(9,8)
            }
            else{
                selecionaPeca(b.btn98,9,8)
            }
        }

        b.btn99.setOnClickListener{
            if((b.btn99.text.isEmpty() || b.btn99.text.isBlank())) {
                jogada(9,9)
            }
            else{
                selecionaPeca(b.btn99,9,9)
            }
        }


    }


}