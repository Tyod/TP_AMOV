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
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pt.isec.andreiagraca.amov_tp.R
import pt.isec.andreiagraca.amov_tp.databinding.ActivityModo2Binding
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.random.Random
import android.widget.TextView
import androidx.activity.viewModels
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pt.isec.ans.rascunhos.utils.ImageUtils
import java.io.ByteArrayOutputStream


const val SERVER_MODE = 0
const val CLIENT_MODE = 1

const val SERVER_PORT = 9999

const val MOVE_NONE = 0
const val MOVE_ROCK = 1
const val MOVE_PAPER = 2
const val MOVE_SCISSORS = 3
const val ME = 1
const val OTHER = 2
const val NONE = 0
var TURN=0

class Modo2Activity : AppCompatActivity() {
    lateinit var b: ActivityModo2Binding

    private val servidor: ServidorModo2Model by viewModels()
    private var dlg: AlertDialog? = null



    //Modelo do Jogo
    enum class State {
        STARTING, PLAYING_BOTH, PLAYING_ME, PLAYING_OTHER, ROUND_ENDED, GAME_OVER, FINISH
    }

    enum class ConnectionState {
        SETTING_PARAMETERS, SERVER_CONNECTING, CLIENT_CONNECTING, CONNECTION_ESTABLISHED,
        CONNECTION_ERROR, CONNECTION_ENDED
    }


    val state = MutableLiveData(State.STARTING)
    val connectionState = MutableLiveData(ConnectionState.SETTING_PARAMETERS)

    var myMove = false
    var otherMove = MOVE_NONE
    var myWins = 0
    var otherWins = 0
    var totalGames = 0
    var lastVictory = NONE

    private var socketC1: Socket? = null
    private val socketIC1: InputStream?
        get() = socketC1?.getInputStream()
    private val socketOC1: OutputStream?
        get() = socketC1?.getOutputStream()

    private var socketC2: Socket? = null
    private val socketIC2: InputStream?
        get() = socketC2?.getInputStream()
    private val socketOC2: OutputStream?
        get() = socketC2?.getOutputStream()

    private var socketS: Socket? = null
    private val socketIS: InputStream?
        get() = socketS?.getInputStream()
    private val socketOS: OutputStream?
        get() = socketS?.getOutputStream()


    private var serverSocket: ServerSocket? = null

    private var threadComm1: Thread? = null
    private var threadComm2: Thread? = null
    private var threadCommS: Thread? = null


    var clientes:Int=0

    val pecaJog0 = "\uD83D\uDD35" //Blue
    val pecaJog1 = "\uD83D\uDD34" //Red
    var pecaJogAtual : String = ""
    var minhaPeca:String=""


    var tabuleiro = Array(8) {Array(8) {""} }
    var tabPosicoesFinais = Array(8) { Array(2) {0} }

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
    var jogBomba:Boolean=false

    var jog0troca:Boolean=false
    var jog1troca:Boolean=false
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
    var terminou=false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b= ActivityModo2Binding.inflate(layoutInflater)
        setContentView(b.root)


        connectionState.observe(this) { state ->
            Log.i("CONNECTION OBSERVABLE",connectionState.value.toString())
            if ( state != ConnectionState.SETTING_PARAMETERS &&
                state != ConnectionState.SERVER_CONNECTING && dlg?.isShowing == true && servidor.clientes==2) {
                dlg?.dismiss()
                dlg = null
                servidor.preparaJogo()
            }

            if (state == ConnectionState.CONNECTION_ERROR) {
                finish()
            }
            if (state == ConnectionState.CONNECTION_ENDED)
                finish()
        }

        if (connectionState.value != ConnectionState.CONNECTION_ESTABLISHED) {
            when (intent.getIntExtra("mode", SERVER_MODE)) {
                SERVER_MODE -> startAsServer()
                CLIENT_MODE -> startAsClient()
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.opcoes,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ajudaJogada -> {
                Toast.makeText(this, R.string.ajuda_selecionada, Toast.LENGTH_SHORT).show()
                if (nAjudas == 0) {
                    enviaJogada(socketOS, AJUDA_JOGADA,0,0)
                } else
                    tiraAjuda()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        dlg?.apply {
            if (isShowing)
                dismiss()
        }
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
                    Toast.makeText(this@Modo2Activity, getString(R.string.error_address), Toast.LENGTH_LONG).show()
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
        Log.i("Criar dados","Criar dados")
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

    private fun atendeServidor(newSocket: Socket) {
        if (threadCommS != null)
            return

        socketS = newSocket

        threadCommS = thread {
            try {
                if (socketIS == null)
                    return@thread

                connectionState.postValue(ConnectionState.CONNECTION_ESTABLISHED)
                val bufI = socketIS!!.bufferedReader()
                Log.i("TAG","@2")


                while (state.value != State.GAME_OVER) {
                    Log.i("TAG", "Cliente em espera")


                    //var myObj=bufI.read
                    var message = bufI.readLine()
                    var obj = Json.decodeFromString<Jogada>(message)
                    val op = obj.op

                    Log.i("TAG", "Mensagem recebida")

                    when (op) {
                        SORTEIA_JOGADOR-> {
                            pecaJogAtual=obj.pecaJogAtual!!
                            minhaPeca= obj.minhaPeca!!
                            myMove=obj.vez!!

                            runOnUiThread(Runnable {

                                Log.i("VEZ_JOGADOR", "" + myMove + " | " + pecaJogAtual)

                                if(pecaJogAtual==pecaJog0){
                                    vezJogador0()
                                } else {
                                    vezJogador1()
                                }

                                bombaSelec = false
                                b.btnPassaVez.isEnabled = false
                            })

                            enviaDados(socketOS)
                        }
                        DADOS->{
                            var nomeJogAdv=obj.nome!!
                            var imagemJogAdv=obj.imagem!!
                            var pecaAdv=obj.minhaPeca!!

                            runOnUiThread(Runnable {
                                if(pecaAdv==pecaJog0){
                                    ImageUtils.setBitmap(b.profileImageJog1)
                                    b.nomeJog1.text = ImageUtils.nomeJogador

                                    val imageBytes = Base64.decode(imagemJogAdv, 0)
                                    val image =
                                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                                    ImageUtils.setBitmap(b.profileImageJog0, image)
                                    if (nomeJogAdv != null)
                                        b.nomeJog0.text = nomeJogAdv
                                }
                                else{
                                    ImageUtils.setBitmap(b.profileImageJog0)
                                    b.nomeJog0.text = ImageUtils.nomeJogador

                                    val imageBytes = Base64.decode(imagemJogAdv, 0)
                                    val image =
                                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                                    ImageUtils.setBitmap(b.profileImageJog1, image)
                                    if (nomeJogAdv != null)
                                        b.nomeJog1.text = nomeJogAdv
                                }

                            })
                        }
                        VEZ_JOGADOR->{
                            runOnUiThread(Runnable {
                                val pecaAtual = obj.pecaJogAtual!!
                                myMove = obj.vez!!

                                Log.i("VEZ_JOGADOR", "" + myMove + " | " + pecaAtual)

                                if (pecaAtual == pecaJog1) {
                                    vezJogador1()
                                } else {
                                        vezJogador0()
                                }

                                bombaSelec = false
                                b.btnPassaVez.isEnabled = false

                                if(obj.troca!!)
                                    b.btnPassaVez.isEnabled = true
                            })
                        }
                        TABULEIRO-> {
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
                            Log.i("TAG", "Recebida mensagem que jogo terminou")
                            runOnUiThread(Runnable {
                                jogoTerminou(op)
                            })
                        }
                        VENCEDOR_PECAS_34->{
                            Log.i("TAG", "Recebida mensagem que jogo terminou")
                            runOnUiThread(Runnable {
                                jogoTerminou(op)
                            })
                        }
                        VENCEDOR_PECAS_35->{
                            Log.i("TAG", "Recebida mensagem que jogo terminou")
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

    fun preparaJogo(){
        btn=b.btn00
        selectedBtns=Array(3){btn}
        sorteiaJogador()
        acionaListeners()
        preparaTabuleiro()
        atualizaVista()
        // verificaVencedor()
        b.numPecasJog0.text = str.toString()
        b.numPecasJog1.text = str.toString()

        state.postValue(State.PLAYING_BOTH)
    }


    fun stopServer() {
        serverSocket?.close()
        connectionState.postValue(ConnectionState.CONNECTION_ENDED)
        serverSocket = null
    }

    fun stopGame() {
        try {
            state.postValue(State.GAME_OVER)
            connectionState.postValue(ConnectionState.CONNECTION_ERROR)
            socketC1?.close()
            socketC1 = null
            threadComm1?.interrupt()
            threadComm1 = null

            socketC2?.close()
            socketC2 = null
            threadComm2?.interrupt()
            threadComm2 = null

            socketS?.close()
            socketS = null
            threadCommS?.interrupt()
            threadCommS = null
        } catch (_: Exception) { }
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

        if(linha==1 && coluna==0) {ajudaBtns[nAjudas]=b.btn10; return }
        if(linha==1 && coluna==1) { ajudaBtns[nAjudas]=b.btn11; return }
        if(linha==1 && coluna==2) { ajudaBtns[nAjudas]=b.btn12; return }
        if(linha==1 && coluna==3) { ajudaBtns[nAjudas]=b.btn13; return }
        if(linha==1 && coluna==4) { ajudaBtns[nAjudas]=b.btn14; return }
        if(linha==1 && coluna==5) { ajudaBtns[nAjudas]=b.btn15; return }
        if(linha==1 && coluna==6) { ajudaBtns[nAjudas]=b.btn16; return }
        if(linha==1 && coluna==7) { ajudaBtns[nAjudas]=b.btn17; return }

        if(linha==2 && coluna==0) { ajudaBtns[nAjudas]=b.btn20; return }
        if(linha==2 && coluna==1) { ajudaBtns[nAjudas]=b.btn21; return }
        if(linha==2 && coluna==2) { ajudaBtns[nAjudas]=b.btn22; return }
        if(linha==2 && coluna==3) { ajudaBtns[nAjudas]=b.btn23; return }
        if(linha==2 && coluna==4) { ajudaBtns[nAjudas]=b.btn24; return }
        if(linha==2 && coluna==5) { ajudaBtns[nAjudas]=b.btn25; return }
        if(linha==2 && coluna==6) { ajudaBtns[nAjudas]=b.btn26; return }
        if(linha==2 && coluna==7) { ajudaBtns[nAjudas]=b.btn27; return }

        if(linha==3 && coluna==0) { ajudaBtns[nAjudas]=b.btn30; return }
        if(linha==3 && coluna==1) { ajudaBtns[nAjudas]=b.btn31; return }
        if(linha==3 && coluna==2) { ajudaBtns[nAjudas]=b.btn32; return }
        if(linha==3 && coluna==3) { ajudaBtns[nAjudas]=b.btn33; return }
        if(linha==3 && coluna==4) { ajudaBtns[nAjudas]=b.btn34; return }
        if(linha==3 && coluna==5) { ajudaBtns[nAjudas]=b.btn35; return }
        if(linha==3 && coluna==6) { ajudaBtns[nAjudas]=b.btn36; return }
        if(linha==3 && coluna==7) { ajudaBtns[nAjudas]=b.btn37; return }

        if(linha==4 && coluna==0) { ajudaBtns[nAjudas]=b.btn40; return }
        if(linha==4 && coluna==1) { ajudaBtns[nAjudas]=b.btn41; return }
        if(linha==4 && coluna==2) { ajudaBtns[nAjudas]=b.btn42; return }
        if(linha==4 && coluna==3) { ajudaBtns[nAjudas]=b.btn43; return }
        if(linha==4 && coluna==4) { ajudaBtns[nAjudas]=b.btn44; return }
        if(linha==4 && coluna==5) { ajudaBtns[nAjudas]=b.btn45; return }
        if(linha==4 && coluna==6) { ajudaBtns[nAjudas]=b.btn46; return }
        if(linha==4 && coluna==7) { ajudaBtns[nAjudas]=b.btn47; return }

        if(linha==5 && coluna==0) { ajudaBtns[nAjudas]=b.btn50; return }
        if(linha==5 && coluna==1) { ajudaBtns[nAjudas]=b.btn51; return }
        if(linha==5 && coluna==2) { ajudaBtns[nAjudas]=b.btn52; return }
        if(linha==5 && coluna==3) { ajudaBtns[nAjudas]=b.btn53; return }
        if(linha==5 && coluna==4) { ajudaBtns[nAjudas]=b.btn54; return }
        if(linha==5 && coluna==5) { ajudaBtns[nAjudas]=b.btn55; return }
        if(linha==5 && coluna==6) { ajudaBtns[nAjudas]=b.btn56; return }
        if(linha==5 && coluna==7) { ajudaBtns[nAjudas]=b.btn57; return }

        if(linha==6 && coluna==0) { ajudaBtns[nAjudas]=b.btn60; return }
        if(linha==6 && coluna==1) { ajudaBtns[nAjudas]=b.btn61; return }
        if(linha==6 && coluna==2) { ajudaBtns[nAjudas]=b.btn62; return }
        if(linha==6 && coluna==3) { ajudaBtns[nAjudas]=b.btn63; return }
        if(linha==6 && coluna==4) { ajudaBtns[nAjudas]=b.btn64; return }
        if(linha==6 && coluna==5) { ajudaBtns[nAjudas]=b.btn65; return }
        if(linha==6 && coluna==6) { ajudaBtns[nAjudas]=b.btn66; return }
        if(linha==6 && coluna==7) { ajudaBtns[nAjudas]=b.btn67; return }

        if(linha==7 && coluna==0) { ajudaBtns[nAjudas]=b.btn70; return }
        if(linha==7 && coluna==1) { ajudaBtns[nAjudas]=b.btn71; return }
        if(linha==7 && coluna==2) { ajudaBtns[nAjudas]=b.btn72; return }
        if(linha==7 && coluna==3) { ajudaBtns[nAjudas]=b.btn73; return }
        if(linha==7 && coluna==4) { ajudaBtns[nAjudas]=b.btn74; return }
        if(linha==7 && coluna==5) { ajudaBtns[nAjudas]=b.btn75; return }
        if(linha==7 && coluna==6) { ajudaBtns[nAjudas]=b.btn76; return }
        if(linha==7 && coluna==7) { ajudaBtns[nAjudas]=b.btn77; return }
    }

    private fun ajudaCantoSupEsq(linha:Int, coluna:Int){
        if(linha-2<0)
            return
        if(coluna-2<0)
            return

        var jogAdv:String?=null

        if(pecaJogAtual==pecaJog0)
            jogAdv=pecaJog1
        else
            jogAdv=pecaJog0

        if(tabuleiro[linha-1][coluna-1]==jogAdv) {
            var i=linha-2
            var j=coluna-2
            while(i>=0 && j>=0){
                if (tabuleiro[i][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][j]==""){
                 //   botaoAjuda(i,j)
                 //   ajudaBtns[nAjudas]?.isActivated = true
                    ajudas[nAjudas][0]=i
                    ajudas[nAjudas][1]=j
                    nAjudas++
                    break
                }
                i--
                j--
            }
        }
    }

    private fun ajudaEsq(linha:Int, coluna:Int){
        if(coluna-2<0)
            return

        var jogAdv:String?=null

        if(pecaJogAtual==pecaJog0)
            jogAdv=pecaJog1
        else
            jogAdv=pecaJog0

        if(tabuleiro[linha][coluna-1]==jogAdv) {
            for (j in (coluna - 2) downTo 0) {
                if (tabuleiro[linha][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[linha][j]==""){
               //     botaoAjuda(linha,j)
               //     ajudaBtns[nAjudas]?.isActivated = true
                    ajudas[nAjudas][0]=linha
                    ajudas[nAjudas][1]=j
                    nAjudas++
                    return
                }
            }
        }
    }

    private fun ajudaInfEsq(linha:Int, coluna:Int){
        if(linha+2>7)
            return
        if(coluna-2<0)
            return

        var jogAdv:String?=null

        if(pecaJogAtual==pecaJog0)
            jogAdv=pecaJog1
        else
            jogAdv=pecaJog0

        if(tabuleiro[linha+1][coluna-1]==jogAdv) {
            var i=linha+2
            var j=coluna-2
            while(i<8 && j>=0){
                if (tabuleiro[i][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][j]==""){
                //    botaoAjuda(i,j)
                //    ajudaBtns[nAjudas]?.isActivated = true
                    ajudas[nAjudas][0]=i
                    ajudas[nAjudas][1]=j
                    nAjudas++
                    break
                }
                i++
                j--
            }
        }
    }

    private fun ajudaAbaixo(linha:Int, coluna:Int){
        if(linha+2>7)
            return

        var jogAdv:String?=null

        if(pecaJogAtual==pecaJog0)
            jogAdv=pecaJog1
        else
            jogAdv=pecaJog0

        if(tabuleiro[linha+1][coluna]==jogAdv) {
            for(i in (linha+2) until 8){
                if (tabuleiro[i][coluna] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][coluna]==""){
                 //   botaoAjuda(i,coluna)
                 //   ajudaBtns[nAjudas]?.isActivated = true
                    ajudas[nAjudas][0]=i
                    ajudas[nAjudas][1]=coluna
                    nAjudas++
                    return
                }
            }
        }
    }

    private fun ajudaInfDir(linha:Int, coluna:Int){
        if(linha+2>7)
            return
        if(coluna+2>7)
            return

        var jogAdv:String?=null

        if(pecaJogAtual==pecaJog0)
            jogAdv=pecaJog1
        else
            jogAdv=pecaJog0

        if(tabuleiro[linha+1][coluna+1]==jogAdv) {
            var i=linha+2
            var j=coluna+2
            while(i<8 && j<8){
                if (tabuleiro[i][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][j]==""){
                //    botaoAjuda(i,j)
                //    ajudaBtns[nAjudas]?.isActivated = true
                    ajudas[nAjudas][0]=i
                    ajudas[nAjudas][1]=j
                    nAjudas++
                    return
                }
                i++
                j++
            }
        }
    }

    private fun ajudaDir(linha:Int, coluna:Int){
        if(coluna+2>7)
            return

        var jogAdv:String?=null

        if(pecaJogAtual==pecaJog0)
            jogAdv=pecaJog1
        else
            jogAdv=pecaJog0

        if(tabuleiro[linha][coluna+1]==jogAdv) {
            for (j in (coluna + 2) until 8) {
                if (tabuleiro[linha][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[linha][j]==""){
              //      botaoAjuda(linha,j)
              //      ajudaBtns[nAjudas]?.isActivated = true
                    ajudas[nAjudas][0]=linha
                    ajudas[nAjudas][1]=j
                    nAjudas++
                    return
                }
            }
        }
    }

    private fun ajudaSupDir(linha:Int, coluna:Int){
        if(linha-2<0)
            return
        if(coluna+2>7)
            return

        var jogAdv:String?=null

        if(pecaJogAtual==pecaJog0)
            jogAdv=pecaJog1
        else
            jogAdv=pecaJog0

        if(tabuleiro[linha-1][coluna+1]==jogAdv) {
            var i=linha-2
            var j=coluna+2
            while(i>=0 && j<8){
                if (tabuleiro[i][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][j]==""){
     //               botaoAjuda(i,j)
     //               ajudaBtns[nAjudas]?.isActivated = true
                    ajudas[nAjudas][0]=i
                    ajudas[nAjudas][1]=j
                    nAjudas++
                    break
                }
                i--
                j++
            }
        }
    }

    private fun ajudaAcima(linha:Int, coluna:Int){
        if(linha-2<0)
            return

        var jogAdv:String?=null

        if(pecaJogAtual==pecaJog0)
            jogAdv=pecaJog1
        else
            jogAdv=pecaJog0

        if(tabuleiro[linha-1][coluna]==jogAdv) {
            for(i in (linha-2) downTo  0){
                if (tabuleiro[i][coluna] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][coluna]==""){
//                    botaoAjuda(i,coluna)
                    ajudas[nAjudas][0]=i
                    ajudas[nAjudas][1]=coluna
//                    ajudaBtns[nAjudas]?.isActivated = true
                    nAjudas++
                    return
                }
            }
        }
    }

    private fun ajudaJogada(){
        for(i in 0 until 8){
            for(j in 0 until 8){
                if(tabuleiro[i][j]==pecaJogAtual){
                    ajudaCantoSupEsq(i,j)
                    ajudaEsq(i,j)
                    ajudaInfEsq(i,j)
                    ajudaAbaixo(i,j)
                    ajudaInfDir(i,j)
                    ajudaDir(i,j)
                    ajudaSupDir(i,j)
                    ajudaAcima(i,j)
                }
            }
        }
    }


    private fun limpaTabuleiroPosicoesFinais(){
        tabPosicoesFinais[0][0]=-1;tabPosicoesFinais[0][1]=-1;tabPosicoesFinais[1][0]=-1;tabPosicoesFinais[1][1]=-1;tabPosicoesFinais[2][0]=-1
        tabPosicoesFinais[2][1]=-1;tabPosicoesFinais[3][0]=-1;tabPosicoesFinais[3][1]=-1;tabPosicoesFinais[4][0]=-1;tabPosicoesFinais[4][1]=-1
        tabPosicoesFinais[5][0]=-1;tabPosicoesFinais[5][1]=-1;tabPosicoesFinais[6][0]=-1;tabPosicoesFinais[6][1]=-1;tabPosicoesFinais[7][0]=-1;
        tabPosicoesFinais[7][1]=-1;
    }

    private fun confirmaPosicaoValida(linha:Int, coluna:Int): Boolean {

        var j = 0

        limpaTabuleiroPosicoesFinais()

        CentCim = true     //Cima
        CentCimDir = true  //Cima à direita
        CentDir = true     //Direita
        CentBaiDir = true  //Direita em baixo
        CentBai = true     //Baixo
        CentBaiEsq = true  //Baixo à esquerda
        CentEsq = true     //Esquerda
        CentCimaEsq = true //Cima à esquerda

        //Verificar Vizinhança Vazia
        if(linha!=7 && linha!=0 && coluna!=0 && coluna!=7){
            if(tabuleiro[linha+1][coluna]=="" && tabuleiro[linha+1][coluna+1]=="" && tabuleiro[linha][coluna+1]==""
                && tabuleiro[linha-1][coluna+1]=="" && tabuleiro[linha-1][coluna]=="" && tabuleiro[linha-1][coluna-1]==""
                && tabuleiro[linha][coluna-1]=="" && tabuleiro[linha+1][coluna-1]=="")
                return false
        }
        else{
            if(linha==0 && coluna!=0 && coluna!=7) {
                if(tabuleiro[linha+1][coluna]=="" && tabuleiro[linha+1][coluna+1]=="" && tabuleiro[linha][coluna+1]==""
                    && tabuleiro[linha][coluna-1]=="" && tabuleiro[linha+1][coluna-1]=="")
                    return false
            }


            if(coluna==0 && linha!=0 && linha!=7){
                if(tabuleiro[linha+1][coluna]=="" && tabuleiro[linha+1][coluna+1]=="" && tabuleiro[linha][coluna+1]==""
                    && tabuleiro[linha-1][coluna+1]=="" && tabuleiro[linha-1][coluna]=="")
                    return false
            }


            if(linha==7 && coluna!=0 && coluna!=7){
                if(tabuleiro[linha][coluna+1]=="" && tabuleiro[linha-1][coluna+1]=="" && tabuleiro[linha-1][coluna]==""
                    && tabuleiro[linha-1][coluna-1]=="" && tabuleiro[linha][coluna-1]=="")
                    return false
            }


            if(coluna==7 && linha!=0 && linha!=7){
                if(tabuleiro[linha+1][coluna]=="" && tabuleiro[linha-1][coluna]=="" && tabuleiro[linha-1][coluna-1]==""
                    && tabuleiro[linha][coluna-1]=="" && tabuleiro[linha+1][coluna-1]=="")
                    return false
            }


            if(linha==0 && coluna==0){
                if(tabuleiro[linha+1][coluna]=="" && tabuleiro[linha+1][coluna+1]=="" && tabuleiro[linha][coluna+1]=="")
                    return false
            }


            if(linha==0 && coluna==7){
                if(tabuleiro[linha+1][coluna]=="" && tabuleiro[linha][coluna-1]=="" && tabuleiro[linha+1][coluna-1]=="")
                    return false
            }


            if(linha==7 && coluna==7){
                if(tabuleiro[linha-1][coluna]=="" && tabuleiro[linha-1][coluna-1]=="" && tabuleiro[linha][coluna-1]=="")
                    return false
            }


            if(linha==7 && coluna==0){
                if(tabuleiro[linha][coluna+1]=="" && tabuleiro[linha-1][coluna+1]=="" && tabuleiro[linha-1][coluna]=="")
                    return false
            }
        }

        //VERIFICAR COMBO PEÇAS (VERTICAL E HORIZONTAl)
        //Centro -> Cima
        if(linha == 0)
            CentCim=false

        for(i in linha-1 downTo 0){

            println("Centro->Cima $i // $coluna")
            if(tabuleiro[i][coluna]==""){
                CentCim = false
                break
            }

            if(tabuleiro[i][coluna] == pecaJogAtual){
                if(i==linha-1)
                    CentCim=false
                else {
                    tabPosicoesFinais[0][0] = i; tabPosicoesFinais[0][1] = coluna;
                }

                break
            }

            if(i==0)
                CentCim = false
        }


        //Centro -> Direita
        if(coluna ==7)
            CentDir = false

        for(i in coluna+1 until 8){

            println("Centro->Direita $linha // $i")
            if(tabuleiro[linha][i] == ""){
                CentDir = false
                break
            }

            if(tabuleiro[linha][i] == pecaJogAtual){
                if(i==coluna+1)
                    CentDir=false
                else {
                    tabPosicoesFinais[2][0] = linha; tabPosicoesFinais[2][1] = i
                }

                break
            }

            if(i==7)
                CentDir = false
        }

        //Centro -> Baixo
        if(linha == 7)
            CentBai = false

        for(i in linha+1 until 8){

            println("Centro->Baixo $i // $coluna")
            if(tabuleiro[i][coluna] == ""){
                CentBai = false
                break
            }

            if(tabuleiro[i][coluna] == pecaJogAtual){
                if(i==linha+1)
                    CentBai=false
                else {
                    tabPosicoesFinais[4][0] = i; tabPosicoesFinais[4][1] = coluna
                }

                break
            }

            if(i==7)
                CentBai = false
        }


        //Centro -> Esquerda
        if(coluna == 0)
            CentEsq=false

        for(i in coluna-1 downTo 0){

            println("Centro->Esquerda $linha // $i")
            if(tabuleiro[linha][i] == ""){
                CentEsq = false
                break
            }

            if(tabuleiro[linha][i] == pecaJogAtual){
                if(i==coluna-1)
                    CentEsq = false
                else {
                    tabPosicoesFinais[6][0] = linha; tabPosicoesFinais[6][1] = i
                }

                break
            }

            if(i==0)
                CentEsq = false
        }


        //VERIFICAR COMBO PEÇAS (DIAGONAIS)
        //Centro -> Cima//Direira
        if(linha==0 || coluna==7)
            CentCimDir=false
        else{
            j = coluna+1

            for(i in linha-1 downTo 0){

                println("Centro->Cima-Direita $i // $j")
                if(tabuleiro[i][j] == ""){
                    CentCimDir = false
                    break
                }

                if(tabuleiro[i][j]==pecaJogAtual){
                    if(i==linha-1 && j==coluna+1)
                        CentCimDir=false
                    else {
                        tabPosicoesFinais[1][0] = i; tabPosicoesFinais[1][1] = j
                    }

                    break
                }

                if(j==7) {
                    CentCimDir = false
                    break
                }

                j++

                if(i==0) {
                    CentCimDir = false
                }
            }
        }



        //Centro-> Baixo//Direita
        if(linha==7 || coluna==7)
            CentBaiDir=false
        else {
            j = coluna+1

            for(i in linha+1 until 8){

                println("Centro->Baixo-Direita $i // $j")
                if(tabuleiro[i][j] == ""){
                    CentBaiDir = false
                    break
                }

                if(tabuleiro[i][j]==pecaJogAtual){
                    if(i==linha+1 && j==coluna+1)
                        CentBaiDir=false
                    else {
                        tabPosicoesFinais[3][0] = i; tabPosicoesFinais[3][1] = j
                    }

                    break
                }


                if(j==7) {
                    CentBaiDir = false
                    break
                }

                j++

                if(i==7) {
                    CentBaiDir = false
                }
            }
        }



        //Centro -> Baixo//Esquerda

        if(linha==7 || coluna==0)
            CentBaiEsq=false
        else{
            j = coluna-1

            for(i in linha+1 until 8){

                println("Centro->Baixo-Esquerda $i // $j")
                if(tabuleiro[i][j] == ""){
                    CentBaiEsq = false
                    break
                }

                if(tabuleiro[i][j]==pecaJogAtual){
                    if(i==linha+1 && j==coluna-1)
                        CentBaiEsq=false
                    else {
                        tabPosicoesFinais[5][0] = i; tabPosicoesFinais[5][1] = j
                    }

                    break
                }

                if(j==0) {
                    CentBaiEsq = false
                    break
                }

                j--

                if(i==7) {
                    CentBaiEsq = false
                }
            }
        }




        //Centro -> Esquerda//Cima
        if(linha==0 || coluna==0)
            CentCimaEsq=false
        else{
            j = coluna-1

            for(i in linha-1 downTo  0){

                println("Centro->Cima-Direita $i // $j")
                if(tabuleiro[i][j] == ""){
                    CentCimaEsq = false
                    break
                }

                if(tabuleiro[i][j]==pecaJogAtual){
                    if(i==linha-1 && j==coluna-1)
                        CentCimaEsq=false
                    else {
                        tabPosicoesFinais[7][0] = i; tabPosicoesFinais[7][1] = j
                    }

                    break
                }

                if(j==0) {
                    CentCimaEsq = false
                    break
                }

                j--

                if(i==0) {
                    CentCimaEsq = false
                }
            }
        }


        println("\n\nCentro->Cima == $CentCim")
        println("Centro->Cima//Direita == $CentCimDir" )
        println("Centro->Direita == $CentDir")
        println("Centro->Baixo//Direita == $CentBaiDir")
        println("Centro->Baixo == $CentBai")
        println("Centro->Baixo//Esquerda == $CentBaiEsq")
        println("Centro->Esquerda == $CentEsq")
        println("Centro->Cima//Esquerda == $CentCimaEsq")

        println("\n\nCentro->Cima == " + tabPosicoesFinais[0][0] + "//" + tabPosicoesFinais[0][1])
        println("Centro->Cima//Direita == " + tabPosicoesFinais[1][0] + "//" + tabPosicoesFinais[1][1])
        println("Centro->Direita == " + tabPosicoesFinais[2][0] + "//" + tabPosicoesFinais[2][1])
        println("Centro->Baixo//Direita == " + tabPosicoesFinais[3][0] + "//" + tabPosicoesFinais[3][1])
        println("Centro->Baixo == " + tabPosicoesFinais[4][0] + "//" + tabPosicoesFinais[4][1])
        println("Centro->Baixo//Esquerda == " + tabPosicoesFinais[5][0] + "//" + tabPosicoesFinais[5][1])
        println("Centro->Esquerda == " + tabPosicoesFinais[6][0] + "//" + tabPosicoesFinais[6][1])
        println("Centro->Cima//Esquerda == " + tabPosicoesFinais[7][0] + "//" + tabPosicoesFinais[7][1])

        return !(!CentCim && !CentCimDir && !CentDir && !CentBaiDir && !CentBai && !CentBaiEsq && !CentEsq && !CentCimaEsq)
    }



    private fun limpaBtns(){
        for(i in 0 until 3){
            selectedBtns[i]?.isSelected=false
        }
        pecasSelecionadas=0
    }

    private fun atualizaTabuleiro(linha:Int, coluna:Int){
        if(bombaSelec){
            tabuleiro[linha][coluna] = pecaJogAtual
            limpaBtns()
            tiraAjuda()
            return
        }
        tabuleiro[linha][coluna] = pecaJogAtual

        if(CentCim){
            for(i in linha downTo tabPosicoesFinais[0][0]){
                tabuleiro[i][coluna] = pecaJogAtual
            }
        }

        if(CentCimDir){
            var j=coluna
            for(i in linha downTo tabPosicoesFinais[1][0]){
                tabuleiro[i][j] = pecaJogAtual
                j++
            }
        }

        if(CentDir){
            for(i in coluna until tabPosicoesFinais[2][1]){
                tabuleiro[linha][i] = pecaJogAtual
            }
        }

        if(CentBaiDir){
            var j = coluna
            for(i in linha until tabPosicoesFinais[3][0]){
                tabuleiro[i][j] = pecaJogAtual
                j++
            }
        }

        if(CentBai){
            for(i in linha until tabPosicoesFinais[4][0]){
                tabuleiro[i][coluna] = pecaJogAtual
            }
        }

        if(CentBaiEsq){
            var j = coluna
            for(i in linha until  tabPosicoesFinais[5][0]){
                tabuleiro[i][j] = pecaJogAtual
                j--
            }
        }

        if(CentEsq){
            for(i in coluna downTo tabPosicoesFinais[6][1]){
                tabuleiro[linha][i] = pecaJogAtual
            }
        }

        if(CentCimaEsq){
            var j=coluna
            for(i in linha downTo tabPosicoesFinais[7][0]){
                tabuleiro[i][j] = pecaJogAtual
                j--
            }
        }

  /*      limpaBtns()
        tiraAjuda()
        atualizaVista()
        verificaVencedor()*/
    }

    //Cliente
    fun jogoTerminou(op:Int){
        when(op) {
            EMPATE->{
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.empate)
                builder.setMessage(R.string.empateee)


                builder.setPositiveButton(R.string.novJog) { dialog, which ->
                    // preparaNovoJogo()
                    state.postValue(State.GAME_OVER)
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
            VENCEDOR_PECAS_34-> {
                    vezJogador1()

                    if (myMove) {
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
                            b.profileImageJog0.borderColor = Color.argb(255, 253, 253, 150)
                        }

                        builder.show()
                    } else {
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
                            b.profileImageJog0.borderColor = Color.argb(255, 253, 253, 150)
                        }

                        builder.show()
                    }
                }
                VENCEDOR_PECAS_35->{
                    vezJogador0()

                    if (myMove) {
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
                            b.profileImageJog0.borderColor = Color.argb(255, 253, 253, 150)
                        }

                        builder.show()
                    } else {
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
                            b.profileImageJog0.borderColor = Color.argb(255, 253, 253, 150)
                        }

                        builder.show()
                    }
                }
            }
    }


    fun preparaNovoJogo() {
        for(i in 0 until 8){
            for(j in 0 until 8){
                tabuleiro[i][j]=""
            }
        }

        sorteiaJogador()
        preparaTabuleiro()
    //    atualizaVista()
        //verificaVencedor()
        b.numPecasJog0.text = str.toString()
        b.numPecasJog1.text = str.toString()

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

        state.postValue(State.STARTING)
    }

    private fun vezJogador0(){
        b.profileImageJog0.borderColor = Color.BLUE
        b.profileImageJog1.borderColor = Color.argb(255,253,253,150)
    }

    private fun vezJogador1(){
        b.profileImageJog1.borderColor = Color.RED
        b.profileImageJog0.borderColor = Color.argb(255,253,253,150)
    }

    private fun preparaTabuleiro() {
        tabuleiro[3][3] = "\uD83D\uDD35"
        tabuleiro[4][3] = "\uD83D\uDD34"
        tabuleiro[3][4] = "\uD83D\uDD34"
        tabuleiro[4][4] = "\uD83D\uDD35"

        atualizaVista()
    }

    private fun atualizaVista() {
        b.btn00.text = tabuleiro[0][0];b.btn01.text = tabuleiro[0][1]; b.btn02.text = tabuleiro[0][2]; b.btn03.text = tabuleiro[0][3]; b.btn04.text = tabuleiro[0][4]; b.btn05.text = tabuleiro[0][5]; b.btn06.text = tabuleiro[0][6]; b.btn07.text = tabuleiro[0][7]
        b.btn10.text = tabuleiro[1][0]; b.btn11.text = tabuleiro[1][1]; b.btn12.text = tabuleiro[1][2]; b.btn13.text = tabuleiro[1][3]; b.btn14.text = tabuleiro[1][4]; b.btn15.text = tabuleiro[1][5]; b.btn16.text = tabuleiro[1][6]; b.btn17.text = tabuleiro[1][7]
        b.btn20.text = tabuleiro[2][0]; b.btn21.text = tabuleiro[2][1]; b.btn22.text = tabuleiro[2][2]; b.btn23.text = tabuleiro[2][3]; b.btn24.text = tabuleiro[2][4]; b.btn25.text = tabuleiro[2][5]; b.btn26.text = tabuleiro[2][6]; b.btn27.text = tabuleiro[2][7]
        b.btn30.text = tabuleiro[3][0]; b.btn31.text = tabuleiro[3][1]; b.btn32.text = tabuleiro[3][2]; b.btn33.text = tabuleiro[3][3]; b.btn34.text = tabuleiro[3][4]; b.btn35.text = tabuleiro[3][5]; b.btn36.text = tabuleiro[3][6]; b.btn37.text = tabuleiro[3][7]
        b.btn40.text = tabuleiro[4][0]; b.btn41.text = tabuleiro[4][1]; b.btn42.text = tabuleiro[4][2]; b.btn43.text = tabuleiro[4][3]; b.btn44.text = tabuleiro[4][4]; b.btn45.text = tabuleiro[4][5]; b.btn46.text = tabuleiro[4][6]; b.btn47.text = tabuleiro[4][7]
        b.btn50.text = tabuleiro[5][0]; b.btn51.text = tabuleiro[5][1]; b.btn52.text = tabuleiro[5][2]; b.btn53.text = tabuleiro[5][3]; b.btn54.text = tabuleiro[5][4]; b.btn55.text = tabuleiro[5][5]; b.btn56.text = tabuleiro[5][6]; b.btn57.text = tabuleiro[5][7]
        b.btn60.text = tabuleiro[6][0]; b.btn61.text = tabuleiro[6][1]; b.btn62.text = tabuleiro[6][2]; b.btn63.text = tabuleiro[6][3]; b.btn64.text = tabuleiro[6][4]; b.btn65.text = tabuleiro[6][5]; b.btn66.text = tabuleiro[6][6]; b.btn67.text = tabuleiro[6][7]
        b.btn70.text = tabuleiro[7][0]; b.btn71.text = tabuleiro[7][1]; b.btn72.text = tabuleiro[7][2]; b.btn73.text = tabuleiro[7][3]; b.btn74.text = tabuleiro[7][4]; b.btn75.text = tabuleiro[7][5]; b.btn76.text = tabuleiro[7][6]; b.btn77.text = tabuleiro[7][7]
    }


    private fun sorteiaJogador() {
        if(Random.nextInt(0,2)==1) {
            pecaJogAtual = pecaJog1
            b.profileImageJog1.borderColor = Color.RED
            b.profileImageJog0.borderColor = Color.argb(255,253,253,150)
        }
        else {
            pecaJogAtual = pecaJog0
            b.profileImageJog0.borderColor = Color.BLUE
            b.profileImageJog1.borderColor = Color.argb(255,253,253,150)
        }
    }



    private fun jogadaBomba(linha: Int, coluna:Int){
        var col=coluna-1;
        var lin=linha-1;

        for(i in 0 until 3){
            for(j in 0 until 3){
                if((lin>=0 && lin<=7) && (col>=0 && col<=7)){
                    if(lin!=linha || col!=coluna) {
                        tabuleiro[lin][col] = ""
                    }
                }
                col++
            }
            lin+=1
            col=coluna-1
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
                if(bombaSelec) {
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

    private fun trocaPecas(){
        for(i in 0 until 3){
            if(tabuleiro[pecas[i][0]][pecas[i][1]]==pecaJog0){
                tabuleiro[pecas[i][0]][pecas[i][1]]=pecaJog1
            }
            else{
                tabuleiro[pecas[i][0]][pecas[i][1]]=pecaJog0
            }
        }


    }

    private fun selecionaBotao(btn: Button, linha : Int, coluna : Int){
        btn.isSelected = true
        pecas[pecasSelecionadas][0] = linha
        pecas[pecasSelecionadas][1] = coluna
        selectedBtns[pecasSelecionadas] = btn
        pecasSelecionadas++
    }

    private fun selecionaPeca(btn: Button, linha: Int, coluna:Int) {

        if (btn.isSelected) {
            btn.isSelected = false
            pecasSelecionadas--
        }

        if (myMove) {
            if (pecasSelecionadas < 2) {
                if (!jogTroca) {
                    if (btn.text == pecaJogAtual && !btn.isSelected) {
                        selecionaBotao(btn,linha,coluna)
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
                    if (btn.text == pecaJog1 && pecasSelecionadas == 2 && !btn.isSelected) {
                        selecionaBotao(btn,linha,coluna)
                    }
                    else{
                        Toast.makeText(
                            applicationContext,
                            R.string.selecione_peca_adversaria,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    if (btn.text == pecaJog0 && pecasSelecionadas == 2 && !btn.isSelected) {
                        selecionaBotao(btn,linha,coluna)
                    }
                    else{
                        Toast.makeText(
                            applicationContext,
                            R.string.selecione_peca_adversaria,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
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
                    limpaBtns()
                    jogTroca=true
        /*            trocaPecas()
                    atualizaVista()
                    atualizaContador()
                    trocaJogador()*/
                }.show()
        }
    }

    private fun atualizaContador() {
        var pjog0 = 0
        var pjog1 = 0

        for (i in 0 until 8) {
            for (j in 0 until 8) {

                if (tabuleiro[i][j] == pecaJog0)
                    pjog0++

                if (tabuleiro[i][j] == pecaJog1) {
                    pjog1++
                }

            }
        }

        b.numPecasJog0.text = pjog0.toString()
        b.numPecasJog1.text = pjog1.toString()
    }

    fun enviaJogada(socketO: OutputStream?,operacao:Int,linha: Int,coluna: Int) {
        if (connectionState.value != ConnectionState.CONNECTION_ESTABLISHED)
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
        state.postValue(State.PLAYING_OTHER)
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
        if (connectionState.value != ConnectionState.CONNECTION_ESTABLISHED)
            return

        Log.i("TAG","Troca peças")

        socketO?.run {
            thread {
                try {
                    var data=Jogada(DADOS)
                    data.nome=ImageUtils.nomeJogador
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



    private fun acionaListeners(){
        //BOTÕES ESPECIAIS

        b.btnPassaVez.setOnClickListener{
           // trocaJogador()
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
    }


}