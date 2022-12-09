package pt.isec.andreiagraca.amov_tp.activities

import android.app.AlertDialog
import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat.setBackgroundTintList
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pt.isec.andreiagraca.amov_tp.R
import pt.isec.andreiagraca.amov_tp.databinding.ActivityEscolhemodoBinding
import pt.isec.andreiagraca.amov_tp.databinding.ActivityModo1Binding
import kotlin.random.Random


class Modo1Activity : AppCompatActivity() {
    lateinit var b: ActivityModo1Binding
    val jog0 = "\uD83D\uDD35" //Blue
    val jog1 = "\uD83D\uDD34" //Red
    var jogAtual : String = ""
    val possJogada = "✖"
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
    var jog0troca:Boolean=false
    var jog1troca:Boolean=false
    var pecasSelecionadas:Int=0
    var pecas=Array(3){Array(2){0} }
    var btn: Button? =null
    var selectedBtns=Array(3){btn}
    var nAjudas:Int=0
    var ajudaBtns=Array(64){btn}
    var str:Int=0
    var jogoAcabou=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b= ActivityModo1Binding.inflate(layoutInflater)
        setContentView(b.root)
        btn=b.btn00
        selectedBtns=Array(3){btn}
        sorteiaJogador()
        acionaListeners()
        preparaTabuleiro()
        repoePontuacao()
       // verificaVencedor()
        b.numPecasJog0.text = str.toString()
        b.numPecasJog1.text = str.toString()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.opcoes,menu)
        return true
    }

    private fun tiraAjuda(){
        for(i in 0 until nAjudas){
            ajudaBtns[i]?.isActivated=false
        }

        nAjudas=0
    }

    fun onCriarDados() {
            val db = Firebase.firestore
            val dados = hashMapOf(
                "Jogador1" to 0,
                "Jogador2" to 0
            )
            db.collection("Modo1").document("Modo").set(dados)

            db.collection("Modo1").get().addOnSuccessListener(this) { query ->
                for (doc in query.documents)
                    Log.i(ContentValues.TAG, "onCriarDados: ${doc.id}")
            }
    }


    fun repoePontuacao(){
        val db = Firebase.firestore
        val v = db.collection("Modo1").document("IehOdQwSDgCmU1IoC7Mt")
        db.runTransaction {transaction ->
            transaction.update(v,"Jogador1",0)
            transaction.update(v,"Jogador2",0)

            null
        }
    }

    fun onActualizarDadosJog0() { //azul

        val db = Firebase.firestore
        val v = db.collection("Modo1").document("IehOdQwSDgCmU1IoC7Mt")
        db.runTransaction {transaction ->
            val doc = transaction.get(v)
            val vitoria = doc.getLong("Jogador1")!! + 1
            transaction.update(v,"Jogador1",vitoria)

            null
        }
    }

    fun onActualizarDadosJog1() { //vermelho

        val db = Firebase.firestore
        val v = db.collection("Modo1").document("IehOdQwSDgCmU1IoC7Mt")
        db.runTransaction {transaction ->
            val doc = transaction.get(v)
            val vitoria = doc.getLong("Jogador2")!! + 1
            transaction.update(v,"Jogador2",vitoria)

            null
        }
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

        if(jogAtual==jog0)
            jogAdv=jog1
        else
            jogAdv=jog0

        if(tabuleiro[linha-1][coluna-1]==jogAdv) {
            var i=linha-2
            var j=coluna-2
            while(i>=0 && j>=0){
                    if (tabuleiro[i][j] == jogAtual) {
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

    private fun ajudaEsq(linha:Int, coluna:Int){
        if(coluna-2<0)
            return

        var jogAdv:String?=null

        if(jogAtual==jog0)
            jogAdv=jog1
        else
            jogAdv=jog0

        if(tabuleiro[linha][coluna-1]==jogAdv) {
                for (j in (coluna - 2) downTo 0) {
                    if (tabuleiro[linha][j] == jogAtual) {
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

    private fun ajudaInfEsq(linha:Int, coluna:Int){
        if(linha+2>7)
            return
        if(coluna-2<0)
            return

        var jogAdv:String?=null

        if(jogAtual==jog0)
            jogAdv=jog1
        else
            jogAdv=jog0

        if(tabuleiro[linha+1][coluna-1]==jogAdv) {
            var i=linha+2
            var j=coluna-2
            while(i<8 && j>=0){
                    if (tabuleiro[i][j] == jogAtual) {
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

    private fun ajudaAbaixo(linha:Int, coluna:Int){
        if(linha+2>7)
            return

        var jogAdv:String?=null

        if(jogAtual==jog0)
            jogAdv=jog1
        else
            jogAdv=jog0

        if(tabuleiro[linha+1][coluna]==jogAdv) {
            for(i in (linha+2) until 8){
                    if (tabuleiro[i][coluna] == jogAtual) {
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

    private fun ajudaInfDir(linha:Int, coluna:Int){
        if(linha+2>7)
            return
        if(coluna+2>7)
            return

        var jogAdv:String?=null

        if(jogAtual==jog0)
            jogAdv=jog1
        else
            jogAdv=jog0

        if(tabuleiro[linha+1][coluna+1]==jogAdv) {
            var i=linha+2
            var j=coluna+2
            while(i<8 && j<8){
                    if (tabuleiro[i][j] == jogAtual) {
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

    private fun ajudaDir(linha:Int, coluna:Int){
        if(coluna+2>7)
            return

        var jogAdv:String?=null

        if(jogAtual==jog0)
            jogAdv=jog1
        else
            jogAdv=jog0

        if(tabuleiro[linha][coluna+1]==jogAdv) {
            for (j in (coluna + 2) until 8) {
                if (tabuleiro[linha][j] == jogAtual) {
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

    private fun ajudaSupDir(linha:Int, coluna:Int){
        if(linha-2<0)
            return
        if(coluna+2>7)
            return

        var jogAdv:String?=null

        if(jogAtual==jog0)
            jogAdv=jog1
        else
            jogAdv=jog0

        if(tabuleiro[linha-1][coluna+1]==jogAdv) {
            var i=linha-2
            var j=coluna+2
            while(i>=0 && j<8){
                if (tabuleiro[i][j] == jogAtual) {
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

    private fun ajudaAcima(linha:Int, coluna:Int){
        if(linha-2<0)
            return

        var jogAdv:String?=null

        if(jogAtual==jog0)
            jogAdv=jog1
        else
            jogAdv=jog0

        if(tabuleiro[linha-1][coluna]==jogAdv) {
            for(i in (linha-2) downTo  0){
                if (tabuleiro[i][coluna] == jogAtual) {
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
        for(i in 0 until 8){
            for(j in 0 until 8){
                if(tabuleiro[i][j]==jogAtual){
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.ajudaJogada->{
                Toast.makeText(this,R.string.ajuda_selecionada,Toast.LENGTH_SHORT).show()
                if(nAjudas==0) {
                    ajudaJogada()
                }
                else
                    tiraAjuda()
            }
        }

        return super.onOptionsItemSelected(item)
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

             if(tabuleiro[i][coluna] == jogAtual){
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

            if(tabuleiro[linha][i] == jogAtual){
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

            if(tabuleiro[i][coluna] == jogAtual){
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

            if(tabuleiro[linha][i] == jogAtual){
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

                if(tabuleiro[i][j]==jogAtual){
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

                if(tabuleiro[i][j]==jogAtual){
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

                if(tabuleiro[i][j]==jogAtual){
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

                if(tabuleiro[i][j]==jogAtual){
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
            tabuleiro[linha][coluna] = jogAtual
            limpaBtns()
            tiraAjuda()
            return
        }
        tabuleiro[linha][coluna] = jogAtual

       if(CentCim){
           for(i in linha downTo tabPosicoesFinais[0][0]){
               tabuleiro[i][coluna] = jogAtual
           }
       }

       if(CentCimDir){
           var j=coluna
           for(i in linha downTo tabPosicoesFinais[1][0]){
               tabuleiro[i][j] = jogAtual
               j++
           }
       }

       if(CentDir){
           for(i in coluna until tabPosicoesFinais[2][1]){
               tabuleiro[linha][i] = jogAtual
           }
       }

       if(CentBaiDir){
           var j = coluna
           for(i in linha until tabPosicoesFinais[3][0]){
               tabuleiro[i][j] = jogAtual
               j++
           }
       }

       if(CentBai){
           for(i in linha until tabPosicoesFinais[4][0]){
               tabuleiro[i][coluna] = jogAtual
           }
       }

       if(CentBaiEsq){
           var j = coluna
           for(i in linha until  tabPosicoesFinais[5][0]){
               tabuleiro[i][j] = jogAtual
               j--
           }
       }

       if(CentEsq){
           for(i in coluna downTo tabPosicoesFinais[6][1]){
               tabuleiro[linha][i] = jogAtual
           }
       }

       if(CentCimaEsq){
           var j=coluna
           for(i in linha downTo tabPosicoesFinais[7][0]){
               tabuleiro[i][j] = jogAtual
               j--
           }
       }

        limpaBtns()
        tiraAjuda()
        atualizaVista()
        verificaVencedor()
    }

    private fun verificaVencedor() {
        var pjog0=0
        var pjog1=0
        var parcelVazias = false

        onCriarDados();

       for(i in 0 until 8){
           for(j in 0 until 8){

               if(tabuleiro[i][j]=="")
                   parcelVazias=true

               if(tabuleiro[i][j]==jog0)
                   pjog0++

               if(tabuleiro[i][j]==jog1){
                   pjog1++
               }

           }
       }

        b.numPecasJog0.text = pjog0.toString()
        b.numPecasJog1.text = pjog1.toString()

        if(pjog0==pjog1 && jogoAcabou){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.empate)
            builder.setMessage(R.string.empateee)


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
                preparaNovoJogo()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                //b.txtIndicadorDeJogador.text = R.string.empate.toString()
            }

            builder.show()
        }

       if(pjog0 == 0){
           val builder = AlertDialog.Builder(this)
           builder.setTitle(R.string.fim_jogo)
           builder.setMessage(R.string.vencedor_pecas_34)

           onActualizarDadosJog1()

           builder.setPositiveButton(R.string.novJog) { dialog, which ->
               preparaNovoJogo()
           }

           builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
               b.btnTrocaPecas.isEnabled = false
               b.btnPecaBomba.isEnabled = false
               b.btnPassaVez.isEnabled = false
               //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
               b.CorAtual.text = "\uD83D\uDD34"
           }

           builder.show()
       }

       if(pjog1 == 0){
           val builder = AlertDialog.Builder(this)
           builder.setTitle(R.string.fim_jogo)
           builder.setMessage(R.string.vencedor_pecas_35)
           onActualizarDadosJog0()


           builder.setPositiveButton(R.string.novJog) { dialog, which ->
               preparaNovoJogo()
           }

           builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
               b.btnTrocaPecas.isEnabled = false
               b.btnPecaBomba.isEnabled = false
               b.btnPassaVez.isEnabled = false
               //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
               b.CorAtual.text = "\uD83D\uDD35"
           }

           builder.show()
       }

       if(!parcelVazias && pjog0>pjog1){
           val builder = AlertDialog.Builder(this)
           builder.setTitle(R.string.fim_jogo)
           builder.setMessage(R.string.vencedor_pecas_35)
           onActualizarDadosJog0();


           builder.setPositiveButton(R.string.novJog) { dialog, which ->
               preparaNovoJogo()
           }

           builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
               b.btnTrocaPecas.isEnabled = false
               b.btnPecaBomba.isEnabled = false
               b.btnPassaVez.isEnabled = false
              // b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
               b.CorAtual.text = "\uD83D\uDD35"
           }

           builder.show()
       }

       if(!parcelVazias && pjog1>pjog0){
           val builder = AlertDialog.Builder(this)
           builder.setTitle(R.string.fim_jogo)
           builder.setMessage(R.string.vencedor_pecas_34)
           onActualizarDadosJog1()


           builder.setPositiveButton(R.string.novJog) { dialog, which ->
               preparaNovoJogo()
           }

           builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
               b.btnTrocaPecas.isEnabled = false
               b.btnPecaBomba.isEnabled = false
               b.btnPassaVez.isEnabled = false
              // b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
               b.CorAtual.text = "\uD83D\uDD34"
           }

           builder.show()
       }
        if(parcelVazias && pjog0>pjog1 && jogoAcabou){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.fim_jogo)
            builder.setMessage(R.string.vencedor_pecas_35)
            onActualizarDadosJog0()


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
                preparaNovoJogo()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
               // b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                b.CorAtual.text = "\uD83D\uDD35"
            }

            builder.show()
        }

        if(parcelVazias && pjog1>pjog0 && jogoAcabou){
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.fim_jogo)
            builder.setMessage(R.string.vencedor_pecas_34)
            onActualizarDadosJog1()


            builder.setPositiveButton(R.string.novJog) { dialog, which ->
                preparaNovoJogo()
            }

            builder.setNegativeButton(R.string.consultarTab) { dialog, which ->
                b.btnTrocaPecas.isEnabled = false
                b.btnPecaBomba.isEnabled = false
                b.btnPassaVez.isEnabled = false
                //b.txtIndicadorDeJogador.text = R.string.vencedor.toString()
                b.CorAtual.text = "\uD83D\uDD34"
            }

            builder.show()
        }
    }

    private fun preparaNovoJogo() {
        for(i in 0 until 8){
            for(j in 0 until 8){
                tabuleiro[i][j]=""
            }
        }

        sorteiaJogador()
        preparaTabuleiro()
        atualizaVista()
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
            jogAtual = jog1
            b.CorAtual.text = jog1
        }
        else {
            jogAtual = jog0
            b.CorAtual.text = jog0
        }
    }


    private fun trocaJogador(){
        var jogoTerminou=false

        ajudaJogada()
        if(nAjudas==0)
            jogoTerminou=true
        tiraAjuda()

        if(jogAtual == jog1) {
            jogAtual = jog0
            b.CorAtual.text = jog0
        }
        else {
            jogAtual = jog1
            b.CorAtual.text = jog1
        }

        ajudaJogada()

        if(nAjudas==0 && jogoTerminou==false)
            b.btnPassaVez.isEnabled=true
        else
            b.btnPassaVez.isEnabled=false

        if(nAjudas==0 && jogoTerminou==true) {
            tiraAjuda()
            jogoAcabou=true
            verificaVencedor()
        }

        tiraAjuda()
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

        if(jogAtual==jog0)
            jog0bomba=true
        else
            jog1bomba=true

        bombaSelec=false
    }

    fun onBomba(view:android.view.View){
        if(!bombaSelec){
            if((jogAtual==jog0 && jog0bomba==true) || (jogAtual==jog1 && jog1bomba==true)){
                Snackbar.make(view,
                    R.string.bomba_ja_jogada,
                    Snackbar.LENGTH_SHORT).show()
            }
            else {
                Snackbar.make(
                    view,
                    R.string.bomba_selecionada,
                    Snackbar.LENGTH_SHORT
                ).show()
                bombaSelec = true
                b.btnPecaBomba.setBackgroundResource(R.drawable.button_shadow)
            }
        }
        else{
            Snackbar.make(view,
                R.string.bomba_desselecionada,
                Snackbar.LENGTH_SHORT).show()
            bombaSelec=false
            b.btnTrocaPecas.setBackgroundResource(R.drawable.round_btn)
        }
    }

    private fun trocaPecas(){
        for(i in 0 until 3){
            if(tabuleiro[pecas[i][0]][pecas[i][1]]==jog0){
                tabuleiro[pecas[i][0]][pecas[i][1]]=jog1
            }
            else{
                tabuleiro[pecas[i][0]][pecas[i][1]]=jog0
            }
        }

        limpaBtns()

        if(jogAtual==jog0){
            jog0troca=true
        }
        else
            jog1troca=true

    }

    private fun selecionaPeca(btn:Button, linha: Int, coluna:Int){

        if (btn.isSelected == true) {
            btn.isSelected = !btn.isSelected
            pecasSelecionadas--
        }

        if(pecasSelecionadas<2) {
                if (jog0 == jogAtual && jog0troca == false) {
                    if (btn.text == jogAtual && !btn.isSelected) {
                        btn.isSelected = true
                        pecas[pecasSelecionadas][0] = linha
                        pecas[pecasSelecionadas][1] = coluna
                        selectedBtns[pecasSelecionadas]=btn
                        pecasSelecionadas++
                    }
                } else {
                    if (jog1 == jogAtual && jog1troca == false) {
                        if (btn.text == jogAtual && !btn.isSelected) {
                            btn.isSelected = true
                            pecas[pecasSelecionadas][0] = linha
                            pecas[pecasSelecionadas][1] = coluna
                            selectedBtns[pecasSelecionadas]=btn
                            pecasSelecionadas++
                        }
                    } else
                        Toast.makeText(
                            applicationContext,
                            R.string.posicao_invalida,
                            Toast.LENGTH_SHORT
                        ).show()

                }
        }
        else{
                if (jogAtual == jog0) {
                        if (btn.text == jog1 && pecasSelecionadas==2 && !btn.isSelected) {
                            btn.isSelected = true
                            pecas[pecasSelecionadas][0] = linha
                            pecas[pecasSelecionadas][1] = coluna
                            selectedBtns[pecasSelecionadas]=btn
                            pecasSelecionadas++
                        }
                } else {
                        if (btn.text == jog0 && pecasSelecionadas==2 && !btn.isSelected) {
                            btn.isSelected = true
                            pecas[pecasSelecionadas][0] = linha
                            pecas[pecasSelecionadas][1] = coluna
                            selectedBtns[pecasSelecionadas]=btn
                            pecasSelecionadas++
                        }
                }
        }
        if(pecasSelecionadas==2)
            Toast.makeText(applicationContext, R.string.selecione_peca_adversaria, Toast.LENGTH_SHORT).show()
    }

    fun onTroca(view:android.view.View){
        if((jog0==jogAtual && jog0troca==true) || (jog1==jogAtual && jog1troca==true)){
            Snackbar.make(view,
                R.string.limite_troca,
                Snackbar.LENGTH_SHORT).show()
            return
        }
        if(pecasSelecionadas<2){
            Snackbar.make(view,
                R.string.selecione_peca,
                Snackbar.LENGTH_SHORT).show()
            return
        }
        if(pecasSelecionadas==2){
            Snackbar.make(view,
                R.string.selecione_adversaria,
                Snackbar.LENGTH_SHORT).show()
            return
        }
        if(pecasSelecionadas==3){
            Snackbar.make(view,
                R.string.confirmar_troca,
                Snackbar.LENGTH_SHORT)
                .setAction(R.string.sim){
                    trocaPecas()
                    atualizaVista()
                    atualizaContador()
                    trocaJogador()
                }.show()
        }
    }

    private fun atualizaContador() {
        var pjog0 = 0
        var pjog1 = 0

        for (i in 0 until 8) {
            for (j in 0 until 8) {

                if (tabuleiro[i][j] == jog0)
                    pjog0++

                if (tabuleiro[i][j] == jog1) {
                    pjog1++
                }

            }
        }

        b.numPecasJog0.text = pjog0.toString()
        b.numPecasJog1.text = pjog1.toString()
    }

    private fun jogada(linha:Int, coluna:Int){
        if(bombaSelec){
            atualizaTabuleiro(linha, coluna)
            jogadaBomba(linha,coluna)
            //atualizaVista()
            //atualizaContador()
            //trocaJogador()
            limpaBtns()
            tiraAjuda()
            atualizaVista()
            verificaVencedor()
            trocaJogador()
        }
        else {
            if (confirmaPosicaoValida(linha, coluna)) {
                atualizaTabuleiro(linha, coluna)
                trocaJogador()
            }
            else
                Toast.makeText(applicationContext, R.string.posicao_invalida, Toast.LENGTH_SHORT).show()
        }
    }



    private fun acionaListeners(){
        //BOTÕES ESPECIAIS

        b.btnPassaVez.setOnClickListener{
            trocaJogador()
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