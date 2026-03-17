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
| **Membership Tiers** | `BASIC` (3 books/7 days) · `STANDARD` (5 books/14 days) · `PREMIUM` (10 books/30 days) |
| **Borrow Records** | Borrow → Return → Archive lifecycle with overdue detection |
| **Late Fees** | Auto-calculated at ₹1/day, capped at ₹100 |
| **Pagination & Sorting** | Configurable on all list endpoints (`pageNo`, `pageSize`, `sortBy`, `sortDir`) |
| **Advanced Filtering** | Multi-field search on books, authors, members, borrow records |
| **Global Error Handling** | Centralized exception handling with structured JSON error responses |
| **Input Validation** | Group-based validation (Create vs Update) with descriptive messages |
| **API Documentation** | Interactive Swagger/OpenAPI documentation |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Framework** | Spring Boot 4.0.1 |
| **Language** | Java 25 |
| **Security** | Spring Security 6 + JWT (jjwt 0.13.0) |
| **ORM** | Spring Data JPA / Hibernate |
| **Database** | MySQL |
| **API Docs** | Swagger/OpenAPI 3.0 |
| **Validation** | Jakarta Bean Validation |
| **Utilities** | Lombok |
| **Build Tool** | Maven |

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
└── service/                 # Business logic layer
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
| `GET` | `/archived` | Get all archived records | ADMIN, LIBRARIAN |
| `GET` | `/non-archived` | Get all active records | ADMIN, LIBRARIAN |
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

## Getting Started

### Prerequisites
- **Java 25** (JDK)
- **Maven 3.9+**
- **MySQL 8.0+**

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/saikrishnask15/LibraryManagementSystem.git
   cd LibraryManagementSystem
   ```

2. **Create the database**
   ```sql
   CREATE DATABASE libraryManagementSystem;
   ```

3. **Configure database** — Update `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/libraryManagementSystem
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

4. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

5. **Access the API** at
   ```
   API Base URL: http://localhost:8080
   Swagger UI: http://localhost:8080/swagger-ui.html 
   ```
---

##  API Documentation

### Interactive API Docs

Full API documentation is available via Swagger UI:

**URL:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Features:
-  Interactive endpoint testing
-  Request/response schemas
-  Authentication flow
-  Error response examples

### Authentication

All endpoints (except `/api/auth/*`) require JWT authentication.

**Add token to requests:**
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token expiration:** 24 hours

---

### Quick Test
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123","email":"admin@example.com"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'
```

---

## Authentication Flow

```
1. POST /api/auth/register  →  Creates user account (default role: MEMBER)
2. POST /api/auth/login     →  Returns JWT token (valid for 24 hours)
3. Use token in header       →  Authorization: Bearer <token>
```

---

## Membership Tiers

| Tier | Max Books | Borrow Period | Monthly Fee |
|------|-----------|---------------|-------------|
| **BASIC** | 3 | 7 days | Free |
| **STANDARD** | 5 | 14 days | $10.00 |
| **PREMIUM** | 10 | 30 days | $20.00 |

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
