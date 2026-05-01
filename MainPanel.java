import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MainPanel — Layar Utama Sistem Loker RFID
 * Universitas Negeri Malang
 *
 * Fitur:
 *  1. Login Admin via kartu RFID → buka AdminPanel
 *  2. Tombol Registrasi User → buka PendaftaranLoker
 *  3. Buka Loker → validasi kartu ke DB, kirim perintah ke ESP32
 *
 * ── KONFIGURASI ────────────────────────────────────────────
 *  UID_ADMIN   : UID chip kartu admin (ganti sesuai kartu admin)
 *  SERIAL_PORT : Port COM ESP32 (ganti sesuai Device Manager)
 * ────────────────────────────────────────────────────────────
 */
public class MainPanel extends JFrame {

    // ── ⚙ Konfigurasi ────────────────────────────────────────
    private static final String UID_ADMIN   = "5313D913"; // ← ganti UID kartu admin
    private static final String SERIAL_PORT = "COM3";     // ← ganti sesuai port ESP32
    // ─────────────────────────────────────────────────────────

    // ── Warna ─────────────────────────────────────────────────
    private static final Color C_BG        = new Color(15, 23, 42);
    private static final Color C_CARD      = new Color(30, 41, 59);
    private static final Color C_CARD2     = new Color(51, 65, 85);
    private static final Color C_PRIMARY   = new Color(59, 130, 246);
    private static final Color C_PRIMARY_D = new Color(37, 99, 235);
    private static final Color C_SUCCESS   = new Color(34, 197, 94);
    private static final Color C_SUCCESS_D = new Color(22, 163, 74);
    private static final Color C_DANGER    = new Color(239, 68, 68);
    private static final Color C_WARNING   = new Color(251, 191, 36);
    private static final Color C_WHITE     = Color.WHITE;
    private static final Color C_MUTED     = new Color(148, 163, 184);
    private static final Color C_BORDER    = new Color(71, 85, 105);

    // ── Font ──────────────────────────────────────────────────
    private static final Font FONT_HUGE   = new Font("Segoe UI", Font.BOLD,  28);
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  16);
    private static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_BTN    = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_MONO   = new Font("Courier New", Font.BOLD, 14);

    // ── Komponen ──────────────────────────────────────────────
    private JLabel      lblStatusKartu;
    private JLabel      lblUIDTerakhir;
    private JLabel      lblClock;
    private JLabel      lblDbStatus;
    private JLabel      lblEspStatus;
    private JPanel      pnlStatusBadge;
    private SerialHelper serialHelper;

    // State animasi
    private Timer timerReset;
    private Timer timerPulse;
    private boolean scanning = false;

    public MainPanel() {
        setTitle("Sistem Loker RFID — Universitas Negeri Malang");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(480, 660);
        setMinimumSize(new Dimension(420, 600));
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
        checkDb();
        initSerial();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (serialHelper != null) serialHelper.tutup();
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  UI Builder
    // ─────────────────────────────────────────────────────────
    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);

        root.add(buildHeader(),   BorderLayout.NORTH);
        root.add(buildCenter(),   BorderLayout.CENTER);
        root.add(buildStatusBar(),BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(32, 28, 16, 28));

        // Logo + judul
        JPanel top = new JPanel(new GridLayout(3, 1, 0, 6));
        top.setOpaque(false);

        JLabel logo = new JLabel("", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));

        JLabel title = new JLabel("SISTEM LOKER RFID", SwingConstants.CENTER);
        title.setFont(FONT_HUGE);
        title.setForeground(C_WHITE);

        JLabel sub = new JLabel("Universitas Negeri Malang", SwingConstants.CENTER);
        sub.setFont(FONT_BODY);
        sub.setForeground(C_MUTED);

        top.add(logo);
        top.add(title);
        top.add(sub);

        p.add(top, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(8, 24, 16, 24));

        // ── Kartu RFID area ──
        p.add(buildRfidArea());
        p.add(Box.createVerticalStrut(20));

        // ── 3 tombol aksi ──
        p.add(buildTombolAksi());
        p.add(Box.createVerticalStrut(20));

        // ── Info petunjuk ──
        p.add(buildPetunjuk());

        return p;
    }

    /** Area scan kartu — animasi + status */
    private JPanel buildRfidArea() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(C_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(28, 20, 28, 20)));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        // Ikon kartu animasi
        JLabel ikonKartu = new JLabel("💳", SwingConstants.CENTER);
        ikonKartu.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        ikonKartu.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Animasi pulse pada ikon
        timerPulse = new Timer(800, e -> {
            float alpha = scanning ? 0.5f : 1.0f;
            ikonKartu.setForeground(scanning
                ? new Color(59, 130, 246)
                : new Color(255, 255, 255));
        });
        timerPulse.start();

        // Status teks
        lblStatusKartu = new JLabel("Tempelkan Kartu", SwingConstants.CENTER);
        lblStatusKartu.setFont(FONT_TITLE);
        lblStatusKartu.setForeground(C_WHITE);
        lblStatusKartu.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("ke modul RFID di bawah", SwingConstants.CENTER);
        hint.setFont(FONT_BODY);
        hint.setForeground(C_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Badge status
        pnlStatusBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pnlStatusBadge.setOpaque(false);
        pnlStatusBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
        setBadge("● Menunggu kartu...", C_MUTED);

        // UID terakhir
        lblUIDTerakhir = new JLabel(" ", SwingConstants.CENTER);
        lblUIDTerakhir.setFont(FONT_MONO);
        lblUIDTerakhir.setForeground(C_MUTED);
        lblUIDTerakhir.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(ikonKartu);
        card.add(Box.createVerticalStrut(14));
        card.add(lblStatusKartu);
        card.add(Box.createVerticalStrut(4));
        card.add(hint);
        card.add(Box.createVerticalStrut(14));
        card.add(pnlStatusBadge);
        card.add(Box.createVerticalStrut(8));
        card.add(lblUIDTerakhir);

        return card;
    }

    /** 3 tombol: Buka Loker, Registrasi, Admin */
    private JPanel buildTombolAksi() {
        JPanel p = new JPanel(new GridLayout(3, 1, 0, 10));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        // Tombol 2: Registrasi User
        JButton btnRegistrasi = buatTombolBesar(
            "Registrasi User",
            "Daftarkan kartu mahasiswa baru",
            C_PRIMARY, C_PRIMARY_D);
        btnRegistrasi.addActionListener(e -> bukaRegistrasi());

        // Tombol 3: Login Admin
        JButton btnAdmin = buatTombolBesar(
            "Login Admin",
            "Tempelkan kartu admin ke RFID",
            C_CARD2, C_BORDER);
        btnAdmin.addActionListener(e ->
            showInfo("Tempelkan kartu admin ke RFID untuk membuka Admin Panel."));
        
        p.add(btnRegistrasi);


        return p;
    }

    /** Panel petunjuk kecil di bawah */
    private JPanel buildPetunjuk() {
        JPanel p = new JPanel(new GridLayout(3, 1, 0, 4));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        addPetunjuk(p, "🟢", "Kartu terdaftar  →  Loker terbuka otomatis");
        addPetunjuk(p, "🔵", "Kartu admin       →  Admin Panel terbuka");
        addPetunjuk(p, "🔴", "Kartu tidak dikenal  →  Akses ditolak");

        return p;
    }

    private void addPetunjuk(JPanel p, String ikon, String teks) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        JLabel lIkon = new JLabel(ikon);
        lIkon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));
        JLabel lTeks = new JLabel(teks);
        lTeks.setFont(FONT_SMALL);
        lTeks.setForeground(C_MUTED);
        row.add(lIkon);
        row.add(lTeks);
        p.add(row);
    }

    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(15, 23, 42));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

        JPanel kiri = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        kiri.setOpaque(false);

        lblDbStatus  = makeStatusDot("DB: ...", C_MUTED);
        lblEspStatus = makeStatusDot("ESP32: ...", C_MUTED);
        kiri.add(lblDbStatus);
        kiri.add(lblEspStatus);

        lblClock = new JLabel();
        lblClock.setFont(FONT_SMALL);
        lblClock.setForeground(C_MUTED);
        lblClock.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));

        Timer tClock = new Timer(1000, e -> lblClock.setText(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss"))));
        tClock.start();
        lblClock.setText(LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss")));

        p.add(kiri,     BorderLayout.WEST);
        p.add(lblClock, BorderLayout.EAST);
        return p;
    }

    // ─────────────────────────────────────────────────────────
    //  Logika Utama — proses kartu yang ditempel
    // ─────────────────────────────────────────────────────────
        private void prosesKartu(String uid) {
            scanning = true;

            if (timerReset != null) timerReset.stop();
            timerReset = new Timer(4000, e -> resetTampilan());
            timerReset.setRepeats(false);
            timerReset.start();

            // Cek admin
            if (uid.equalsIgnoreCase(UID_ADMIN)) {
                System.out.println("[DEBUG] Kartu admin terdeteksi");
                tampilkanHasil("Admin Terdeteksi", "Membuka Admin Panel...",
                    C_PRIMARY, true);
                Timer t = new Timer(800, e -> bukaAdminPanel());
                t.setRepeats(false);
                t.start();
                return;
            }

            // Validasi ke DB
            new Thread(() -> {
                System.out.println("[DEBUG] Validasi UID ke database: " + uid);
                String nama = DatabaseHelper.validasiKartu(uid);
                System.out.println("[DEBUG] Hasil validasi: " + (nama != null ? nama : "TIDAK DITEMUKAN"));

                SwingUtilities.invokeLater(() -> {
                    if (nama != null) {
                        int idUser = DatabaseHelper.getIdUserByUID(uid);
                        System.out.println("[DEBUG] id_user: " + idUser);
                        int idLokerAktif = DatabaseHelper.getLokerAktifUser(idUser);
                        System.out.println("[DEBUG] id_loker_aktif: " + idLokerAktif);
                        if (serialHelper != null) serialHelper.kirimPerintah("wiw");
                        

                        tampilkanHasil(
                            "Selamat datang, " + nama + "!",
                            "Memeriksa status loker...",
                            C_SUCCESS, true);

                        Timer t = new Timer(800, e -> {
                            if (idLokerAktif != -1) {
                                int nomorLoker = DatabaseHelper.getNomorLoker(idLokerAktif);
                                tampilkanDialogTutupLoker(nama, idUser, idLokerAktif, nomorLoker);
                            } else {
                                PilihLoker dialog = new PilihLoker(
                                    MainPanel.this, nama, idUser, serialHelper);
                                dialog.setVisible(true);
                                resetTampilan();
                            }
                        });
                        t.setRepeats(false);
                        t.start();
                    } else {
                        if (serialHelper != null) serialHelper.kirimPerintah("wiww");
                        
                        tampilkanHasil(
                            "Kartu Tidak Dikenal",
                            "UID tidak terdaftar di database",
                            C_DANGER, false);
                    }
                });
            }).start();
        }
    
        private void tampilkanDialogTutupLoker(String nama, int idUser, 
                                            int idLoker, int nomorLoker) {
        // Buat dialog konfirmasi tutup loker
        JDialog dialog = new JDialog(this, "Tutup Loker", true);
        dialog.setSize(380, 260);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(15, 23, 42));

        // Header
        JPanel header = new JPanel(new GridLayout(3, 1, 0, 4));
        header.setBackground(new Color(22, 163, 74));
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel lJudul = new JLabel("Loker Aktif Ditemukan");
        lJudul.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lJudul.setForeground(Color.WHITE);

        JLabel lNama = new JLabel("User: " + nama);
        lNama.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lNama.setForeground(new Color(187, 247, 208));

        JLabel lLoker = new JLabel("Loker nomor: " + nomorLoker + " sedang terpakai");
        lLoker.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lLoker.setForeground(new Color(187, 247, 208));

        header.add(lJudul);
        header.add(lNama);
        header.add(lLoker);

        // Isi
        JPanel isi = new JPanel(new GridLayout(2, 1, 0, 8));
        isi.setBackground(new Color(15, 23, 42));
        isi.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel lPertanyaan = new JLabel(
            "<html>Apakah kamu ingin <b>mengambil barang</b> dan<br>" +
            "mengosongkan loker " + nomorLoker + "?</html>");
        lPertanyaan.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lPertanyaan.setForeground(new Color(148, 163, 184));

        isi.add(lPertanyaan);

        // Tombol
        JPanel tombol = new JPanel(new GridLayout(1, 2, 10, 0));
        tombol.setOpaque(false);

        JButton btnBatal = new JButton("Batal");
        btnBatal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBatal.setBackground(new Color(51, 65, 85));
        btnBatal.setForeground(Color.WHITE);
        btnBatal.setBorderPainted(false);
        btnBatal.setFocusPainted(false);
        btnBatal.addActionListener(e -> {
            dialog.dispose();
            resetTampilan();
        });

        JButton btnTutup = new JButton("Tutup & Kosongkan Loker");
        btnTutup.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnTutup.setBackground(new Color(34, 197, 94));
        btnTutup.setForeground(Color.WHITE);
        btnTutup.setBorderPainted(false);
        btnTutup.setFocusPainted(false);
        btnTutup.addActionListener(e -> {
            // Kirim perintah buka ke ESP32 agar user bisa ambil barang
            if (serialHelper != null) serialHelper.kirimPerintah("BUKA_LOKER");

            // Update database
            new Thread(() -> {
                boolean ok = DatabaseHelper.tutupLoker(idUser, idLoker);
                SwingUtilities.invokeLater(() -> {
                    dialog.dispose();
                    if (ok) {
                        tampilkanHasil(
                            "Loker " + nomorLoker + " dikosongkan!",
                            "Silakan ambil barang kamu.",
                            C_SUCCESS, true);
                    } else {
                        tampilkanHasil(
                            "Gagal menutup loker",
                            "Coba tempelkan kartu kembali.",
                            C_DANGER, false);
                    }
                    Timer t = new Timer(4000, ev -> resetTampilan());
                    t.setRepeats(false);
                    t.start();
                });
            }).start();
        });

        tombol.add(btnBatal);
        tombol.add(btnTutup);
        isi.add(tombol);

        root.add(header, BorderLayout.NORTH);
        root.add(isi,    BorderLayout.CENTER);

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private void tampilkanHasil(String judul, String sub, Color warna, boolean berhasil) {
        lblStatusKartu.setText(judul);
        lblStatusKartu.setForeground(warna);
        setBadge((berhasil ? "● " : "● ") + sub, warna);

        // Warnai border card
        Component rfidArea = ((JPanel) getContentPane()
            .getComponent(1)).getComponent(0);
        if (rfidArea instanceof JPanel) {
            ((JPanel) rfidArea).setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(warna, 2, true),
                BorderFactory.createEmptyBorder(28, 20, 28, 20)));
        }
    }

    private void resetTampilan() {
        scanning = false;
        lblStatusKartu.setText("Tempelkan Kartu");
        lblStatusKartu.setForeground(C_WHITE);
        lblUIDTerakhir.setText(" ");
        setBadge("Menunggu kartu...", C_MUTED);

        Component rfidArea = ((JPanel) getContentPane()
            .getComponent(1)).getComponent(0);
        if (rfidArea instanceof JPanel) {
            ((JPanel) rfidArea).setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(C_BORDER, 1, true),
                BorderFactory.createEmptyBorder(28, 20, 28, 20)));
        }
    }

    private void setBadge(String teks, Color warna) {
        pnlStatusBadge.removeAll();
        JLabel badge = new JLabel(teks);
        badge.setFont(FONT_SMALL);
        badge.setForeground(warna);
        pnlStatusBadge.add(badge);
        pnlStatusBadge.revalidate();
        pnlStatusBadge.repaint();
    }

    // ─────────────────────────────────────────────────────────
    //  Buka jendela lain
    // ─────────────────────────────────────────────────────────
    private void bukaAdminPanel() {
        AdminPanel admin = new AdminPanel(serialHelper);
        admin.setVisible(true);
        admin.setAlwaysOnTop(true);
    }

    private void bukaRegistrasi() {
        // 1. Lepas port Serial agar PendaftaranLoker bisa pakai
        if (serialHelper != null) {
            serialHelper.tutup();
            serialHelper = null;
        }

        // 2. Sembunyikan MainPanel
        this.setVisible(false);

        // 3. Buka PendaftaranLoker
        PendaftaranLoker pendaftaran = new PendaftaranLoker();
        pendaftaran.setVisible(true);

        // 4. Saat PendaftaranLoker ditutup → MainPanel aktif kembali
        pendaftaran.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // Tunggu sebentar agar port benar-benar dilepas PendaftaranLoker
                Timer t = new Timer(1500, ev -> {
                    initSerial();      // hubungkan serial kembali
                    setVisible(true);  // tampilkan MainPanel
                    toFront();
                });
                t.setRepeats(false);
                t.start();
            }
        });
    }

    private void showInfo(String pesan) {
        setBadge("ℹ  " + pesan, C_PRIMARY);
        Timer t = new Timer(3000, e -> setBadge("● Menunggu kartu...", C_MUTED));
        t.setRepeats(false);
        t.start();
    }

    // ─────────────────────────────────────────────────────────
    //  Serial & Database
    // ─────────────────────────────────────────────────────────
    private void initSerial() {
        serialHelper = new SerialHelper(SERIAL_PORT);

        // Saat kartu ditempel → proses di sini
        serialHelper.setOnUIDDiterima(uid -> {
            SwingUtilities.invokeLater(() -> prosesKartu(uid));
        });

        serialHelper.setOnStatusDiterima(status -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("[ESP32 Status] " + status);
                if ("LOKER_BUKA".equals(status)) {
                    setBadge("Loker sedang terbuka (5 detik)...", C_SUCCESS);
                } else if ("LOKER_TUTUP".equals(status)) {
                    setBadge("Loker tertutup kembali", C_MUTED);
                }
            });
        });

        boolean ok = serialHelper.mulaiMembaca();
        if (ok) {
            lblEspStatus.setText("ESP32: Terhubung");
            lblEspStatus.setForeground(new Color(134, 239, 172));
        } else {
            lblEspStatus.setText("ESP32: Tidak ditemukan");
            lblEspStatus.setForeground(new Color(252, 165, 165));
        }
    }

    private void checkDb() {
        new Thread(() -> {
            boolean ok = DatabaseHelper.testKoneksi();
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    lblDbStatus.setText("DB: Terhubung");
                    lblDbStatus.setForeground(new Color(134, 239, 172));
                } else {
                    lblDbStatus.setText("DB: Gagal");
                    lblDbStatus.setForeground(new Color(252, 165, 165));
                }
            });
        }).start();
    }

    // ─────────────────────────────────────────────────────────
    //  Helper Widget
    // ─────────────────────────────────────────────────────────
    private JButton buatTombolBesar(String judul, String sub, Color bg, Color bgHover) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setLayout(new BorderLayout(0, 2));
        btn.setBackground(bg);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel lJudul = new JLabel(judul);
        lJudul.setFont(FONT_BTN);
        lJudul.setForeground(C_WHITE);

        JLabel lSub = new JLabel(sub);
        lSub.setFont(FONT_SMALL);
        lSub.setForeground(new Color(255, 255, 255, 160));

        btn.add(lJudul, BorderLayout.CENTER);
        btn.add(lSub,   BorderLayout.SOUTH);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bgHover); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });

        return btn;
    }

    private JLabel makeStatusDot(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SMALL);
        l.setForeground(color);
        return l;
    }

    // ─────────────────────────────────────────────────────────
    //  Main — jalankan MainPanel sebagai entry point utama
    // ─────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new MainPanel().setVisible(true));
    }
}
