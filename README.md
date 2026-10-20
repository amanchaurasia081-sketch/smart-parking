# 🅿️ SmartPark - Smart Working Parking Management System

[![Live Demo](https://img.shields.io/badge/Demo-Live%20Application-brightgreen?style=for-the-badge&logo=github)](https://amanchaurasia081-sketch.github.io/smart-parking/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4%20%2F%204.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25%20%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

**SmartPark** is a full-stack, enterprise-grade Smart Parking Management System designed to streamline parking slot allocation, real-time availability tracking, automated pricing, vehicle entry/exit auditing, and parking lot administrative controls.

---

## 🌐 Live Deployment

- **Live Web Application**: [https://amanchaurasia081-sketch.github.io/smart-parking/](https://amanchaurasia081-sketch.github.io/smart-parking/)
- **GitHub Repository**: [https://github.com/amanchaurasia081-sketch/smart-parking](https://github.com/amanchaurasia081-sketch/smart-parking)
- **Deployment Platform**: GitHub Pages with Automated CI/CD & SSL Encryption

---

## 🔑 Demo Login Credentials

For quick evaluation and testing:

| Role | Email | Password | Features |
| :--- | :--- | :--- | :--- |
| ⚙️ **Administrator** | `admin@smartpark.com` | `admin123` | Full Admin Dashboard, Slot Management (Add/Edit/Delete/Release), Global Booking Log |
| 👤 **Standard User** | `demo@user.com` | `user123` | Slot Reservation, Vehicle Entry/Exit, Payment Status, Booking Cancellation |

*(New user registration is also available directly on the web interface)*

---

## 🚀 Key Features

### 1. 🅿️ Real-Time Interactive Parking Grid
- Live visual ground-floor parking map with status indicators (**Available**, **Occupied**, **Selected**).
- Real-time slot availability counter and occupancy percentage progress bar.
- One-click slot selection and dynamic hourly price estimation (₹20/hour).

### 2. 🔐 Dual-Role Authentication & Security
- Secure registration and login powered by BCrypt password encryption.
- Role-based views: Personalized User Dashboard vs. Full Admin Control Panel.

### 3. 🚗 Comprehensive Vehicle Lifecycle
- **Reservation**: Book upcoming slots with vehicle plate registration.
- **Vehicle Entry**: Timestamped check-in when vehicle pulls into the slot (`parked` status).
- **Vehicle Exit**: Timestamped checkout when vehicle departs; automatically releases slot back to `AVAILABLE`.
- **Payment Reconciliation**: Direct payment tracking (`Pending` / `Paid`).
- **Cancellation**: One-click reservation cancel with automatic slot release.

### 4. ⚙️ Administrative Control Panel
- Real-time statistics across total slots, occupied slots, available slots, and total revenue.
- Slot inventory management: Add new parking bays (e.g. `A21`, `B01`), edit bay designations, or delete slots.
- Emergency slot release override.
- Global booking audit trail.

### 5. ⚡ Hybrid Architecture (Spring Boot + Standalone Mode)
- **Spring Boot Live REST API**: Connects to Spring Boot backend (`http://localhost:8081/api`) with H2 in-memory or MySQL database.
- **Resilient Client Engine**: Transparently falls back to persistent client-side storage when running standalone or hosted on GitHub Pages, ensuring zero downtime and 100% feature availability anywhere.

---

## 🛠️ Technology Stack

- **Backend**: Spring Boot, Spring Data JPA, Spring Security, BCrypt, Maven.
- **Databases**: 
  - **H2 In-Memory Database** (default zero-configuration mode for instant startup anywhere).
  - **MySQL 8.x** (optional enterprise production profile).
- **Frontend**: Responsive HTML5, Modern CSS3 with CSS Variables, ES6+ JavaScript.
- **Hosting & CI/CD**: GitHub Pages, Git.

---

## 💻 Local Setup & Running

### Prerequisites
- Java Development Kit (JDK 25 or higher)
- Git

### Quick Launch (Windows)
Double-click `start-app.bat` to launch the Spring Boot server and automatically open the application in your browser!

### Running via Command Line
```powershell
# 1. Clone the repository
git clone https://github.com/amanchaurasia081-sketch/smart-parking.git
cd smart-parking

# 2. Run Spring Boot Backend
.\mvnw.cmd spring-boot:run

# 3. Open browser at:
# http://localhost:8081/
```

### Database Console
When running the Spring Boot backend:
- H2 Web Console: [http://localhost:8081/h2-console](http://localhost:8081/h2-console)
- JDBC URL: `jdbc:h2:mem:smart_parking`
- Username: `sa`
- Password: *(leave blank)*

---

## 📡 REST API Reference

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/slots` | Retrieve all parking slots |
| `GET` | `/api/slots/available` | Retrieve all currently available slots |
| `POST` | `/api/slots` | Add a new parking slot (Admin) |
| `PUT` | `/api/slots/{id}` | Update slot number or status |
| `DELETE` | `/api/slots/{id}` | Delete a parking slot |
| `PUT` | `/api/slots/{id}/release`| Force release a parking slot |
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Authenticate user credentials |
| `GET` | `/api/bookings` | Retrieve all bookings (Admin) |
| `GET` | `/api/bookings/user/{email}` | Retrieve bookings for a specific user |
| `POST` | `/api/bookings` | Create a new parking reservation |
| `PUT` | `/api/bookings/{id}/entry` | Record vehicle arrival |
| `PUT` | `/api/bookings/{id}/exit` | Record vehicle departure |
| `PUT` | `/api/bookings/{id}/payment?status=Paid` | Update payment status |
| `PUT` | `/api/bookings/{id}/cancel` | Cancel an active reservation |

---

## 👤 Author

Developed by **Aman Chaurasia** ([@amanchaurasia081-sketch](https://github.com/amanchaurasia081-sketch))
