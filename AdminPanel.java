import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AdminPanel — GUI Admin Sistem Loker RFID
 * Universitas Negeri Malang
 *
 * Fitur:
 *  - Lihat semua user terdaftar
 *  - Hapus user dari database
 *  - Block user (kartu jadi nonaktif, tidak bisa akses loker)
 *  - Unblock user (aktifkan kembali kartu)
 *  - Cari user berdasarkan nama / prodi
 */
public class AdminPanel extends BaseAdminPanel {

    // ── Warna ─────────────────────────────────────────────────
    private static final Color C_BG        = new Color(245, 246, 250);
    private static final Color C_WHITE     = Color.WHITE;
    private static final Color C_PRIMARY   = new Color(37, 99, 235);
    private static final Color C_PRIMARY_D = new Color(29, 78, 216);
    private static final Color C_DANGER    = new Color(220, 38, 38);
    private static final Color C_DANGER_L  = new Color(254, 226, 226);
    private static final Color C_SUCCESS   = new Color(22, 163, 74);
    private static final Color C_SUCCESS_L = new Color(220, 252, 231);
    private static final Color C_WARNING   = new Color(217, 119, 6);
    private static final Color C_WARNING_L = new Color(254, 243, 199);
    private static final Color C_BORDER    = new Color(209, 213, 219);
    private static final Color C_TEXT      = new Color(17, 24, 39);
    private static final Color C_MUTED     = new Color(107, 114, 128);
    private static final Color C_HEADER_BG = new Color(248, 250, 252);
    private static final Color C_ROW_ALT   = new Color(249, 250, 251);
    private static final Color C_BLOCKED   = new Color(254, 242, 242);

    // ── Font ──────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  18);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.BOLD,  12);
    private static final Font FONT_INPUT  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_BTN    = new Font("Segoe UI", Font.BOLD,  12);
    private static final Font FONT_TABLE  = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_MONO   = new Font("Courier New", Font.PLAIN, 12);

    // ── Komponen ──────────────────────────────────────────────
    private JTable          tabelUser;
    private DefaultTableModel modelTabel;
    private JTextField      tfCari;
    private JLabel          lblStatus;
    private JLabel          lblDbStatus;
    private JLabel          lblJumlahUser;
    private JButton         btnBlock, btnEmergency, btnHapus, btnRefresh, btnUnblock;
    private SerialHelper serialHelper;
    

    // Kolom tabel
    private static final String[] KOLOM = {
        "ID", "Nama Mahasiswa", "Program Studi", "UID Kartu", "Status Kartu", "Terdaftar"
    };

    public AdminPanel(SerialHelper serialHelper) {
        this.serialHelper = serialHelper;
        setTitle("Admin Panel — Sistem Loker RFID Universitas Negeri Malang");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(920, 620);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);

        initUI();
        muatDataUser();
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
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setBackground(C_PRIMARY);
        p.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        // Kiri: ikon + judul
        JLabel icon = new JLabel("🛡");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        JPanel txt = new JPanel(new GridLayout(2, 1, 0, 2));
        txt.setOpaque(false);
        JLabel title = new JLabel("Admin Panel — Manajemen User");
        title.setFont(FONT_TITLE);
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Sistem Loker RFID  ·  Universitas Negeri Malang");
        sub.setFont(FONT_BODY);
        sub.setForeground(new Color(186, 215, 255));
        txt.add(title);
        txt.add(sub);

        // Kanan: tombol buka form pendaftaran
        p.add(icon,             BorderLayout.WEST);
        p.add(txt,              BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        p.add(buildToolbar(), BorderLayout.NORTH);
        p.add(buildTabel(),   BorderLayout.CENTER);
        p.add(buildActionBar(), BorderLayout.SOUTH);

        return p;
    }

    /** Toolbar: search + refresh + jumlah user */
    private JPanel buildToolbar() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);

        // Search
        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setBackground(C_WHITE);
        searchBox.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        searchBox.setMaximumSize(new Dimension(320, 36));

        JLabel iconSearch = new JLabel("🔍");
        iconSearch.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        tfCari = new JTextField();
        tfCari.setFont(FONT_INPUT);
        tfCari.setBorder(null);
        tfCari.setBackground(C_WHITE);
        tfCari.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filterTabel(tfCari.getText()); }
        });

        searchBox.add(iconSearch, BorderLayout.WEST);
        searchBox.add(tfCari,     BorderLayout.CENTER);

        // Kanan: label jumlah + refresh
        JPanel kanan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        kanan.setOpaque(false);

        lblJumlahUser = new JLabel("0 user");
        lblJumlahUser.setFont(FONT_BODY);
        lblJumlahUser.setForeground(C_MUTED);

        btnRefresh = createButton("Refresh", C_WHITE, C_TEXT);
        btnRefresh.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        btnRefresh.addActionListener(e -> muatDataUser());

        kanan.add(lblJumlahUser);
        kanan.add(btnRefresh);

        p.add(searchBox, BorderLayout.CENTER);
        p.add(kanan,     BorderLayout.EAST);
        return p;
    }

    /** Tabel user */
    private JScrollPane buildTabel() {
        modelTabel = new DefaultTableModel(KOLOM, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabelUser = new JTable(modelTabel);
        tabelUser.setFont(FONT_TABLE);
        tabelUser.setRowHeight(38);
        tabelUser.setShowVerticalLines(false);
        tabelUser.setShowHorizontalLines(true);
        tabelUser.setGridColor(new Color(229, 231, 235));
        tabelUser.setSelectionBackground(new Color(219, 234, 254));
        tabelUser.setSelectionForeground(C_TEXT);
        tabelUser.setFillsViewportHeight(true);
        tabelUser.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabelUser.getTableHeader().setBackground(C_HEADER_BG);
        tabelUser.getTableHeader().setForeground(C_MUTED);
        tabelUser.getTableHeader().setBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
        tabelUser.getTableHeader().setReorderingAllowed(false);

        // Lebar kolom
        int[] lebar = {40, 200, 180, 130, 100, 130};
        for (int i = 0; i < lebar.length; i++) {
            tabelUser.getColumnModel().getColumn(i).setPreferredWidth(lebar[i]);
        }
        tabelUser.getColumnModel().getColumn(0).setMaxWidth(50);

        // Custom renderer: warna baris blocked + kolom status badge
        tabelUser.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String status = (String) t.getValueAt(row, 4);
                boolean blocked = "nonaktif".equalsIgnoreCase(status);

                if (!sel) {
                    c.setBackground(blocked ? C_BLOCKED : (row % 2 == 0 ? C_WHITE : C_ROW_ALT));
                    c.setForeground(blocked ? C_DANGER : C_TEXT);
                }

                // Kolom UID pakai font mono
                if (col == 3) ((JLabel)c).setFont(FONT_MONO);

                // Kolom status pakai warna khusus
                if (col == 4) {
                    if (!sel) {
                        c.setForeground(blocked ? C_DANGER : C_SUCCESS);
                        ((JLabel)c).setFont(new Font("Segoe UI", Font.BOLD, 11));
                    }
                    ((JLabel)c).setText(blocked ? "🔴  Diblokir" : "🟢  Aktif");
                }

                ((JLabel)c).setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        });

        // Update tombol saat pilih baris
        tabelUser.getSelectionModel().addListSelectionListener(e -> updateTombol());

        JScrollPane scroll = new JScrollPane(tabelUser);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        scroll.getViewport().setBackground(C_WHITE);
        return scroll;
    }

    /** Tombol aksi bawah tabel */
    private JPanel buildActionBar() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        // Kiri: status
        lblStatus = new JLabel(" ");
        lblStatus.setFont(FONT_BODY);

        // Kanan: tombol-tombol
        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tombol.setOpaque(false);

        btnUnblock = createButton("Aktifkan Kartu", C_SUCCESS_L, C_SUCCESS);
        btnUnblock.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_SUCCESS, 1, true),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        btnUnblock.setEnabled(false);
        btnUnblock.addActionListener(e -> unblockUser());

        btnBlock = createButton("Blokir Kartu", C_WARNING_L, C_WARNING);
        btnBlock.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_WARNING, 1, true),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        btnBlock.setEnabled(false);
        btnBlock.addActionListener(e -> blockUser());

        btnHapus = createButton("🗑  Hapus User", C_DANGER_L, C_DANGER);
        btnHapus.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_DANGER, 1, true),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        btnHapus.setEnabled(false);
        btnHapus.addActionListener(e -> hapusUser());

        btnEmergency = createButton("🚨  Emergency Mode", 
            new Color(127, 29, 29), new Color(254, 202, 202));
        btnEmergency.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(220, 38, 38), 1, true),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        btnEmergency.addActionListener(e -> bukaEmergencyMode());

        tombol.add(btnUnblock);
        tombol.add(btnBlock);
        tombol.add(btnHapus);
        tombol.add(btnEmergency);

        p.add(lblStatus, BorderLayout.CENTER);
        p.add(tombol,    BorderLayout.EAST);
        return p;
    }

    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        p.setBackground(new Color(31, 41, 55));

        lblDbStatus = makeStatusDot("DB: menghubungkan...", Color.ORANGE);

        JLabel clock = new JLabel();
        clock.setFont(FONT_SMALL);
        clock.setForeground(new Color(156, 163, 175));
        Timer t = new Timer(1000, e -> clock.setText(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss"))));
        t.start();
        clock.setText(LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss")));

        p.add(lblDbStatus);
        p.add(Box.createHorizontalStrut(20));
        p.add(clock);
        return p;
    }

    // ─────────────────────────────────────────────────────────
    //  Data & Logika
    // ─────────────────────────────────────────────────────────
    @Override
    protected void muatDataUser() {
        modelTabel.setRowCount(0);
        new Thread(() -> {
            String sql = """
                SELECT u.id_user, u.nama, u.prodi,
                       k.uid_kartu, k.status, u.waktuDaftar
                FROM   user u
                JOIN   kartu_rfid k ON u.id_kartu = k.id_kartu
                ORDER  BY u.id_user DESC
                """;
            try (Connection con = DatabaseHelper.getConnection();
                 Statement  st  = con.createStatement();
                 ResultSet  rs  = st.executeQuery(sql)) {

                int jumlah = 0;
                while (rs.next()) {
                    final Object[] baris = {
                        rs.getInt("id_user"),
                        rs.getString("nama"),
                        rs.getString("prodi"),
                        rs.getString("uid_kartu"),
                        rs.getString("status"),
                        rs.getTimestamp("waktuDaftar") != null
                            ? rs.getTimestamp("waktuDaftar")
                                .toLocalDateTime()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            : "-"
                    };
                    SwingUtilities.invokeLater(() -> modelTabel.addRow(baris));
                    jumlah++;
                }
                final int total = jumlah;
                SwingUtilities.invokeLater(() -> {
                    lblJumlahUser.setText(total + " user terdaftar");
                    lblDbStatus.setText("DB: Terhubung");
                    lblDbStatus.setForeground(new Color(134, 239, 172));
                    showStatus("Data berhasil dimuat — " + total + " user.", C_SUCCESS_L, C_SUCCESS);
                });
            } catch (SQLException e) {
                SwingUtilities.invokeLater(() -> {
                    lblDbStatus.setText("DB: Gagal koneksi");
                    lblDbStatus.setForeground(new Color(252, 165, 165));
                    showStatus("Gagal memuat data: " + e.getMessage(), C_DANGER_L, C_DANGER);
                });
            }
        }).start();
    }

    private void filterTabel(String keyword) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelTabel);
        tabelUser.setRowSorter(sorter);
        if (keyword == null || keyword.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + keyword, 1, 2, 3));
        }
    }

    /** Blokir user — set status kartu = nonaktif */
    private void blockUser() {
        int row = tabelUser.getSelectedRow();
        if (row < 0) return;
        int modelRow = tabelUser.convertRowIndexToModel(row);

        String nama   = (String) modelTabel.getValueAt(modelRow, 1);
        String status = (String) modelTabel.getValueAt(modelRow, 4);

        if ("nonaktif".equalsIgnoreCase(status)) {
            showStatus("User " + nama + " sudah diblokir.", C_WARNING_L, C_WARNING);
            return;
        }

        int konfirmasi = JOptionPane.showConfirmDialog(this,
            "<html><b>Blokir kartu milik:</b><br><br>" +
            "Nama : " + nama + "<br>" +
            "Prodi: " + modelTabel.getValueAt(modelRow, 2) + "<br><br>" +
            "<i>User tidak akan bisa mengakses loker sampai kartu diaktifkan kembali.</i></html>",
            "Konfirmasi Blokir", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (konfirmasi != JOptionPane.YES_OPTION) return;

        int idUser = (int) modelTabel.getValueAt(modelRow, 0);
        new Thread(() -> {
            String sql = """
                UPDATE kartu_rfid SET status = 'nonaktif'
                WHERE  id_kartu = (SELECT id_kartu FROM user WHERE id_user = ?)
                """;
            try (Connection con = DatabaseHelper.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idUser);
                ps.executeUpdate();
                SwingUtilities.invokeLater(() -> {
                    muatDataUser();
                    showStatus("✅  Kartu " + nama + " berhasil diblokir.", C_WARNING_L, C_WARNING);
                });
            } catch (SQLException e) {
                SwingUtilities.invokeLater(() ->
                    showStatus("❌  Gagal blokir: " + e.getMessage(), C_DANGER_L, C_DANGER));
            }
        }).start();
    }

    /** Unblock user — set status kartu = aktif */
    private void unblockUser() {
        int row = tabelUser.getSelectedRow();
        if (row < 0) return;
        int modelRow = tabelUser.convertRowIndexToModel(row);

        String nama   = (String) modelTabel.getValueAt(modelRow, 1);
        String status = (String) modelTabel.getValueAt(modelRow, 4);

        if ("aktif".equalsIgnoreCase(status)) {
            showStatus("Kartu " + nama + " sudah aktif.", C_SUCCESS_L, C_SUCCESS);
            return;
        }

        int konfirmasi = JOptionPane.showConfirmDialog(this,
            "<html><b>Aktifkan kembali kartu milik:</b><br><br>" +
            "Nama : " + nama + "<br>" +
            "Prodi: " + modelTabel.getValueAt(modelRow, 2) + "<br><br>" +
            "<i>User akan bisa mengakses loker kembali.</i></html>",
            "Konfirmasi Aktifkan", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

        if (konfirmasi != JOptionPane.YES_OPTION) return;

        int idUser = (int) modelTabel.getValueAt(modelRow, 0);
        new Thread(() -> {
            String sql = """
                UPDATE kartu_rfid SET status = 'aktif'
                WHERE  id_kartu = (SELECT id_kartu FROM user WHERE id_user = ?)
                """;
            try (Connection con = DatabaseHelper.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idUser);
                ps.executeUpdate();
                SwingUtilities.invokeLater(() -> {
                    muatDataUser();
                    showStatus("✅  Kartu " + nama + " berhasil diaktifkan.", C_SUCCESS_L, C_SUCCESS);
                });
            } catch (SQLException e) {
                SwingUtilities.invokeLater(() ->
                    showStatus("❌  Gagal aktifkan: " + e.getMessage(), C_DANGER_L, C_DANGER));
            }
        }).start();
    }

    /** Hapus user dari database (cascade hapus kartu juga) */
    private void hapusUser() {
        int row = tabelUser.getSelectedRow();
        if (row < 0) return;
        int modelRow = tabelUser.convertRowIndexToModel(row);

        String nama  = (String) modelTabel.getValueAt(modelRow, 1);
        String prodi = (String) modelTabel.getValueAt(modelRow, 2);
        String uid   = (String) modelTabel.getValueAt(modelRow, 3);

        // Konfirmasi 2 kali untuk menghapus (tindakan tidak bisa diundo)
        int konfirmasi1 = JOptionPane.showConfirmDialog(this,
            "<html><b>⚠ Hapus user berikut dari database?</b><br><br>" +
            "Nama &nbsp;: " + nama  + "<br>" +
            "Prodi &nbsp;: " + prodi + "<br>" +
            "UID &nbsp;&nbsp;: " + uid   + "<br><br>" +
            "<span color='#DC2626'><b>Tindakan ini tidak bisa dibatalkan!</b><br>" +
            "Data user dan riwayat akses loker akan terhapus permanen.</span></html>",
            "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (konfirmasi1 != JOptionPane.YES_OPTION) return;

        int konfirmasi2 = JOptionPane.showConfirmDialog(this,
            "<html>Apakah kamu yakin ingin menghapus <b>" + nama + "</b>?<br>" +
            "Ini adalah konfirmasi terakhir.</html>",
            "Konfirmasi Akhir", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);

        if (konfirmasi2 != JOptionPane.YES_OPTION) return;

        int idUser = (int) modelTabel.getValueAt(modelRow, 0);
        new Thread(() -> {
            String sqlGetKartu    = "SELECT id_kartu FROM user WHERE id_user = ?";
            String sqlHapusAkses  = "DELETE FROM akses_loker WHERE id_user = ?";
            String sqlHapusUser   = "DELETE FROM user WHERE id_user = ?";
            String sqlHapusKartu  = "DELETE FROM kartu_rfid WHERE id_kartu = ?";

            try (Connection con = DatabaseHelper.getConnection()) {
                con.setAutoCommit(false);
                try {
                    // 1. Ambil id_kartu milik user ini
                    int idKartu = -1;
                    try (PreparedStatement ps = con.prepareStatement(sqlGetKartu)) {
                        ps.setInt(1, idUser);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) idKartu = rs.getInt("id_kartu");
                    }

                    // 2. Hapus riwayat akses_loker dulu
                    try (PreparedStatement ps = con.prepareStatement(sqlHapusAkses)) {
                        ps.setInt(1, idUser);
                        ps.executeUpdate();
                    }

                    // 3. Hapus user
                    try (PreparedStatement ps = con.prepareStatement(sqlHapusUser)) {
                        ps.setInt(1, idUser);
                        ps.executeUpdate();
                    }

                    // 4. Hapus kartu_rfid
                    if (idKartu != -1) {
                        try (PreparedStatement ps = con.prepareStatement(sqlHapusKartu)) {
                            ps.setInt(1, idKartu);
                            ps.executeUpdate();
                        }
                    }

                    con.commit();
                    SwingUtilities.invokeLater(() -> {
                        muatDataUser();
                        showStatus("User " + nama + " berhasil dihapus.",
                            C_DANGER_L, C_DANGER);
                    });

                } catch (SQLException ex) {
                    con.rollback();
                    SwingUtilities.invokeLater(() ->
                        showStatus("Gagal hapus: " + ex.getMessage(),
                            C_DANGER_L, C_DANGER));
                }

            } catch (SQLException e) {
                SwingUtilities.invokeLater(() ->
                    showStatus("Gagal koneksi: " + e.getMessage(),
                            C_DANGER_L, C_DANGER));
            }
        }).start();
    }

    /** Update status tombol berdasarkan baris yang dipilih */
    private void updateTombol() {
        int row = tabelUser.getSelectedRow();
        if (row < 0) {
            btnHapus.setEnabled(false);
            btnBlock.setEnabled(false);
            btnUnblock.setEnabled(false);
            return;
        }
        int modelRow = tabelUser.convertRowIndexToModel(row);
        String status = (String) modelTabel.getValueAt(modelRow, 4);
        boolean blocked = "nonaktif".equalsIgnoreCase(status);

        btnHapus.setEnabled(true);
        btnBlock.setEnabled(!blocked);
        btnUnblock.setEnabled(blocked);

        String nama = (String) modelTabel.getValueAt(modelRow, 1);
        lblStatus.setText("Dipilih: " + nama + "  |  Status kartu: " +
            (blocked ? "Diblokir" : "Aktif"));
        lblStatus.setForeground(blocked ? C_DANGER : C_SUCCESS);
    }

    private void showStatus(String msg, Color bg, Color fg) {
        lblStatus.setText(msg);
        lblStatus.setForeground(fg);
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
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.darker()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    private JLabel makeStatusDot(String text, Color color) {
        JLabel l = new JLabel("● " + text);
        l.setFont(FONT_SMALL);
        l.setForeground(color);
        return l;
    }
    
    private void bukaEmergencyMode() {
        // Buat dialog Emergency
        JDialog dialog = new JDialog(this, "Emergency Mode", true);
        dialog.setSize(620, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(15, 23, 42));

        // ── Header merah ──
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setBackground(new Color(127, 29, 29));
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel lJudul = new JLabel("🚨Emergency Mode — Manajemen Loker Darurat");
        lJudul.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lJudul.setForeground(new Color(254, 202, 202));

        JLabel lSub = new JLabel(
            "Buka paksa loker yang tersangkut atau kosongkan semua loker sekaligus");
        lSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lSub.setForeground(new Color(252, 165, 165));

        header.add(lJudul);
        header.add(lSub);

        // ── Tabel loker terisi ──
        String[] kolom = {"No. Loker", "Nama User", "Prodi", "Waktu Buka", "Aksi"};
        String[][] dataLoker = DatabaseHelper.getLokerTerisi();

        DefaultTableModel modelEmergency = new DefaultTableModel(kolom, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (String[] row : dataLoker) {
            modelEmergency.addRow(new Object[]{
                "Loker " + row[1], row[2], row[3], row[4], row[0] + "|" + row[5]
            });
        }

        JTable tabel = new JTable(modelEmergency);
        tabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabel.setRowHeight(36);
        tabel.setBackground(new Color(30, 41, 59));
        tabel.setForeground(new Color(226, 232, 240));
        tabel.setGridColor(new Color(71, 85, 105));
        tabel.setSelectionBackground(new Color(127, 29, 29));
        tabel.setSelectionForeground(Color.WHITE);
        tabel.getTableHeader().setBackground(new Color(20, 30, 48));
        tabel.getTableHeader().setForeground(new Color(148, 163, 184));
        tabel.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        tabel.getColumnModel().getColumn(4).setMaxWidth(0);  // sembunyikan kolom id
        tabel.getColumnModel().getColumn(4).setMinWidth(0);
        tabel.getColumnModel().getColumn(4).setPreferredWidth(0);

        JScrollPane scroll = new JScrollPane(tabel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(71, 85, 105)));
        scroll.getViewport().setBackground(new Color(30, 41, 59));

        // Label info jumlah
        JLabel lblInfo = new JLabel(dataLoker.length == 0
            ? "Semua loker dalam kondisi kosong"
            : dataLoker.length + " loker sedang terisi");
        lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblInfo.setForeground(dataLoker.length == 0
            ? new Color(34, 197, 94) : new Color(251, 191, 36));
        lblInfo.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 0));

        JPanel tengah = new JPanel(new BorderLayout(0, 6));
        tengah.setBackground(new Color(15, 23, 42));
        tengah.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        tengah.add(lblInfo,  BorderLayout.NORTH);
        tengah.add(scroll,   BorderLayout.CENTER);

        // ── Tombol bawah ──
        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setBackground(new Color(20, 30, 48));
        bottom.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(71, 85, 105)),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        // Tombol buka loker terpilih
        JButton btnBukaSatu = new JButton("Buka Loker Terpilih");
        btnBukaSatu.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBukaSatu.setBackground(new Color(217, 119, 6));
        btnBukaSatu.setForeground(Color.WHITE);
        btnBukaSatu.setBorderPainted(false);
        btnBukaSatu.setFocusPainted(false);
        btnBukaSatu.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBukaSatu.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btnBukaSatu.addActionListener(e -> {
            int baris = tabel.getSelectedRow();
            if (baris < 0) {
                JOptionPane.showMessageDialog(dialog,
                    "Pilih loker dulu dari tabel!", "Peringatan",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            String idData    = (String) modelEmergency.getValueAt(baris, 4);
            String nomorLoker = ((String) modelEmergency.getValueAt(baris, 0))
                .replace("Loker ", "");
            int idLoker  = Integer.parseInt(idData.split("\\|")[0]);
            int idAkses  = Integer.parseInt(idData.split("\\|")[1]);

            int konfirm = JOptionPane.showConfirmDialog(dialog,
                "<html>Buka paksa dan kosongkan <b>Loker " + nomorLoker + "</b>?<br><br>" +
                "User: " + modelEmergency.getValueAt(baris, 1) + "</html>",
                "Konfirmasi Emergency", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (konfirm != JOptionPane.YES_OPTION) return;

            new Thread(() -> {
                boolean ok = DatabaseHelper.emergencyKosongkanLoker(idLoker, idAkses);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        if (serialHelper != null) serialHelper.kirimPerintah("op");
                        modelEmergency.removeRow(baris);
                        lblInfo.setText("Loker " + nomorLoker + " berhasil dikosongkan");
                        lblInfo.setForeground(new Color(34, 197, 94));                 
                        muatDataUser(); // refresh tabel utama AdminPanel
                    } else {
                        lblInfo.setText("Gagal mengosongkan loker");
                        lblInfo.setForeground(new Color(239, 68, 68));
                    }
                });
            }).start();
        });

        // Tombol kosongkan semua
        JButton btnKosongkanSemua = new JButton("⚡  Kosongkan Semua Loker");
        btnKosongkanSemua.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnKosongkanSemua.setBackground(new Color(127, 29, 29));
        btnKosongkanSemua.setForeground(new Color(254, 202, 202));
        btnKosongkanSemua.setBorderPainted(false);
        btnKosongkanSemua.setFocusPainted(false);
        btnKosongkanSemua.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnKosongkanSemua.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btnKosongkanSemua.addActionListener(e -> {
            if (dataLoker.length == 0) {
                JOptionPane.showMessageDialog(dialog,
                    "Semua loker sudah kosong!", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int konfirm = JOptionPane.showConfirmDialog(dialog,
                "<html><b>⚠ Kosongkan SEMUA loker yang terisi?</b><br><br>" +
                "Tindakan ini akan:<br>" +
                "- Mengosongkan " + dataLoker.length + " loker sekaligus<br>" +
                "- Menutup semua sesi akses yang aktif<br><br>" +
                "<span color='#DC2626'>Gunakan hanya dalam kondisi darurat!</span></html>",
                "Konfirmasi Emergency", JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE);

            if (konfirm != JOptionPane.YES_OPTION) return;

            new Thread(() -> {
                boolean ok = DatabaseHelper.emergencyKosongkanSemua();
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        if (serialHelper != null) serialHelper.kirimPerintah("op");
                        modelEmergency.setRowCount(0);
                        lblInfo.setText("Semua loker berhasil dikosongkan");
                        lblInfo.setForeground(new Color(34, 197, 94));
                        muatDataUser();
                    } else {
                        lblInfo.setText("Gagal mengosongkan semua loker");
                        lblInfo.setForeground(new Color(239, 68, 68));
                    }
                });
            }).start();
        });

        // Tombol tutup
        JButton btnTutup = new JButton("Tutup");
        btnTutup.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnTutup.setBackground(new Color(51, 65, 85));
        btnTutup.setForeground(Color.WHITE);
        btnTutup.setBorderPainted(false);
        btnTutup.setFocusPainted(false);
        btnTutup.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTutup.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btnTutup.addActionListener(e -> dialog.dispose());

        JPanel kanan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        kanan.setOpaque(false);
        kanan.add(btnTutup);
        kanan.add(btnBukaSatu);
        kanan.add(btnKosongkanSemua);

        bottom.add(kanan, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(tengah, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }
    // ─────────────────────────────────────────────────────────
    //  Main — bisa dijalankan langsung atau dari PendaftaranLoker
    // ─────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new AdminPanel(null).setVisible(true));
    }
}
