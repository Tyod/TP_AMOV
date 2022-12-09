package pt.isec.andreiagraca.amov_tp.activities

import android.app.AlertDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pt.isec.andreiagraca.amov_tp.R
import pt.isec.andreiagraca.amov_tp.databinding.ActivityModo2Binding
import pt.isec.andreiagraca.amov_tp.databinding.ActivityModo3Binding
import pt.isec.ans.rascunhos.utils.ImageUtils
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.random.Random



class ServidorModo2Model: ViewModel() {
    lateinit var b: ActivityModo2Binding
    lateinit var cliente: Modo2Activity

    private var dlg: AlertDialog? = null

    val state = MutableLiveData(Modo2Activity.State.STARTING)
    val connectionState = MutableLiveData(Modo2Activity.ConnectionState.SETTING_PARAMETERS)

    private var serverSocket: ServerSocket? = null

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

    private var threadComm1: Thread? = null
    private var threadComm2: Thread? = null

    private var nomeJo0:String?=null
    private var nomeJog1:String?=null

    private var imagemJog0: String?=null
    private var imagemJog1: String?=null

    val pecaJog0 = "\uD83D\uDD35" //Blue
    val pecaJog1 = "\uD83D\uDD34" //Red
    var pecaJogAtual: String = ""

    var bombaSelec: Boolean = false
    var troca: Boolean = false
    var passaVez: Boolean = false

    var jog0bomba: Boolean = false
    var jog1bomba: Boolean = false
    var jogBomba: Boolean = false

    var jog0troca: Boolean = false
    var jog1troca: Boolean = false
    var jogTroca: Boolean = false

    var CentCim: Boolean = true     //Cima
    var CentCimDir: Boolean = true  //Cima à direita
    var CentDir: Boolean = true     //Direita
    var CentBaiDir: Boolean = true  //Direita em baixo
    var CentBai: Boolean = true     //Baixo
    var CentBaiEsq: Boolean = true  //Baixo à esquerda
    var CentEsq: Boolean = true     //Esquerda
    var CentCimaEsq: Boolean = true //Cima à esquerda

    var clientes: Int = 0

    var tabuleiro = Array(10) { Array(10) { "" } }
    var tabPosicoesFinais = Array(10) { Array(2) { 0 } }


    var pecasSelecionadas: Int = 0
    var pecas = Array(3) { Array(2) { 0 } }
    var btn: Button? = null
    var selectedBtns = Array(3) { btn }
    var nAjudas: Int = 0
    var ajudaBtns = Array(64) { btn }
    var ajudas = Array(64) { Array(2) { 0 } }

    var str: Int = 0
    var jogoAcabou = false
    var terminou = false


    fun startServer() {
        if (serverSocket != null ||
            socketC1 != null || socketC2 != null ||
            connectionState.value != Modo2Activity.ConnectionState.SETTING_PARAMETERS
        )
            return


        connectionState.postValue(Modo2Activity.ConnectionState.SERVER_CONNECTING)
        Log.i("START SERVER", connectionState.value.toString())


        preparaJogo()

        thread {

            serverSocket = ServerSocket(SERVER_PORT)
            serverSocket?.apply {
                try {
                    while (clientes < 2) {
                        val newSocket = serverSocket!!.accept()
                        clientes++
                        atendeCliente(newSocket)
                        cliente.connectionState.postValue(Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED)
                        Log.i("CLIENT ACCEPTED", "CLIENT ACCEPTED")
                    }
                } catch (_: Exception) {
                    connectionState.postValue(Modo2Activity.ConnectionState.CONNECTION_ERROR)
                } finally {
                    serverSocket?.close()
                    serverSocket = null
                }
            }
        }
    }

    fun stopServer() {
        serverSocket?.close()
        connectionState.postValue(Modo2Activity.ConnectionState.CONNECTION_ENDED)
        serverSocket = null
    }

    fun repoePontuacao(){
        val db = Firebase.firestore
        val v = db.collection("Modo2").document("IehOdQwSDgCmU1IoC7Mt")
        db.runTransaction {transaction ->
            transaction.update(v,"Jogador1",0)
            transaction.update(v,"Jogador2",0)

            null
        }
    }


    fun onActualizarDadosJog0() { //azul

        val db = Firebase.firestore
        val v = db.collection("Modo2").document("OPQQAFbOzjvxKNnooQL3")
        db.runTransaction {transaction ->
            val doc = transaction.get(v)
            val vitoria = doc.getLong("Jogador1")!! + 1
            transaction.update(v,"Jogador1",vitoria)

            null
        }
    }

    fun onActualizarDadosJog1() { //vermelho

        val db = Firebase.firestore
        val v = db.collection("Modo2").document("OPQQAFbOzjvxKNnooQL3")
        db.runTransaction {transaction ->
            val doc = transaction.get(v)
            val vitoria = doc.getLong("Jogador2")!! + 1
            transaction.update(v,"Jogador2",vitoria)

            null
        }
    }



    fun atendeCliente(newSocket: Socket) {
        var threadC = thread {
            var meuCliente=clientes

            if(meuCliente==1) {
                socketC1 = newSocket
            }
            else{
                    socketC2 = newSocket
                    enviaSorteio(SORTEIA_JOGADOR)
            }

            connectionState.postValue(Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED)
            Log.i("CONNECTION",connectionState.value.toString())

            try {
                if(meuCliente==1) {
                    if (socketIC1 == null)
                        return@thread
                }
                else{
                        if (socketIC2 == null)
                            return@thread
                }

                //connectionState.setValue(ConnectionState.CONNECTION_ESTABLISHED)
                //Log.i("CONNECTION",connectionState.value.toString())
                var bufI:BufferedReader?=null
                if(meuCliente==1)
                    bufI = socketIC1!!.bufferedReader()
                else{
                        bufI = socketIC2!!.bufferedReader()
                }

                Log.i("TAG","CLiente "+meuCliente)

                while (state.value != Modo2Activity.State.GAME_OVER) {
                    Log.i("TAG","Servidor em espera do Cliente "+meuCliente)


                    var message = bufI.readLine()
                    var obj=Json.decodeFromString<Jogada>(message)
                    val op=obj.op
                    Log.i("TAG","Mensagem recebida do Cliente "+meuCliente)


                    if(op==DADOS){
                        var nome=obj.nome!!
                        var imagem=obj.imagem

                        when(meuCliente){
                            1->{
                                nomeJo0=nome
                                imagemJog0=imagem

                                enviaDados(socketOC2,nomeJo0!!,imagemJog0!!,pecaJog0)
                            }
                            2->{
                                nomeJog1=nome
                                imagemJog1=imagem

                                enviaDados(socketOC1,nomeJog1!!,imagemJog1!!,pecaJog1)
                            }
                        }
                    }
                    if (op == JOGADA) {
                        var linha=obj.linha!!
                        var coluna=obj.coluna!!
                        val bomba=obj.jogadaEspecial!!

                        if (bomba) {
                            if ((meuCliente==1 &&!jog0bomba) || (meuCliente==2 && !jog1bomba)) {
                                atualizaTabuleiro(linha, coluna)
                                jogadaBomba(linha, coluna)

                                if(meuCliente==1){
                                    jog0bomba=true
                                    enviaTabuleiro(socketOC1, TABULEIRO,true)
                                    enviaTabuleiro(socketOC2, TABULEIRO,false)
                                }
                                else{
                                    jog1bomba=true
                                    enviaTabuleiro(socketOC1, TABULEIRO,false)
                                    enviaTabuleiro(socketOC2, TABULEIRO,true)
                                }

                                verificaVencedor()

                            }

                        }
                        else {
                            if (confirmaPosicaoValida(linha, coluna)) {
                                atualizaTabuleiro(linha, coluna)
                                enviaTabuleiro(socketOC1, TABULEIRO, false)
                                enviaTabuleiro(socketOC2, TABULEIRO, false)
                                verificaVencedor()

                                Log.i("Servidor", "TURN: " + TURN)

                            } else {
                                Log.i("Posicao Invalida", "Posicao invalida")
                            }
                        }
                    }
                    if(op== TROCA_PECAS){
                        if((meuCliente==1 && !jog0troca) || (meuCliente==2 && !jog1troca)) {
                            pecas = obj.pecas!!
                            trocaPecas()

                            if(meuCliente==1) {
                                enviaTabuleiro(socketOC1, TROCA_PECAS,true)
                                enviaTabuleiro(socketOC2, TROCA_PECAS,false)
                            }
                            else{
                                enviaTabuleiro(socketOC1, TROCA_PECAS,false)
                                enviaTabuleiro(socketOC2, TROCA_PECAS,true)
                            }

                            verificaVencedor()
                        }
                    }
                    if(op== PASSA_VEZ){
                            Log.i("TAG","Troca de jogador")
                    }
                    if(op== AJUDA_JOGADA){
                        ajudaJogada()
                        if(meuCliente==1)
                            enviaTabuleiro(socketOC1, AJUDA_JOGADA,false)
                        else{
                            enviaTabuleiro(socketOC2, AJUDA_JOGADA,false)
                        }

                    }

                    Log.i("STATE",""+state.value)
                    if(op!= AJUDA_JOGADA && op!=DADOS && !terminou) {
                        Log.i("STATE","FINISH: "+state.value)

                        Thread.sleep(500)
                        trocaJogador()

                        if(pecaJogAtual==pecaJog0) {
                            if(troca)
                                vezDeJogar(socketOC1, VEZ_JOGADOR, true, pecaJog0,troca)
                            else
                                vezDeJogar(socketOC1, VEZ_JOGADOR, true, pecaJog0,false)

                            vezDeJogar(socketOC2, VEZ_JOGADOR,false,pecaJog1,false)
                        }
                        else{
                                vezDeJogar(socketOC1, VEZ_JOGADOR, false, pecaJog0,false)
                                if(troca)
                                    vezDeJogar(socketOC2, VEZ_JOGADOR,true,pecaJog1,troca)
                                else
                                    vezDeJogar(socketOC2, VEZ_JOGADOR,true,pecaJog1,false)

                        }
                    }
            }
            } catch (e : Exception) {
                Log.i("EXCEPTION","Exception: Atende cliente 1 "+e)
            } finally {
                Log.i("FINNALLY","Atende cliente 1")

                // stopGame()
            }
        }
    }

    fun preparaJogo() {
            btn = b.btn00
            selectedBtns = Array(3) { btn }
            sorteiaJogador()
            preparaTabuleiro()

            state.postValue(Modo2Activity.State.PLAYING_BOTH)
    }

    private fun enviaSorteio(op:Int){
        if (pecaJogAtual==pecaJog0) {
            vezDeJogar(socketOC1, op,true,pecaJogAtual,false)
            vezDeJogar(socketOC2, op,false,pecaJog1,false)
        }
        else {
            vezDeJogar(socketOC2, op,true,pecaJogAtual,false)
            vezDeJogar(socketOC1, op,false,pecaJog0,false)
        }
    }

    private fun sorteiaJogador() {
        var temp = Random.nextInt(0,2)
        if(temp==0) {
            vezJogador0()
        }
        if(temp==1){
            vezJogador1()
        }
    }

    private fun preparaTabuleiro() {
        tabuleiro[3][3] = "\uD83D\uDD35"
        tabuleiro[4][3] = "\uD83D\uDD34"
        tabuleiro[3][4] = "\uD83D\uDD34"
        tabuleiro[4][4] = "\uD83D\uDD35"

        //   atualizaVista()
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
/*
        if(pecaJogAtual==pecaJog0)
            jog0bomba=true
        else
            jog1bomba=true

        bombaSelec=false*/
    }

    private fun ajudaCantoSupEsq(linha:Int, coluna:Int,jogAdv:String){
        if(linha-2<0)
            return
        if(coluna-2<0)
            return


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

    private fun ajudaEsq(linha:Int, coluna:Int, jogAdv:String){
        if(coluna-2<0)
            return


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

    private fun ajudaInfEsq(linha:Int, coluna:Int, jogAdv:String){
        if(linha+2>7)
            return
        if(coluna-2<0)
            return


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

    private fun ajudaAbaixo(linha:Int, coluna:Int, jogAdv:String){
        if(linha+2>7)
            return


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

    private fun ajudaInfDir(linha:Int, coluna:Int ,jogAdv:String){
        if(linha+2>7)
            return
        if(coluna+2>7)
            return


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

    private fun ajudaDir(linha:Int, coluna:Int ,jogAdv:String){
        if(coluna+2>7)
            return

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

    private fun ajudaSupDir(linha:Int, coluna:Int ,jogAdv:String){
        if(linha-2<0)
            return
        if(coluna+2>7)
            return


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

    private fun ajudaAcima(linha:Int, coluna:Int ,jogAdv:String){
        if(linha-2<0)
            return

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

        var jogAdv:String?=null

        if(pecaJogAtual==pecaJog0)
            jogAdv=pecaJog1
        else
            jogAdv=pecaJog0

        for(i in 0 until 8){
            for(j in 0 until 8){
                if(tabuleiro[i][j]==pecaJogAtual){
                    ajudaCantoSupEsq(i,j,jogAdv)
                    ajudaEsq(i,j,jogAdv)
                    ajudaInfEsq(i,j,jogAdv)
                    ajudaAbaixo(i,j,jogAdv)
                    ajudaInfDir(i,j,jogAdv)
                    ajudaDir(i,j,jogAdv)
                    ajudaSupDir(i,j,jogAdv)
                    ajudaAcima(i,j,jogAdv)
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

    private fun tiraAjuda(){
        for(i in 0 until nAjudas){
            ajudaBtns[i]?.isActivated=false
        }

        nAjudas=0
    }

    private fun vezJogador0(){
        pecaJogAtual = pecaJog0
    }

    private fun vezJogador1(){
        pecaJogAtual = pecaJog1
    }

    private fun trocaJogador(){
        var jogoTerminou=false

        ajudaJogada()
        if(nAjudas==0)
            jogoTerminou=true
        tiraAjuda()

        if(pecaJogAtual == pecaJog1) {
            vezJogador0()
        }
        else {
            vezJogador1()
        }

        ajudaJogada()

        if(nAjudas==0 && jogoTerminou==true) {
            tiraAjuda()
            jogoAcabou=true
            verificaVencedor()
        }

        if(nAjudas==0)
        {
            troca=true
        }
        else
            troca=false
        // else
        //     b.btnPassaVez.isEnabled=false


        tiraAjuda()
    }

    private fun trocaPecas(){
        var minhaPeca=tabuleiro[pecas[0][0]][pecas[0][1]]

        for(i in 0 until 2){
            tabuleiro[pecas[i][0]][pecas[i][1]]=tabuleiro[pecas[2][0]][pecas[2][1]]
        }

        tabuleiro[pecas[2][0]][pecas[2][1]]=minhaPeca

        limpaBtns()

        if(pecaJogAtual==pecaJog0){
            jog0troca=true
        }
        else {
            jog1troca = true
        }

    }


    fun verificaVencedor() {
        var pjog0=0
        var pjog1=0
        var parcelVazias = false

        cliente!!.onCriarDados()

        for(i in 0 until 8){
            for(j in 0 until 8){

                if(tabuleiro[i][j]=="")
                    parcelVazias=true

                if(tabuleiro[i][j]==pecaJog0)
                    pjog0++

                if(tabuleiro[i][j]==pecaJog1){
                    pjog1++
                }

            }
        }

        //    b.numPecasJog0.text = pjog0.toString()
        //    b.numPecasJog1.text = pjog1.toString()

        if(pjog0==pjog1 && jogoAcabou){
            Log.i("TAG","Empate")
            terminou=true
            state.postValue(Modo2Activity.State.GAME_OVER)
            enviaTabuleiro(socketOC1,EMPATE,false)
            enviaTabuleiro(socketOC2,EMPATE,false)
            return
        }

        if(pjog0 == 0){
            Log.i("TAG","JOGO TERMINOU PECAS 34")
            terminou=true
            onActualizarDadosJog1()
            state.postValue(Modo2Activity.State.GAME_OVER)
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_34,false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_34,false)
            return
        }

        if(pjog1 == 0){
            Log.i("TAG","JOGO TERMINOU PECAS 35")
            terminou=true
            onActualizarDadosJog0()
            state.postValue(Modo2Activity.State.GAME_OVER)
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_35,false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_35,false)
            return
        }

        if(!parcelVazias && pjog0>pjog1){
            Log.i("TAG","JOGO TERMINOU PECAS 35")
            terminou=true
            onActualizarDadosJog0()
            state.postValue(Modo2Activity.State.GAME_OVER)
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_35,false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_35,false)
            return
        }

        if(!parcelVazias && pjog1>pjog0){
            Log.i("TAG","JOGO TERMINOU PECAS 34")
            terminou=true
            onActualizarDadosJog1()
            state.postValue(Modo2Activity.State.GAME_OVER)
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_34,false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_34,false)
            return
        }
        if(parcelVazias && pjog0>pjog1 && jogoAcabou){
            Log.i("TAG","JOGO TERMINOU PECAS 35")
            terminou=true
            onActualizarDadosJog0()
            state.postValue(Modo2Activity.State.GAME_OVER)
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_35,false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_35,false)
            return
        }

        if(parcelVazias && pjog1>pjog0 && jogoAcabou){
            Log.i("TAG","JOGO TERMINOU PECAS 34")
            terminou=true
            onActualizarDadosJog1()
            state.postValue(Modo2Activity.State.GAME_OVER)
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_34,false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_34,false)
            return
        }
    }

    fun vezDeJogar(socketO: OutputStream?,op:Int,vez:Boolean,minhaPeca:String,trocaPecas:Boolean){
        if (connectionState.value != Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED)
            return


        socketO?.run {
            thread {
                try {
                    var data=Jogada(op)
                    data.pecaJogAtual=pecaJogAtual
                    data.minhaPeca=minhaPeca
                    data.vez=vez
                    data.troca=trocaPecas
                    Log.i("TAG","@12")

                    var str= Json.encodeToString(data)
                    Log.i("TAG","@13")
                    val printStream = PrintStream(this)
                    printStream.println(str)
                    Log.i("TAG","@14")
                    printStream.flush()
                } catch (e: Exception) {
                    Log.i("Exception",e.toString())
                    //stopGame()
                }
                catch (e: java.lang.Exception){
                    Log.i("TAG","Error parsing JSON")
                }
            }
        }

        Log.i("TAG","Vez de jogar enviada")
    }

    fun enviaTabuleiro(socketO: OutputStream?,op:Int,jogadaEspecial:Boolean){
        if (connectionState.value != Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED)
            return

        Log.i("TAG","Envia Tabuleiro")

        socketO?.run {
            thread {
                try {
                    var data=Jogada(op)
                    data.tabuleiro=tabuleiro

                    if(op== TABULEIRO || op== TROCA_PECAS){
                        data.jogadaEspecial=jogadaEspecial
                    }
                    else{
                        if(op== AJUDA_JOGADA)
                        {
                            data.nAjudas=nAjudas
                            data.ajudas=ajudas
                        }
                    }
                    Log.i("TAG","@15")

                    var str= Json.encodeToString(data)
                    Log.i("TAG","@16")
                    val printStream = PrintStream(this)
                    printStream.println(str)
                    Log.i("TAG","@17")
                    printStream.flush()
                    Log.i("TAG","Tabuleiro enviado")
                } catch (e: Exception) {
                    Log.i("Exception", e.toString())
                    //stopGame()
                }
            }
        }
    }

    fun enviaDados(socketO: OutputStream?,nomeJog:String, imagemJog: String, pecaJog:String){
        if (connectionState.value != Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED)
            return

        Log.i("TAG","Envia Dados")

        socketO?.run {
            thread {
                try {
                    var data=Jogada(DADOS)
                    data.nome= nomeJog
                    data.imagem= imagemJog
                    data.minhaPeca=pecaJog

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

}