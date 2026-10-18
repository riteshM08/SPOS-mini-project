# ✈️ SkyWings Airline Reservation System

A complete, full-stack Airline Reservation System built with **Java 17 + JDBC**, **HTML/CSS/JavaScript**, and **PostgreSQL** (SQLite fallback for offline use).

## 📋 Table of Contents

- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [Database Setup](#database-setup)
- [Default Credentials](#default-credentials)
- [Features](#features)
- [API Reference](#api-reference)
- [Architecture](#architecture)
- [Troubleshooting](#troubleshooting)

## 🛠️ Technology Stack

| Layer      | Technology           |
|------------|----------------------|
| Frontend   | HTML5, CSS3, JavaScript (ES6+) — no external frontend libraries |
| Backend    | Java 17 |
| DB Driver  | JDBC — PostgreSQL driver v42.7.3, SQLite driver v3.45.2 |
| Database   | PostgreSQL 15+ (primary) / SQLite (automatic fallback) |
| HTTP Server| `com.sun.net.httpserver.HttpServer` (built-in JDK) |

> **No frameworks used.** No Spring, Hibernate, Flask, Bootstrap, React, jQuery, or any external library beyond the JDBC drivers.

## 📁 Project Structure

```
SPOS miniproject/
├── run.bat                     # ⬅️ START HERE — Windows startup script
├── compile.bat                 # Recompile all Java sources
├── README.md                   # This file
│
├── src/com/airline/
│   ├── AirlineServer.java      # Main entry point (port 8080)
│   ├── dao/
│   │   ├── DBConnection.java   # JDBC connection manager (PG → SQLite fallback)
│   │   ├── UserDAO.java        # Registration, authentication (PBKDF2)
│   │   ├── FlightDAO.java      # Flight search, seat maps
│   │   ├── BookingDAO.java     # ACID transactional booking & cancellation
│   │   └── AdminDAO.java       # Dashboard stats, flight CRUD
│   ├── handler/
│   │   ├── AuthHandler.java    # REST: /api/auth/*
│   │   ├── FlightHandler.java  # REST: /api/airports, /api/flights/*
│   │   ├── BookingHandler.java # REST: /api/bookings/*
│   │   ├── AdminHandler.java   # REST: /api/admin/*
│   │   ├── HandlerHelper.java  # JSON helpers, CORS, auth extraction
│   │   └── StaticFileHandler.java # Serves frontend files
│   ├── model/
│   │   ├── User.java, Airport.java, Flight.java
│   │   ├── Seat.java, Booking.java, Passenger.java, Payment.java
│   ├── util/
│   │   ├── JsonUtil.java           # Custom zero-dependency JSON parser/serializer
│   │   ├── SecurityUtil.java       # PBKDF2-HMAC-SHA256, session tokens, PNR gen
│   │   └── SchedulerSimulator.java # OS CPU Scheduling (FCFS & Round Robin)
│
├── frontend/
│   ├── index.html              # Single-page application (all 15 screens)
│   ├── css/
│   │   ├── style.css           # Full premium CSS (navy/sky-blue theme)
│   │   └── print.css           # Boarding pass print stylesheet
│   └── js/
│       ├── api.js              # Fetch-based REST API client
│       └── app.js              # SPA controller (routing, state, UI logic)
│
├── database/
│   ├── schema.sql              # PostgreSQL schema (7 tables, FK, indexes)
│   ├── seed_data.sql           # 7 airports, 13 flights, seats, demo booking
│   └── reset_db.sql            # Drop and recreate all tables
│
├── lib/                        # JDBC drivers (bundled)
│   ├── postgresql-42.7.3.jar
│   ├── sqlite-jdbc-3.45.2.0.jar
│   ├── slf4j-api-1.7.36.jar
│   └── slf4j-simple-1.7.36.jar
│
└── bin/                        # Compiled .class files (auto-generated)
```

## 🚀 Quick Start

### Prerequisites

- **JDK 17** — Already extracted to `C:\Users\manth\.jdk-17\` (no system PATH needed)
- **PostgreSQL** (optional) — The app auto-falls back to SQLite if PostgreSQL is unavailable

### Step 1 — Compile (first time only)

Double-click **`compile.bat`** or run in PowerShell:

```powershell
.\compile.bat
```

### Step 2 — Start the Server

Double-click **`run.bat`** or run in PowerShell:

```powershell
.\run.bat
```

### Step 3 — Open in Browser

Navigate to: **http://localhost:8080**

That's it! The app will automatically:
- Attempt to connect to PostgreSQL on `localhost:5432`
- Fall back to SQLite (`airline_db.sqlite`) if PostgreSQL is unavailable
- Create all tables and seed demo data on first run

## 🗄️ Database Setup

### Option A — SQLite (Default, No Setup Required)

The app uses SQLite automatically when PostgreSQL isn't running. Data is stored in `airline_db.sqlite` in the project root.

**To reset the SQLite database:**
```powershell
Remove-Item airline_db.sqlite
.\run.bat   # Will recreate and reseed on startup
```

### Option B — PostgreSQL

1. Install and start PostgreSQL 15+
2. Create the database:
   ```sql
   CREATE DATABASE airline_db;
   ```
3. Run schema and seed scripts:
   ```powershell
   psql -U postgres -d airline_db -f database/schema.sql
   psql -U postgres -d airline_db -f database/seed_data.sql
   ```
4. Configure connection (optional — defaults to `postgres/postgres`):
   ```powershell
   $env:DB_HOST="localhost"
   $env:DB_PORT="5432"
   $env:DB_NAME="airline_db"
   $env:DB_USER="postgres"
   $env:DB_PASSWORD="your_password"
   .\run.bat
   ```

### Database Schema

```
users           — User accounts with PBKDF2-hashed passwords, roles
airports        — Airport codes, names, cities, countries
flights         — Flight schedules, fares, seat capacity, status
seats           — Per-seat records with booking status, class, version (optimistic lock)
bookings        — Reservations with PNR, status, fare, cancellation info
passengers      — Individual passenger details per booking
payments        — Simulated payment records per booking
```

## 🔑 Default Credentials

| Role      | Email                     | Password     |
|-----------|---------------------------|--------------|
| Admin     | `admin@skywings.com`      | `Admin@123`  |
| Passenger | `john.doe@example.com`    | `Password@123` |
| Passenger | `jane.smith@example.com`  | `Password@123` |

## 🌟 Features

### Passenger Features
- ✅ User registration and login (PBKDF2-HMAC-SHA256 password hashing)
- ✅ Flight search by source, destination, date, and passenger count
- ✅ Interactive seat selection (color-coded: available / selected / booked)
- ✅ Multi-passenger booking with individual details
- ✅ Simulated payment (Credit Card / Debit Card / UPI / Net Banking)
- ✅ Booking confirmation with unique PNR
- ✅ Booking history with full details
- ✅ Booking cancellation with 90% refund simulation

### Admin Features
- ✅ Dashboard: total flights, bookings, revenue, occupancy rate
- ✅ Add / edit / deactivate flights
- ✅ View all reservations with passenger and payment details
- ✅ Real-time seat tracker per flight
- ✅ **Operating System CPU Scheduling Simulator:** FCFS (First Come, First Served) and Round Robin (RR) with visual Gantt chart, metrics (Average Waiting Time, Average Turnaround Time), and process table.

### Technical Features
- ✅ ACID transactions for booking (no double-booking)
- ✅ Optimistic locking on seat records (version column)
- ✅ Concurrent booking protection
- ✅ SQL injection prevention (parameterized queries throughout)
- ✅ Role-based access control (admin vs passenger)
- ✅ Session token management (24-hour expiry)
- ✅ PostgreSQL → SQLite automatic failover
- ✅ SPA frontend with 15 screens, no page reloads

## 📡 API Reference

All endpoints are under `http://localhost:8080/api/`

### Auth (`/api/auth/`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | No | Register new user |
| POST | `/api/auth/login` | No | Login, receive session token |
| POST | `/api/auth/logout` | Bearer | Invalidate session |
| GET  | `/api/auth/me` | Bearer | Get current user info |

### Flights (`/api/flights/` and `/api/airports`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/airports` | No | List all airports |
| GET | `/api/flights/search?source=DEL&dest=BOM&date=2025-12-15&passengers=2` | No | Search flights |
| GET | `/api/flights/{id}` | No | Flight details |
| GET | `/api/flights/{id}/seats` | No | Seat map for flight |

### Bookings (`/api/bookings/`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/bookings/create` | Bearer (passenger) | Create booking |
| GET  | `/api/bookings/my` | Bearer (passenger) | My booking history |
| GET  | `/api/bookings/{pnr}` | Bearer (passenger) | Booking details by PNR |
| POST | `/api/bookings/cancel` | Bearer (passenger) | Cancel a booking |

### Admin (`/api/admin/`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET  | `/api/admin/stats` | Bearer (admin) | Dashboard statistics |
| GET  | `/api/admin/flights` | Bearer (admin) | All flights with details |
| POST | `/api/admin/flights/add` | Bearer (admin) | Add new flight |
| PUT  | `/api/admin/flights/update` | Bearer (admin) | Update flight |
| POST | `/api/admin/flights/deactivate` | Bearer (admin) | Deactivate flight |
| GET  | `/api/admin/bookings` | Bearer (admin) | All reservations |
| GET  | `/api/admin/tracker` | Bearer (admin) | Seat tracker |
| GET  | `/api/admin/scheduling?algo=FCFS&quantum=2` | Bearer (admin) | CPU Scheduling Simulation (FCFS & Round Robin) |

## 🏗️ Architecture

```
Browser (HTML/CSS/JS SPA)
    ↓ fetch() REST calls (JSON)
Java HttpServer (port 8080)
    ↓ StaticFileHandler → frontend/
    ↓ AuthHandler / FlightHandler / BookingHandler / AdminHandler
    ↓ DAO layer (UserDAO, FlightDAO, BookingDAO, AdminDAO)
    ↓ JDBC
PostgreSQL 15+ ← → SQLite (fallback)
```

**Security model:**
- Passwords: PBKDF2-HMAC-SHA256, 100,000 iterations, 256-bit key, per-user random salt
- Sessions: 64-byte random hex tokens, 24-hour expiry, server-side in-memory store
- Authorization: Bearer token in `Authorization` header, role enforced per endpoint
- SQL injection: 100% parameterized queries, no string concatenation in SQL

## 🔧 Manual PowerShell Commands

```powershell
$env:JAVA_HOME = 'C:\Users\manth\.jdk-17'
$env:Path = "C:\Users\manth\.jdk-17\bin;$env:Path"
javac -encoding UTF-8 -cp "lib/*" -d bin (Get-ChildItem -Recurse -Filter "*.java" src | % { $_.FullName })
java -cp "bin;lib/*" com.airline.AirlineServer
Remove-Item airline_db.sqlite -ErrorAction SilentlyContinue
```

## 🔍 Troubleshooting

### Server won't start
- Ensure port 8080 is not in use: `netstat -an | findstr 8080`
- Check JDK path exists: `dir C:\Users\manth\.jdk-17\bin\java.exe`
- Run `compile.bat` first if `bin/` is empty

### Login fails
- Ensure `airline_db.sqlite` is fresh (delete and restart server to reseed)
- Check default credentials above

### "Port already in use"
- Find and kill the old Java process: `taskkill /F /IM java.exe`

### PostgreSQL connection refused
- This is normal if PostgreSQL isn't installed — SQLite is used automatically
- No action needed for SQLite mode

### Flight search returns no results
- The seed flights have dates in the near future; search without a date filter to see all
- Or search DEL→BOM, BOM→BLR, DEL→DXB etc. (routes in the seed data)

## 🗺️ Available Flight Routes (Seed Data)

| Route | Flights |
|-------|---------|
| DEL → BOM | SW-101, SW-102, SW-115 |
| BOM → BLR | SW-201, SW-202 |
| DEL → BLR | SW-301 |
| DEL → DXB | SW-401 |
| BOM → DXB | SW-402 |
| DXB → LHR | SW-501 |
| DEL → LHR | SW-601 |
| DEL → JFK | SW-701 |
| DEL → SIN | SW-801 |

*SkyWings Airline Reservation System — Academic Project*
