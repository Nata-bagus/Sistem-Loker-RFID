import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PilihLoker — Jendela pemilihan nomor loker
 * Muncul setelah kartu mahasiswa berhasil divalidasi di MainPanel
 *
 * Alur:
 *  1. Tampilkan grid semua loker (kosong = bisa dipilih, terisi = abu-abu)
 *  2. User klik nomor loker yang diinginkan
 *  3. Konfirmasi → kirim perintah BUKA_LOKER ke ESP32
 *  4. Update status loker di database → catat di akses_loker
 *  5. Tutup otomatis setelah loker terbuka
 */
public class PilihLoker extends JDialog {

    // ── Warna ─────────────────────────────────────────────────
    private static final Color C_BG        = new Color(15, 23, 42);
    private static final Color C_CARD      = new Color(30, 41, 59);
    private static final Color C_BORDER    = new Color(71, 85, 105);
    private static final Color C_WHITE     = Color.WHITE;
    private static final Color C_MUTED     = new Color(148, 163, 184);
    private static final Color C_PRIMARY   = new Color(59, 130, 246);
    private static final Color C_PRIMARY_D = new Color(37, 99, 235);
    private static final Color C_SUCCESS   = new Color(34, 197, 94);
    private static final Color C_SUCCESS_D = new Color(22, 163, 74);
    private static final Color C_DANGER    = new Color(239, 68, 68);
    private static final Color C_GRAY      = new Color(51, 65, 85);
    private static final Color C_GRAY_TEXT = new Color(100, 116, 139);

    // ── Font ──────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  16);
    private static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_LOKER  = new Font("Segoe UI", Font.BOLD,  18);
    private static final Font FONT_BTN    = new Font("Segoe UI", Font.BOLD,  12);

    // ── Data ──────────────────────────────────────────────────
    private final String namaUser;
    private final int    idUser;
    private final SerialHelper serialHelper;
    private int lokerDipilih = -1;

    private JLabel  lblStatus;
    private JPanel  gridLoker;
    private JButton btnBuka;
    private JLabel  lblLokerDipilih;

    // Daftar tombol loker untuk update warna
    private final List<JButton> tombolLoker = new ArrayList<>();

    public PilihLoker(Frame parent, String namaUser, int idUser, SerialHelper serialHelper) {
        super(parent, "Pilih Loker — " + namaUser, true);
        this.namaUser     = namaUser;
        this.idUser       = idUser;
        this.serialHelper = serialHelper;

        setSize(500, 560);
        setMinimumSize(new Dimension(440, 500));
        setLocationRelativeTo(parent);
        setResizable(false);

        initUI();
        muatDataLoker();
    }

    // ─────────────────────────────────────────────────────────
    //  UI Builder
    // ─────────────────────────────────────────────────────────
    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);

        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildCenter(),  BorderLayout.CENTER);
        root.add(buildBottom(),  BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_PRIMARY_D);
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel icon = new JLabel("📦", SwingConstants.LEFT);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JPanel txt = new JPanel(new GridLayout(2, 1, 0, 3));
        txt.setOpaque(false);
        txt.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JLabel title = new JLabel("Pilih Nomor Loker");
        title.setFont(FONT_TITLE);
        title.setForeground(C_WHITE);

        JLabel sub = new JLabel("Selamat datang, " + namaUser + "!");
        sub.setFont(FONT_BODY);
        sub.setForeground(new Color(186, 215, 255));

        txt.add(title);
        txt.add(sub);

        p.add(icon, BorderLayout.WEST);
        p.add(txt,  BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));

        // Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legend.setOpaque(false);
        addLegend(legend, C_PRIMARY, "Kosong (bisa dipilih)");
        addLegend(legend, C_GRAY,    "Terisi");
        addLegend(legend, C_SUCCESS, "Dipilih");

        // Grid loker
        gridLoker = new JPanel(new GridLayout(4, 5, 10, 10));
        gridLoker.setBackground(C_BG);
        gridLoker.setOpaque(false);

        // Status loading
        lblStatus = new JLabel("Memuat data loker...", SwingConstants.CENTER);
        lblStatus.setFont(FONT_BODY);
        lblStatus.setForeground(C_MUTED);

        p.add(legend,    BorderLayout.NORTH);
        p.add(gridLoker, BorderLayout.CENTER);
        p.add(lblStatus, BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildBottom() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(new Color(20, 30, 48));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        // Info loker dipilih
        lblLokerDipilih = new JLabel("Belum ada loker dipilih");
        lblLokerDipilih.setFont(FONT_BODY);
        lblLokerDipilih.setForeground(C_MUTED);

        // Tombol
        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tombol.setOpaque(false);

        JButton btnBatal = createButton("Batal", C_GRAY, C_WHITE);
        btnBatal.addActionListener(e -> dispose());

        btnBuka = createButton("🔓  Buka Loker", C_SUCCESS, C_WHITE);
        btnBuka.setEnabled(false);
        btnBuka.addActionListener(e -> bukaLokerDipilih());

        tombol.add(btnBatal);
        tombol.add(btnBuka);

        p.add(lblLokerDipilih, BorderLayout.CENTER);
        p.add(tombol,          BorderLayout.EAST);
        return p;
    }

    private void addLegend(JPanel p, Color warna, String teks) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setOpaque(false);
        JLabel kotak = new JLabel("  ");
        kotak.setOpaque(true);
        kotak.setBackground(warna);
        kotak.setPreferredSize(new Dimension(14, 14));
        JLabel label = new JLabel(teks);
        label.setFont(FONT_SMALL);
        label.setForeground(C_MUTED);
        item.add(kotak);
        item.add(label);
        p.add(item);
    }

    // ─────────────────────────────────────────────────────────
    //  Muat data loker dari database
    // ─────────────────────────────────────────────────────────
    private void muatDataLoker() {
        new Thread(() -> {
            String sql = "SELECT id_loker, nomor_loker, status FROM loker ORDER BY nomor_loker";
            try (Connection con = DatabaseHelper.getConnection();
                 Statement  st  = con.createStatement();
                 ResultSet  rs  = st.executeQuery(sql)) {

                List<int[]> dataLoker = new ArrayList<>();
                while (rs.next()) {
                    dataLoker.add(new int[]{
                        rs.getInt("id_loker"),
                        rs.getInt("nomor_loker"),
                        rs.getString("status").equals("kosong") ? 0 : 1
                    });
                }

                SwingUtilities.invokeLater(() -> {
                    gridLoker.removeAll();
                    tombolLoker.clear();

                    for (int[] loker : dataLoker) {
                        int idLoker     = loker[0];
                        int nomorLoker  = loker[1];
                        boolean terisi  = loker[2] == 1;

                        JButton btn = buatTombolLoker(nomorLoker, idLoker, terisi);
                        gridLoker.add(btn);
                        tombolLoker.add(btn);
                    }

                    int jumlahKosong = (int) dataLoker.stream()
                        .filter(l -> l[2] == 0).count();
                    lblStatus.setText(jumlahKosong + " loker tersedia dari "
                        + dataLoker.size() + " total loker");
                    lblStatus.setForeground(jumlahKosong > 0 ? C_SUCCESS : C_DANGER);

                    gridLoker.revalidate();
                    gridLoker.repaint();
                });

            } catch (SQLException e) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Gagal memuat loker: " + e.getMessage());
                    lblStatus.setForeground(C_DANGER);
                });
            }
        }).start();
    }

    /** Buat 1 tombol loker */
    private JButton buatTombolLoker(int nomorLoker, int idLoker, boolean terisi) {
        JButton btn = new JButton(String.valueOf(nomorLoker)) {
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

        btn.setFont(FONT_LOKER);
        btn.setForeground(terisi ? C_GRAY_TEXT : C_WHITE);
        btn.setBackground(terisi ? C_GRAY : C_PRIMARY);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(72, 72));
        btn.setToolTipText(terisi ? "Loker " + nomorLoker + " — Terisi" 
                                  : "Loker " + nomorLoker + " — Kosong");

        if (!terisi) {
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (lokerDipilih != idLoker)
                        btn.setBackground(C_PRIMARY_D);
                }
                public void mouseExited(MouseEvent e) {
                    if (lokerDipilih != idLoker)
                        btn.setBackground(C_PRIMARY);
                }
            });
            btn.addActionListener(e -> pilihLoker(idLoker, nomorLoker, btn));
        } else {
            btn.setEnabled(false);
        }

        return btn;
    }

    /** Saat user klik salah satu loker */
    private void pilihLoker(int idLoker, int nomorLoker, JButton btnDipilih) {
        // Reset warna tombol sebelumnya
        for (JButton b : tombolLoker) {
            if (b.isEnabled()) b.setBackground(C_PRIMARY);
        }

        // Highlight tombol yang dipilih
        btnDipilih.setBackground(C_SUCCESS);
        lokerDipilih = idLoker;

        // Update info bawah
        lblLokerDipilih.setText("Loker " + nomorLoker + " dipilih  →  klik Buka Loker");
        lblLokerDipilih.setForeground(C_SUCCESS);
        btnBuka.setEnabled(true);
    }

    /** Buka loker yang dipilih */
    private void bukaLokerDipilih() {
        if (lokerDipilih == -1) return;

        btnBuka.setEnabled(false);
        btnBuka.setText("⏳  Membuka...");

        new Thread(() -> {
            try (Connection con = DatabaseHelper.getConnection()) {
                con.setAutoCommit(false);
                try {
                    // 1. Update status loker → terisi
                    String sqlLoker = "UPDATE loker SET status = 'terisi' WHERE id_loker = ?";
                    try (PreparedStatement ps = con.prepareStatement(sqlLoker)) {
                        ps.setInt(1, lokerDipilih);
                        ps.executeUpdate();
                    }

                    // 2. Catat di akses_loker
                    String sqlAkses = "INSERT INTO akses_loker (id_user, id_loker, waktu_buka) VALUES (?, ?, NOW())";
                    try (PreparedStatement ps = con.prepareStatement(sqlAkses)) {
                        ps.setInt(1, idUser);
                        ps.setInt(2, lokerDipilih);
                        ps.executeUpdate();
                    }

                    con.commit();

                    // 3. Kirim perintah buka ke ESP32
                    if (serialHelper != null) {
                        serialHelper.kirimPerintah("BUKA_LOKER");
                    }

                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setText("✅  Loker berhasil dibuka! Silakan masukkan barang.");
                        lblStatus.setForeground(C_SUCCESS);

                        // Tutup dialog otomatis setelah 3 detik
                        Timer t = new Timer(3000, e -> dispose());
                        t.setRepeats(false);
                        t.start();
                    });

                } catch (SQLException ex) {
                    con.rollback();
                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setText("❌  Gagal: " + ex.getMessage());
                        lblStatus.setForeground(C_DANGER);
                        btnBuka.setEnabled(true);
                        btnBuka.setText("🔓  Buka Loker");
                    });
                }
            } catch (SQLException e) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("❌  Gagal koneksi: " + e.getMessage());
                    lblStatus.setForeground(C_DANGER);
                    btnBuka.setEnabled(true);
                    btnBuka.setText("🔓  Buka Loker");
                });
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────────────────
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
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.darker()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }
}
