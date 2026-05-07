SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

CREATE TABLE `akses_loker` (
  `id_akses` int NOT NULL,
  `id_user` int DEFAULT NULL,
  `id_loker` int DEFAULT NULL,
  `waktu_buka` datetime DEFAULT NULL,
  `waktu_tutup` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `akses_loker` (`id_akses`, `id_user`, `id_loker`, `waktu_buka`, `waktu_tutup`) VALUES
(1, 1, 7, '2026-05-01 14:55:55', '2026-05-01 14:56:05'),
(2, 1, 2, '2026-05-01 15:14:38', '2026-05-01 15:15:03'),
(3, 1, 3, '2026-05-01 15:45:19', '2026-05-01 15:46:14'),
(4, 2, 8, '2026-05-01 15:47:10', '2026-05-01 15:47:24'),
(5, 3, 9, '2026-05-01 15:52:41', '2026-05-01 15:53:12'),
(6, 1, 17, '2026-05-01 15:57:19', '2026-05-01 16:51:19'),
(7, 1, 2, '2026-05-01 17:32:20', '2026-05-01 17:33:26'),
(8, 1, 2, '2026-05-01 17:36:50', '2026-05-01 17:43:44'),
(9, 1, 2, '2026-05-01 17:44:38', '2026-05-01 17:45:38'),
(10, 1, 9, '2026-05-01 18:55:11', '2026-05-01 18:55:21'),
(11, 1, 4, '2026-05-01 19:00:35', '2026-05-04 13:14:27'),
(12, 4, 4, '2026-05-04 13:17:10', '2026-05-04 13:17:37'),
(13, 1, 9, '2026-05-04 13:38:14', '2026-05-04 13:46:09'),
(14, 1, 9, '2026-05-04 13:46:40', '2026-05-04 13:46:51'),
(15, 1, 8, '2026-05-04 14:29:03', '2026-05-04 14:29:19'),
(16, 1, 20, '2026-05-04 14:31:21', '2026-05-04 14:31:41'),
(17, 1, 1, '2026-05-04 14:33:30', '2026-05-04 14:33:38'),
(18, 1, 2, '2026-05-04 14:43:02', '2026-05-04 14:43:11'),
(19, 1, 7, '2026-05-08 00:56:25', '2026-05-08 00:57:23'),
(20, 1, 7, '2026-05-08 00:57:36', '2026-05-08 00:57:45');

CREATE TABLE `kartu_rfid` (
  `id_kartu` int NOT NULL,
  `uid_kartu` varchar(50) DEFAULT NULL,
  `status` enum('aktif','nonaktif') DEFAULT 'aktif'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `kartu_rfid` (`id_kartu`, `uid_kartu`, `status`) VALUES
(1, '1411D530', 'aktif'),
(2, '167C561A', 'aktif'),
(3, '46F5331A', 'aktif'),
(4, '44EADB30', 'aktif');

CREATE TABLE `loker` (
  `id_loker` int NOT NULL,
  `nomor_loker` int DEFAULT NULL,
  `status` enum('kosong','terisi') DEFAULT 'kosong'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `loker` (`id_loker`, `nomor_loker`, `status`) VALUES
(1, 1, 'kosong'),
(2, 2, 'kosong'),
(3, 3, 'kosong'),
(4, 4, 'kosong'),
(5, 5, 'kosong'),
(6, 6, 'kosong'),
(7, 7, 'kosong'),
(8, 8, 'kosong'),
(9, 9, 'kosong'),
(10, 10, 'kosong'),
(11, 11, 'kosong'),
(12, 12, 'kosong'),
(13, 13, 'kosong'),
(14, 14, 'kosong'),
(15, 15, 'kosong'),
(16, 16, 'kosong'),
(17, 17, 'kosong'),
(18, 18, 'kosong'),
(19, 19, 'kosong'),
(20, 20, 'kosong');

CREATE TABLE `tabel_universal` (
`id_user` int
,`nama` varchar(100)
,`nomor_loker` int
,`prodi` varchar(50)
,`status_loker` enum('kosong','terisi')
,`uid_kartu` varchar(50)
,`waktu_buka` datetime
,`waktu_tutup` datetime
);

CREATE TABLE `user` (
  `id_user` int NOT NULL,
  `nama` varchar(100) DEFAULT NULL,
  `prodi` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `id_kartu` int DEFAULT NULL,
  `waktuDaftar` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `user` (`id_user`, `nama`, `prodi`, `id_kartu`, `waktuDaftar`) VALUES
(1, 'Nata Bagus', 'S1 Pendidikan Teknik Informatika', 1, '2026-05-01 07:54:13'),
(2, 'Nanda Muhammad', 'S1 Pendidikan Teknik Informatika', 2, '2026-05-01 07:54:33'),
(3, 'Rinaldi Putra', 'S1 Pendidikan Teknik Informatika', 3, '2026-05-01 08:52:21'),
(4, 'Reno', 'S1 Pendidikan Teknik Informatika', 4, '2026-05-04 05:13:40');

ALTER TABLE `akses_loker`
  ADD PRIMARY KEY (`id_akses`),
  ADD KEY `id_user` (`id_user`),
  ADD KEY `id_loker` (`id_loker`);

ALTER TABLE `kartu_rfid`
  ADD PRIMARY KEY (`id_kartu`),
  ADD UNIQUE KEY `uid_kartu` (`uid_kartu`);

ALTER TABLE `loker`
  ADD PRIMARY KEY (`id_loker`);

ALTER TABLE `user`
  ADD PRIMARY KEY (`id_user`),
  ADD KEY `id_kartu` (`id_kartu`);

ALTER TABLE `akses_loker`
  MODIFY `id_akses` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

ALTER TABLE `kartu_rfid`
  MODIFY `id_kartu` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

ALTER TABLE `loker`
  MODIFY `id_loker` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

ALTER TABLE `user`
  MODIFY `id_user` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

DROP TABLE IF EXISTS `tabel_universal`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `tabel_universal`  AS SELECT `u`.`id_user` AS `id_user`, `u`.`nama` AS `nama`, `u`.`prodi` AS `prodi`, `k`.`uid_kartu` AS `uid_kartu`, `l`.`nomor_loker` AS `nomor_loker`, `l`.`status` AS `status_loker`, `a`.`waktu_buka` AS `waktu_buka`, `a`.`waktu_tutup` AS `waktu_tutup` FROM (((`user` `u` join `kartu_rfid` `k` on((`u`.`id_kartu` = `k`.`id_kartu`))) left join `akses_loker` `a` on((`u`.`id_user` = `a`.`id_user`))) left join `loker` `l` on((`a`.`id_loker` = `l`.`id_loker`))) ;

ALTER TABLE `akses_loker`
  ADD CONSTRAINT `akses_loker_ibfk_1` FOREIGN KEY (`id_user`) REFERENCES `user` (`id_user`),
  ADD CONSTRAINT `akses_loker_ibfk_2` FOREIGN KEY (`id_loker`) REFERENCES `loker` (`id_loker`);

ALTER TABLE `user`
  ADD CONSTRAINT `user_ibfk_1` FOREIGN KEY (`id_kartu`) REFERENCES `kartu_rfid` (`id_kartu`);
COMMIT;
