package pt.isec.andreiagraca.amov_tp.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import pt.isec.andreiagraca.amov_tp.databinding.ActivityIndicaModoDeLigacaoBinding


class IndicaModoDeLigacao : AppCompatActivity() {
    lateinit var b: ActivityIndicaModoDeLigacaoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityIndicaModoDeLigacaoBinding.inflate(layoutInflater)
        setContentView(b.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        b.modoServidor.setOnClickListener{
            startGame(SERVER_MODE)
        }

        b.modoCliente.setOnClickListener(){
            startGame(CLIENT_MODE)
        }
    }

    fun startGame(mode : Int) {
        when (intent.getIntExtra("GameMode", MODO_2)) {
            MODO_2 -> {
                val intent = Intent(this,Modo2Activity::class.java).apply {
                    putExtra("mode",mode)
                }
                startActivity(intent)
            }
            MODO_3 ->{
                if(mode== SERVER_MODE){
                    val intent = Intent(this,Modo3Activity::class.java).apply {
                        putExtra("mode",mode)
                    }
                    startActivity(intent)
                }
                else{
                    val intent = Intent(this,Modo3Activity::class.java).apply {
                        putExtra("mode",mode)
                    }
                    startActivity(intent)
                }

            }
        }
    }

    
}