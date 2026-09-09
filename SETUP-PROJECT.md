# SETUP-PROJECT — Mise en place de l'environnement de développement VVS

Ce guide met en place, pour toute l'équipe, la base de développement de **valimbavaka-core** (backend) et **valimbavaka-ui** (frontend) conforme à [`resources/Convention_Dev_VVS_V2.md`](resources/Convention_Dev_VVS_V2.md), avec **Liquibase comme source unique de vérité du schéma de base de données**.

À suivre une seule fois par développeur (setup initial), puis à chaque fois qu'une nouvelle feature démarre (squelette de dossiers).

---

## 0. Prérequis

| Outil | Version | Vérifier avec |
|---|---|---|
| JDK | 21 | `java -version` |
| Maven | 3.9+ (ou wrapper `./mvnw`) | `mvn -v` |
| Node.js | 20+ | `node -v` |
| npm | 11+ | `npm -v` |
| Docker Desktop | récent | `docker -v` |
| Git | récent | `git -v` |

IDE recommandé : IntelliJ IDEA / Rider (déjà configuré dans `.idea/`) + extension Angular Language Service côté frontend.

---

## 1. Base de données locale (Docker Compose + PostgreSQL)

Tout le monde travaille contre la **même image Postgres**, démarrée localement — jamais de partage d'une base distante en dev.

Créer `docker-compose.yml` à la racine du repo :

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: valimbavaka-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME:-valimbavaka}
      POSTGRES_USER: ${DB_USERNAME:-valimbavaka}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-valimbavaka}
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - valimbavaka-postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-valimbavaka} -d ${DB_NAME:-valimbavaka}"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  valimbavaka-postgres-data:
```

Créer `.env.example` à la racine (chacun copie en `.env`, jamais commité) :

```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=valimbavaka
DB_USERNAME=valimbavaka
DB_PASSWORD=valimbavaka
```

Ajouter au `.gitignore` racine : `.env`

Démarrer :
```bash
cp .env.example .env
docker compose up -d postgres
```

---

## 2. Liquibase — source unique de vérité du schéma

La dépendance `spring-boot-starter-liquibase` est déjà dans `valimbavaka-core/pom.xml`. Il reste à la câbler et à poser la convention de migration.

### 2.1 Configuration — `valimbavaka-core/src/main/resources/application.properties`

```properties
spring.application.name=valimbavaka-core

# --- Datasource (docker compose en local, override via variables d'env ailleurs) ---
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:valimbavaka}
spring.datasource.username=${DB_USERNAME:valimbavaka}
spring.datasource.password=${DB_PASSWORD:valimbavaka}

# --- JPA / Hibernate ---
# Liquibase possede le schema. Hibernate ne cree/modifie JAMAIS une table, il valide seulement
# que les entites correspondent a ce que Liquibase a applique.
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.format_sql=true

# --- Liquibase ---
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
```

`ddl-auto=validate` est **non négociable** : c'est ce qui garantit que toute évolution de schéma passe par Liquibase et est donc tracée, revue en PR, et rejouable à l'identique sur tous les postes / environnements.

### 2.2 Structure des changelogs

```
valimbavaka-core/src/main/resources/db/changelog/
├── db.changelog-master.xml
└── changes/
    └── README.md
```

**`db.changelog-master.xml`**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <!--
        Chaque changement de schema = un fichier dans changes/, jamais modifie une fois merge sur main.
        Nommage : <sequence 4 chiffres>__<feature>__<description-kebab-case>.sql
        Le tri alphabetique du dossier = ordre d'application (d'ou le zero-padding).
        Voir changes/README.md pour le detail et le gabarit.
    -->
    <includeAll path="db/changelog/changes"
                relativeToChangelogFile="true"
                endsWithFilter=".sql"
                errorIfMissingOrEmpty="false"/>

</databaseChangeLog>
```

`endsWithFilter=".sql"` permet de garder un `README.md` dans `changes/` sans que Liquibase n'essaie de le parser (avec Liquibase 5.x, l'attribut `filter`/`resourceFilter` attend le nom qualifié d'une classe `IncludeAllFilter`, pas une regex — `endsWithFilter` est le bon attribut pour un simple filtre par suffixe). `errorIfMissingOrEmpty="false"` évite une erreur au tout premier démarrage quand le dossier est encore vide.

**`changes/README.md`** (convention pour toute l'équipe)
```markdown
# Migrations Liquibase

Liquibase est la source de verite unique du schema (`spring.jpa.hibernate.ddl-auto=validate`).

## Regles

1. Un fichier = un changement logique coherent (une table, un ajout de colonne, un index...).
2. **Jamais modifie une fois merge sur `main`** — Liquibase calcule un checksum par changeset ;
   modifier un fichier deja applique casse la synchro pour toute l'equipe. Besoin de corriger ?
   Nouveau fichier, nouveau changeset.
3. Nommage : `<sequence>__<feature>__<description-kebab-case>.sql`
   - `sequence` : 4 chiffres zero-paddes, strictement croissants (`0001`, `0002`, ...).
   - `feature` : le package feature concerne (`auth`, `stock`, `order`...) ou `core` pour le transverse.
   - `description` : kebab-case, explicite.
   - Exemple : `0001__auth__create-users-table.sql`
4. Chaque fichier est un changelog SQL formate Liquibase avec un `changeSet` unique et un
   `rollback` explicite.

## Gabarit

    --liquibase formatted sql

    --changeset <auteur>:0001__auth__create-users-table
    --comment: Table users pour la feature auth
    CREATE TABLE users (
        id              UUID PRIMARY KEY,
        email           VARCHAR(255) NOT NULL UNIQUE,
        password_hash   VARCHAR(255) NOT NULL,
        status          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
        created_at      TIMESTAMP    NOT NULL DEFAULT now(),
        updated_at      TIMESTAMP    NOT NULL DEFAULT now()
    );
    --rollback DROP TABLE users;

## Avant d'ouvrir une PR

- Le changeset id est unique dans tout le projet.
- Un `--rollback` exploitable est fourni.
- La migration a ete testee en local (`docker compose up -d postgres` puis lancer l'appli).
```

### 2.3 Workflow quotidien

- Nouvelle table/colonne → nouveau fichier dans `changes/`, jamais d'édition d'un fichier déjà mergé.
- `mvn spring-boot:run` applique automatiquement les changelogs en attente au démarrage (comportement par défaut du starter).
- Besoin d'appliquer sans démarrer l'appli : `mvn liquibase:update`.
- Rollback du dernier changeset en local : `mvn liquibase:rollback -Dliquibase.rollbackCount=1`.

---

## 3. Squelette backend — Feature-Based Layering (FBL)

Package racine existant : `org.vvs.valimbavaka`. Créer le transverse (`core/`, `security/`, `shared/`) **avant** la première feature métier — c'est le socle que toutes les features utiliseront.

```
org.vvs.valimbavaka
├── core
│   ├── config      → WebConfig, JpaAuditingConfig
│   ├── exception   → BaseException, DomainException, ResourceNotFoundException,
│   │                 UnauthorizedException, ForbiddenException, ConflictException,
│   │                 GlobalExceptionHandler
│   └── util
├── security
│   ├── annotation  → HasPermission
│   ├── config      → SecurityConfig
│   ├── entrypoint  → RestAuthenticationEntryPoint
│   ├── manager     → PermissionManager
│   └── middleware  → JwtFilter
└── shared
    ├── domain       → BaseEntity
    ├── dto          → ApiResponse, PageResponse
    └── mapper
```

### 3.1 `shared/dto/ApiResponse.java`

```java
package org.vvs.valimbavaka.shared.dto;

import java.time.Instant;

public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }
}
```

### 3.2 `shared/dto/PageResponse.java`

```java
package org.vvs.valimbavaka.shared.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
```

### 3.3 `shared/domain/BaseEntity.java`

```java
package org.vvs.valimbavaka.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
```

### 3.4 `core/config/JpaAuditingConfig.java`

```java
package org.vvs.valimbavaka.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

### 3.5 `core/config/WebConfig.java`

```java
package org.vvs.valimbavaka.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Origines CORS explicites uniquement — jamais de wildcard "*" (convention section 9.3). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${vvs.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

### 3.6 `core/exception/*` — hiérarchie + handler global

```java
package org.vvs.valimbavaka.core.exception;

public abstract class BaseException extends RuntimeException {
    protected BaseException(String message) { super(message); }
    protected BaseException(String message, Throwable cause) { super(message, cause); }
}
```

```java
package org.vvs.valimbavaka.core.exception;

/** 400 Bad Request — erreur de validation métier. */
public class DomainException extends BaseException {
    public DomainException(String message) { super(message); }
    public DomainException(String message, Throwable cause) { super(message, cause); }
}
```

```java
package org.vvs.valimbavaka.core.exception;

/** 404 Not Found. */
public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String message) { super(message); }
}
```

```java
package org.vvs.valimbavaka.core.exception;

/** 401 Unauthorized — non authentifié. */
public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) { super(message); }
}
```

```java
package org.vvs.valimbavaka.core.exception;

/** 403 Forbidden — authentifié mais pas autorisé. */
public class ForbiddenException extends BaseException {
    public ForbiddenException(String message) { super(message); }
}
```

```java
package org.vvs.valimbavaka.core.exception;

/** 409 Conflict — doublon, contrainte violée. */
public class ConflictException extends BaseException {
    public ConflictException(String message) { super(message); }
}
```

```java
package org.vvs.valimbavaka.core.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.vvs.valimbavaka.shared.dto.ApiResponse;

import java.util.stream.Collectors;

/** Point unique de traduction des exceptions en réponses HTTP — les controllers ne font jamais de try/catch. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomain(DomainException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Erreur non gérée", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("Une erreur inattendue est survenue"));
    }
}
```

### 3.7 `security/*` — deny by default + JWT

```java
package org.vvs.valimbavaka.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Ex.: {@code @HasPermission("ORDER_WRITE")} sur une méthode de service. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAuthority('{value}')")
public @interface HasPermission {
    String value();
}
```

```java
package org.vvs.valimbavaka.security.manager;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class PermissionManager {

    public boolean hasPermission(String permission) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(permission));
    }
}
```

```java
package org.vvs.valimbavaka.security.entrypoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.vvs.valimbavaka.shared.dto.ApiResponse;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.fail("Authentification requise")));
    }
}
```

```java
package org.vvs.valimbavaka.security.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** TODO (feature auth) : extraire le JWT du header Authorization, le valider, peupler le SecurityContext. */
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        // TODO : lire "Authorization: Bearer <token>", valider, SecurityContextHolder.setAuthentication(...)
        filterChain.doFilter(request, response);
    }
}
```

```java
package org.vvs.valimbavaka.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.vvs.valimbavaka.security.entrypoint.RestAuthenticationEntryPoint;
import org.vvs.valimbavaka.security.middleware.JwtFilter;

/** Deny by default : toute route est protégée sauf déclaration explicite (convention section 9.2). */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ROUTES = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password"
    };

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final JwtFilter jwtFilter;

    public SecurityConfig(RestAuthenticationEntryPoint restAuthenticationEntryPoint, JwtFilter jwtFilter) {
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> { })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Nécessaire pour que le "{value}" de @HasPermission soit résolu (Spring Security 6.4+).
    @Bean
    static AnnotationTemplateExpressionDefaults templateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }
}
```

> Ce squelette sécurité est transverse et volontairement minimal (deny-by-default + filtre JWT en attente d'implémentation). La feature `auth` elle-même (User, AuthService, login, génération du JWT...) est une feature métier à créer séparément selon le patron de la section 4.2 de la convention — elle n'est pas du setup transverse.

---

## 4. Squelette frontend — Angular Feature-Based

```
valimbavaka-ui/src/app/
├── guards/           → auth-guard.ts
├── interceptors/      → auth-interceptor.ts
├── directives/        → has-permission-directive.ts
├── layouts/
│   └── main-layout/   → main-layout.ts / .html / .css
├── models/            → api-response.ts, page.ts
├── services/           (vide au départ — un *-service.ts par ressource, par feature)
├── shared/
│   └── components/     (vide au départ — composants réutilisables)
├── utils/
└── features/           (vide au départ — une feature = un sous-dossier)
```

### 4.1 `tsconfig.json` — activer le mode strict

Dans `compilerOptions`, ajouter :
```json
"strict": true,
```

### 4.2 `models/api-response.ts` / `models/page.ts`

```typescript
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  timestamp: string;
}
```

```typescript
export interface Page<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```

### 4.3 `interceptors/auth-interceptor.ts`

```typescript
import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // TODO (feature auth) : récupérer le token depuis le service d'auth
  const token: string | null = null;

  if (!token) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
```

### 4.4 `guards/auth-guard.ts`

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  // TODO (feature auth) : vérifier l'état d'authentification réel
  const isAuthenticated = false;

  return isAuthenticated ? true : router.createUrlTree(['/login']);
};
```

### 4.5 `directives/has-permission-directive.ts`

```typescript
import { Directive, TemplateRef, ViewContainerRef, inject, input, effect } from '@angular/core';

@Directive({ selector: '[appHasPermission]' })
export class HasPermissionDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainerRef = inject(ViewContainerRef);

  readonly appHasPermission = input.required<string>();

  constructor() {
    effect(() => {
      // TODO (feature auth) : vérifier la permission réelle de l'utilisateur courant
      const hasPermission = false;
      this.viewContainerRef.clear();
      if (hasPermission) {
        this.viewContainerRef.createEmbeddedView(this.templateRef);
      }
    });
  }
}
```

### 4.6 Câbler l'intercepteur — `app.config.ts`

```typescript
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
```

### 4.7 `layouts/main-layout/main-layout.ts`

```typescript
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayout {}
```

`main-layout.html` :
```html
<router-outlet />
```

---

## 5. Qualité automatisée (recommandé)

### 5.1 Backend — Checkstyle

Ajouter dans `valimbavaka-core/pom.xml` (fait respecter automatiquement : ligne 120 car., méthode 30 lignes, 4 paramètres max, pas d'import wildcard) :

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.6.0</version>
    <configuration>
        <configLocation>checkstyle.xml</configLocation>
        <consoleOutput>true</consoleOutput>
        <failOnViolation>true</failOnViolation>
    </configuration>
    <executions>
        <execution>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

`valimbavaka-core/checkstyle.xml` (extrait minimal — à enrichir au besoin) :
```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
        "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
    <module name="TreeWalker">
        <module name="LineLength"><property name="max" value="120"/></module>
        <module name="MethodLength"><property name="max" value="30"/></module>
        <module name="ParameterNumber"><property name="max" value="4"/></module>
        <module name="AvoidStarImport"/>
        <module name="UnusedImports"/>
        <module name="TypeName"/>
        <module name="MethodName"/>
        <module name="MemberName"/>
        <module name="ConstantName"/>
        <module name="PackageName"><property name="format" value="^[a-z]+(\.[a-z][a-z0-9]*)*$"/></module>
    </module>
</module>
```

Lancer : `mvn verify`

### 5.2 Frontend — ESLint

```bash
cd valimbavaka-ui
npx ng add @angular-eslint/schematics
```

Ajoute automatiquement la config recommandée + le target `lint` dans `angular.json`. Compléter ensuite `eslint.config.js` avec `"@typescript-eslint/no-explicit-any": "error"` pour interdire `any` sans dérogation explicite (convention section 1.4).

Lancer : `npm run lint`

---

## 6. Checklist finale

- [ ] `docker compose up -d postgres` démarre sans erreur
- [ ] `.env` créé localement (jamais commité)
- [ ] `application.properties` pointe sur Liquibase (`spring.liquibase.change-log=...`) et `ddl-auto=validate`
- [ ] `db/changelog/db.changelog-master.xml` + `changes/README.md` en place
- [ ] `mvn spring-boot:run` démarre sans erreur (Liquibase s'exécute au boot, log `liquibase.changelog : Reading from ...`)
- [ ] Packages `core/`, `security/`, `shared/` créés côté backend
- [ ] Dossiers `guards/`, `interceptors/`, `directives/`, `layouts/`, `models/`, `services/`, `shared/`, `utils/`, `features/` créés côté frontend
- [ ] `tsconfig.json` : `"strict": true`
- [ ] `npm start` (Angular) et `mvn spring-boot:run` (backend) tournent en parallèle sans erreur CORS
- [ ] (recommandé) Checkstyle backend + ESLint frontend branchés

Pour toute règle non couverte ici (nommage détaillé, SOLID, patterns, tests, grille de revue de code...), se référer à [`resources/Convention_Dev_VVS_V2.md`](resources/Convention_Dev_VVS_V2.md).
