import java.sql.*;

public class DatabaseHelper {

    // ── ⚙ Konfigurasi Koneksi ───────────────────────────────────
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/tugasakhirbasva"
                                        + "?useSSL=false&allowPublicKeyRetrieval=true"
                                        + "&serverTimezone=Asia/Jakarta";
    private static final String DB_USER = "root";       // ← ganti sesuai user MySQL-mu
    private static final String DB_PASS = "";           // ← ganti sesuai password MySQL-mu
    // ────────────────────────────────────────────────────────────

    /** Buka & kembalikan koneksi baru setiap kali dipanggil */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    /**
     * Test apakah koneksi ke database berhasil.
     * Dipanggil saat aplikasi pertama kali dibuka.
     */
    public static boolean testKoneksi() {
        try (Connection con = getConnection()) {
            return con != null && !con.isClosed();
        } catch (SQLException e) {
            System.err.println("[DB] Koneksi gagal: " + e.getMessage());
            return false;
        }
    }

    /**
     * Simpan pendaftaran mahasiswa ke database.
     *
     * Alur:
     *  1. Cek apakah UID kartu sudah terdaftar → tolak duplikat
     *  2. INSERT ke tabel kartu_rfid (uid_kartu, status='aktif')
     *  3. INSERT ke tabel user (nama, prodi, id_kartu → FK ke kartu_rfid)
     *
     * @param nama      Nama lengkap mahasiswa
     * @param prodi     Program studi
     * @param uidKartu  UID chip dari RC522
     * @return          id_kartu yang baru dibuat di tabel kartu_rfid
     * @throws Exception jika UID duplikat atau error SQL
     */
    public static int simpanPendaftaran(String nama, String prodi, String uidKartu)
            throws Exception {

        try (Connection con = getConnection()) {
            con.setAutoCommit(false); // Gunakan transaksi agar atomik

            try {
                // 1. Cek duplikat UID
                if (isUIDSudahTerdaftar(con, uidKartu)) {
                    throw new Exception("UID kartu '" + uidKartu + "' sudah terdaftar!");
                }

                // 2. INSERT ke kartu_rfid
                int kartuId = insertKartuRfid(con, uidKartu);

                // 3. INSERT ke user
                insertUser(con, nama, prodi, kartuId);

                con.commit();
                System.out.println("[DB] Pendaftaran berhasil — " + nama
                    + " | Prodi: " + prodi + " | UID: " + uidKartu
                    + " | id_kartu: " + kartuId);
                return kartuId;

            } catch (Exception ex) {
                con.rollback();
                throw ex; // lempar kembali ke caller
            }
        }
    }

    /** Cek apakah uid_kartu sudah ada di tabel kartu_rfid */
    private static boolean isUIDSudahTerdaftar(Connection con, String uid)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM kartu_rfid WHERE uid_kartu = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uid);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /**
     * INSERT satu baris ke kartu_rfid.
     * @return id_kartu yang di-generate AUTO_INCREMENT
     */
    private static int insertKartuRfid(Connection con, String uid)
            throws SQLException {
        String sql = "INSERT INTO kartu_rfid (uid_kartu, status) VALUES (?, 'aktif')";
        try (PreparedStatement ps = con.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, uid);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
            throw new SQLException("Gagal mendapatkan id_kartu yang baru.");
        }
    }

    /**
     * INSERT satu baris ke tabel user.
     */
    private static void insertUser(Connection con, String nama, String prodi, int kartuId)
            throws SQLException {
        String sql = "INSERT INTO user (nama, prodi, id_kartu) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nama);
            ps.setString(2, prodi);
            ps.setInt(3, kartuId);
            ps.executeUpdate();
        }
    }

    /**
     * Ambil semua user yang sudah terdaftar (untuk tab riwayat, opsional).
     * Kembalikan sebagai array String[][] agar mudah dimasukkan ke JTable.
     */
    public static String[][] getDaftarUser() {
        String sql = """
            SELECT u.id_user, u.nama, u.prodi,
                   k.uid_kartu, k.status
            FROM   user u
            JOIN   kartu_rfid k ON u.id_kartu = k.id_kartu
            ORDER  BY u.id_user DESC
            LIMIT  100
            """;
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        try (Connection con = getConnection();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new String[]{
                    String.valueOf(rs.getInt("id_user")),
                    rs.getString("nama"),
                    rs.getString("prodi"),
                    rs.getString("uid_kartu"),
                    rs.getString("status")
                });
            }
        } catch (SQLException e) {
            System.err.println("[DB] getDaftarUser gagal: " + e.getMessage());
        }
        return rows.toArray(new String[0][]);
    }

    /** Ambil id_user berdasarkan UID kartu */
    public static int getIdUserByUID(String uid) {
        String sql = """
            SELECT u.id_user FROM user u
            JOIN   kartu_rfid k ON u.id_kartu = k.id_kartu
            WHERE  k.uid_kartu = ? AND k.status = 'aktif'
            """;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id_user");
        } catch (SQLException e) {
            System.err.println("[DB] getIdUserByUID gagal: " + e.getMessage());
        }
        return -1;
    }
    
        /** Cek apakah user sedang memakai loker (waktu_tutup masih NULL) */
    public static int getLokerAktifUser(int idUser) {
        String sql = """
            SELECT id_loker FROM akses_loker
            WHERE  id_user = ? AND waktu_tutup IS NULL
            ORDER  BY waktu_buka DESC
            LIMIT  1
            """;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id_loker");
        } catch (SQLException e) {
            System.err.println("[DB] getLokerAktifUser gagal: " + e.getMessage());
        }
        return -1; // tidak sedang pakai loker
    }

    /** Tutup loker — update waktu_tutup + kosongkan status loker */
    public static boolean tutupLoker(int idUser, int idLoker) {
        String sqlAkses = """
            UPDATE akses_loker SET waktu_tutup = NOW()
            WHERE  id_user = ? AND id_loker = ? AND waktu_tutup IS NULL
            """;
        String sqlLoker = "UPDATE loker SET status = 'kosong' WHERE id_loker = ?";
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sqlAkses)) {
                    ps.setInt(1, idUser);
                    ps.setInt(2, idLoker);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(sqlLoker)) {
                    ps.setInt(1, idLoker);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException ex) {
                con.rollback();
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[DB] tutupLoker gagal: " + e.getMessage());
            return false;
        }
    }

    /** Ambil nomor loker dari id_loker */
    public static int getNomorLoker(int idLoker) {
        String sql = "SELECT nomor_loker FROM loker WHERE id_loker = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idLoker);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("nomor_loker");
        } catch (SQLException e) {
            System.err.println("[DB] getNomorLoker gagal: " + e.getMessage());
        }
        return -1;
    }
    /**
     * Validasi kartu saat mengakses loker.
     * Kembalikan nama mahasiswa jika kartu valid, null jika tidak ditemukan.
     */
    public static String validasiKartu(String uidKartu) {
        String sql = """
            SELECT u.nama FROM user u
            JOIN   kartu_rfid k ON u.id_kartu = k.id_kartu
            WHERE  k.uid_kartu = ? AND k.status = 'aktif'
            """;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uidKartu);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("nama");
        } catch (SQLException e) {
            System.err.println("[DB] validasiKartu gagal: " + e.getMessage());
        }
        return null; // tidak ditemukan / tidak aktif
    }
}
