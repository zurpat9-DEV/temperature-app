# Temperature Proxy API

REST API proxy service that fetches current temperature data from Open-Meteo API and returns normalized responses.

## Features

- Current weather data retrieval (temperature, wind speed)
- Response caching with 60-second TTL
- Rate limiting (100 requests/minute per IP)
- Health checks with Kubernetes probes
- Prometheus metrics endpoint
- OpenAPI documentation with Swagger UI

## Requirements

- Java 21
- Maven 3.9+
- Docker and Docker Compose (for containerized deployment)
- Kubernetes cluster (for production deployment)

## Quick Start

### Local Development

```bash
cd temperature-proxy
./mvnw spring-boot:run
```

### Docker Compose

```bash
cd temperature-proxy
docker compose up --build
```

### Kubernetes

```bash
cd temperature-proxy/k8s
kubectl apply -k .
```

## API Usage

### Get Current Weather

```bash
curl "http://localhost:8080/api/v1/weather/current?lat=52.52&lon=13.41"
```

Response:
```json
{
  "location": {
    "lat": 52.52,
    "lon": 13.41
  },
  "current": {
    "temperatureC": 15.5,
    "windSpeedKmh": 10.2
  },
  "source": "open-meteo",
  "retrievedAt": "2026-01-11T10:12:54Z"
}
```

### Parameters

| Parameter | Type   | Required | Constraints        |
|-----------|--------|----------|--------------------|
| lat       | double | Yes      | -90.0 to 90.0      |
| lon       | double | Yes      | -180.0 to 180.0    |

### Error Responses

| Status | Code                      | Description                   |
|--------|---------------------------|-------------------------------|
| 400    | INVALID_COORDINATES       | Invalid latitude or longitude |
| 429    | RATE_LIMIT_EXCEEDED       | Too many requests             |
| 502    | UPSTREAM_ERROR            | Open-Meteo API error          |
| 504    | UPSTREAM_TIMEOUT          | Open-Meteo did not respond    |

## Endpoints

| Endpoint                          | Description              |
|-----------------------------------|--------------------------|
| GET /api/v1/weather/current       | Get current weather      |
| GET /actuator/health              | Health check             |
| GET /actuator/health/liveness     | Liveness probe           |
| GET /actuator/health/readiness    | Readiness probe          |
| GET /actuator/metrics             | Application metrics      |
| GET /actuator/prometheus          | Prometheus metrics       |
| GET /swagger-ui.html              | Swagger UI               |
| GET /v3/api-docs                  | OpenAPI specification    |

## Configuration

Key configuration properties (can be overridden via environment variables):

| Property                              | Default | Description                    |
|---------------------------------------|---------|--------------------------------|
| app.open-meteo.timeout                | 1s      | Upstream request timeout       |
| app.open-meteo.connect-timeout        | 500ms   | Connection timeout             |
| app.cache.ttl                         | 60s     | Cache time-to-live             |
| app.cache.max-size                    | 10000   | Maximum cache entries          |
| app.rate-limit.requests-per-minute    | 100     | Rate limit per IP              |

## Architecture

The service follows hexagonal architecture (ports and adapters):

```
com.temperature.proxy/
  domain/
    model/        # Value objects (Coordinates, Temperature, etc.)
    port/
      in/         # Input ports (GetCurrentWeatherUseCase)
      out/        # Output ports (WeatherDataProvider)
  application/
    service/      # Use case implementations (WeatherService)
  infrastructure/
    adapter/
      in/web/     # REST controllers
      out/        # External service adapters (OpenMeteo, Cache)
    config/       # Spring configuration
    health/       # Health indicators
    metrics/      # Custom metrics
```

## Kubernetes Verification

### Quick Verification (No Cluster Required)

Automated verification script validates all Kubernetes manifests:

```bash
chmod +x verify-k8s.sh
./verify-k8s.sh
```

This script performs:
- YAML syntax validation
- Kustomization completeness check
- Kustomize build test
- Resource structure validation

### Manual Verification

```bash
# YAML syntax check
cd k8s
for file in *.yaml; do python3 -c "import yaml; yaml.safe_load(open('$file'))"; done

# Kustomize build
kubectl kustomize k8s/ > k8s/built-manifests.yaml
```

### Local Cluster Testing

#### Option 1: kind (Kubernetes in Docker)

```bash
# Install kind
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x kind && sudo mv kind /usr/local/bin/

# Create cluster and deploy
kind create cluster --name test
docker build -t temperature-proxy:test .
kind load docker-image temperature-proxy:test --name test
kubectl apply -k k8s/

# Test
kubectl wait --for=condition=ready pod -l app=temperature-proxy -n temperature-proxy --timeout=180s
kubectl port-forward -n temperature-proxy svc/temperature-proxy 8080:8080
```

#### Option 2: Minikube

```bash
minikube start --cpus=2 --memory=4096
eval $(minikube docker-env)
docker build -t temperature-proxy:latest .
kubectl apply -k k8s/
minikube service temperature-proxy -n temperature-proxy
```

### GitHub Actions CI/CD

Automated Kubernetes validation runs on every push/PR:
- Workflow: `.github/workflows/kubernetes-validate.yml`
- Validates: YAML syntax, Kustomize build, kubeval, kube-linter
- Integration test: Deploys to kind cluster and tests endpoints

See `k8s/VERIFICATION.md` for detailed verification guide.

## Kubernetes Resources

| Resource              | Name                       | Description                    |
|-----------------------|----------------------------|--------------------------------|
| Namespace             | temperature-proxy          | Isolated namespace             |
| Deployment            | temperature-proxy          | 2 replica pods                 |
| Service               | temperature-proxy          | ClusterIP service              |
| HPA                   | temperature-proxy-hpa      | Auto-scaling (2-10 pods)       |
| Ingress               | temperature-proxy-ingress  | External access                |
| PodDisruptionBudget   | temperature-proxy-pdb      | Minimum 1 pod available        |
| NetworkPolicy         | temperature-proxy-netpol   | Network isolation              |
| ServiceAccount        | temperature-proxy-sa       | Dedicated service account      |
| ConfigMap             | temperature-proxy-config   | Environment configuration      |

## Resource Limits

| Resource | Request | Limit  |
|----------|---------|--------|
| CPU      | 100m    | 500m   |
| Memory   | 256Mi   | 512Mi  |

## Testing

### Run Unit Tests

```bash
./mvnw test
```

### Run All Tests (Including Integration)

```bash
./mvnw verify
```

### Test Coverage Report

```bash
./mvnw verify
open target/site/jacoco/index.html
```

## Build

### Build JAR

```bash
./mvnw clean package -DskipTests
```

### Build Docker Image

```bash
docker build -t temperature-proxy:latest .
```

## Monitoring

### Prometheus Metrics

Available at `/actuator/prometheus`:

- `weather.requests.total` - Total weather requests
- `weather.cache.hits` - Cache hit count
- `weather.cache.misses` - Cache miss count
- `weather.upstream.latency` - Open-Meteo API call duration
- `cache.gets` - Cache operations
- `http.server.requests` - HTTP request metrics

### Health Checks

- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness` (includes Open-Meteo connectivity)

## License

MIT
