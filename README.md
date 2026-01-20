# Temperature Proxy API

REST API proxy for fetching current temperature from Open-Meteo with rate limiting, caching, and observability.

## Features

- Hexagonal architecture with clean separation of concerns
- Rate limiting (100 requests/minute per IP)
- Response caching (5-minute TTL)
- Health checks and readiness probes
- Prometheus metrics
- OpenAPI/Swagger documentation
- Docker deployment with health checks
- Graceful shutdown
- Production-ready observability

## Requirements

- Java 21
- Maven 3.9+
- Docker (for containerized deployment)

## Quick Start

### First Time Setup

```bash
# Build all modules
mvn clean install

# Navigate to temperature-proxy module
cd temperature-proxy
```

### Common Operations

```bash
# Run tests
mvn test

# Run with coverage
mvn test jacoco:report

# Build without tests
mvn clean install -DskipTests

# Run application
mvn spring-boot:run

# Run via Docker
docker compose up --build
```

### Testing the API

```bash
# Get temperature for Berlin
curl "http://localhost:8080/api/v1/temperature?latitude=52.52&longitude=13.41"

# Check health
curl http://localhost:8080/actuator/health

# View Swagger UI
curl http://localhost:8080/swagger-ui/index.html
```

## API Endpoints

### Get Temperature

```bash
GET /api/v1/temperature?latitude={lat}&longitude={lon}
```

**Parameters:**
- `latitude` (required): Latitude in decimal degrees (-90 to 90)
- `longitude` (required): Longitude in decimal degrees (-180 to 180)

**Response:**
```json
{
  "latitude": 52.52,
  "longitude": 13.41,
  "temperature": 15.2,
  "unit": "celsius",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**Rate Limit:**
- 100 requests per minute per IP
- Returns HTTP 429 when exceeded

**Caching:**
- 5-minute cache per location
- Reduces external API calls

## API Examples

All examples below were tested against the running application and are guaranteed to work.

### Health Check

```bash
curl -s http://localhost:8080/actuator/health | jq .
```

Response:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "livenessState": { "status": "UP" },
    "openMeteo": { "status": "UP", "details": { "service": "Open-Meteo API", "status": "reachable" } },
    "ping": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```

### Weather API Endpoints

#### Get Current Weather (Berlin)

```bash
curl -s "http://localhost:8080/api/v1/weather/current?lat=52.52&lon=13.41" | jq .
```

Response:
```json
{
  "location": {
    "lat": 52.52,
    "lon": 13.41
  },
  "current": {
    "temperatureC": 3.6,
    "windSpeedKmh": 9.3
  },
  "source": "open-meteo",
  "retrievedAt": "2026-01-16T00:00:54.752455145Z"
}
```

#### Additional Test Locations

Warsaw:
```bash
curl -s "http://localhost:8080/api/v1/weather/current?lat=52.23&lon=21.01" | jq .
```

New York:
```bash
curl -s "http://localhost:8080/api/v1/weather/current?lat=40.71&lon=-74.01" | jq .
```

London:
```bash
curl -s "http://localhost:8080/api/v1/weather/current?lat=51.51&lon=-0.13" | jq .
```

### Error Cases

#### Missing Latitude

```bash
curl -s "http://localhost:8080/api/v1/weather/current?lon=13.41" | jq .
```

Response:
```json
{
  "code": "INVALID_COORDINATES",
  "message": "Parameter 'lat' is required",
  "status": 400,
  "timestamp": "2026-01-16T00:00:47.919314166Z",
  "path": "/api/v1/weather/current"
}
```

#### Invalid Latitude (Out of Range)

```bash
curl -s "http://localhost:8080/api/v1/weather/current?lat=100&lon=13.41" | jq .
```

#### Invalid Longitude (Out of Range)

```bash
curl -s "http://localhost:8080/api/v1/weather/current?lat=52.52&lon=200" | jq .
```

### OpenAPI Specification

```bash
curl -s http://localhost:8080/v3/api-docs | jq .
```

### Swagger UI

Access interactive API documentation:
```
http://localhost:8080/swagger-ui/index.html
```

## Observability

### Health Checks

```bash
# General health
curl http://localhost:8080/actuator/health

# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests

# Prometheus scraping endpoint
curl http://localhost:8080/actuator/prometheus
```

## Development

### Technology Stack

- Java 21
- Spring Boot 3.4.1
- Maven
- Docker
- OpenAPI/Swagger
- Bucket4j (rate limiting)
- Caffeine (caching)
- Micrometer (metrics)

### Code Quality

**Standards:**
- Hexagonal architecture enforced by ArchUnit
- 75% minimum code coverage
- Java 21 features (records, sealed types, pattern matching)
- No TODO/FIXME in production code
- No System.out.println usage
- Automated code formatting (Spotless)

**Testing:**
- JUnit 5
- AssertJ for assertions
- Mockito for mocking
- Parameterized tests for edge cases
- Integration tests with WireMock

### Running Tests

```bash
cd temperature-proxy

# Unit tests
make test

# Integration tests
mvn verify

# Architecture tests
mvn test -Dtest=ArchitectureTest

# Coverage report
make coverage
# View report at target/site/jacoco/index.html
```

### Code Formatting

```bash
cd temperature-proxy

# Check formatting
make format-check

# Apply formatting
make format
```

The local CI pipeline runs:
1. Clean build
2. Compilation
3. Unit tests
4. Coverage report (minimum 75%)
5. Code quality checks
6. Formatting verification
7. Docker build and deployment
8. Health check and API verification

### GitHub Actions

The project includes comprehensive GitHub Actions workflows:

**CI Pipeline (`ci.yml`)**
- Triggers: Push/PR to main or develop
- Steps: Build, test, coverage check, Docker build, quality checks
- Artifacts: Test results, coverage report, JAR file

**Release Pipeline (`release.yml`)**
- Triggers: Git tag `v*`
- Steps: Build release, push Docker image to GHCR, create GitHub release

**Dependency Check (`dependency-update.yml`)**
- Schedule: Weekly (Monday 2:00 AM UTC)
- Checks: Maven updates, security vulnerabilities, CVE scanning

## Security

### Rate Limiting

- Bucket4j token bucket algorithm
- 100 requests per minute per IP
- Configurable in `application.yml`

### Security Headers

- Content-Security-Policy
- X-Content-Type-Options
- X-Frame-Options
- Strict-Transport-Security

### Dependency Scanning

- Weekly OWASP dependency checks
- Automated CVE detection
- Fail build on CVSS >= 7

## Configuration

### Application Properties

```yaml
# Rate limiting
rate-limit:
  capacity: 100
  refill-rate: 100
  refill-period: 1m

# Cache
cache:
  ttl: 5m

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```