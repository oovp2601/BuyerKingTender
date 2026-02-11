# BuyerKing Tender System

Sistem tender/lelang dimana **Pembeli adalah RAJA** dengan antarmuka chatbot.

## 🎯 Konsep Utama
- Pembeli membuat permintaan (tender)
- Penjual berlomba memberikan penawaran terbaik
- Pembeli memilih penawaran dengan harga/ketentuan terbaik
- Semua beban kerja ada di penjual, pembeli hanya menentukan kebutuhan

## 🛠️ Teknologi
- Java (JDK 8+)
- MySQL Database
- JDBC Driver (mysql-connector-j-9.6.0.jar)

## 📁 Struktur Database
1. **buyers** - Data pembeli
2. **sellers** - Data penjual
3. **products** - Katalog produk
4. **tenders** - Permintaan tender
5. **bids** - Penawaran dari penjual
6. **chat_logs** - Riwayat percakapan

## How to Run

The application is now Web-based!

### Prerequisites
- Java JDK 11 or later
- MySQL Server

### Steps
1. Ensure MySQL database is running.
2. Run `database/buyerking_schema.sql` in MySQL Workbench or CLI if not already imported.
3. Edit `src/config.properties` and set your MySQL username/password.
4. Run the application:

**Windows:**
```bat
run.bat
```

**Mac/Linux:**
```bash
chmod +x run.sh
./run.sh
```

5. Open your browser and go to: **http://localhost:8080**

### Features
- **Tender Dashboard**: View active tenders.
- **AI Chatbot**: Q&A about tenders (Type "list tenders", "help").
- **Modern UI**: Responsive design with Dark Mode.# BuyerKingTender-BuyesSystem.java
