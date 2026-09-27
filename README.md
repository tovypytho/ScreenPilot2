<div align="center">

# ScreenPilot

**Tangkap soal di layar, analisis dengan Gemini, lalu gunakan jawabannya.**

Aplikasi Android native berbasis Kotlin dan Jetpack Compose.

[![Build Android](https://github.com/tovypytho/ScreenPilot2/actions/workflows/android-build.yml/badge.svg)](https://github.com/tovypytho/ScreenPilot2/actions/workflows/android-build.yml)
![Android 9+](https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)

</div>

---

## Cara kerja

```text
Aktifkan ScreenPilot → izinkan perekaman layar → ketuk bubble → Gemini menganalisis soal
```

| Jenis soal | Hasil yang diberikan |
| --- | --- |
| Pilihan ganda, satu jawaban | Nomor jawaban muncul pada popup kecil di atas layar. |
| Pilihan ganda, beberapa jawaban | Semua nomor yang benar muncul, misalnya `(1,2)`. |
| Essay atau isian | Jawaban otomatis disalin ke clipboard dan disimpan di riwayat. Pengguna tinggal menempelkannya. |
| Soal belum jelas atau terpotong | Aplikasi tidak membuat jawaban tebakan. |

Untuk soal yang memanjang melewati satu layar, **tekan lama bubble dua kali**: pertama untuk bagian awal, kedua setelah menggulir ke bagian lanjutan. ScreenPilot menggabungkan kedua tangkapan sebelum menganalisis.

> [!NOTE]
> Android 13 ke atas menampilkan konfirmasi sistem saat teks disalin. Itu adalah tampilan Android; ScreenPilot tidak menambahkan toast untuk penyalinan jawaban essay.

## Fitur utama

### Jawaban essay sesuai kebutuhan

- **Clipboard selalu aktif** untuk jawaban essay yang berhasil, tanpa perlu menekan tombol salin.
- **Notifikasi essay opsional** melalui switch **Notifikasi jawaban essay** di aplikasi. Bawaannya mati; jika dinyalakan, notifikasi tetap senyap dan clipboard tetap bekerja.
- Isi jawaban pada notifikasi disembunyikan di layar kunci. Riwayat tetap menyimpan hasil analisis.

Pada Android 15, sistem juga dapat menyamarkan isi notifikasi selama perekaman layar aktif. Jawaban essay tetap tersedia di clipboard.

### Pengelolaan hingga 10 Gemini API key

- Tambahkan, beri label, aktifkan/nonaktifkan, atau hapus key per slot. Key disimpan dengan penyimpanan terenkripsi berbasis **Android Keystore**.
- Strategi bawaan **Round Robin** menggilir slot yang aktif dan layak untuk permintaan berikutnya. Jika suatu key mendapat kegagalan yang dapat dialihkan, ScreenPilot mencoba key layak berikutnya sesuai aturan failover.
- Tombol **Cek Semua** dan **Cek** per slot mengirim permintaan teks singkat ke model yang dipilih. Hasil dan waktu pemeriksaan ditampilkan pada slot; pemeriksaan hanya berjalan saat diminta dan memakai sedikit kuota/token.
- Pilihan strategi lain, batas percobaan, serta aturan cooldown tetap tersedia di halaman aplikasi.

> [!IMPORTANT]
> Batas penggunaan Gemini berlaku **per proyek Google Cloud**, bukan per API key. Beberapa key dari proyek yang sama tetap berbagi kuota; Round Robin meratakan pemilihan key, bukan menambah kuota proyek.

### Kendali sesi dan diagnostik

ScreenPilot memakai MediaProjection dan foreground service selama sesi tangkapan layar aktif. Halaman aplikasi menyediakan pengaturan bubble, kualitas tangkapan, riwayat, serta informasi diagnostik sesi. Pengolahan JPEG dijalankan di luar thread utama untuk mengurangi hambatan pada tampilan saat screenshot diproses.

## Mulai menggunakan

1. Pasang APK debug dari artefak workflow [Build ScreenPilot Android](https://github.com/tovypytho/ScreenPilot2/actions/workflows/android-build.yml), atau build sendiri dengan langkah di bawah.
2. Buka aplikasi, masukkan minimal satu Gemini API key, lalu pilih model yang dapat digunakan oleh key tersebut.
3. Berikan izin **tampilan di atas aplikasi lain** dan setujui dialog **perekaman layar** saat mengaktifkan ScreenPilot. Pada Android 13 ke atas, berikan izin notifikasi jika ingin menerima notifikasi essay.
4. Ketuk bubble untuk soal pada satu layar, atau tekan lama dua kali untuk soal bertahap. Untuk essay, buka kolom jawaban tujuan lalu **Paste/Tempel**.

Pada Android 15, sistem dapat menghentikan sesi perekaman layar ketika ponsel dikunci atau pengguna menghentikan berbagi layar. Jika itu terjadi, buka aplikasi dan aktifkan sesi baru.

## Build dari source

| Kebutuhan | Versi proyek |
| --- | --- |
| JDK | 21 |
| Android SDK | Compile SDK 36.1, target SDK 35 |
| Android minimum | API 28 / Android 9 |

Buka root proyek di Android Studio dan tunggu Gradle selesai melakukan sinkronisasi. Untuk validasi melalui terminal:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

APK debug berada di `app/build/outputs/apk/debug/app-debug.apk`. Workflow GitHub Actions menjalankan compile, unit test, build APK, dan lint, kemudian mengunggah APK sebagai artefak **ScreenPilot-debug-apk**.

## Catatan privasi

API key dimasukkan saat aplikasi berjalan dan tidak perlu ditaruh di source, `.env`, `BuildConfig`, atau `local.properties`. Jawaban essay masuk ke clipboard Android dan riwayat aplikasi. Jika opsi penyimpanan screenshot dinyalakan, hasil tangkapan juga disimpan ke galeri sesuai pengaturan aplikasi.

---

<div align="center">

Dibuat untuk Android, dengan Kotlin, Jetpack Compose, dan Gemini API.

</div>
