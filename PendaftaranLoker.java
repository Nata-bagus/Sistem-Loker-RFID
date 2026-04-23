import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sistem Loker RFID - Universitas Negeri Malang
 * GUI Pendaftaran Mahasiswa
 *
 * Kebutuhan:
 *   - MySQL Connector/J (mysql-connector-j-8.x.x.jar) di classpath
 *   - Database: lihat DatabaseHelper.java (buat tabel otomatis)
 *   - Simulasi RFID: tekan tombol "Scan Kartu" untuk mensimulasikan
 *     pembacaan RC522. Di hardware nyata, ganti dengan SerialPort / Pi4J.
 */
public class PendaftaranLoker extends JFrame {

    // ── Warna & Font ──────────────────────────────────────────────
    private static final Color C_BG        = new Color(245, 246, 250);
    private static final Color C_WHITE     = Color.WHITE;
    private static final Color C_PRIMARY   = new Color(37, 99, 235);
    private static final Color C_PRIMARY_L = new Color(219, 234, 254);
    private static final Color C_SUCCESS   = new Color(22, 163, 74);
    private static final Color C_SUCCESS_L = new Color(220, 252, 231);
    private static final Color C_DANGER    = new Color(220, 38, 38);
    private static final Color C_DANGER_L  = new Color(254, 226, 226);
    private static final Color C_WARNING   = new Color(217, 119, 6);
    private static final Color C_WARNING_L = new Color(254, 243, 199);
    private static final Color C_BORDER    = new Color(209, 213, 219);
    private static final Color C_TEXT      = new Color(17, 24, 39);
    private static final Color C_MUTED     = new Color(107, 114, 128);

    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,   18);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.BOLD,   12);
    private static final Font FONT_INPUT  = new Font("Segoe UI", Font.PLAIN,  13);
    private static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN,  12);
    private static final Font FONT_MONO   = new Font("Courier New", Font.BOLD, 13);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN,  11);
    private static final Font FONT_BTN    = new Font("Segoe UI", Font.BOLD,   13);

    // ── Komponen UI ───────────────────────────────────────────────
    private JTextField  tfNama;
    private JComboBox<String> cbProdi;
    private JTextField  tfIdKartu;
    private JButton     btnScan;
    private JButton     btnDaftar;
    private JButton     btnReset;
    private JButton     btnBack;
    private JLabel      lblStatus;
    private JLabel      lblDbStatus;
    private JLabel      lblRfidStatus;
    private JPanel      pnlStatusBar;

    // Simulasi: di hardware nyata, UID dibaca dari RC522 via serial
    private boolean kartuSudahDibaca = false;
    private SerialHelper serialHelper;
    private static final String SERIAL_PORT = "COM9";

    public PendaftaranLoker() {
        setTitle("Sistem Loker RFID — Universitas Negeri Malang");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(550, 660);
        setMinimumSize(new Dimension(460, 580));
        setLocationRelativeTo(null);
        setBackground(C_BG);

        initUI();
        checkDbConnection();
        initSerial();
    }

    // ─────────────────────────────────────────────────────────────
    //  UI Builder
    // ─────────────────────────────────────────────────────────────
    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);

        root.add(buildHeader(),    BorderLayout.NORTH);
        root.add(buildFormPanel(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    /** Header biru dengan logo & judul */
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_PRIMARY);
        p.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel icon = new JLabel("🛅");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));
        icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));

        JPanel txt = new JPanel(new GridLayout(2, 1, 0, 3));
        txt.setOpaque(false);

        JLabel title = new JLabel("PENDAFTARAN USER");
        title.setFont(FONT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Universitas Negeri Malang  ·  Sistem RFID");
        sub.setFont(FONT_BODY);
        sub.setForeground(new Color(186, 215, 255));

        txt.add(title);
        txt.add(sub);

        p.add(icon, BorderLayout.WEST);
        p.add(txt,  BorderLayout.CENTER);
        return p;
    }

    /** Panel utama dengan form & tombol */
    private JScrollPane buildFormPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ── Kartu putih untuk form ──
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(20, 22, 20, 22)
        ));

        card.add(sectionLabel("Data Mahasiswa"));
        card.add(Box.createVerticalStrut(12));

        // Nama
        card.add(fieldLabel("Nama Lengkap", true));
        card.add(Box.createVerticalStrut(5));
        tfNama = createTextField("Masukan nama lengkap mahasiswa");
        card.add(tfNama);
        card.add(Box.createVerticalStrut(14));

        // Prodi
        card.add(fieldLabel("Program Studi", true));
        card.add(Box.createVerticalStrut(5));
        cbProdi = createProdiCombo();
        card.add(cbProdi);
        card.add(Box.createVerticalStrut(20));

        card.add(separator());
        card.add(Box.createVerticalStrut(16));

        // RFID section
        card.add(sectionLabel("Kartu RFID"));
        card.add(Box.createVerticalStrut(4));

        JLabel hint = new JLabel("Tempelkan kartu mahasiswa ke modul RC522, lalu klik tombol Scan.");
        hint.setFont(FONT_SMALL);
        hint.setForeground(C_MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(hint);
        card.add(Box.createVerticalStrut(12));

        card.add(fieldLabel("ID Kartu (UID)", true));
        card.add(Box.createVerticalStrut(5));

        JPanel rfidRow = new JPanel(new BorderLayout(8, 0));
        rfidRow.setOpaque(false);
        rfidRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        tfIdKartu = new JTextField();
        tfIdKartu.setFont(FONT_MONO);
        tfIdKartu.setEditable(false);
        tfIdKartu.setBackground(C_PRIMARY_L);
        tfIdKartu.setForeground(C_PRIMARY);
        tfIdKartu.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_PRIMARY, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        tfIdKartu.setText("— Belum ada kartu —");


        rfidRow.add(tfIdKartu, BorderLayout.CENTER);
        rfidRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(rfidRow);
        card.add(Box.createVerticalStrut(8));

        lblRfidStatus = statusChip("⏳  Menunggu kartu ditempelkan...", C_WARNING_L, C_WARNING);
        card.add(lblRfidStatus);

        p.add(card);
        p.add(Box.createVerticalStrut(14));

        // ── Status simpan ──
        lblStatus = new JLabel(" ");
        lblStatus.setFont(FONT_BODY);
        lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lblStatus);
        p.add(Box.createVerticalStrut(14));

        // 1. Buat tombol dulu
        btnReset  = createButton("Reset", C_WHITE, C_TEXT);
        btnReset.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        btnReset.addActionListener(e -> resetForm());

        btnDaftar = createButton("Daftar & Simpan ke Database", C_PRIMARY, Color.WHITE);
        btnDaftar.addActionListener(e -> daftarkan());

        btnBack = createButton("Kembali", C_SUCCESS, Color.WHITE);
        btnBack.addActionListener(e -> Balik());

        // 2. Susun layout
        JPanel baris1 = new JPanel(new GridLayout(1, 1, 0, 0));
        baris1.setOpaque(false);
        baris1.add(btnDaftar);

        JPanel baris2 = new JPanel(new GridLayout(1, 2, 8, 0));
        baris2.setOpaque(false);
        baris2.add(btnReset);
        baris2.add(btnBack);

        JPanel btnRow = new JPanel(new GridLayout(2, 1, 0, 8));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.add(baris1);
        btnRow.add(baris2);

        p.add(btnRow);
        return new JScrollPane(p,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    /** Status bar bawah */
    private JPanel buildStatusBar() {
        pnlStatusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        pnlStatusBar.setBackground(new Color(31, 41, 55));

        lblDbStatus = makeStatusDot("DB: menghubungkan...", Color.ORANGE);
        JLabel rfid  = makeStatusDot("RC522: Siap", new Color(134, 239, 172));

        // Jam
        JLabel clock = new JLabel();
        clock.setFont(FONT_SMALL);
        clock.setForeground(new Color(156, 163, 175));
        Timer t = new Timer(1000, e -> {
            clock.setText(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss")));
        });
        t.start();
        clock.setText(LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss")));

        pnlStatusBar.add(lblDbStatus);
        pnlStatusBar.add(rfid);
        pnlStatusBar.add(Box.createHorizontalStrut(20));
        pnlStatusBar.add(clock);
        return pnlStatusBar;
    }

    // ─────────────────────────────────────────────────────────────
    //  Helper widget builders
    // ─────────────────────────────────────────────────────────────
    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(C_PRIMARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel fieldLabel(String text, boolean required) {
        String mark = required ? "  <span color='#DC2626'>*</span>" : "";
        JLabel l = new JLabel("<html><b>" + text + "</b>" + mark + "</html>");
        l.setFont(FONT_LABEL);
        l.setForeground(C_TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField createTextField(String placeholder) {
        JTextField tf = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(C_MUTED);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    Insets ins = getInsets();
                    g2.drawString(placeholder, ins.left + 2,
                        (getHeight() - g2.getFontMetrics().getHeight()) / 2
                        + g2.getFontMetrics().getAscent());
                    g2.dispose();
                }
            }
        };
        tf.setFont(FONT_INPUT);
        tf.setForeground(C_TEXT);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(C_PRIMARY, 2, true),
                    BorderFactory.createEmptyBorder(5, 9, 5, 9)));
            }
            public void focusLost(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(C_BORDER, 1, true),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            }
        });
        return tf;
    }

    private JComboBox<String> createProdiCombo() {
        String[] prodiList = {
            "-- Pilih Program Studi --",
            "S1 Teknik Informatika",
            "S1 Teknik Elektro",
            "S1 Teknik Mesin",
            "S1 Teknik Sipil",
            "S1 Tata Boga",
            "S1 Arsitektur",
            "S1 Pendidikan Teknik Informatika",
            "S1 Pendidikan Teknik Elektro",
            "S1 Pendidikan Teknik Mesin",
            "S1 Pendidikan Teknik Bangunan",
            "S1 Pendidikan Tata Boga",
        };
        JComboBox<String> cb = new JComboBox<>(prodiList);
        cb.setFont(FONT_INPUT);
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb.setBackground(C_WHITE);
        return cb;
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BTN);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.addMouseListener(new MouseAdapter() {
            Color orig = bg;
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.darker());
            }
            public void mouseExited(MouseEvent e)  { btn.setBackground(orig); }
        });
        return btn;
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(C_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private JLabel statusChip(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text) {
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
        l.setFont(FONT_SMALL);
        l.setForeground(fg);
        l.setBackground(bg);
        l.setOpaque(false);
        l.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel makeStatusDot(String text, Color dotColor) {
        JLabel l = new JLabel("● " + text);
        l.setFont(FONT_SMALL);
        l.setForeground(dotColor);
        return l;
    }

    // ─────────────────────────────────────────────────────────────
    //  Logika Pendaftaran → Database
    // ─────────────────────────────────────────────────────────────
    private void daftarkan() {
        // Validasi input
        String nama = tfNama.getText().trim();
        String prodi = (String) cbProdi.getSelectedItem();

        if (nama.isEmpty()) {
            showStatus("⚠  Nama tidak boleh kosong!", C_DANGER_L, C_DANGER);
            tfNama.requestFocus();
            return;
        } 
        
        if (nama.matches("[a-zA-Z\\s]+")) {
           
        } else {
            // Input TIDAK VALID (mengandung angka atau simbol)
            showStatus("Nama jangan menggunakan angka, huruf saja", C_DANGER_L, C_DANGER);
            tfNama.requestFocus();
            return;
        }
        
        if (prodi == null || prodi.startsWith("--")) {
            showStatus("⚠  Silakan pilih Program Studi!", C_DANGER_L, C_DANGER);
            cbProdi.requestFocus();
            return;
        }
        if (!kartuSudahDibaca) {
            showStatus("⚠  Kartu belum di-scan! Tempelkan kartu lalu klik Scan.", C_DANGER_L, C_DANGER);
            return;
        }

        String uidKartu = tfIdKartu.getText().trim();

        btnDaftar.setEnabled(false);
        btnDaftar.setText("⏳  Menyimpan...");

        // Jalankan di thread terpisah agar GUI tidak freeze
        new Thread(() -> {
            try {
                int kartuId = DatabaseHelper.simpanPendaftaran(nama, prodi, uidKartu);
                SwingUtilities.invokeLater(() -> {
                    showStatus("✅  Berhasil! " + nama + " terdaftar. ID Kartu DB: " + kartuId,
                        C_SUCCESS_L, C_SUCCESS);
                    btnDaftar.setEnabled(true);
                    btnDaftar.setText("💾  Daftar & Simpan ke Database");
                    JOptionPane.showMessageDialog(this,
                        "<html><b>Pendaftaran Berhasil!</b><br><br>" +
                        "Nama&nbsp;&nbsp;: " + nama + "<br>" +
                        "Prodi&nbsp;&nbsp;: " + prodi + "<br>" +
                        "UID Kartu: " + uidKartu + "<br>" +
                        "ID di DB : " + kartuId + "<br><br>" +
                        "<i>Kartu siap digunakan untuk membuka loker.</i></html>",
                        "Pendaftaran Berhasil", JOptionPane.INFORMATION_MESSAGE);
                    resetForm();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    showStatus("❌  Gagal simpan: " + ex.getMessage(), C_DANGER_L, C_DANGER);
                    btnDaftar.setEnabled(true);
                    btnDaftar.setText("💾  Daftar & Simpan ke Database");
                });
            }
        }).start();
    }

    private void resetForm() {
        tfNama.setText("");
        cbProdi.setSelectedIndex(0);
        tfIdKartu.setText("— Belum ada kartu —");
        kartuSudahDibaca = false;
        lblStatus.setText(" ");
        lblRfidStatus.setText("⏳  Menunggu kartu ditempelkan...");
        lblRfidStatus.setBackground(C_WARNING_L);
        lblRfidStatus.setForeground(C_WARNING);
    }
    
    private void Balik(){
        if (serialHelper != null){
            serialHelper.tutup();
            serialHelper = null;
        }
        this.setVisible(false);
        
        MainPanel main = new MainPanel();
        main.setVisible(true);
    }

    private void showStatus(String msg, Color bg, Color fg) {
        lblStatus.setText("<html><span style='padding:6px 10px;'>" + msg + "</span></html>");
        lblStatus.setOpaque(true);
        lblStatus.setBackground(bg);
        lblStatus.setForeground(fg);
        lblStatus.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(fg, 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
    }

    private void checkDbConnection() {
        new Thread(() -> {
            boolean ok = DatabaseHelper.testKoneksi();
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    lblDbStatus.setText("● DB: Terhubung ✓");
                    lblDbStatus.setForeground(new Color(134, 239, 172));
                } else {
                    lblDbStatus.setText("● DB: Gagal koneksi ✗");
                    lblDbStatus.setForeground(new Color(252, 165, 165));
                }
            });
        }).start();
    }
    
    private void initSerial() {
        serialHelper = new SerialHelper(SERIAL_PORT);
        serialHelper.setOnUIDDiterima(uid -> {
            SwingUtilities.invokeLater(() -> {
                tfIdKartu.setText(uid);
                kartuSudahDibaca = true;
                lblRfidStatus.setText("Kartu Terdeteksi: " + uid);
                lblRfidStatus.setBackground(C_SUCCESS_L);
                lblRfidStatus.setForeground(C_SUCCESS);
                
            });
        });   

    serialHelper.setOnStatusDiterima(status -> {
        System.out.println("[ESP32 Status] " + status);
    });

    boolean berhasil = serialHelper.mulaiMembaca();
    if (berhasil) {
        lblRfidStatus.setText("✅  ESP32 terhubung di " + SERIAL_PORT);
        lblRfidStatus.setBackground(C_SUCCESS_L);
        lblRfidStatus.setForeground(C_SUCCESS);
    } else {
        lblRfidStatus.setText("❌  ESP32 tidak ditemukan di " + SERIAL_PORT);
        lblRfidStatus.setBackground(C_DANGER_L);
        lblRfidStatus.setForeground(C_DANGER);
    }
}

    // ─────────────────────────────────────────────────────────────
    //  Main
    // ─────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Pakai sistem Look & Feel agar tombol terlihat rapi di Windows/macOS
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new PendaftaranLoker().setVisible(true));
    }
}