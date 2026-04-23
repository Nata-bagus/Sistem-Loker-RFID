import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class SerialHelper {

    private SerialPort port;
    private Thread threadBaca;
    private boolean sedangBerjalan = false;

    private Consumer<String> onUIDDiterima;
    private Consumer<String> onStatusDiterima;

    public SerialHelper(String namaPort) {
        port = SerialPort.getCommPort(namaPort);
        // Sesuaikan Baud Rate dengan rfid_loker.ino (115200)
        port.setBaudRate(115200);
        port.setNumDataBits(8);
        port.setNumStopBits(1);
        port.setParity(0);
        // Timeout sangat penting agar Java tidak "freeze" saat menunggu data
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0);
    }

    public void setOnUIDDiterima(Consumer<String> callback) {
        this.onUIDDiterima = callback;
    }

    public void setOnStatusDiterima(Consumer<String> callback) {
        this.onStatusDiterima = callback;
    }

    public boolean mulaiMembaca() {
        if (port.openPort()) {
            // AKTIFKAN DTR & RTS (Wajib untuk ESP32-S3)
            port.setDTR();
            port.setRTS();
            
            System.out.println("[Serial] Berhasil membuka port: " + port.getSystemPortName());
            sedangBerjalan = true;

            threadBaca = new Thread(() -> {
                // Menggunakan BufferedReader karena lebih handal daripada Scanner untuk Serial
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(port.getInputStream()))) {
                    while (sedangBerjalan && port.isOpen()) {
                        if (port.bytesAvailable() > 0) {
                            String data = reader.readLine();
                            if (data != null) {
                                data = data.trim();
                                System.out.println("[ESP32 -> Java] " + data); // Debug di console

                                if (data.startsWith("UID:") && onUIDDiterima != null) {
                                    onUIDDiterima.accept(data.substring(4));
                                } else if (data.startsWith("STATUS:") && onStatusDiterima != null) {
                                    onStatusDiterima.accept(data.substring(7));
                                }
                            }
                        }
                        Thread.sleep(20); // Delay kecil agar tidak membebani CPU
                    }
                } catch (Exception e) {
                    System.err.println("[Serial] Error saat membaca: " + e.getMessage());
                }
            });

            threadBaca.setDaemon(true);
            threadBaca.start();
            return true;
        } else {
            System.err.println("[Serial] Gagal membuka port.");
            return false;
        }
    }

    public void kirimPerintah(String perintah) {
        if (port != null && port.isOpen()) {
            byte[] data = (perintah + "\n").getBytes();
            port.writeBytes(data, data.length);
            System.out.println("[Java -> ESP32] " + perintah);
        }
    }

    public void tutup() {
        sedangBerjalan = false;
        if (port != null && port.isOpen()) {
            port.closePort();
            System.out.println("[Serial] Port ditutup.");
        }
    }
}