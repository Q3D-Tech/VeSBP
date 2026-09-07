package com.example.vesbp

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Отдельная точка входа для банковских ссылок и «Поделиться».
 * Её компонент можно безопасно включать и отключать, не затрагивая запуск приложения.
 */
class LinkReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openVeSbp(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openVeSbp(intent)
    }

    private fun openVeSbp(source: Intent?) {
        val target = Intent(this, MainActivity::class.java).apply {
            action = source?.action
            // У Intent setType() очищает data. Передаём их одним вызовом,
            // иначе банковская ссылка пропадает при type == null.
            setDataAndType(source?.data, source?.type)
            source?.getStringExtra(Intent.EXTRA_TEXT)?.let {
                putExtra(Intent.EXTRA_TEXT, it)
            }
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(target)
        finish()
    }
}
