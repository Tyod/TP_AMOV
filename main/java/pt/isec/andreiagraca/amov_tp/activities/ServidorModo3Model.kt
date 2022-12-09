package pt.isec.andreiagraca.amov_tp.activities

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import pt.isec.andreiagraca.amov_tp.R
import pt.isec.andreiagraca.amov_tp.databinding.ActivityModo2Binding
import pt.isec.andreiagraca.amov_tp.databinding.ActivityModo3Binding
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.random.Random

class ServidorModo3Model  : ViewModel(){
    lateinit var b: ActivityModo3Binding
    lateinit var cliente: Modo3Activity


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

    private var socketC3: Socket? = null
    private val socketIC3: InputStream?
        get() = socketC3?.getInputStream()
    private val socketOC3: OutputStream?
        get() = socketC3?.getOutputStream()

    private var threadComm1: Thread? = null
    private var threadComm2: Thread? = null
    private var threadComm3: Thread? = null


    val pecaJog0 = "\uD83D\uDD35" //Blue
    val pecaJog1 = "\uD83D\uDD34" //Red
    val pecaJog2 = "\uD83D\uDFE2" //Green
    var pecaJogAtual : String = ""

    var bombaSelec:Boolean=false
    var troca:Boolean=false
    var passaVez:Boolean=false

    private var nomeJog0:String?=null
    private var nomeJog1:String?=null
    private var nomeJog2:String?=null

    private var imagemJog0: String?=null
    private var imagemJog1: String?=null
    private var imagemJog2: String?=null

    private var advs1:Boolean=false
    private var advs2:Boolean=false
    private var advs3:Boolean=false

    var jog0bomba:Boolean=false
    var jog1bomba:Boolean=false
    var jog2bomba:Boolean=false
    var jogBomba:Boolean=false

    var jog0troca:Boolean=false
    var jog1troca:Boolean=false
    var jog2troca:Boolean=false
    var jogTroca:Boolean=false

    var CentCim : Boolean = true     //Cima
    var CentCimDir : Boolean = true  //Cima à direita
    var CentDir : Boolean = true     //Direita
    var CentBaiDir : Boolean = true  //Direita em baixo
    var CentBai : Boolean = true     //Baixo
    var CentBaiEsq : Boolean = true  //Baixo à esquerda
    var CentEsq : Boolean = true     //Esquerda
    var CentCimaEsq : Boolean = true //Cima à esquerda

    var clientes:Int=0

    var tabuleiro = Array(10) {Array(10) {""} }
    var tabPosicoesFinais = Array(10) { Array(2) {0} }


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



    fun startServer() {
        if (serverSocket != null ||
            socketC1 != null || socketC2!=null ||
            connectionState.value != Modo2Activity.ConnectionState.SETTING_PARAMETERS)
            return


        connectionState.postValue(Modo2Activity.ConnectionState.SERVER_CONNECTING)
        Log.i("START SERVER",connectionState.value.toString())


        preparaJogo()

        thread {

            serverSocket = ServerSocket(SERVER_PORT)
            serverSocket?.apply {
                try {
                    while(clientes<3) {
                        val newSocket = serverSocket!!.accept()
                        clientes++
                        atendeCliente(newSocket)
                        cliente.connectionState.postValue(Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED)
                        Log.i("CLIENT ACCEPTED","CLIENT ACCEPTED")
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
        val v = db.collection("Modo3").document("IehOdQwSDgCmU1IoC7Mt")
        db.runTransaction {transaction ->
            transaction.update(v,"Jogador1",0)
            transaction.update(v,"Jogador2",0)

            null
        }
    }


    fun onActualizarDadosJog0() { //azul

        val db = Firebase.firestore
        val v = db.collection("Modo3").document("knrV9FAet0HxFxmoGvw8")
        db.runTransaction {transaction ->
            val doc = transaction.get(v)
            val vitoria = doc.getLong("Jogador1")!! + 1
            transaction.update(v,"Jogador1",vitoria)

            null
        }
    }

    fun onActualizarDadosJog1() { //vermelho

        val db = Firebase.firestore
        val v = db.collection("Modo3").document("knrV9FAet0HxFxmoGvw8")
        db.runTransaction {transaction ->
            val doc = transaction.get(v)
            val vitoria = doc.getLong("Jogador2")!! + 1
            transaction.update(v,"Jogador2",vitoria)

            null
        }
    }

    fun onActualizarDadosJog2() { //verde

        val db = Firebase.firestore
        val v = db.collection("Modo3").document("knrV9FAet0HxFxmoGvw8")
        db.runTransaction {transaction ->
            val doc = transaction.get(v)
            val vitoria = doc.getLong("Jogador3")!! + 1
            transaction.update(v,"Jogador3",vitoria)

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
            if(meuCliente==2) {
                socketC2 = newSocket
            }
            else {
                socketC3 = newSocket
                enviaSorteio(SORTEIA_JOGADOR)

            }
        }

        connectionState.postValue(Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED)
        Log.i("CONNECTION",connectionState.value.toString())


            try {
                if(meuCliente==1) {
                    if (socketIC1 == null)
                        return@thread
                }
                else{
                    if(meuCliente==2){
                        if (socketIC2 == null)
                            return@thread
                    }
                    else{
                        if (socketIC3 == null)
                            return@thread
                    }
                }

                //connectionState.setValue(ConnectionState.CONNECTION_ESTABLISHED)
                //Log.i("CONNECTION",connectionState.value.toString())
                var bufI:BufferedReader?=null
                if(meuCliente==1)
                bufI = socketIC1!!.bufferedReader()
                else{
                    if(meuCliente==2)
                        bufI = socketIC2!!.bufferedReader()
                    else
                        bufI = socketIC3!!.bufferedReader()
                }

                Log.i("TAG","CLiente "+meuCliente)

                while (state.value != Modo2Activity.State.GAME_OVER) {
                    Log.i("TAG","Servidor em espera do Cliente "+meuCliente)


                    //var myObj=bufI.read
                    var message = bufI.readLine()
                    var obj=Json.decodeFromString<Jogada>(message)
                    val op=obj.op
                    Log.i("TAG","Mensagem recebida do Cliente "+meuCliente)
                    //Log.i("ERROR",linha.toString())
                    //var coluna= bufI.read()
                    //Log.i("ERROR",coluna.toString())


                    if(op==DADOS){
                        var nome=obj.nome!!
                        var imagem=obj.imagem!!

                        Log.i("TAG","Mensagem recebida do Cliente "+meuCliente)

                        when(meuCliente){
                            1->{
                                nomeJog0=nome
                                imagemJog0=imagem

                            }
                            2->{
                                nomeJog1=nome
                                imagemJog1=imagem

                            }
                            3->{
                                nomeJog2=nome
                                imagemJog2=imagem

                            }
                        }
                        if(nomeJog1!=null && nomeJog2!=null && !advs1) {
                            enviaDados(socketOC1, nomeJog1!!, imagemJog1!!, pecaJog1,nomeJog2!!,imagemJog2!!,pecaJog2)
                            advs1=true
                        }
                        if(nomeJog0!=null && nomeJog2!=null &&!advs2){
                            enviaDados(socketOC2, nomeJog0!!, imagemJog0!!, pecaJog0,nomeJog2!!,imagemJog2!!,pecaJog2)
                            advs2=true
                        }
                        if(nomeJog0!=null && nomeJog1!=null && !advs3){
                            enviaDados(socketOC3, nomeJog0!!, imagemJog0!!, pecaJog0,nomeJog1!!,imagemJog1!!,pecaJog1)
                            advs3=true
                        }
                    }
                    if (op == JOGADA) {
                        var linha=obj.linha!!
                        var coluna=obj.coluna!!
                        val bomba=obj.jogadaEspecial!!

                        if (bomba) {
                            if ((meuCliente==1 &&!jog0bomba) || (meuCliente==2 && !jog1bomba) ||(meuCliente==3 && !jog2bomba)) {
                                atualizaTabuleiro(linha, coluna)
                                jogadaBomba(linha, coluna)

                                if(meuCliente==1){
                                    jog0bomba=true
                                    enviaTabuleiro(socketOC1, TABULEIRO,true)
                                    enviaTabuleiro(socketOC2, TABULEIRO,false)
                                    enviaTabuleiro(socketOC3, TABULEIRO,false)
                                }
                                else{
                                    if(meuCliente==2){
                                        jog1bomba=true
                                        enviaTabuleiro(socketOC1, TABULEIRO,false)
                                        enviaTabuleiro(socketOC2, TABULEIRO,true)
                                        enviaTabuleiro(socketOC3, TABULEIRO,false)
                                    }
                                    else{
                                        jog2bomba=true
                                        enviaTabuleiro(socketOC1, TABULEIRO,false)
                                        enviaTabuleiro(socketOC2, TABULEIRO,false)
                                        enviaTabuleiro(socketOC3, TABULEIRO,true)
                                    }
                                }

                                verificaVencedor()

                            }

                        }
                        else {
                            if (confirmaPosicaoValida(linha, coluna)) {
                                atualizaTabuleiro(linha, coluna)
                                enviaTabuleiro(socketOC1, TABULEIRO, false)
                                enviaTabuleiro(socketOC2, TABULEIRO, false)
                                enviaTabuleiro(socketOC3, TABULEIRO, false)
                                verificaVencedor()

                                Log.i("Servidor", "TURN: " + TURN)

                            } else {
                                Log.i("Posicao Invalida", "Posicao invalida")
                            }
                        }
                    }
                    if(op== TROCA_PECAS){
                        if((meuCliente==1 && !jog0troca) || (meuCliente==2 && !jog1troca) || (meuCliente==3 && !jog2troca)) {
                            pecas = obj.pecas!!
                            trocaPecas()

                            if(meuCliente==1) {
                                enviaTabuleiro(socketOC1, TROCA_PECAS,true)
                                enviaTabuleiro(socketOC2, TROCA_PECAS,false)
                                enviaTabuleiro(socketOC3, TROCA_PECAS,false)
                            }
                            else{
                                if(meuCliente==2){
                                    enviaTabuleiro(socketOC1, TROCA_PECAS,false)
                                    enviaTabuleiro(socketOC2, TROCA_PECAS,true)
                                    enviaTabuleiro(socketOC3, TROCA_PECAS,false)
                                }
                                else{
                                    enviaTabuleiro(socketOC1, TROCA_PECAS,false)
                                    enviaTabuleiro(socketOC2, TROCA_PECAS,false)
                                    enviaTabuleiro(socketOC3, TROCA_PECAS,true)
                                }
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
                            if(meuCliente==2)
                                enviaTabuleiro(socketOC2, AJUDA_JOGADA,false)
                            else
                                enviaTabuleiro(socketOC3, AJUDA_JOGADA,false)
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
                            vezDeJogar(socketOC3, VEZ_JOGADOR,false,pecaJog2,false)
                        }
                        else{
                            if(pecaJogAtual==pecaJog1){
                                vezDeJogar(socketOC1, VEZ_JOGADOR, false, pecaJog0,false)
                                if(troca)
                                vezDeJogar(socketOC2, VEZ_JOGADOR,true,pecaJog1,troca)
                                else
                                    vezDeJogar(socketOC2, VEZ_JOGADOR,true,pecaJog1,false)

                                vezDeJogar(socketOC3, VEZ_JOGADOR,false,pecaJog2,false)
                            }
                            else{
                                vezDeJogar(socketOC1, VEZ_JOGADOR, false, pecaJog0,false)
                                vezDeJogar(socketOC2, VEZ_JOGADOR,false,pecaJog1,false)
                                if(troca)
                                vezDeJogar(socketOC3, VEZ_JOGADOR,true,pecaJog2,troca)
                                else
                                    vezDeJogar(socketOC3, VEZ_JOGADOR,true,pecaJog2,false)
                            }
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
        preparaTabuleiro()

        state.postValue(Modo2Activity.State.PLAYING_BOTH)
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

        limpaBtns()
        tiraAjuda()
        verificaVencedor()
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
        if(linha!=9 && linha!=0 && coluna!=0 && coluna!=9){
            if(tabuleiro[linha+1][coluna]=="" && tabuleiro[linha+1][coluna+1]=="" && tabuleiro[linha][coluna+1]==""
                && tabuleiro[linha-1][coluna+1]=="" && tabuleiro[linha-1][coluna]=="" && tabuleiro[linha-1][coluna-1]==""
                && tabuleiro[linha][coluna-1]=="" && tabuleiro[linha+1][coluna-1]=="")
                return false
        }
        else{
            if(linha==0 && coluna!=0 && coluna!=9) {
                if(tabuleiro[linha+1][coluna]=="" && tabuleiro[linha+1][coluna+1]=="" && tabuleiro[linha][coluna+1]==""
                    && tabuleiro[linha][coluna-1]=="" && tabuleiro[linha+1][coluna-1]=="")
                    return false
            }


            if(coluna==0 && linha!=0 && linha!=9){
                if(tabuleiro[linha+1][coluna]=="" && tabuleiro[linha+1][coluna+1]=="" && tabuleiro[linha][coluna+1]==""
                    && tabuleiro[linha-1][coluna+1]=="" && tabuleiro[linha-1][coluna]=="")
                    return false
            }


            if(linha==9 && coluna!=0 && coluna!=9){
                if(tabuleiro[linha][coluna+1]=="" && tabuleiro[linha-1][coluna+1]=="" && tabuleiro[linha-1][coluna]==""
                    && tabuleiro[linha-1][coluna-1]=="" && tabuleiro[linha][coluna-1]=="")
                    return false
            }


            if(coluna==9 && linha!=0 && linha!=9){
                if(tabuleiro[linha+1][coluna]=="" && tabuleiro[linha-1][coluna]=="" && tabuleiro[linha-1][coluna-1]==""
                    && tabuleiro[linha][coluna-1]=="" && tabuleiro[linha+1][coluna-1]=="")
                    return false
            }


            if(linha==0 && coluna==0){
                if(tabuleiro[linha+1][coluna]=="" && tabuleiro[linha+1][coluna+1]=="" && tabuleiro[linha][coluna+1]=="")
                    return false
            }


            if(linha==0 && coluna==9){
                if(tabuleiro[linha+1][coluna]=="" && tabuleiro[linha][coluna-1]=="" && tabuleiro[linha+1][coluna-1]=="")
                    return false
            }


            if(linha==9 && coluna==9){
                if(tabuleiro[linha-1][coluna]=="" && tabuleiro[linha-1][coluna-1]=="" && tabuleiro[linha][coluna-1]=="")
                    return false
            }


            if(linha==9 && coluna==0){
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
        if(coluna ==9)
            CentDir = false

        for(i in coluna+1 until 10){

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

            if(i==9)
                CentDir = false
        }

        //Centro -> Baixo
        if(linha == 9)
            CentBai = false

        for(i in linha+1 until 10){

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

            if(i==9)
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
        if(linha==0 || coluna==9)
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

                if(j==9) {
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
        if(linha==9 || coluna==9)
            CentBaiDir=false
        else {
            j = coluna+1

            for(i in linha+1 until 10){

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


                if(j==9) {
                    CentBaiDir = false
                    break
                }

                j++

                if(i==9) {
                    CentBaiDir = false
                }
            }
        }



        //Centro -> Baixo//Esquerda

        if(linha==9 || coluna==0)
            CentBaiEsq=false
        else{
            j = coluna-1

            for(i in linha+1 until 10){

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

                if(i==9) {
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

    private fun jogadaBomba(linha: Int, coluna:Int){
        var col=coluna-1;
        var lin=linha-1;

        for(i in 0 until 3){
            for(j in 0 until 3){
                if((lin>=0 && lin<=9) && (col>=0 && col<=9)){
                    if(lin!=linha || col!=coluna) {
                        tabuleiro[lin][col] = ""
                    }
                }
                col++
            }
            lin+=1
            col=coluna-1
        }

        if(pecaJogAtual==pecaJog0)
            jog0bomba=true
        else {
            if(pecaJogAtual==pecaJog1)
                jog1bomba = true
            else
                jog2bomba=true
        }

        bombaSelec=false
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
            if(pecaJogAtual==pecaJog1)
                jog1troca = true
            else
                jog2troca=true
        }

    }

    private fun limpaTabuleiroPosicoesFinais(){
        tabPosicoesFinais[0][0]=-1;tabPosicoesFinais[0][1]=-1;tabPosicoesFinais[1][0]=-1;tabPosicoesFinais[1][1]=-1;tabPosicoesFinais[2][0]=-1
        tabPosicoesFinais[2][1]=-1;tabPosicoesFinais[3][0]=-1;tabPosicoesFinais[3][1]=-1;tabPosicoesFinais[4][0]=-1;tabPosicoesFinais[4][1]=-1
        tabPosicoesFinais[5][0]=-1;tabPosicoesFinais[5][1]=-1;tabPosicoesFinais[6][0]=-1;tabPosicoesFinais[6][1]=-1;tabPosicoesFinais[7][0]=-1;
        tabPosicoesFinais[7][1]=-1;tabPosicoesFinais[8][0]=-1;tabPosicoesFinais[8][1]=-1;tabPosicoesFinais[9][0]=-1;tabPosicoesFinais[9][1]=-1;
    }

    fun verificaVencedor() {
        var pjog0 = 0
        var pjog1 = 0
        var pjog2 = 0
        var parcelVazias = false

        cliente.onCriarDados()

        for (i in 0 until 10) {
            for (j in 0 until 10) {

                if (tabuleiro[i][j] == "")
                    parcelVazias = true

                if (tabuleiro[i][j] == pecaJog0)
                    pjog0++

                if (tabuleiro[i][j] == pecaJog1) {
                    pjog1++
                }

                if (tabuleiro[i][j] == pecaJog2)
                    pjog2++

            }
        }

        //    b.numPecasJog0.text = pjog0.toString()
        //    b.numPecasJog1.text = pjog1.toString()

        if (pjog0 == pjog1 && pjog1 == pjog2 && jogoAcabou) {
            Log.i("TAG", "Empate")
            terminou = true
            state.postValue(Modo2Activity.State.GAME_OVER)
            enviaTabuleiro(socketOC1, EMPATE, false)
            enviaTabuleiro(socketOC2, EMPATE, false)
            enviaTabuleiro(socketOC3, EMPATE, false)
            return
        }

        if (pjog0 == 0 && pjog2 == 0 && pjog1 > 0) {
            Log.i("TAG", "JOGO TERMINOU VENCEDOR PECAS 34")
            terminou = true
            state.postValue(Modo2Activity.State.GAME_OVER)
            onActualizarDadosJog1()
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_34, false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_34, false)
            enviaTabuleiro(socketOC3, VENCEDOR_PECAS_34, false)
            return
        }

        if (pjog1 == 0 && pjog2 == 0 && pjog0 > 0) {
            terminou = true
            state.postValue(Modo2Activity.State.GAME_OVER)
            onActualizarDadosJog0()
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_35, false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_35, false)
            enviaTabuleiro(socketOC3, VENCEDOR_PECAS_35, false)
        }

        if (pjog0 == 0 && pjog1 == 0 && pjog2 > 0) {
            terminou = true
            state.postValue(Modo2Activity.State.GAME_OVER)
            onActualizarDadosJog2()
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_36, false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_36, false)
            enviaTabuleiro(socketOC3, VENCEDOR_PECAS_36, false)
        }

        if (!parcelVazias && pjog0 > pjog1 && pjog0 > pjog2) {
            terminou = true
            state.postValue(Modo2Activity.State.GAME_OVER)
            onActualizarDadosJog0()
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_35, false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_35, false)
            enviaTabuleiro(socketOC3, VENCEDOR_PECAS_35, false)
        }

        if (!parcelVazias && pjog1 > pjog0 && pjog1 > pjog2) {
            terminou = true
            state.postValue(Modo2Activity.State.GAME_OVER)
            onActualizarDadosJog1()
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_34, false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_34, false)
            enviaTabuleiro(socketOC3, VENCEDOR_PECAS_34, false)
            return
        }

        if (!parcelVazias && pjog2 > pjog0 && pjog2 > pjog1) {
            terminou = true
            state.postValue(Modo2Activity.State.GAME_OVER)
            onActualizarDadosJog2()
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_36, false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_36, false)
            enviaTabuleiro(socketOC3, VENCEDOR_PECAS_36, false)
        }

        if (parcelVazias && pjog0 > pjog1 && pjog0 > pjog2 && jogoAcabou) {
            terminou = true
            state.postValue(Modo2Activity.State.GAME_OVER)
            onActualizarDadosJog0()
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_35, false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_35, false)
            enviaTabuleiro(socketOC3, VENCEDOR_PECAS_35, false)
        }

        if (parcelVazias && pjog1 > pjog0 && pjog1 > pjog2 && jogoAcabou) {
            terminou = true
            state.postValue(Modo2Activity.State.GAME_OVER)
            onActualizarDadosJog1()
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_34, false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_34, false)
            enviaTabuleiro(socketOC3, VENCEDOR_PECAS_34, false)
        }
        if(parcelVazias && pjog2>pjog0 && pjog2>pjog1 && jogoAcabou){
            terminou = true
            state.postValue(Modo2Activity.State.GAME_OVER)
            onActualizarDadosJog2()
            enviaTabuleiro(socketOC1, VENCEDOR_PECAS_36, false)
            enviaTabuleiro(socketOC2, VENCEDOR_PECAS_36, false)
            enviaTabuleiro(socketOC3, VENCEDOR_PECAS_36, false)
        }

        if(pjog0==pjog1 && pjog0>pjog2 && jogoAcabou){
            terminou = true
            state.postValue(Modo2Activity.State.GAME_OVER)
            enviaTabuleiro(socketOC1, EMPATE_VERMELHO_AZUL, false)
            enviaTabuleiro(socketOC2, EMPATE_VERMELHO_AZUL, false)
            enviaTabuleiro(socketOC3, EMPATE_VERMELHO_AZUL, false)
        }

        if(pjog0==pjog2 && pjog0>pjog1 && jogoAcabou){
            terminou = true
            state.postValue(Modo2Activity.State.GAME_OVER)
            enviaTabuleiro(socketOC1, EMPATE_AZUL_VERDE, false)
            enviaTabuleiro(socketOC2, EMPATE_AZUL_VERDE, false)
            enviaTabuleiro(socketOC3, EMPATE_AZUL_VERDE, false)
        }

        if(pjog1==pjog2 && pjog1>pjog0 && jogoAcabou){
            terminou = true
            state.postValue(Modo2Activity.State.GAME_OVER)
            enviaTabuleiro(socketOC1, EMPATE_VERMELHO_VERDE, false)
            enviaTabuleiro(socketOC2, EMPATE_VERMELHO_VERDE, false)
            enviaTabuleiro(socketOC3, EMPATE_VERMELHO_VERDE, false)
        }
    }

    private fun tiraAjuda(){
        for(i in 0 until nAjudas){
            ajudaBtns[i]?.isActivated=false
        }

        nAjudas=0
    }

    private fun limpaBtns(){
        for(i in 0 until 3){
            selectedBtns[i]?.isSelected=false
        }
        pecasSelecionadas=0
    }

    private fun vezJogador0(){
        pecaJogAtual = pecaJog0
    }

    private fun vezJogador1(){
        pecaJogAtual = pecaJog1
    }

    private fun vezJogador2(){
        pecaJogAtual = pecaJog2
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


    private fun trocaJogador() {
        var jogoTerminou1=false
        var jogoTerminou2=false

        ajudaJogada()
        if(nAjudas==0)
            jogoTerminou1=true
        tiraAjuda()

        troca()
        ajudaJogada()
        if(nAjudas==0)
            jogoTerminou2=true
        tiraAjuda()

        troca()
        ajudaJogada()
        if(nAjudas==0 && jogoTerminou1 && jogoTerminou2) {
            tiraAjuda()
            jogoAcabou=true
            verificaVencedor()
        }

        tiraAjuda()

        troca()
        troca()

        ajudaJogada()

        if(nAjudas==0)
        {
            troca=true
        }
        else
            troca=false

        tiraAjuda()

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

    private fun ajudaEsq(linha:Int, coluna:Int,jogAdv1:String, jogAdv2:String){
        if(coluna-2<0)
            return

        if(tabuleiro[linha][coluna-1]==jogAdv1 || tabuleiro[linha][coluna-1]==jogAdv2) {
            for (j in (coluna - 2) downTo 0) {
                if (tabuleiro[linha][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[linha][j]==""){
                    ajudas[nAjudas][0]=linha
                    ajudas[nAjudas][1]=j
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

    private fun ajudaAbaixo(linha:Int, coluna:Int,jogAdv1:String, jogAdv2:String){
        if(linha+2>9)
            return

        if(tabuleiro[linha+1][coluna]==jogAdv1 || tabuleiro[linha+1][coluna]==jogAdv2) {
            for(i in (linha+2) until 10){
                if (tabuleiro[i][coluna] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][coluna]==""){
                    ajudas[nAjudas][0]=i
                    ajudas[nAjudas][1]=coluna
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

    private fun ajudaDir(linha:Int, coluna:Int,jogAdv1:String, jogAdv2:String){
        if(coluna+2>9)
            return

        if(tabuleiro[linha][coluna+1]==jogAdv1 || tabuleiro[linha][coluna+1]==jogAdv2) {
            for (j in (coluna + 2) until 10) {
                if (tabuleiro[linha][j] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[linha][j]==""){
                    ajudas[nAjudas][0]=linha
                    ajudas[nAjudas][1]=j
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

    private fun ajudaAcima(linha:Int, coluna:Int,jogAdv1:String, jogAdv2:String){
        if(linha-2<0)
            return

        if(tabuleiro[linha-1][coluna]==jogAdv1 || tabuleiro[linha-1][coluna]==jogAdv2 ) {
            for(i in (linha-2) downTo  0){
                if (tabuleiro[i][coluna] == pecaJogAtual) {
                    return
                }
                if(tabuleiro[i][coluna]==""){
                    ajudas[nAjudas][0]=i
                    ajudas[nAjudas][1]=coluna
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


    private fun enviaSorteio(op:Int){
        if (pecaJogAtual==pecaJog0) {
            vezDeJogar(socketOC1, op,true,pecaJogAtual,false)
            var temp = Random.nextInt(0,2)
                if(temp==0){
                    vezDeJogar(socketOC2, op,false,pecaJog1,false)
                    vezDeJogar(socketOC3, op,false,pecaJog2,false)
                }
            else{
                    vezDeJogar(socketOC2, op,false,pecaJog2,false)
                    vezDeJogar(socketOC3, op,false,pecaJog1,false)
            }
        }
        else {
            if(pecaJogAtual==pecaJog1){
                vezDeJogar(socketOC2, op,true,pecaJogAtual,false)
                var temp = Random.nextInt(0,2)
                if(temp==0){
                    vezDeJogar(socketOC1, op,false,pecaJog0,false)
                    vezDeJogar(socketOC3, op,false,pecaJog2,false)
                }
                else{
                    vezDeJogar(socketOC1, op,false,pecaJog2,false)
                    vezDeJogar(socketOC3, op,false,pecaJog0,false)
                }
            }
            else{
                vezDeJogar(socketOC3, op,true,pecaJogAtual,false)
                var temp = Random.nextInt(0,2)
                if(temp==0){
                    vezDeJogar(socketOC1, op,false,pecaJog0,false)
                    vezDeJogar(socketOC2, op,false,pecaJog1,false)
                }
                else{
                    vezDeJogar(socketOC1, op,false,pecaJog0,false)
                    vezDeJogar(socketOC2, op,false,pecaJog1,false)
                }
            }
        }
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

    fun enviaDados(socketO: OutputStream?,nomeAdv1:String, imagemAdv1: String, pecaAdv1:String, nomeAdv2:String, imagemAdv2:String, pecaAdv2:String){
        if (connectionState.value != Modo2Activity.ConnectionState.CONNECTION_ESTABLISHED)
            return

        Log.i("TAG","Envia Dados")

        socketO?.run {
            thread {
                try {
                    var data=Jogada(DADOS)
                    data.nome= nomeAdv1
                    data.imagem= imagemAdv1
                    data.minhaPeca= pecaAdv1
                    data.nome2=nomeAdv2
                    data.imagem2=imagemAdv2
                    data.peca2=pecaAdv2

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