package com.example.lostbtc

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import org.bitcoinj.core.ECKey
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val pool = Executors.newSingleThreadExecutor()
    private lateinit var addressInput: EditText
    private lateinit var result: TextView
    private lateinit var ownAddress: TextView
    private lateinit var ownKey: TextView
    private var generatedAddress: String? = null
    private var generatedWif: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 14, 18)
        window.navigationBarColor = Color.rgb(12, 14, 18)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(12, 14, 18))
        }
        root.addView(TextView(this).apply {
            text = "LOST BTC"
            textSize = 30f
            setTextColor(Color.rgb(247, 147, 26))
            gravity = Gravity.CENTER
        }, matchWrap())
        root.addView(TextView(this).apply {
            text = "Bitcoin address monitor"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }, matchWrap())

        root.addView(section("МОЙ КОШЕЛЁК"))
        root.addView(Button(this).apply {
            text = "СОЗДАТЬ НОВЫЙ КОШЕЛЁК"
            setOnClickListener { createWallet() }
        }, matchWrap())
        ownAddress = TextView(this).apply {
            text = "Адрес: пока не создан"
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        ownKey = TextView(this).apply {
            text = "Приватный ключ: не создан"
            setTextColor(Color.rgb(230, 170, 110))
            textSize = 13f
        }
        root.addView(ownAddress, matchWrap())
        root.addView(ownKey, matchWrap())

        root.addView(section("ПРОВЕРКА ПУБЛИЧНОГО АДРЕСА"))
        addressInput = EditText(this).apply {
            hint = "BTC-адрес (1…, 3… или bc1…)"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 14f
            setSingleLine(true)
            setPadding(14, 12, 14, 12)
            setBackgroundColor(Color.rgb(30, 33, 40))
        }
        root.addView(addressInput, matchWrap())
        root.addView(Button(this).apply {
            text = "ПРОВЕРИТЬ БАЛАНС"
            setOnClickListener { checkAddress(addressInput.text.toString().trim()) }
        }, matchWrap())
        result = TextView(this).apply {
            text = "Введите адрес, чтобы проверить публичные данные блокчейна."
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(4, 16, 4, 4)
        }
        root.addView(result, matchWrap())
        root.addView(TextView(this).apply {
            text = "Важно: приложение не может определить приватный ключ по адресу. Не вводи seed-фразы или приватные ключи на сторонних сайтах. Эта сборка пока не подписывает и не отправляет транзакции."
            textSize = 13f
            setTextColor(Color.rgb(170, 175, 185))
            setPadding(4, 18, 4, 4)
        }, matchWrap())

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
    }

    private fun section(text: String) = TextView(this).apply {
        this.text = text
        textSize = 17f
        setTextColor(Color.rgb(247, 147, 26))
        setPadding(0, 18, 0, 8)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 10 }


    private fun createWallet() {
        try {
            val params = MainNetParams.get()
            val key = ECKey()
            val address = key.toAddress(params).toString()
            val wif = key.getPrivateKeyAsWiF(params)
            generatedAddress = address
            generatedWif = wif
            ownAddress.text = "ТВОЙ BTC-АДРЕС:\n$address"
            ownKey.text = "ПРИВАТНЫЙ КЛЮЧ (WIF):\n$wif\n\nСохрани его офлайн. Любой, кто получит этот ключ, сможет распоряжаться средствами."
            addressInput.setText(address)
            Toast.makeText(this, "Кошелёк создан локально на устройстве", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось создать кошелёк: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAddress(address: String) {
        if (!address.matches(Regex("^(1|3|bc1)[a-zA-HJ-NP-Z0-9]{20,90}$"))) {
            result.text = "Проверь адрес. Введи публичный Bitcoin-адрес."
            return
        }
        result.text = "Запрос к блокчейну…"
        pool.execute {
            try {
                val conn = URL("https://mempool.space/api/address/$address")
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", "LostBTC-Android/1.1")
                if (conn.responseCode != 200) throw Exception("API ответил кодом ${conn.responseCode}")
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val chain = json.getJSONObject("chain_stats")
                val mempool = json.getJSONObject("mempool_stats")
                val funded = chain.getLong("funded_txo_sum") + mempool.getLong("funded_txo_sum")
                val spent = chain.getLong("spent_txo_sum") + mempool.getLong("spent_txo_sum")
                val sats = funded - spent
                val txCount = chain.getInt("tx_count") + mempool.getInt("tx_count")
                runOnUiThread {
                    result.text = "Адрес:\n$address\n\nБаланс: %.8f BTC\nСатоши: %d\nТранзакций: %d\n\nИсточник: mempool.space"
                        .format(sats / 100_000_000.0, sats, txCount)
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread { result.text = "Ошибка запроса: ${e.message}\nПроверь интернет и адрес." }
            }
        }
    }

    override fun onDestroy() {
        pool.shutdownNow()
        super.onDestroy()
    }
}
