
import java.time.LocalDate;
import java.time.Period;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Supplier;
import javax.swing.JTextArea;
import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author mhmmd
 */
public class PenghitungUmurHelper {
    // Menghitung umur secara detail (tahun, bulan, hari)
    public String hitungUmurDetail(LocalDate lahir, LocalDate sekarang) {
        Period period = Period.between(lahir, sekarang);
    
        return period.getYears() + " tahun, " + period.getMonths() + " bulan, " + period.getDays() + " hari";
    }
    
    // Menghitung hari ulang tahun berikutnya
    public LocalDate hariUlangTahunBerikutnya(LocalDate lahir, LocalDate sekarang) {
        LocalDate ulangTahunBerikutnya = lahir.withYear(sekarang.getYear());

        if (!ulangTahunBerikutnya.isAfter(sekarang)) {
            ulangTahunBerikutnya = ulangTahunBerikutnya.plusYears(1);
        }

        return ulangTahunBerikutnya;
    }
    
    // Menerjemahkan teks hari ke bahasa Indonesia
    public String getDayOfWeekInIndonesian(LocalDate date) {
    
    switch (date.getDayOfWeek()) {
        case MONDAY:
            return "Senin";
        
        case TUESDAY:
            return "Selasa";
        
        case WEDNESDAY:
            return "Rabu";
        
        case THURSDAY:
            return "Kamis";
        
        case FRIDAY:
            return "Jumat";
    
        case SATURDAY:
            return "Sabtu";
    
        case SUNDAY:
            return "Minggu";
    
        default:
            return "";
        }
    }
    
    // Mendapatkan peristiwa penting secara baris per baris
    public void getPeristiwaBarisPerBaris(LocalDate tanggal, JTextArea txtAreaPeristiwa, Supplier<Boolean> shouldStop) {
        try {
            // Periksa jika thread seharusnya dihentikan sebelum dimulai
            if (shouldStop.get()) {
                return;
            }

            // Membuat URL untuk mengambil data peristiwa
            String urlString = "https://byabbe.se/on-this-day/" +
                               tanggal.getMonthValue() + "/" +
                               tanggal.getDayOfMonth() + "/events.json";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            // Mengecek kode respons dari server
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("HTTP response code: " + responseCode +
                                    ". Silakan coba lagi nanti atau cek koneksi internet.");
            }

            // Membaca data dari koneksi
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                // Periksa jika thread seharusnya dihentikan saat membaca data
                if (shouldStop.get()) {
                    in.close();
                    conn.disconnect();
                    javax.swing.SwingUtilities.invokeLater(() -> 
                        txtAreaPeristiwa.setText("Pengambilan data dibatalkan.\n")
                    );
                    return;
                }
                content.append(inputLine);
            }

            in.close();
            conn.disconnect();

            // Parsing JSON
            JSONObject json = new JSONObject(content.toString());
            JSONArray events = json.getJSONArray("events");

            for (int i = 0; i < events.length(); i++) {
                // Periksa jika thread seharusnya dihentikan sebelum memproses data
                if (shouldStop.get()) {
                    javax.swing.SwingUtilities.invokeLater(() -> 
                        txtAreaPeristiwa.setText("Pengambilan data dibatalkan.\n")
                    );
                    return;
                }

                // Mendapatkan detail peristiwa
                JSONObject event = events.getJSONObject(i);
                String year = event.getString("year");
                String description = event.getString("description");
                String translatedDescription = translateToIndonesian(description);
                String peristiwa = year + ": " + translatedDescription;

                // Menambahkan peristiwa ke JTextArea
                javax.swing.SwingUtilities.invokeLater(() -> 
                    txtAreaPeristiwa.append(peristiwa + "\n")
                );
            }

            // Jika tidak ada peristiwa pada tanggal yang diminta
            if (events.length() == 0) {
                javax.swing.SwingUtilities.invokeLater(() -> 
                    txtAreaPeristiwa.setText("Tidak ada peristiwa penting yang ditemukan pada tanggal ini.")
                );
            }
        } catch (Exception e) {
            // Menangani kesalahan dan menampilkan pesan error di JTextArea
            javax.swing.SwingUtilities.invokeLater(() -> 
                txtAreaPeristiwa.setText("Gagal mendapatkan data peristiwa: " + e.getMessage())
            );
        }
    }

    // Menerjemahkan teks ke bahasa Indonesia
    private String translateToIndonesian(String text) {
        try {
            // Mengonversi teks ke URL dengan encoding
            String urlString = "https://lingva.ml/api/v1/en/id/" + text.replace(" ", "%20");
            URL url = new URL(urlString);

            // Membuka koneksi HTTP
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            // Memeriksa kode respons
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP response code: " + responseCode);
            }

            // Membaca respons dari server
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            // Menutup stream dan koneksi
            in.close();
            conn.disconnect();

            // Parsing JSON untuk mendapatkan terjemahan
            JSONObject json = new JSONObject(content.toString());
            return json.getString("translation");

        } catch (Exception e) {
            // Mengembalikan teks asli dengan pesan gagal diterjemahkan
            return text + " (Gagal diterjemahkan)";
        }
    }

}
