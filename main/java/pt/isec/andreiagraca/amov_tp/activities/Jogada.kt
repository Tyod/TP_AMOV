package pt.isec.andreiagraca.amov_tp.activities

import android.graphics.Bitmap
import android.widget.Button
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

const val VEZ_JOGADOR = 0
const val JOGADA = 1
const val TABULEIRO=2
const val BOMBA_JA_JOGADA=3
const val TROCA_PECAS=4
const val PECAS_JA_TROCADAS=5
const val EMPATE=6
const val VENCEDOR_PECAS_34=7
const val VENCEDOR_PECAS_35=8
const val PASSA_VEZ=9
const val AJUDA_JOGADA=10
const val NOVO_JOGO=11
const val SORTEIA_JOGADOR=12
const val VENCEDOR_PECAS_36=13
const val EMPATE_VERMELHO_AZUL=14
const val EMPATE_AZUL_VERDE=15
const val EMPATE_VERMELHO_VERDE=16
const val DADOS=17


@Serializable
data class Jogada(val op:Int){
    var linha:Int?=null
    var coluna:Int?=null
    var jogadaEspecial:Boolean?=null
    var pecaJogAtual:String?=null
    var vez:Boolean?=null
    var tabuleiro: Array<Array<String>>?=null
    var pecas: Array<Array<Int>>?=null
    var ajudas: Array<Array<Int>>?=null
    var nAjudas:Int?=null
    var minhaPeca:String?=null
    var troca:Boolean?=null
    var nome:String?=null
    var imagem: String?=null
    var nome2:String?=null
    var imagem2:String?=null
    var peca2:String?=null
}



