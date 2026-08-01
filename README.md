# 🔍 Campus Lost and Found System

A modern Spring Boot & Vanilla JS web application designed for campus communities to report, search, match, and claim lost and found items efficiently.

---

## ✨ Features

- **🔐 Dual Authentication**:
  - Email OTP-based user authentication & session management.
  - Role-Based Access Control (Users vs. Admins).
- **📋 Lost & Found Item Management**:
  - Report lost items with image uploads, descriptions, categories, and locations.
  - Browse active items with search and category filters.
- **🤖 Automated Matching Engine**:
  - Intelligent background matching algorithm to match lost items with reported found items.
  - Automatic email notifications sent to reporters upon potential matches.
- **⚡ Admin Control Panel**:
  - Manage users, block malicious accounts, monitor matches, and track reported claims.
- **📱 Responsive UI**:
  - Glassmorphic, modern dashboard interface built with responsive CSS and HTML5.

---

## 🛠️ Technology Stack

- **Backend**: Java 17+, Spring Boot, Spring Security, Spring Data JPA
- **Database**: Oracle DB / H2 / PostgreSQL compatible
- **Email Service**: Spring Boot Mail (`JavaMailSender`)
- **Security**: JWT (JSON Web Tokens), BCrypt Password Hashing
- **Frontend**: HTML5, CSS3, JavaScript (Fetch API)

---

## 🚀 Quick Start

### 1. Prerequisites
- **JDK 17** or higher
- **Maven 3.8+** (or included `./mvnw` wrapper)
- **Oracle DB** (or your preferred SQL Database)

### 2. Environment Setup
1. Copy `src/main/resources/application.properties.example` to `application.properties`.
2. Configure your database credentials and Gmail App password:

```properties
# Database
spring.datasource.url=jdbc:oracle:thin:@localhost:1522/xepdb1
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

# Email Service
spring.mail.username=your_email@gmail.com
spring.mail.password=your_16_digit_app_password

# Security & Admin Setup
admin.email=admin@example.com
admin.password=your_admin_password
jwt.secret=your_custom_long_jwt_secret_key
```

### 3. Run the Application
Using Maven Wrapper:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Once running, navigate to:
- **Application Portal**: `http://localhost:8082`
- **Login / Register**: `http://localhost:8082/login.html`
- **Admin Dashboard**: `http://localhost:8082/admin.html`

---

## 🔒 Security & Privacy Notice

All sensitive configurations (passwords, JWT secrets, mail credentials) are externalized using environment variables and default placeholders. **Do not commit actual production credentials to public repositories.**

---

## 📄 License

This project is open-source under the MIT License.
