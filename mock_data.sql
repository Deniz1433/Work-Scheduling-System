-- Mock Data for Attendance System
-- Departments, Roles, and Users

-- 1. DEPARTMENTS
INSERT INTO department (id, name, min_days, child_department_id) VALUES
(1, 'Yönetim', 3, NULL),
(2, 'İnsan Kaynakları', 2, NULL),
(3, 'Muhasebe', 2, NULL),
(4, 'Satış', 3, NULL),
(5, 'Pazarlama', 2, NULL),
(6, 'Bilgi Teknolojileri', 2, NULL),
(7, 'Üretim', 3, NULL),
(8, 'Kalite Kontrol', 2, NULL),
(9, 'Lojistik', 2, NULL),
(10, 'Müşteri Hizmetleri', 2, NULL);

-- 2. ROLES
INSERT INTO role (id, name, description, is_active) VALUES
(1, 'ROLE_admin', 'Sistem Yöneticisi', true),
(2, 'ROLE_manager', 'Departman Müdürü', true),
(3, 'ROLE_team_leader', 'Takım Lideri', true),
(4, 'ROLE_employee', 'Çalışan', true),
(5, 'ROLE_hr_manager', 'İK Müdürü', true),
(6, 'ROLE_accountant', 'Muhasebeci', true),
(7, 'ROLE_sales_rep', 'Satış Temsilcisi', true),
(8, 'ROLE_developer', 'Yazılım Geliştirici', true),
(9, 'ROLE_designer', 'Tasarımcı', true),
(10, 'ROLE_analyst', 'Analist', true);

-- 3. USERS (35 kişi)
INSERT INTO users (id, first_name, last_name, email, username, password, role_id, department_id) VALUES
-- Yönetim (5 kişi)
(1, 'Ahmet', 'Yılmaz', 'ahmet.yilmaz@company.com', 'ahmet.yilmaz', '$2a$10$encrypted_password', 1, 1),
(2, 'Fatma', 'Demir', 'fatma.demir@company.com', 'fatma.demir', '$2a$10$encrypted_password', 2, 1),
(3, 'Mehmet', 'Kaya', 'mehmet.kaya@company.com', 'mehmet.kaya', '$2a$10$encrypted_password', 2, 1),
(4, 'Ayşe', 'Özkan', 'ayse.ozkan@company.com', 'ayse.ozkan', '$2a$10$encrypted_password', 2, 1),
(5, 'Ali', 'Çelik', 'ali.celik@company.com', 'ali.celik', '$2a$10$encrypted_password', 3, 1),

-- İnsan Kaynakları (4 kişi)
(6, 'Zeynep', 'Arslan', 'zeynep.arslan@company.com', 'zeynep.arslan', '$2a$10$encrypted_password', 5, 2),
(7, 'Can', 'Yıldız', 'can.yildiz@company.com', 'can.yildiz', '$2a$10$encrypted_password', 3, 2),
(8, 'Elif', 'Kurt', 'elif.kurt@company.com', 'elif.kurt', '$2a$10$encrypted_password', 4, 2),
(9, 'Burak', 'Şahin', 'burak.sahin@company.com', 'burak.sahin', '$2a$10$encrypted_password', 4, 2),

-- Muhasebe (3 kişi)
(10, 'Selin', 'Aydın', 'selin.aydin@company.com', 'selin.aydin', '$2a$10$encrypted_password', 6, 3),
(11, 'Emre', 'Koç', 'emre.koc@company.com', 'emre.koc', '$2a$10$encrypted_password', 6, 3),
(12, 'Deniz', 'Erdoğan', 'deniz.erdogan@company.com', 'deniz.erdogan', '$2a$10$encrypted_password', 4, 3),

-- Satış (5 kişi)
(13, 'Gizem', 'Türk', 'gizem.turk@company.com', 'gizem.turk', '$2a$10$encrypted_password', 2, 4),
(14, 'Onur', 'Güneş', 'onur.gunes@company.com', 'onur.gunes', '$2a$10$encrypted_password', 7, 4),
(15, 'Merve', 'Aktaş', 'merve.aktas@company.com', 'merve.aktas', '$2a$10$encrypted_password', 7, 4),
(16, 'Serkan', 'Özdemir', 'serkan.ozdemir@company.com', 'serkan.ozdemir', '$2a$10$encrypted_password', 7, 4),
(17, 'Esra', 'Korkmaz', 'esra.korkmaz@company.com', 'esra.korkmaz', '$2a$10$encrypted_password', 7, 4),

-- Pazarlama (4 kişi)
(18, 'Ceren', 'Bulut', 'ceren.bulut@company.com', 'ceren.bulut', '$2a$10$encrypted_password', 2, 5),
(19, 'Kaan', 'Yalçın', 'kaan.yalcin@company.com', 'kaan.yalcin', '$2a$10$encrypted_password', 9, 5),
(20, 'Seda', 'Özkan', 'seda.ozkan@company.com', 'seda.ozkan', '$2a$10$encrypted_password', 9, 5),
(21, 'Tolga', 'Kılıç', 'tolga.kilic@company.com', 'tolga.kilic', '$2a$10$encrypted_password', 4, 5),

-- Bilgi Teknolojileri (6 kişi)
(22, 'Ozan', 'Tekin', 'ozan.tekin@company.com', 'ozan.tekin', '$2a$10$encrypted_password', 2, 6),
(23, 'İrem', 'Yılmaz', 'irem.yilmaz@company.com', 'irem.yilmaz', '$2a$10$encrypted_password', 8, 6),
(24, 'Ege', 'Demir', 'ege.demir@company.com', 'ege.demir', '$2a$10$encrypted_password', 8, 6),
(25, 'Defne', 'Kara', 'defne.kara@company.com', 'defne.kara', '$2a$10$encrypted_password', 8, 6),
(26, 'Arda', 'Sönmez', 'arda.sonmez@company.com', 'arda.sonmez', '$2a$10$encrypted_password', 8, 6),
(27, 'Yağmur', 'Çetin', 'yagmur.cetin@company.com', 'yagmur.cetin', '$2a$10$encrypted_password', 10, 6),

-- Üretim (4 kişi)
(28, 'Murat', 'Aksoy', 'murat.aksoy@company.com', 'murat.aksoy', '$2a$10$encrypted_password', 2, 7),
(29, 'Sevgi', 'Yıldırım', 'sevgi.yildirim@company.com', 'sevgi.yildirim', '$2a$10$encrypted_password', 4, 7),
(30, 'Hakan', 'Özkan', 'hakan.ozkan@company.com', 'hakan.ozkan', '$2a$10$encrypted_password', 4, 7),
(31, 'Aylin', 'Kaya', 'aylin.kaya@company.com', 'aylin.kaya', '$2a$10$encrypted_password', 4, 7),

-- Kalite Kontrol (2 kişi)
(32, 'Büşra', 'Taş', 'busra.tas@company.com', 'busra.tas', '$2a$10$encrypted_password', 3, 8),
(33, 'Kemal', 'Yılmaz', 'kemal.yilmaz@company.com', 'kemal.yilmaz', '$2a$10$encrypted_password', 4, 8),

-- Lojistik (1 kişi)
(34, 'Rıza', 'Demir', 'riza.demir@company.com', 'riza.demir', '$2a$10$encrypted_password', 3, 9),

-- Müşteri Hizmetleri (1 kişi)
(35, 'Selin', 'Kurt', 'selin.kurt@company.com', 'selin.kurt', '$2a$10$encrypted_password', 3, 10);

-- 4. PERMISSIONS (Temel izinler)
INSERT INTO permission (id, name, description) VALUES
(1, 'USER_READ', 'Kullanıcı okuma izni'),
(2, 'USER_WRITE', 'Kullanıcı yazma izni'),
(3, 'ATTENDANCE_READ', 'Devam durumu okuma izni'),
(4, 'ATTENDANCE_WRITE', 'Devam durumu yazma izni'),
(5, 'ATTENDANCE_APPROVE', 'Devam durumu onaylama izni'),
(6, 'DEPARTMENT_READ', 'Departman okuma izni'),
(7, 'DEPARTMENT_WRITE', 'Departman yazma izni'),
(8, 'ROLE_READ', 'Rol okuma izni'),
(9, 'ROLE_WRITE', 'Rol yazma izni'),
(10, 'REPORT_READ', 'Rapor okuma izni');

-- 5. ROLE PERMISSIONS (Rol-İzin ilişkileri)
INSERT INTO role_permission (id, role_id, permission_id) VALUES
-- Admin tüm izinlere sahip
(1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4), (5, 1, 5), (6, 1, 6), (7, 1, 7), (8, 1, 8), (9, 1, 9), (10, 1, 10),

-- Manager izinleri
(11, 2, 1), (12, 2, 3), (13, 2, 4), (14, 2, 5), (15, 2, 6), (16, 2, 10),

-- Team Leader izinleri
(17, 3, 1), (18, 3, 3), (19, 3, 4), (20, 3, 5), (21, 3, 10),

-- Employee izinleri
(22, 4, 3), (23, 4, 4),

-- HR Manager izinleri
(24, 5, 1), (25, 5, 2), (26, 5, 6), (27, 5, 7), (28, 5, 8), (29, 5, 9), (30, 5, 10),

-- Accountant izinleri
(31, 6, 3), (32, 6, 4), (33, 6, 10),

-- Sales Rep izinleri
(34, 7, 3), (35, 7, 4),

-- Developer izinleri
(36, 8, 3), (37, 8, 4),

-- Designer izinleri
(38, 9, 3), (39, 9, 4),

-- Analyst izinleri
(40, 10, 3), (41, 10, 4), (42, 10, 10);

-- 6. ATTENDANCE (Örnek devam durumu verileri - gelecek hafta için: 4-8 Ağustos 2025)
INSERT INTO attendance (id, user_id, week_start, monday, tuesday, wednesday, thursday, friday, is_approved) VALUES
-- Yönetim departmanı
(1, '1', '2025-08-04', 0, 0, 1, 1, 5, true),
(2, '2', '2025-08-04', 1, 1, 0, 4, 5, false),
(3, '3', '2025-08-04', 1, 2, 3, 1, 5, false),
(4, '4', '2025-08-04', 1, 2, 1, 0, 5, true),
(5, '5', '2025-08-04', 0, 1, 1, 1, 5, false),

-- İnsan Kaynakları departmanı
(6, '6', '2025-08-04', 1, 1, 1, 1, 1, true),
(7, '7', '2025-08-04', 1, 1, 0, 1, 1, false),
(8, '8', '2025-08-04', 1, 0, 1, 1, 1, false),
(9, '9', '2025-08-04', 1, 1, 1, 0, 1, true),

-- Muhasebe departmanı
(10, '10', '2025-08-04', 1, 1, 1, 1, 1, true),
(11, '11', '2025-08-04', 1, 1, 0, 1, 1, false),
(12, '12', '2025-08-04', 1, 0, 1, 1, 1, false),

-- Satış departmanı
(13, '13', '2025-08-04', 1, 1, 1, 1, 1, true),
(14, '14', '2025-08-04', 1, 1, 0, 1, 1, false),
(15, '15', '2025-08-04', 1, 0, 1, 1, 1, false),
(16, '16', '2025-08-04', 1, 1, 1, 0, 1, true),
(17, '17', '2025-08-04', 0, 1, 1, 1, 1, false),

-- Pazarlama departmanı
(18, '18', '2025-08-04', 1, 1, 1, 1, 1, true),
(19, '19', '2025-08-04', 1, 1, 0, 1, 1, false),
(20, '20', '2025-08-04', 1, 0, 1, 1, 1, false),
(21, '21', '2025-08-04', 1, 1, 1, 0, 1, true),

-- Bilgi Teknolojileri departmanı
(22, '22', '2025-08-04', 1, 1, 1, 1, 1, true),
(23, '23', '2025-08-04', 1, 1, 0, 1, 1, false),
(24, '24', '2025-08-04', 1, 0, 1, 1, 1, false),
(25, '25', '2025-08-04', 1, 1, 1, 0, 1, true),
(26, '26', '2025-08-04', 0, 1, 1, 1, 1, false),
(27, '27', '2025-08-04', 1, 1, 1, 1, 0, false),

-- Üretim departmanı
(28, '28', '2025-08-04', 1, 1, 1, 1, 1, true),
(29, '29', '2025-08-04', 1, 1, 0, 1, 1, false),
(30, '30', '2025-08-04', 1, 0, 1, 1, 1, false),
(31, '31', '2025-08-04', 1, 1, 1, 0, 1, true),

-- Kalite Kontrol departmanı
(32, '32', '2025-08-04', 1, 1, 1, 1, 1, true),
(33, '33', '2025-08-04', 1, 1, 0, 1, 1, false),

-- Lojistik departmanı
(34, '34', '2025-08-04', 1, 1, 1, 1, 1, true),

-- Müşteri Hizmetleri departmanı
(35, '35', '2025-08-04', 1, 1, 1, 1, 1, true);

-- 7. HOLIDAYS (2025 yılı resmi tatilleri)
INSERT INTO holiday (id, name, date, end_date, country_code) VALUES
(1, 'Yılbaşı', '2025-01-01', '2025-01-01', 'TR'),
(2, 'Ulusal Egemenlik ve Çocuk Bayramı', '2025-04-23', '2025-04-23', 'TR'),
(3, 'Emek ve Dayanışma Günü', '2025-05-01', '2025-05-01', 'TR'),
(4, 'Atatürk''ü Anma, Gençlik ve Spor Bayramı', '2025-05-19', '2025-05-19', 'TR'),
(5, 'Demokrasi ve Milli Birlik Günü', '2025-07-15', '2025-07-15', 'TR'),
(6, 'Zafer Bayramı', '2025-08-30', '2025-08-30', 'TR'),
(7, 'Cumhuriyet Bayramı', '2025-10-29', '2025-10-29', 'TR'),
(8, 'Kurban Bayramı', '2025-06-07', '2025-06-10', 'TR'),
(9, 'Ramazan Bayramı', '2025-03-31', '2025-04-02', 'TR');

-- 8. EXCUSES (Örnek mazeret kayıtları - gelecek hafta için)
INSERT INTO excuse (id, user_id, excuse_date, excuse_type, description, is_approved) VALUES
(1, '2', '2025-08-05', 1, 'Hastalık nedeniyle izin', false),
(2, '3', '2025-08-06', 2, 'Acil aile durumu', true),
(3, '7', '2025-08-05', 1, 'Doktor randevusu', true),
(4, '8', '2025-08-07', 3, 'Kişisel mazeret', false),
(5, '11', '2025-08-06', 1, 'Hastalık', true),
(6, '14', '2025-08-05', 2, 'Acil durum', false),
(7, '15', '2025-08-07', 1, 'Sağlık sorunu', true),
(8, '19', '2025-08-06', 3, 'Kişisel mazeret', false),
(9, '23', '2025-08-05', 1, 'Hastalık', true),
(10, '24', '2025-08-07', 2, 'Acil aile durumu', false),
(11, '26', '2025-08-06', 1, 'Doktor randevusu', true),
(12, '29', '2025-08-05', 3, 'Kişisel mazeret', false),
(13, '30', '2025-08-07', 1, 'Hastalık', true),
(14, '33', '2025-08-06', 2, 'Acil durum', false);

-- 9. ROLE HIERARCHY (Rol hiyerarşisi)
INSERT INTO role_hierarchy (id, parent_role, child_role) VALUES
(1, 'ROLE_admin', 'ROLE_manager'),
(2, 'ROLE_admin', 'ROLE_hr_manager'),
(3, 'ROLE_manager', 'ROLE_team_leader'),
(4, 'ROLE_team_leader', 'ROLE_employee'),
(5, 'ROLE_hr_manager', 'ROLE_employee'),
(6, 'ROLE_manager', 'ROLE_sales_rep'),
(7, 'ROLE_manager', 'ROLE_developer'),
(8, 'ROLE_manager', 'ROLE_designer'),
(9, 'ROLE_manager', 'ROLE_analyst'),
(10, 'ROLE_manager', 'ROLE_accountant');

-- 10. ROLE NODE POSITIONS (Rol pozisyonları - görsel hiyerarşi için)
INSERT INTO role_node_position (role, pos_x, pos_y) VALUES
('ROLE_admin', 400, 50),
('ROLE_manager', 200, 150),
('ROLE_hr_manager', 600, 150),
('ROLE_team_leader', 100, 250),
('ROLE_employee', 50, 350),
('ROLE_sales_rep', 300, 250),
('ROLE_developer', 500, 250),
('ROLE_designer', 700, 250),
('ROLE_analyst', 900, 250),
('ROLE_accountant', 1100, 250); 