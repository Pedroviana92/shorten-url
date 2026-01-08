# PostgreSQL Integration Implementation Plan

## Phase 1: PostgreSQL Installation & Setup on WSL

### Step 1.1: Install PostgreSQL on WSL

```bash
# Update package list
sudo apt update

# Install PostgreSQL
sudo apt install postgresql postgresql-contrib

# Start PostgreSQL service
sudo service postgresql start

# Check PostgreSQL status
sudo service postgresql status
```

### Step 1.2: Configure PostgreSQL

```bash
# Switch to postgres user
sudo -u postgres psql

# Inside PostgreSQL prompt, create database and user
CREATE DATABASE url_shortener_db;
CREATE USER url_app_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE url_shortener_db TO url_app_user;

# Grant schema privileges (PostgreSQL 15+)
\c url_shortener_db
GRANT ALL ON SCHEMA public TO url_app_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO url_app_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO url_app_user;

# Exit PostgreSQL
\q
```

### Step 1.3: Verify Connection

```bash
# Test connection with the new user
psql -U url_app_user -d url_shortener_db -h localhost
# Enter password when prompted

# Exit
\q
```

---

## Phase 2: Project Configuration

### Step 2.1: Add Dependencies to `pom.xml`

Add these dependencies (Spring Data JPA is needed):

```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- PostgreSQL Driver (already in your pom.xml) -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Validation API -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### Step 2.2: Configure `application.properties`

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/url_shortener_db
spring.datasource.username=url_app_user
spring.datasource.password=your_secure_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Logging
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

**Important `ddl-auto` options:**
- `update` - Updates schema automatically (good for development)
- `create` - Drops and recreates schema on startup (data loss!)
- `create-drop` - Creates schema on startup, drops on shutdown
- `validate` - Only validates schema (production)
- `none` - No schema management

---

## Phase 3: Clean Architecture Implementation

### **Project Structure:**

```
src/main/java/com/shorten/url/shortenUrl/
├── controller/           # API Layer (Presentation)
│   ├── MainPageController.java
│   └── ViewController.java
├── service/             # Business Logic Layer
│   ├── UrlShortenerService.java
│   └── impl/
│       └── UrlShortenerServiceImpl.java
├── repository/          # Data Access Layer
│   └── ShortenedUrlRepository.java
├── domain/              # Domain Layer (Entities)
│   └── ShortenedUrl.java
├── dto/                 # Data Transfer Objects
│   ├── UrlShortenRequest.java
│   └── UrlShortenResponse.java
└── exception/           # Custom Exceptions
    └── UrlNotFoundException.java
```

### **Architecture Layers:**

1. **Controller** → Handles HTTP requests
2. **Service** → Business logic, validation, orchestration
3. **Repository** → Database operations (Spring Data JPA)
4. **Domain** → Entity models (database tables)
5. **DTO** → Data transfer between layers

---

## Phase 4: Implementation Steps

### Step 4.1: Create Domain Entity

**File:** `src/main/java/com/shorten/url/shortenUrl/domain/ShortenedUrl.java`

```java
package com.shorten.url.shortenUrl.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shortened_urls", indexes = {
    @Index(name = "idx_hash_code", columnList = "hashCode")
})
public class ShortenedUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String hashCode;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public ShortenedUrl() {}

    public ShortenedUrl(String hashCode, String originalUrl) {
        this.hashCode = hashCode;
        this.originalUrl = originalUrl;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHashCode() {
        return hashCode;
    }

    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
```

### Step 4.2: Create Repository Interface

**File:** `src/main/java/com/shorten/url/shortenUrl/repository/ShortenedUrlRepository.java`

```java
package com.shorten.url.shortenUrl.repository;

import com.shorten.url.shortenUrl.domain.ShortenedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, Long> {

    // Find by hashCode
    Optional<ShortenedUrl> findByHashCode(String hashCode);

    // Check if hashCode exists
    boolean existsByHashCode(String hashCode);

    // Find by original URL
    Optional<ShortenedUrl> findByOriginalUrl(String originalUrl);
}
```

### Step 4.3: Create Service Interface

**File:** `src/main/java/com/shorten/url/shortenUrl/service/UrlShortenerService.java`

```java
package com.shorten.url.shortenUrl.service;

import com.shorten.url.shortenUrl.domain.ShortenedUrl;

public interface UrlShortenerService {

    /**
     * Shortens a URL and stores it in the database
     * @param originalUrl The original URL to shorten
     * @return The shortened URL entity
     */
    ShortenedUrl shortenUrl(String originalUrl);

    /**
     * Retrieves the original URL from a hashCode
     * @param hashCode The shortened URL hash code
     * @return The original URL
     * @throws com.shorten.url.shortenUrl.exception.UrlNotFoundException if not found
     */
    String getOriginalUrl(String hashCode);

    /**
     * Validates if a URL format is correct
     * @param url The URL to validate
     * @return true if valid, false otherwise
     */
    boolean isValidUrl(String url);
}
```

### Step 4.4: Create Service Implementation

**File:** `src/main/java/com/shorten/url/shortenUrl/service/impl/UrlShortenerServiceImpl.java`

```java
package com.shorten.url.shortenUrl.service.impl;

import com.shorten.url.shortenUrl.domain.ShortenedUrl;
import com.shorten.url.shortenUrl.repository.ShortenedUrlRepository;
import com.shorten.url.shortenUrl.service.UrlShortenerService;
import org.apache.commons.validator.routines.UrlValidator;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private final ShortenedUrlRepository repository;
    private final Hashids hashids;
    private final UrlValidator urlValidator;
    private final AtomicLong idGenerator;

    @Autowired
    public UrlShortenerServiceImpl(ShortenedUrlRepository repository) {
        this.repository = repository;
        this.hashids = new Hashids("my-secret-salt-2026", 6);
        this.urlValidator = new UrlValidator(new String[]{"http", "https"});
        this.idGenerator = new AtomicLong(1000);
    }

    @Override
    public ShortenedUrl shortenUrl(String originalUrl) {
        // Validate URL
        if (!isValidUrl(originalUrl)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid URL format. Please enter a valid URL starting with http:// or https://"
            );
        }

        // Check if URL already exists (optional - reuse existing short URL)
        /*
        Optional<ShortenedUrl> existing = repository.findByOriginalUrl(originalUrl);
        if (existing.isPresent()) {
            return existing.get();
        }
        */

        // Generate unique hashCode
        String hashCode;
        do {
            long uniqueId = idGenerator.incrementAndGet();
            hashCode = hashids.encode(uniqueId);
        } while (repository.existsByHashCode(hashCode));

        // Create and save entity
        ShortenedUrl shortenedUrl = new ShortenedUrl(hashCode, originalUrl);
        return repository.save(shortenedUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public String getOriginalUrl(String hashCode) {
        return repository.findByHashCode(hashCode)
            .map(ShortenedUrl::getOriginalUrl)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Shortened URL not found"
            ));
    }

    @Override
    public boolean isValidUrl(String url) {
        return url != null && !url.trim().isEmpty() && urlValidator.isValid(url);
    }
}
```

### Step 4.5: Update Controller to Use Service

**Update:** `MainPageController.java`

```java
package com.shorten.url.shortenUrl.controller;

import com.shorten.url.shortenUrl.domain.ShortenedUrl;
import com.shorten.url.shortenUrl.responseHandler.ResponseJson;
import com.shorten.url.shortenUrl.responseHandler.UrlRequest;
import com.shorten.url.shortenUrl.service.UrlShortenerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

@RestController
@RequestMapping("/")
public class MainPageController {

    private final UrlShortenerService urlShortenerService;

    @Autowired
    public MainPageController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @PostMapping("/shorten")
    public ResponseJson shortenUrl(@RequestBody UrlRequest urlRequest) {
        try {
            String url = urlRequest.getUrl();

            // Validate URL (service handles this)
            ShortenedUrl shortenedUrl = urlShortenerService.shortenUrl(url);

            String shortUrl = "http://localhost:8080/" + shortenedUrl.getHashCode();

            return new ResponseJson(shortUrl, url, "Url shortened successfully");

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An error occurred while shortening the URL. Please try again."
            );
        }
    }

    @GetMapping("/{hashCode}")
    public ResponseEntity<Void> redirectToOriginalUrl(@PathVariable String hashCode) {
        try {
            String originalUrl = urlShortenerService.getOriginalUrl(hashCode);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(originalUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);

        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(URI.create("/errors/not-found.html"));
                return new ResponseEntity<>(headers, HttpStatus.FOUND);
            }
            throw e;

        } catch (IllegalArgumentException e) {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/errors/invalid-url.html"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);

        } catch (Exception e) {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/errors/server-error.html"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }
}
```

---

## Phase 5: Testing the Database

### Step 5.1: Simple Database Test Queries

```bash
# Connect to database
psql -U url_app_user -d url_shortener_db -h localhost

# View table schema
\d shortened_urls

# View all records
SELECT * FROM shortened_urls;

# Find by hashCode
SELECT * FROM shortened_urls WHERE hash_code = 'abc123';

# Exit
\q
```

### Step 5.2: Test the Application

1. **Start PostgreSQL** (if not running):
   ```bash
   sudo service postgresql start
   ```

2. **Run Maven install**:
   ```bash
   mvn clean install
   ```

3. **Start Spring Boot app**:
   ```bash
   mvn spring-boot:run
   ```

4. **Test endpoints**:
   - Visit `http://localhost:8080/home`
   - Create a shortened URL
   - Check database for the new record

---

## Phase 6: Benefits of This Architecture

### **Clean Architecture Principles Applied:**

1. **Separation of Concerns**
   - Controller: HTTP handling only
   - Service: Business logic
   - Repository: Data access
   - Domain: Business entities

2. **Dependency Inversion**
   - Interfaces define contracts
   - Implementation details hidden
   - Easy to mock for testing

3. **Single Responsibility**
   - Each class has one reason to change
   - Clear boundaries between layers

4. **Testability**
   - Services can be unit tested independently
   - Repositories can be mocked
   - Controllers can be integration tested

---

## Summary Checklist

- [ ] Install PostgreSQL on WSL
- [ ] Create database and user
- [ ] Add Spring Data JPA dependency
- [ ] Configure `application.properties`
- [ ] Create Entity class (`ShortenedUrl`)
- [ ] Create Repository interface
- [ ] Create Service interface and implementation
- [ ] Update Controller to use Service
- [ ] Test database connection
- [ ] Run application and verify

---

## Additional Notes

### WSL-Specific Considerations

1. **PostgreSQL doesn't start automatically in WSL**
   - Always run `sudo service postgresql start` before using the app
   - Or add to `~/.bashrc`: `alias pgstart='sudo service postgresql start'`

2. **PostgreSQL service commands**:
   ```bash
   sudo service postgresql start    # Start
   sudo service postgresql stop     # Stop
   sudo service postgresql restart  # Restart
   sudo service postgresql status   # Check status
   ```

3. **Common PostgreSQL issues in WSL**:
   - If connection refused: Check if PostgreSQL is running
   - If authentication fails: Verify user/password in `application.properties`
   - If database doesn't exist: Re-run database creation commands

### Database Migration Considerations

For production, consider using database migration tools:
- **Flyway** - Version control for database
- **Liquibase** - Database schema change management

These tools help manage schema changes across environments safely.

### Performance Optimization Tips

1. **Indexes**: Already added index on `hashCode` column for fast lookups
2. **Connection Pooling**: Spring Boot uses HikariCP by default (optimized)
3. **Caching**: Consider adding Spring Cache for frequently accessed URLs
4. **Read Replicas**: For high-traffic scenarios, use read replicas

### Security Best Practices

1. **Never commit database credentials to Git**
   - Use environment variables
   - Use Spring profiles for different environments

2. **Use strong passwords** for database users

3. **Limit database user privileges** to only what's needed

4. **Enable SSL** for database connections in production

---

## Troubleshooting

### Issue: "role does not exist"
```bash
# Create the user again
sudo -u postgres createuser url_app_user
```

### Issue: "database does not exist"
```bash
# Create the database again
sudo -u postgres createdb url_shortener_db
```

### Issue: "Peer authentication failed"
Edit `/etc/postgresql/[version]/main/pg_hba.conf`:
```
# Change from 'peer' to 'md5' or 'trust' for local connections
local   all   all   md5
```
Then restart PostgreSQL: `sudo service postgresql restart`

### Issue: Application can't connect to database
1. Verify PostgreSQL is running: `sudo service postgresql status`
2. Test connection manually: `psql -U url_app_user -d url_shortener_db -h localhost`
3. Check `application.properties` credentials match database setup
