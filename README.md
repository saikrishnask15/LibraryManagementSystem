# Library Management System — REST API

A production-grade **Library Management System** built with **Spring Boot 4.0.1** and **Java 25**, featuring JWT-based authentication, role-based access control, tiered memberships, borrow tracking with automated late-fee calculation, and rich filtering/pagination across all resources.

---

## Key Features
| Area | Highlights |
|------|-----------|
| **Authentication** | JWT token-based auth with secure registration & login |
| **Authorization** | Role-Based Access Control — `ADMIN`, `LIBRARIAN`, `MEMBER` |
| **Books** | Full CRUD, ISBN-unique, multi-category tagging, copy tracking |
| **Authors** | CRUD with linked book listings |
| **Categories** | CRUD with many-to-many book relationships |
| **Members** | Profile management linked to user accounts |
| **Membership Tiers** | `BASIC` (3 books/14 days) · `STANDARD` (5 books/21 days) · `PREMIUM` (10 books/30 days) |
| **Borrow Records** | Borrow → Return → Archive lifecycle with overdue detection |
| **Late Fees** | Auto-calculated at ₹1/day, capped at ₹100 |
| **Email Notifications** | Welcome emails, borrow confirmations, return receipts, overdue reminders |
| **Pagination & Sorting** | Configurable on all list endpoints |
| **Advanced Filtering** | Multi-field search using JPA Specifications |
| **Global Error Handling** | Centralized exception handling with structured JSON responses |
| **Input Validation** | Group-based validation (Create vs Update) with descriptive messages |
| **Scheduled Tasks** | Automated overdue detection and late fee calculation |
| **API Documentation** | Interactive Swagger/OpenAPI documentation |
| **Comprehensive Logging** | Production-ready logging with SLF4J/Logback |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Framework** | Spring Boot 4.0.1 |
| **Language** | Java 25 |
| **Security** | Spring Security 6 + JWT (jjwt 0.13.0) |
| **ORM** | Spring Data JPA / Hibernate |
| **Database** | MySQL 8.0 |
| **Email** | Spring Boot Mail + Gmail SMTP |
| **API Docs** | Swagger/OpenAPI 3.0 (springdoc-openapi 2.3.0) |
| **Validation** | Jakarta Bean Validation |
| **Logging** | SLF4J + Logback |
| **Utilities** | Lombok |
| **Build Tool** | Maven 3.9+ |

---

## Architecture

```
src/main/java/com/example/LibraryManagementSystem/
│
├── config/                  # Security config, JWT filter & utilities
│   ├── SecurityConfig.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtUtil.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── JwtAccessDeniedHandler.java
│   ├── CustomUserDetailsService.java
│   └── openAPI/OpenApiConfig.java  
│
├── controller/              # REST API controllers
│   ├── AuthenticationController.java
│   ├── AuthorController.java
│   ├── BookController.java
│   ├── BorrowRecordController.java
│   ├── CategoryController.java
│   ├── MemberController.java
│   └── UsersController.java
│
├── model/                   # JPA entities
│   ├── Users.java           # Implements UserDetails
│   ├── Book.java
│   ├── Author.java
│   ├── Category.java
│   ├── Member.java
│   ├── BorrowRecord.java
│   └── MembershipType.java  # Enum with tier config
│
├── dto/                     # Request/Response DTOs & Mappers
│   ├── auth/
│   ├── BookDTO/
│   ├── authorDTO/
│   ├── borrowRecordDTO/
│   ├── categoryDTO/
│   ├── memberDTO/
│   ├── common/PageResponse.java
│   ├── mapper/
│   └── validation/ValidateGroups.java
│
├── exception/               # Global exception handling
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── ResourceAlreadyExistsException.java
│   ├── BookNotAvailableException.java
│   ├── BorrowLimitExceededException.java
│   ├── DuplicateBorrowException.java
│   └── ...
│
├── repository/              # Spring Data JPA repositories
│
├── service/                 # Business logic layer
│
├── specification/           # JPA Specifications for filtering
│   └── BookSpecification.java
│
└── scheduler/               # Scheduled tasks
    └── BorrowRecordScheduler.java
```

---

## Database Schema

```mermaid
erDiagram
    USERS ||--o| MEMBER : "has profile"
    AUTHOR ||--o{ BOOK : "writes"
    BOOK }o--o{ CATEGORY : "belongs to"
    MEMBER ||--o{ BORROW_RECORD : "borrows"
    BOOK ||--o{ BORROW_RECORD : "is borrowed"

    USERS {
        int id PK
        string username UK
        string email UK
        string password
        enum role
        boolean enabled
        datetime created_at
        datetime updated_at
    }

    MEMBER {
        int id PK
        string name
        string email UK
        string phone
        date membership_date
        enum membership_type
        int user_id FK
    }

    AUTHOR {
        int id PK
        string name
        string email
        string bio
    }

    BOOK {
        int id PK
        string title
        string isbn UK
        int published_year
        boolean available
        int total_copies
        int available_copies
        int author_id FK
    }

    CATEGORY {
        int id PK
        string name
        string description
    }

    BORROW_RECORD {
        long id PK
        int member_id FK
        int book_id FK
        date borrow_date
        date due_date
        decimal late_fee
        datetime return_date
        enum status
        boolean is_archived
        datetime archived_at
        string archived_by
        string archive_reason
    }
```

---

##  Getting Started

### Prerequisites

- **Java 25** (JDK)
- **Maven 3.9+**
- **MySQL 8.0+**
- **Gmail Account** (for email notifications)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/saikrishnask15/LibraryManagementSystem.git
   cd LibraryManagementSystem
   ```

2. **Create MySQL database**
   ```sql
   CREATE DATABASE library_db;
   ```

3. **Configure application**

   Update `src/main/resources/application.properties`:
   ```properties
   # Database Configuration
   spring.datasource.url=jdbc:mysql://localhost:3306/library_db
   spring.datasource.username=root
   spring.datasource.password=your_password
   
   # JPA Configuration
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
   
   # JWT Configuration
   jwt.secret=your-secret-key-here
   jwt.expiration=86400000
   
   # Email Configuration (Gmail)
   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.username=your-email@gmail.com
   spring.mail.password=your-gmail-app-password
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true
   
   app.email.from=Library System <your-email@gmail.com>
   app.email.enabled=true
   
   # Swagger
   springdoc.swagger-ui.path=/swagger-ui.html
   springdoc.api-docs.path=/api-docs
   ```

4. **Build and run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Access the application**
   - **API Base URL:** `http://localhost:8080`
   - **Swagger UI:** `http://localhost:8080/swagger-ui.html`
   - **API Docs JSON:** `http://localhost:8080/api-docs`

---

##  Email Notifications

The system automatically sends emails for:

### 1. Welcome Email
- **Trigger:** User registration
- **Content:** Account details, membership info, borrowing limits
- **Example:**
  ```
  Subject: Welcome to Library Management System! 
  
  Hello John Doe,
  
  Your account has been successfully created:
  • Username: john
  • Membership: BASIC
  • Can borrow: 3 books for 14 days
  
  Happy Reading!
  ```

### 2. Borrow Confirmation
- **Trigger:** Book borrowed successfully
- **Content:** Book details, due date, late fee policy
- **Example:**
  ```
  Subject: Book Borrowed Successfully! 
  
  You have successfully borrowed:
  • Title: Clean Code
  • Author: Robert C. Martin
  • Due Date: 31 Mar 2026
  
  Please return by the due date to avoid late fees (₹1/day, max ₹100)
  ```

### 3. Return Confirmation
- **Trigger:** Book returned
- **Content:** Return details, late fee (if any)
- **Example:**
  ```
  Subject: Book Returned Successfully! 
  
  Thank you for returning:
  • Title: Clean Code
  • Returned On Time! 
  
  Feel free to borrow more books anytime.
  ```

### 4. Overdue Reminders
- **Trigger:** Daily at 3:00 AM for overdue books
- **Content:** Overdue details, current late fee
- **Example:**
  ```
  Subject: Overdue Book Reminder
  
  Your book is overdue:
  • Title: Clean Code
  • Due Date: 10 Mar 2026 (7 days ago)
  • Late Fee: ₹7.00
  
  Please return soon to avoid additional charges!
  ```

**Note:** To enable email notifications, configure Gmail App Password in `application.properties`


---

## API Documentation

### Interactive Swagger UI

**URL:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

**Features:**
-  Test all 50+ endpoints interactively
-  View request/response schemas
-  Try authentication flow
-  See error response examples

### How to Use Swagger UI

1. **Open Swagger UI**
2. **Authenticate:**
   - Click "Authorize" button (green lock icon)
   - Login using `POST /api/auth/login`:
     ```json
     {
       "username": "admin",
       "password": "admin@123"
     }
     ```
   - Copy the JWT token from response
   - Paste in "Authorize" dialog: `Bearer YOUR_TOKEN`
   - Click "Authorize"
3. **Test endpoints** - All protected endpoints now work!

### Authentication

All endpoints (except `/api/auth/*`) require JWT authentication.

**Add token to requests:**
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token expiration:** 24 hours
 

---

## API Endpoints

### Authentication — `/api/auth`
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| `POST` | `/register` | Register a new user | Public |
| `POST` | `/login` | Login & get JWT token | Public |

### Books — `/api/books`
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| `GET` | `/` | List books (filter by title, ISBN, author, year, category, availability, copies) | Authenticated |
| `GET` | `/{bookId}` | Get book by ID | Authenticated |
| `POST` | `/` | Add a new book | ADMIN, LIBRARIAN |
| `PATCH` | `/{bookId}` | Update book details | ADMIN, LIBRARIAN |
| `DELETE` | `/{bookId}` | Delete a book | ADMIN |

**Filtering parameters:**
- `title`, `isBn`, `authorName`, `available`
- `minYear`, `maxYear`, `categoryIds`
- `minCopies`, `maxCopies`
- `pageNo`, `pageSize`, `sortBy`, `sortDir`

### Authors — `/api/authors`
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| `GET` | `/` | List authors (filter by name, email, id) | Authenticated |
| `GET` | `/{authorId}` | Get author by ID | Authenticated |
| `POST` | `/` | Add a new author | ADMIN, LIBRARIAN |
| `PATCH` | `/{authorId}` | Update author | ADMIN, LIBRARIAN |
| `DELETE` | `/{authorId}` | Delete author | ADMIN |

### Categories — `/api/categories`
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| `GET` | `/` | List categories (filter by id, name, bookIds) | Authenticated |
| `GET` | `/{categoryId}` | Get category by ID | Authenticated |
| `POST` | `/` | Add a new category | ADMIN, LIBRARIAN |
| `PATCH` | `/{categoryId}` | Update category | ADMIN, LIBRARIAN |
| `DELETE` | `/{categoryId}` | Delete category | ADMIN |

### Members — `/api/members`
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| `GET` | `/` | List members (filter by name, email, phone, tier) | ADMIN, LIBRARIAN |
| `GET` | `/{memberId}` | Get member by ID | ADMIN, LIBRARIAN |
| `GET` | `/me` | Get my profile | Authenticated |
| `POST` | `/` | Add a new member | ADMIN, LIBRARIAN |
| `PATCH` | `/{memberId}` | Update member | ADMIN, LIBRARIAN, MEMBER (own) |
| `PATCH` | `/{memberId}/upgrade` | Upgrade membership tier | ADMIN, LIBRARIAN, MEMBER (own) |
| `DELETE` | `/{memberId}` | Delete member | ADMIN |

### Borrow Records — `/api/borrowrecords`
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| `GET` | `/` | List all records (rich filtering) | ADMIN, LIBRARIAN |
| `GET` | `/{borrowRecordId}` | Get record by ID | ADMIN, LIBRARIAN |
| `GET` | `/my-records` | Get my borrow records | Authenticated |
| `POST` | `/` | Create borrow record | ADMIN, LIBRARIAN, MEMBER |
| `PATCH` | `/{borrowRecordId}` | Update record | ADMIN, LIBRARIAN |
| `PATCH` | `/{borrowRecordId}/return` | Process book return | Authenticated |
| `PATCH` | `/{borrowRecordId}/archive` | Archive a record | ADMIN, LIBRARIAN |
| `DELETE` | `/{borrowRecordId}` | Delete record | ADMIN |

### Users — `/api/users`
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| `GET` | `/` | List all users | ADMIN |
| `GET` | `/me` | Get current user | Authenticated |
| `DELETE` | `/{id}` | Delete a user | ADMIN |

---

## Membership Tiers

| Tier | Max Books | Borrow Period | Monthly Fee |
|------|-----------|---------------|-------------|
| **BASIC** | 3 | 7 days | Free |
| **STANDARD** | 5 | 14 days | $10.00 |
| **PREMIUM** | 10 | 30 days | $20.00 |

---

##  Logging

Comprehensive logging throughout the application:

### Log Levels
- **ERROR:** System failures, unexpected errors
- **WARN:** User errors, validation failures, security violations
- **INFO:** Business operations (create, update, delete)
- **DEBUG:** Detailed flow, search queries

### Log Examples
```
2026-03-17 22:15:30 -  Adding new book - Title: Clean Code
2026-03-17 22:15:31 -  Book added successfully - ID: 5
2026-03-17 22:20:00 -  Processing borrow - Member: 1, Book: 5
2026-03-17 22:20:01 -  Book borrowed - Due: 31 Mar 2026
2026-03-17 22:25:00 - ️ Book unavailable - Title: 'Clean Code'
2026-03-17 22:30:00 -  Authentication failed - Invalid credentials
```

### Log Location
- **Console:** Real-time logging
- **File:** `logs/library-management.log`

---

##  Security Features

-  **JWT Authentication** - Stateless token-based auth
-  **BCrypt Password Hashing** - Secure password storage 
-  **Role-Based Access Control** - Method-level security with @PreAuthorize
-  **Global Exception Handling** - Secure error responses
-  **Input Validation** - Bean validation with custom messages
-  **Security Logging** - Track failed logins, access violations

---

##  Project Highlights

### Production-Ready Features

1. **Comprehensive Email System**
   - Welcome emails on registration
   - Instant borrow/return confirmations
   - Automated overdue reminders
   - Professional email templates

2. **Interactive API Documentation**
   - Swagger UI for easy testing
   - Complete endpoint documentation
   - Request/response examples
   - One-click authentication

3. **Enterprise-Grade Logging**
   - Structured logging with SLF4J
   - Security audit trail
   - Business operation tracking
   - Error debugging support

4. **Advanced Architecture**
   - Layered architecture (Controller → Service → Repository)
   - DTO pattern for API contracts
   - Specification pattern for dynamic filtering
   - Scheduled tasks with @Scheduled

5. **Robust Error Handling**
   - Global exception handler
   - Structured error responses
   - User-friendly error messages
   - HTTP status code compliance

---


##  Testing

### Quick Test Flow

1. **Start the application**
   ```bash
   mvn spring-boot:run
   ```

2. **Open Swagger UI**
   ```
   http://localhost:8080/swagger-ui.html
   ```

3. **Register a user**
   ```json
   POST /api/auth/register
   {
     "username": "testuser",
     "password": "Test@123",
     "email": "test@example.com",
     "name": "Test User",
     "phone": "1234567890"
   }
   ```

4. **Check your email** for welcome message! 

5. **Login and test** other endpoints

---

##  Key Statistics

- **50+ REST Endpoints** across 7 controllers
- **6 Core Entities** with relationships
- **3 User Roles** (ADMIN, LIBRARIAN, MEMBER)
- **8 Custom Exceptions** for error handling
- **4 Email Templates** for notifications
- **2 Scheduled Tasks** for automation
- **100% JPA** for database operations

---

## 🛠️ Development

### Running in Development Mode

```bash
# With live reload
mvn spring-boot:run
 
# With debug logging
mvn spring-boot:run -Dlogging.level.com.example.LibraryManagementSystem=DEBUG
```

### Building for Production

```bash
mvn clean package
java -jar target/LibraryManagementSystem-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```
 
---

## Authentication Flow

```
1. POST /api/auth/register  →  Creates user account (default role: MEMBER)
2. POST /api/auth/login     →  Returns JWT token (valid for 24 hours)
3. Use token in header       →  Authorization: Bearer <token>
```

---


## Author

**Sai Krishna Goud**

- GitHub: [@saikrishnask15](https://github.com/saikrishnask15)
- LinkedIn: [sai-krishna-goud](https://www.linkedin.com/in/sai-krishna-goud-b5288a191/)
- Portfolio: [saikrishnaskportfolio.netlify.app](https://saikrishnaskportfolio.netlify.app/)
- Email: saikrishnagoud.dev@gmail.com
---

## License

This project is open-source and available under the [MIT License](LICENSE).
