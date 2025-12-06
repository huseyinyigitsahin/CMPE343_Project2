# CMPE343 Project 2 – Contact Management System (CLI)

A console-based contact management application developed for the CMPE343 course.  
The project provides a role-based menu system (Tester / Manager), secure user authentication, and rich search features on a `contacts` table stored in a MySQL database.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
  - [Authentication & Security](#authentication--security)
  - [Tester Menu](#tester-menu)
  - [Advanced Search](#advanced-search)
  - [Sorting](#sorting)
  - [Manager Features (if applicable)](#manager-features-if-applicable)
- [Data Model](#data-model)
  - [users Table](#users-table)
  - [contacts Table](#contacts-table)
- [Technology Stack](#technology-stack)
- [Setup & Installation](#setup--installation)
  - [1. Database Setup](#1-database-setup)
  - [2. Configure Database Connection](#2-configure-database-connection)
  - [3. Build & Run](#3-build--run)
- [Usage Guide](#usage-guide)
  - [Login](#login)
  - [Tester Menu Flow](#tester-menu-flow)
  - [Search Menu](#search-menu)
- [Code Conventions & Design Notes](#code-conventions--design-notes)
- [Limitations & Possible Improvements](#limitations--possible-improvements)

---

## Overview

This project is a **command-line Contact Management System** that allows authenticated users to:

- Log in with a username and password
- Manage their own password securely
- Browse and search a list of contacts stored in a MySQL database
- Perform both **simple** and **advanced (multi-field)** searches
- Sort contacts by multiple fields

Users are separated by **roles** (e.g. Tester, Manager).  
The `TesterMenu` class represents the main menu for tester users, and manager-specific functionality can be built on top of it (via inheritance) in classes like `ManagerMenu`.

---

## Features

### Authentication & Security

- Login is performed against a `users` table in MySQL.
- Passwords are stored as **SHA-256 hashes** (see `hashPassword` method).
- At login time, the password strength is evaluated and a **password strength banner** is shown:
  - `VERY_WEAK`, `WEAK`, `MEDIUM`, `STRONG`
- When changing their password, users:
  - Must enter the **current password** correctly
  - Get a **random strong password suggestion**
  - See the **calculated strength** of the new password
  - Must confirm the new password
- All password changes are persisted to the `users` table.

### Tester Menu

The main tester menu (`TesterMenu#showMenu`) offers:

1. **Change password**
2. **List all contacts**
3. **Search contacts**
   - Simple search (one field)
   - Advanced search (multiple fields, AND logic)
4. **Sort contacts**
5. **Logout**

All menu interactions are done via the console, with ANSI colors for better readability.

### Advanced Search

Advanced search is one of the main strengths of the project. There are two modes:

1. **Quick Filters**
   - Upcoming birthdays this month
   - Contacts added in the last 10 days
   - Contacts with missing important information (email/phone/linkedin)
2. **Custom Advanced Search (Multi-field, AND)**
   - User can combine up to **6 conditions**
   - Valid fields:
     - First Name
     - Last Name
     - Primary Phone
     - Email
     - Nickname
     - Birth Date
   - Each condition has:
     - A field
     - An operator (`STARTS WITH`, `CONTAINS`, `EQUALS`, or date-based modes)
     - A validated value
   - Birth date supports:
     - Exact date (`YYYY-MM-DD`)
     - By month (number or name, e.g. `11` or `november`)
     - By year (`1999`)
   - All conditions are combined with **logical AND**.
   - Every condition is strongly validated before being added:
     - **Names**: letters only (Turkish letters supported)
     - **Nickname**: letters, digits, `_` and `.`, no spaces
     - **Phone**: digits only; exact comparison requires exactly 10 digits
     - **Email**: format check, forbidden characters, supported domains for equality
   - User can:
     - **Backspace** the last condition (`back` command)
     - Cancel the whole advanced search at any time (`quit` command)

If fewer than **two** conditions are selected, the user is warned that advanced search requires at least two fields. If only one condition is provided, the application recommends using **Simple Search** instead.

### Sorting

Users can sort contacts via **Sort Contacts** menu:

- Sort fields:
  - First Name
  - Last Name
  - Primary Phone
  - Email
  - Birth Date
- Order:
  - Ascending
  - Descending
- For text fields, sorting is done using `LOWER(TRIM(column))` for stable ordering.

Sorted results are printed with the same contact table formatting.

### Manager Features (if applicable)

Although the `TesterMenu` is the core focus, the project is designed to be extended by a `ManagerMenu` that:

- Inherits from `TesterMenu`
- Can add additional **user management** functionality (e.g. creating/updating/deleting users, undo support)

The inheritance design allows **code reuse** of all common features (searching, listing, sorting contacts) while giving managers extra capabilities in their own menu.

---

## Data Model

The project uses at least two main tables: `users` and `contacts`.

> **Note:** Column names below are aligned with what is used in the code. You may have additional columns in your real database.

### `users` Table

Used for authentication and password management.

Minimum columns (based on usage in code):

- `username` (PK, `VARCHAR`)  
- `password_hash` (`VARCHAR`)  
- `name` (`VARCHAR`) – first name  
- `surname` (`VARCHAR`) – last name  

You may also have extra fields such as `role`, `created_at`, etc., depending on your design.

### `contacts` Table

Used by list/search/sort features.

Columns used by the code:

- `contact_id` (PK, `INT`)
- `first_name` (`VARCHAR`)
- `middle_name` (`VARCHAR`, nullable)
- `last_name` (`VARCHAR`)
- `nickname` (`VARCHAR`)
- `phone_primary` (`VARCHAR`)
- `phone_secondary` (`VARCHAR`)
- `email` (`VARCHAR`)
- `linkedin_url` (`VARCHAR`)
- `birth_date` (`DATE` or `DATETIME`)
- `created_at` (`DATETIME`)
- `updated_at` (`DATETIME`)

---

## Technology Stack

- **Language**: Java (JDK 8+ recommended, works fine with newer JDKs as well)
- **Database**: MySQL (e.g. via XAMPP / MySQL Community Server)
- **JDBC Driver**: MySQL Connector/J
- **IDE**: NetBeans (project originally developed with NetBeans)

---

## Setup & Installation

### 1. Database Setup

1. Install **MySQL** (e.g. via XAMPP).
2. Create a database, for example:

   ```sql
   CREATE DATABASE cmpe343_project2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
