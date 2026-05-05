#include <SPI.h>
#include <MFRC522.h>

// ── Pin ESP32-S3 ──────────────────────────────────────────────
#define SS_PIN   5    // SDA/SS RC522 → GPIO5
#define RST_PIN  25   // RST RC522 → GPIO22
#define RELAY_PIN 21   // Relay → GPIO4 (aktif LOW)
#define r 4
#define g 22

// ─────────────────────────────────────────────────────────────
MFRC522 rfid(SS_PIN, RST_PIN);

// Mode: "DAFTAR" atau "AKSES"
// Diatur dari Java lewat Serial: kirim "MODE:DAFTAR" atau "MODE:AKSES"
String modeSaat = "DAFTAR";

void setup() {
  Serial.begin(115200);       // Komunikasi ke Java/komputer
  SPI.begin();
  rfid.PCD_Init();

  pinMode(RELAY_PIN, OUTPUT);
  pinMode(r, OUTPUT);
  pinMode(g, OUTPUT);
  digitalWrite(RELAY_PIN, HIGH); // Relay OFF (aktif LOW)

  // Kasih tahu Java bahwa ESP32 sudah siap
  Serial.println("STATUS:SIAP");
  Serial.println("MODE:" + modeSaat);
}

void loop() {
  // ── Terima perintah dari Java lewat Serial ──
  if (Serial.available()) {
    String perintah = Serial.readStringUntil('\n');
    perintah.trim();

    if (perintah == "MODE:DAFTAR") {
      modeSaat = "DAFTAR";
      Serial.println("STATUS:MODE_DAFTAR");
    }
    else if (perintah == "MODE:AKSES") {
      modeSaat = "AKSES";
      Serial.println("STATUS:MODE_AKSES");
    }
    else if (perintah == "BUKA_LOKER" || perintah == "op") {
      bukaLoker();
    }
    else if (perintah == "wiww") {
      for (int i = 1; i <= 5; i++){
        digitalWrite(r, HIGH);
        delay(50);
        digitalWrite(r, LOW);
        delay(50);
      }
    }
    else if (perintah == "wiw") {
      digitalWrite(g, HIGH);
      delay(2000);
      digitalWrite(g, LOW);
    }
  }

  // ── Baca kartu RFID ──
  if (!rfid.PICC_IsNewCardPresent()) return;
  if (!rfid.PICC_ReadCardSerial())   return;

  // Ambil UID kartu
  String uid = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    if (rfid.uid.uidByte[i] < 0x10) uid += "0";
    uid += String(rfid.uid.uidByte[i], HEX);
  }
  uid.toUpperCase();

  // Kirim UID ke Java lewat Serial
  // Format: "UID:A1B2C3D4"
  Serial.println("UID:" + uid);

  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();

  // Delay agar tidak baca kartu terus-menerus
  delay(1500);
}

// Buka relay selama 3 detik lalu tutup kembali
void bukaLoker() {
  Serial.println("STATUS:LOKER_BUKA");
  digitalWrite(RELAY_PIN, LOW);   // Relay ON → kunci terbuka
  delay(5000);                     // Buka 3 detik
  digitalWrite(RELAY_PIN, HIGH);  // Relay OFF → kunci tertutup
  Serial.println("STATUS:LOKER_TUTUP");
}
