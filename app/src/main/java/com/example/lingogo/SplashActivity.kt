package com.example.lingogo

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest


/**
 * VERSIÓN SIMPLIFICADA (para minSdk 28+)
 * Esta es una actividad de depuración temporal.
 * Su único propósito es calcular la huella SHA-1 REAL
 * con la que la app está firmada en este dispositivo.
 */
class SplashActivity : AppCompatActivity() {

    // ¡CAMBIO! El 'TAG' no debe empezar con minúscula (era una advertencia)
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ¡REVISA ESTO! Asegúrate de que 'activity_splash.xml' exista en 'res/layout/'
        setContentView(R.layout.activity_splash)


        // ¡Función de depuración!
        logAppSignature()

        // Después de 2 segundos, intenta ir al MainActivity (Login)
        val handler = Handler(mainLooper)
        handler.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000)
    }

    /**
     * Calcula la huella SHA-1 de la app y la imprime en el Logcat.
     * VERSIÓN SIMPLIFICADA para API 28+ (tu minSdk es 31)
     */
    private fun logAppSignature() {
        try {
            // ¡CAMBIO! Código simplificado para API 28+
            val packageInfo: PackageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES // Esta era la ref. que faltaba
            )

            // NEW, FIXED CODE
            val signatures: Array<Signature> = packageInfo.signingInfo?.apkContentsSigners ?: emptyArray()


            if (signatures.isEmpty()) {
                Log.e(TAG, "¡ERROR! No se encontraron firmas.")
                return
            }

            Log.d(TAG, "================== HUELLA SHA-1 REAL DE LA APP ==================");

            for (signature in signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val sha1 = Base64.encodeToString(md.digest(), Base64.NO_WRAP)

                // Formato con dos puntos (para comparar)
                val sha1WithColons = sha1.chunked(2).joinToString(":")

                Log.d(TAG, "SHA-1 (Base64): $sha1")
                Log.d(TAG, "SHA-1 (Con :): $sha1WithColons")
            }
            Log.d(TAG, "==================================================================");
            Log.d(TAG, "Copia la huella SHA-1 (Con :) y pégala en la Consola de Firebase y Google Cloud.");


        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener la firma de la app", e)
        }
    }
}