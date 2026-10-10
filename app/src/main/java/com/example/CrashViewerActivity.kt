package dev.andikuneiocontroll

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import dev.andikuneiocontroll.util.CrashHandler
import java.io.File

/**
 * CrashViewerActivity — Activity TERPISAH untuk lihat log crash.
 *
 * ISOLASI TOTAL:
 * - Tidak pakai Compose
 * - Tidak pakai ViewModel
 * - Tidak akses Room DB / PrefsRepository
 * - Hanya native Android View (TextView + Button)
 *
 * Jadi 100% tidak akan force close dari dependency apapun.
 */
class CrashViewerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get crash file
        val crashFile: File? = try {
            CrashHandler.getLatestCrashFile(this)
        } catch (e: Exception) {
            null
        }

        val crashContent: String = try {
            crashFile?.readText() ?: "(Tidak ada log crash)"
        } catch (e: Exception) {
            "(Gagal baca log: ${e.message})"
        }

        // Build UI programmatically — pakai native View, bukan Compose
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1D.toInt())
            setPadding(32, 48, 32, 32)
        }

        // Title
        val titleText = TextView(this).apply {
            text = "⚠ APLIKASI SEBELUMNYA CRASH"
            setTextColor(0xFFFF5252.toInt())
            textSize = 18f
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(titleText)

        // Subtitle
        val subtitleText = TextView(this).apply {
            text = "File: ${crashFile?.name ?: "-"}"
            setTextColor(0xFFFFC107.toInt())
            textSize = 12f
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(subtitleText)

        // Instruction
        val instructionText = TextView(this).apply {
            text = "1. Tap SALIN di bawah\n2. Kirim log ke developer\n3. Tap TUTUP untuk lanjut ke aplikasi"
            setTextColor(0xFFB0B7C3.toInt())
            textSize = 12f
            setPadding(0, 0, 0, 24)
        }
        rootLayout.addView(instructionText)

        // Scrollable log
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(0xFF0F1626.toInt())
            setPadding(24, 24, 24, 24)
        }

        val logText = TextView(this).apply {
            text = crashContent
            setTextColor(0xFFE0E0E0.toInt())
            textSize = 9f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        scrollView.addView(logText)

        // Layout params untuk scrollView
        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        scrollParams.setMargins(0, 0, 0, 24)
        scrollView.layoutParams = scrollParams
        rootLayout.addView(scrollView)

        // Tombol SALIN
        val copyButton = Button(this).apply {
            text = "📋 SALIN LOG"
            textSize = 14f
            setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFFC6FF00.toInt())
            setPadding(0, 32, 0, 32)
            setOnClickListener {
                try {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Crash Log iOControll Tv", crashContent)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(
                        this@CrashViewerActivity,
                        "✓ Log disalin ke clipboard",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@CrashViewerActivity,
                        "Gagal salin: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        val copyParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        copyParams.setMargins(0, 0, 0, 12)
        copyButton.layoutParams = copyParams
        rootLayout.addView(copyButton)

        // Tombol HAPUS
        val deleteButton = Button(this).apply {
            text = "🗑 HAPUS LOG & LANJUT KE APLIKASI"
            textSize = 13f
            setTextColor(0xFFFF5252.toInt())
            setBackgroundColor(0xFF1A1F3A.toInt())
            setPadding(0, 28, 0, 28)
            setOnClickListener {
                try {
                    crashFile?.let { CrashHandler.deleteCrashFile(it) }
                } catch (e: Exception) {
                    // ignore
                }
                // Lanjut ke MainActivity
                val intent = Intent(this@CrashViewerActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        val deleteParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        deleteParams.setMargins(0, 0, 0, 12)
        deleteButton.layoutParams = deleteParams
        rootLayout.addView(deleteButton)

        // Tombol TUTUP (tanpa hapus)
        val closeButton = Button(this).apply {
            text = "TUTUP (Log Tetap Tersimpan)"
            textSize = 12f
            setTextColor(0xFFB0B7C3.toInt())
            setBackgroundColor(0xFF1A1F3A.toInt())
            setPadding(0, 24, 0, 24)
            setOnClickListener {
                // Lanjut ke MainActivity, log TIDAK dihapus
                val intent = Intent(this@CrashViewerActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        rootLayout.addView(closeButton)

        setContentView(rootLayout)
    }
}
