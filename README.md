# Class Automated Attendance System (CAATS)

Welcome to **CAATS** (Class Automated Attendance System), an Android application designed to streamline and automate class attendance using geolocation technology and Supabase cloud services.

---

## Table of Contents

- [Main Goals](#main-goals)
- [System Architecture & Design](#system-architecture--design)
- [Key Features](#key-features)
- [Core Functional Modules](#core-functional-modules)
- [Data Storage & Backend](#data-storage--backend)
- [Testing & Validation Status](#testing--validation-status)
- [Mentor](#mentor)

---

## Main Goals

Our project aims to:

- Implement secure, role-based authentication for students, tutors, and coordinators.
- Enable tutors to easily create attendance sessions linked to classrooms and subjects.
- Automate student attendance marking via GPS geolocation and geofencing.
- Provide students with real-time access to subject-wise and overall attendance records.
- Generate detailed and consolidated attendance reports for tutors and coordinators.
- Auto-generate lists of debarred students (below 70% attendance threshold).
- Ensure scalable, real-time data sync with Supabase.

---

## System Architecture & Design

Built using **Android** (XML for frontend UI, Java for business logic) and **Supabase** (PostgreSQL + PostGIS) for cloud backend as a service.

The system is divided into three role-based interfaces:

### 1. Student App
- Secure login
- Class schedule viewing
- Real-time notifications to mark attendance
- Geofence validation for attendance marking
- Viewing personal attendance records

### 2. Tutor App
- Session creation & management
- Live attendance monitoring
- Subject-wise reports with debarred student lists

### 3. Coordinator App
- High-level interface for generating weekly consolidated reports across all subjects and sections

---

## Key Features

- **Authentication & Profiles:** Secure login with role-based access via Supabase Auth (JWT).
- **Classroom Geofencing:** Classroom boundaries set as polygons; verifies student's GPS location with server-side Point-in-Polygon algorithms using PostGIS.
- **Attendance Session Management:** Tutors create time-limited attendance sessions linked to classrooms and subjects.
- **Attendance Recording:** Secure Supabase RPC validates geofence and records attendance. Automatic marking of absentees after session expiry.
- **Reports & Analytics:** Optimized SQL views/RPCs for aggregating attendance data, calculating percentages, and listing debarred students.
- **Notification:** integrated Firebase Cloud Messaging to notify students of newly created sessions

---

## Core Functional Modules

- **Authentication & Role Management:** Secure login for all roles using JWT and bcrypt.
- **Classroom Geofencing Module:** Validates student’s location using Point-in-Polygon check.
- **Attendance Session Manager:** Handles session creation, notifications, and manual/automatic attendance marking.
- **Attendance Recording Module:** Stores attendance with timestamp, GPS accuracy, device info; uses hashing and indexes.
- **Reporting & Analytics:**
  - Student: View subject/overall attendance %
  - Tutor: Subject-wise reports, debarred list (<70% attendance)
  - Coordinator: Weekly class reports

---

## Data Storage & Backend

All data hosted on Supabase-managed PostgreSQL with PostGIS extension.

### Main Tables:

- `Profiles`: User details (linked with Auth)
- `Students`, `Tutors`, `Coordinators`: Role-specific info
- `Subjects`, `Enrollments`: Relationships for tracking
- `Classrooms`: Geofence polygons for attendance validation
- `Attendance_records`: Storing attendance record
- `Attendance_sessions`: Storing created Sessions

> **Codebase:** Public GitHub repository for collaborative development and integrated CI/CD.  
> **Backend deploys:** Supabase (database, authentication, functions).

---

## Testing & Validation Status

| Test Area                 | Status | Notes                                                                                 |
|---------------------------|--------|---------------------------------------------------------------------------------------|
| Authentication & RLS      | Pass   | Verified secure access and role-specific data views.                                  |
| Session Creation          | Pass   | Tutors can create sessions; auto-expiry cron triggers session status updates.          |
| Geofence Validation       | Pass   | Correctly marks students present/returns error if out of geofence.                    |
| Automatic Absence         | Pass   | Expired sessions insert 'Absent' for unmarked students automatically.                 |
| Report Generation         | Pass   | All roles fetch aggregate reports with accurate attendance percentages.               |
| Debarred Student List     | Pass   | Properly flags students below attendance threshold.                                   |
| PDF Report Generation     | Pass   | Reports downloadable by tutors; no longer causes ANRs.                                |

---

## Mentor

**Mr. Utsav Kumar**  
> Assistant Professor  
> Department of Computer Applications  
> Graphic Era Deemed to be University

---

## Language Composition

- **Java:** 98.9%
- **TypeScript:** 1.1%
 >Typescript was used for setting up edge function that acts as a bridge between supabase and FCM servers. it pushes notifications to the required students

---

## Repository

[GitHub: mohamedsadah/PBL-CAATS](https://github.com/mohamedsadah/PBL-CAATS)

---

> For further details or collaboration requests, please contact:
> ibnaadam806@gmail.com
> +23278901710 - whatsapp
