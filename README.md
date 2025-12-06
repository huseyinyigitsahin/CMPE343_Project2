# 📞 CMPE343 Project 2 – Contact Management System (CLI)

> Bu proje, CMPE343 dersi için geliştirilmiş, rol tabanlı menü sistemi (Tester / Manager), güvenli kullanıcı kimlik doğrulama ve MySQL veritabanında depolanan `contacts` tablosu üzerinde zengin arama özelliklerine sahip, **konsol tabanlı** bir İletişim Yönetim Uygulamasıdır.

## 🌟 Genel Bakış ve Özellikler

Bu **komut satırı** uygulaması, kimliği doğrulanmış kullanıcıların aşağıdaki işlemleri gerçekleştirmesine olanak tanır:

* Kullanıcı adı ve parola ile güvenli bir şekilde **Giriş Yapma**.
* Parolalarını güvenli bir şekilde **Yönetme** (SHA-256 hash'leri).
* MySQL veritabanında depolanan kişileri **Listeleme** ve **Arama**.
* Hem **Basit** (tek alanlı) hem de **Gelişmiş** (çok alanlı, mantıksal AND) arama yapma.
* Kişileri birden çok alana göre **Sıralama**.

Kullanıcılar **rollerine** (örneğin, Tester, Manager) göre ayrılmıştır.

---

### 🛡️ Kimlik Doğrulama ve Güvenlik

* Giriş, MySQL'deki `users` tablosuna karşı yapılır.
* Parolalar **SHA-256 hash'leri** olarak saklanır.
* Giriş sırasında, kullanıcıya parolasının gücü (`VERY_WEAK`, `WEAK`, `MEDIUM`, `STRONG`) gösterilir.
* Parola değiştirme sırasında, kullanıcıya **rastgele güçlü bir parola önerisi** sunulur ve yeni parolanın **güç hesaplaması** gösterilir.

### 🔍 Gelişmiş Arama Yetenekleri

Projenin temel güçlerinden biri olan gelişmiş arama iki ana moda sahiptir:

1.  **Hızlı Filtreler (Quick Filters):**
    * Bu ay doğacak doğum günleri.
    * Son 10 günde eklenen kişiler.
    * Eksik önemli bilgiye sahip kişiler (e-posta/telefon/linkedin).

2.  **Özel Gelişmiş Arama (Multi-field, AND):**
    * Kullanıcı en fazla **6 koşulu** birleştirebilir.
    * Koşullar, `STARTS WITH`, `CONTAINS`, `EQUALS` veya tarih bazlı operatörler kullanılarak **mantıksal AND** ile birleştirilir.
    * **Doğum Tarihi** alanı, tam tarih (`YYYY-MM-DD`), aya (sayı veya ad) veya yıla göre aramayı destekler.
    * Tüm girdi değerleri (Adlar, Telefon, E-posta vb.) eklenmeden önce **kesinlikle doğrulanır** (validate edilir).

### ⚙️ Tester Menüsü (`TesterMenu`)

Tester kullanıcıları için temel menü (`TesterMenu#showMenu`) aşağıdaki seçenekleri sunar:

1.  Parola değiştir
2.  Tüm kişileri listele
3.  Kişileri ara (Basit / Gelişmiş)
4.  Kişileri sırala
5.  Çıkış yap (Logout)

---

## 💾 Veri Modeli (Data Model)

Proje en az iki ana tablo kullanır: `users` ve `contacts`.

### `users` Tablosu

Kimlik doğrulama ve parola yönetimi için kullanılır.

| Sütun Adı | Tür | Açıklama |
| :--- | :--- | :--- |
| `username` | `VARCHAR` (PK) | Kullanıcı Adı |
| `password_hash` | `VARCHAR` | SHA-256 parola hash'i |
| `name` | `VARCHAR` | Kullanıcının Adı |
| `surname` | `VARCHAR` | Kullanıcının Soyadı |

### `contacts` Tablosu

Listeleme, arama ve sıralama özellikleri tarafından kullanılır.

| Sütun Adı | Tür | Açıklama |
| :--- | :--- | :--- |
| `contact_id` | `INT` (PK) | Kişi Kimliği |
| `first_name` | `VARCHAR` | Ad |
| `middle_name` | `VARCHAR` | İkinci Ad (nullable) |
| `last_name` | `VARCHAR` | Soyad |
| `nickname` | `VARCHAR` | Takma Ad |
| `phone_primary` | `VARCHAR` | Birincil Telefon |
| `email` | `VARCHAR` | E-posta Adresi |
| `birth_date` | `DATE` / `DATETIME` | Doğum Tarihi |
| `created_at` | `DATETIME` | Oluşturma Zamanı |
| ... | ... | Diğer alanlar (`phone_secondary`, `linkedin_url`, `updated_at` vb.) |

---

## 🛠️ Teknoloji Yığını (Technology Stack)

| Kategori | Teknoloji |
| :--- | :--- |
| **Dil** | **Java** (JDK 8+ tavsiye edilir) |
| **Veritabanı** | **MySQL** |
| **Veritabanı Sürücüsü** | MySQL Connector/J (JDBC) |
| **Geliştirme Ortamı** | NetBeans (Orijinal IDE) |

---

## 🚀 Kurulum ve Çalıştırma

### 1. Veritabanı Kurulumu

1.  **MySQL**'i kurun (örneğin XAMPP veya MySQL Community Server kullanarak).
2.  Proje için bir veritabanı oluşturun:

    ```sql
    CREATE DATABASE cmpe343_project2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    ```
3.  `users` ve `contacts` tablolarını yukarıdaki Veri Modeline uygun olarak oluşturun.

### 2. Veritabanı Bağlantı Yapılandırması

Projenin kaynak kodunda (genellikle bir yapılandırma dosyasında veya `DBManager`/`DatabaseConnection` sınıfında), **MySQL bağlantı bilgilerinizi** (URL, kullanıcı adı, parola) güncellediğinizden emin olun.

### 3. Derleme ve Çalıştırma

Projenin bir NetBeans projesi olduğu varsayılırsa:

1.  Projeyi NetBeans'te açın veya bağımlılıkları (MySQL Connector/J JAR) ekleyerek komut satırından derleyin.
2.  Projenin ana sınıfını çalıştırın (örneğin `Main.java` veya `Application.java`).

    ```bash
    # Örnek komut satırı çalıştırma
    java -jar contact_management_system.jar
    ```

---

## 👨‍💻 Kullanım Kılavuzu

Uygulama başlatıldığında, kullanıcıdan **Giriş** yapması istenir.

### Giriş

Kullanıcı adı ve parola girin. Parolanın gücü hakkında bir banner göreceksiniz.

### Tester Menü Akışı

Başarılı bir girişten sonra, `TesterMenu` karşınıza çıkacaktır:

+-----------------------------------+ | TESTER MENU - Merhaba [Ad]! | +-----------------------------------+ |
 1. Parola değiştir | | 2. Tüm kişileri listele | | 3. Kişileri ara | | 4. Kişileri sırala | | 5. Çıkış yap | 
 +-----------------------------------+
Seçiminizi girin:

### Arama Menüsü Örneği

Seçenek **3 (Kişileri ara)** seçildiğinde, Basit Arama ve Gelişmiş Arama seçenekleri sunulur. Gelişmiş Arama, kullanıcıdan sırayla koşulları girmesini ister.

* `back`: Son eklenen koşulu geri alır.
* `quit`: Gelişmiş aramayı iptal eder.

---

## 💡 Kısıtlamalar ve Olası İyileştirmeler

* **Manager Özellikleri:** Şu an temel olarak `TesterMenu` odaklıdır. `ManagerMenu` sınıfı üzerinden kullanıcı yönetimi (ekleme, silme, güncelleme) gibi özellikler eklenebilir.
* **Kullanıcı Arayüzü:** Uygulama tamamen konsol tabanlıdır. Daha gelişmiş bir deneyim için GUI (Swing/JavaFX) veya web arayüzüne taşınabilir.
* **Veritabanı İşlemleri:** Hata yönetimi ve bağlantı havuzları iyileştirilebilir.