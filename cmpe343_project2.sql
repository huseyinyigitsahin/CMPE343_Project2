
DROP DATABASE IF EXISTS `cmpe343_project2`;
CREATE DATABASE `cmpe343_project2` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `cmpe343_project2`;

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `name` varchar(100) NOT NULL,
  `surname` varchar(100) NOT NULL,
  `role` varchar(30) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `users` (`user_id`, `username`, `password_hash`, `name`, `surname`, `role`) VALUES
(1, 'tt', '0e07cf830957701d43c183f1515f63e6b68027e528f43ef52b1527a520ddec82', 'Huseyin Yigit', 'Sahin', 'Tester'),
(2, 'jd', 'ad3e69e9aa860657cc6476770fe253d08198746b9fcf9dc3186b47eb85c30335', 'Mert Fahri', 'Cakar', 'Junior Developer'),
(3, 'sd', '03042cf8100db386818cee4ff0f2972431a62ed78edbd09ac08accfabbefd818', 'Burak', 'Arslan', 'Senior Developer'),
(4, 'man', '48b676e2b107da679512b793d5fd4cc4329f0c7c17a97cf6e0e3d1005b600b03', 'Nermin Zehra', 'Sipahioglu', 'Manager');

CREATE TABLE `contacts` (
  `contact_id` int(11) NOT NULL AUTO_INCREMENT,
  `first_name` varchar(50) NOT NULL,
  `middle_name` varchar(50) DEFAULT NULL,
  `last_name` varchar(50) NOT NULL,
  `nickname` varchar(50) DEFAULT NULL,
  `phone_primary` varchar(20) NOT NULL,
  `phone_secondary` varchar(20) DEFAULT NULL,
  `email` varchar(100) NOT NULL,
  `linkedin_url` varchar(255) DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`contact_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `contacts` (`first_name`, `middle_name`, `last_name`, `nickname`, `phone_primary`, `phone_secondary`, `email`, `linkedin_url`, `birth_date`) VALUES
('Ahmet', NULL, 'Yilmaz', 'Hizli', '5551112233', NULL, 'ahmet.yilmaz@mail.com', 'linkedin.com/in/ahmet', '1990-05-15'),
('Ayse', 'Nur', 'Demir', NULL, '5423334455', '5301234567', 'ayse.demir@mail.com', NULL, '1995-11-20'),
('Mehmet', NULL, 'Kaya', 'Memo', '5324445566', NULL, 'mehmet.kaya@mail.com', 'linkedin.com/in/mehmet', '1988-02-10'),
('Fatma', NULL, 'Celik', NULL, '5055556677', NULL, 'fatma.celik@mail.com', NULL, '1992-08-30'),
('Mustafa', 'Ali', 'Sahin', NULL, '5446667788', NULL, 'mustafa.sahin@mail.com', 'linkedin.com/in/mustafa', '1985-04-25'),
('Zeynep', NULL, 'Yildiz', 'Zey', '5337778899', '5059876543', 'zeynep.yildiz@mail.com', NULL, '1998-12-05'),
('Emre', NULL, 'Ozdemir', NULL, '5558889900', NULL, 'emre.ozdemir@mail.com', 'linkedin.com/in/emre', '1993-07-14'),
('Elif', NULL, 'Arslan', NULL, '5429990011', NULL, 'elif.arslan@mail.com', NULL, '2000-01-01'),
('Burak', 'Can', 'Dogan', 'Borek', '5320001122', NULL, 'burak.dogan@mail.com', 'linkedin.com/in/burak', '1991-09-19'),
('Ceren', NULL, 'Kilic', NULL, '5051112233', NULL, 'ceren.kilic@mail.com', NULL, '1996-03-22'),
('Ali', NULL, 'Aslan', NULL, '5442223344', NULL, 'ali.aslan@mail.com', NULL, '1989-06-11'),
('Selin', 'Su', 'Koc', NULL, '5333334455', NULL, 'selin.koc@mail.com', 'linkedin.com/in/selin', '1994-10-08'),
('Oguz', NULL, 'Kurt', 'Wolf', '5554445566', NULL, 'oguz.kurt@mail.com', NULL, '1987-12-12'),
('Gamze', NULL, 'Ozturk', NULL, '5425556677', NULL, 'gamze.ozturk@mail.com', NULL, '1999-05-05'),
('Kemal', NULL, 'Aydin', NULL, '5326667788', NULL, 'kemal.aydin@mail.com', 'linkedin.com/in/kemal', '1986-02-28'),
('Esra', NULL, 'Yavuz', NULL, '5057778899', NULL, 'esra.yavuz@mail.com', NULL, '1997-08-15'),
('Mert', NULL, 'Polat', NULL, '5448889900', NULL, 'mert.polat@mail.com', NULL, '1990-11-11'),
('Derya', NULL, 'Cetin', 'Deniz', '5339990011', NULL, 'derya.cetin@mail.com', 'linkedin.com/in/derya', '1993-04-04'),
('Hakan', NULL, 'Erdogan', NULL, '5550001122', NULL, 'hakan.erdogan@mail.com', NULL, '1984-07-20'),
('Pinar', NULL, 'Guler', NULL, '5421112233', NULL, 'pinar.guler@mail.com', NULL, '1995-09-09'),
('Volkan', NULL, 'Sari', NULL, '5322223344', NULL, 'volkan.sari@mail.com', 'linkedin.com/in/volkan', '1991-03-14'),
('Hande', NULL, 'Bulut', NULL, '5053334455', NULL, 'hande.bulut@mail.com', NULL, '1998-06-25'),
('Onur', NULL, 'Aksoy', NULL, '5444445566', NULL, 'onur.aksoy@mail.com', NULL, '1988-12-30'),
('Gozde', NULL, 'Tekin', NULL, '5335556677', NULL, 'gozde.tekin@mail.com', 'linkedin.com/in/gozde', '1996-01-18'),
('Baris', NULL, 'Unal', NULL, '5556667788', NULL, 'baris.unal@mail.com', NULL, '1992-10-10'),
('Tugce', NULL, 'Coskun', NULL, '5427778899', NULL, 'tugce.coskun@mail.com', NULL, '1999-07-07'),
('Serkan', NULL, 'Sen', NULL, '5328889900', NULL, 'serkan.sen@mail.com', 'linkedin.com/in/serkan', '1985-05-22'),
('Busra', NULL, 'Yuksel', NULL, '5059990011', NULL, 'busra.yuksel@mail.com', NULL, '1994-02-14'),
('Ugur', NULL, 'Korkmaz', NULL, '5440001122', NULL, 'ugur.korkmaz@mail.com', NULL, '1990-08-08'),
('Seda', NULL, 'Keskin', NULL, '5331112233', NULL, 'seda.keskin@mail.com', 'linkedin.com/in/seda', '1997-11-29'),
('Fatih', NULL, 'Avci', NULL, '5552223344', NULL, 'fatih.avci@mail.com', NULL, '1986-06-06'),
('Ebru', NULL, 'Tas', NULL, '5423334455', NULL, 'ebru.tas@mail.com', NULL, '1993-03-03'),
('Cem', NULL, 'Acar', NULL, '5324445566', NULL, 'cem.acar@mail.com', 'linkedin.com/in/cem', '1989-09-21'),
('Melis', NULL, 'Gunes', NULL, '5055556677', NULL, 'melis.gunes@mail.com', NULL, '2000-05-15'),
('Kaan', NULL, 'Bozkurt', NULL, '5446667788', NULL, 'kaan.bozkurt@mail.com', NULL, '1991-12-01'),
('Sinem', NULL, 'Ucar', NULL, '5337778899', NULL, 'sinem.ucar@mail.com', 'linkedin.com/in/sinem', '1995-04-12'),
('Tolga', NULL, 'Turan', NULL, '5558889900', NULL, 'tolga.turan@mail.com', NULL, '1987-10-25'),
('Yasemin', NULL, 'Cakir', NULL, '5429990011', NULL, 'yasemin.cakir@mail.com', NULL, '1998-02-02'),
('Eren', NULL, 'Sonmez', NULL, '5320001122', NULL, 'eren.sonmez@mail.com', 'linkedin.com/in/eren', '1992-06-30'),
('Damla', NULL, 'Kilic', NULL, '5051112233', NULL, 'damla.kilic@mail.com', NULL, '1996-11-19'),
('Murat', NULL, 'Ay', NULL, '5442223344', NULL, 'murat.ay@mail.com', NULL, '1985-03-08'),
('Irem', NULL, 'Pek', NULL, '5333334455', NULL, 'irem.pek@mail.com', 'linkedin.com/in/irem', '1994-08-22'),
('Can', NULL, 'Durmaz', NULL, '5554445566', NULL, 'can.durmaz@mail.com', NULL, '1990-01-15'),
('Nilay', NULL, 'Bas', NULL, '5425556677', NULL, 'nilay.bas@mail.com', NULL, '1999-09-05'),
('Okan', NULL, 'Toprak', NULL, '5326667788', NULL, 'okan.toprak@mail.com', 'linkedin.com/in/okan', '1988-05-20'),
('Ece', NULL, 'Kurt', NULL, '5057778899', NULL, 'ece.kurt@mail.com', NULL, '1993-12-28'),
('Yasin', NULL, 'Kara', NULL, '5448889900', NULL, 'yasin.kara@mail.com', NULL, '1991-07-07'),
('Nazli', NULL, 'Ekinci', NULL, '5339990011', NULL, 'nazli.ekinci@mail.com', 'linkedin.com/in/nazli', '1997-03-11'),
('Bora', NULL, 'Genc', NULL, '5550001122', NULL, 'bora.genc@mail.com', NULL, '1986-10-16'),
('Gizem', NULL, 'Ak', NULL, '5421112233', NULL, 'gizem.ak@mail.com', NULL, '1995-02-24');

CREATE USER IF NOT EXISTS 'myuser'@'localhost' IDENTIFIED BY '1234';
GRANT ALL PRIVILEGES ON cmpe343_project2.* TO 'myuser'@'localhost';
FLUSH PRIVILEGES;