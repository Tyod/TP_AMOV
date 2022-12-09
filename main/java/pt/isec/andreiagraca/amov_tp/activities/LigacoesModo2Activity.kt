package pt.isec.andreiagraca.amov_tp.activities

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import pt.isec.andreiagraca.amov_tp.databinding.ActivityModo1Binding


class LigacoesModo2Activity : AppCompatActivity(){
    lateinit var b: ActivityModo1Binding
    private lateinit var model: ServidorModo2Model
    private var dlg: AlertDialog? = null
/*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b= ActivityModo1Binding.inflate(layoutInflater)
        setContentView(b.root)

        model = ViewModelProvider(this).get(ModeloModo2Activity::class.java)

        model.connectionState.observe(this) { state ->
            if ( state != ModeloModo2Activity.ConnectionState.SETTING_PARAMETERS &&
                state != ModeloModo2Activity.ConnectionState.SERVER_CONNECTING && dlg?.isShowing == true) {
                dlg?.dismiss()
                dlg = null
            }

            if (state == ModeloModo2Activity.ConnectionState.CONNECTION_ERROR) {
                finish()
            }
            if (state == ModeloModo2Activity.ConnectionState.CONNECTION_ENDED)
                finish()
        }

        if (model.connectionState.value != ModeloModo2Activity.ConnectionState.CONNECTION_ESTABLISHED) {
            when (intent.getIntExtra("mode", SERVER_MODE)) {
                SERVER_MODE -> startAsServer()
                CLIENT_MODE -> startAsClient()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        dlg?.apply {
            if (isShowing)
                dismiss()
        }
    }

    private fun startAsServer() {
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
                model.stopServer()
                finish()
            }
            create()
        }
        model.startServer()

        dlg?.show()
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
                    Toast.makeText(this@LigacoesModo2Activity, getString(R.string.error_address), Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    model.startClient(edtBox.text.toString())
                }
            }
            setNeutralButton(getString(R.string.btn_emulator)) { _: DialogInterface, _: Int ->
                model.startClient("10.0.2.2", SERVER_PORT-1)
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
    }*/
}