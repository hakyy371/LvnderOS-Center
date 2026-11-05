package com.lvnder.os;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final int SHIZUKU_CODE = 1001;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Membuat WebView layar penuh secara programmatikal
        webView = new WebView(this);
        setContentView(webView);

        // Atur setelan WebView agar mendukung aset lokal dan JavaScript
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.setWebViewClient(new WebViewClient());

        // Daftarkan jembatan penghubung dari HTML ke Java dengan nama "AndroidApp"
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidApp");

        // Muat file dashboard dari folder assets
        webView.loadUrl("file:///android_asset/index.html");

        // Ekstrak semua file biner internal agar siap dieksekusi Shizuku
        ekstrakBinerTweak();

        // Cek dan minta izin Shizuku saat aplikasi dibuka
        periksaIzinShizuku();
    }

    private void periksaIzinShizuku() {
        try {
            if (Shizuku.isPreV11()) return;

            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                showToast("Shizuku Terhubung & Diizinkan!");
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                showToast("Aplikasi membutuhkan izin Shizuku untuk menjalankan tweak.");
                Shizuku.requestPermission(SHIZUKU_CODE);
            } else {
                Shizuku.requestPermission(SHIZUKU_CODE);
            }
        } catch (Exception e) {
            showToast("Gagal memeriksa Shizuku. Pastikan Shizuku aktif!");
        }
    }

    // Fungsi untuk menyalin file biner dari folder resources ke folder privat aplikasi di HP
    private void ekstrakBinerTweak() {
        String[] binerFiles = {
            "lvnderapp", "lvnderMLBB", "lvnderr60HZ", "lvnderr90HZ", "lvnderr120HZ",
            "lvnderrCPU", "lvnderrdevice", "lvnderrDriverGame", "lvnderrNetwork",
            "lvnderrPERF", "lvnderrRender", "lvnderrSqlite"
        };

        File binDir = new File(getFilesDir(), "bin");
        if (!binDir.exists()) binDir.mkdirs();

        for (String file : binerFiles) {
            File targetFile = new File(binDir, file);
            // Ekstrak ulang file untuk memastikan versi terbaru selalu terpasang
            try (InputStream is = getResources().openRawResource(
                    getResources().getIdentifier(file.toLowerCase(), "raw", getPackageName()));
                 FileOutputStream os = new FileOutputStream(targetFile)) {
                
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                // Beri izin eksekusi (chmod +x) pada file biner tersebut
                targetFile.setExecutable(true, false);
            } catch (Exception ignored) {}
        }
    }

    private void showToast(String pesan) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, pesan, Toast.LENGTH_SHORT).show());
    }

    // Wadah perantara/Interface yang dipanggil oleh tombol di HTML
    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void jalankanBiner(String namaBiner, String argumen) {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                showToast("Izin Shizuku belum diberikan!");
                return;
            }

            // Arahkan jalur eksekusi ke biner privat yang sudah diekstrak di dalam aplikasi
            String jalurBiner = mContext.getFilesDir().getAbsolutePath() + "/bin/" + namaBiner;
            String perintahLengkap = "sh " + jalurBiner + " " + argumen;

            try {
                // Shizuku mengeksekusi perintah biner lewat sistem ADB Shell (sh) resmi
                Shizuku.newProcess(perintahLengkap.split(" "), null, null);
                showToast("Tweak " + namaBiner + " berhasil dipicu!");
            } catch (Exception e) {
                showToast("Gagal mengeksekusi biner via Shizuku!");
            }
        }
    }
}
