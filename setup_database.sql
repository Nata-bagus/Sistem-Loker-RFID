CREATE DATABASE IF NOT EXISTS tugasakhirbasva
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE tugasakhirbasva;

CREATE TABLE IF NOT EXISTS kartu_rfid (
    id_kartu   INT          NOT NULL AUTO_INCREMENT,
    uid_kartu  VARCHAR(50)  NOT NULL UNIQUE,
    status     ENUM('aktif','nonaktif') NOT NULL DEFAULT 'aktif',
    waktu_buat TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_kartu)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user (
    id_user    INT          NOT NULL AUTO_INCREMENT,
    nama       VARCHAR(100) NOT NULL,
    prodi      VARCHAR(50)  NOT NULL,
    id_kartu   INT          NOT NULL,
    waktu_buat TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_user),
    FOREIGN KEY (id_kartu) REFERENCES kartu_rfid(id_kartu)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS loker (
    id_loker     INT  NOT NULL AUTO_INCREMENT,
    nomor_loker  INT  NOT NULL UNIQUE,
    status       ENUM('kosong','terisi') NOT NULL DEFAULT 'kosong',
    PRIMARY KEY (id_loker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO loker (nomor_loker, status) VALUES
(1,'kosong'),(2,'kosong'),(3,'kosong'),(4,'kosong'),(5,'kosong'),
(6,'kosong'),(7,'kosong'),(8,'kosong'),(9,'kosong'),(10,'kosong'),
(11,'kosong'),(12,'kosong'),(13,'kosong'),(14,'kosong'),(15,'kosong'),
(16,'kosong'),(17,'kosong'),(18,'kosong'),(19,'kosong'),(20,'kosong');

CREATE TABLE IF NOT EXISTS akses_loker (
    id_akses    INT       NOT NULL AUTO_INCREMENT,
    id_user     INT       NOT NULL,
    id_loker    INT       NOT NULL,
    waktu_buka  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    waktu_tutup DATETIME  NULL,
    PRIMARY KEY (id_akses),
    FOREIGN KEY (id_user)  REFERENCES user(id_user)   ON DELETE CASCADE,
    FOREIGN KEY (id_loker) REFERENCES loker(id_loker) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SHOW TABLES;
SELECT 'Setup database berhasil!' AS pesan;
