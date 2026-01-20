#!/bin/bash
set -e

echo "=========================================="
echo "Running Local CI Pipeline"
echo "=========================================="
echo ""

cleanup() {
    echo ""
    echo "Cleaning up..."
    docker compose down 2>/dev/null || true
}

trap cleanup EXIT

echo "Step 1/8: Clean build"
mvn clean -q

echo "Step 2/8: Compile"
mvn compile -B

echo "Step 3/8: Run unit tests"
mvn test -B

echo "Step 4/8: Generate coverage report"
mvn jacoco:report -B

COVERAGE=$(grep -oP 'Total.*?([0-9]{1,3})%' target/site/jacoco/index.html | grep -oP '[0-9]{1,3}' | head -1 || echo "0")
echo "Code coverage: ${COVERAGE}%"
if [ "$COVERAGE" -lt 75 ]; then
    echo "WARNING: Code coverage is below 75%"
fi

echo "Step 5/8: Check code quality"
mvn verify -DskipTests -B

echo "Step 6/8: Check for code issues"
if grep -r "TODO\|FIXME" src/main/ --include="*.java" 2>/dev/null; then
    echo "WARNING: Found TODO or FIXME comments"
fi

if grep -r "System.out.println\|System.err.println" src/main/ --include="*.java" 2>/dev/null; then
    echo "WARNING: Found System.out/err.println"
fi

echo "Step 7/8: Check code formatting"
if command -v mvn spotless:check &> /dev/null; then
    mvn spotless:check -B 2>/dev/null || echo "Run 'mvn spotless:apply' to fix formatting"
fi

echo "Step 8/8: Build and test Docker"
docker compose up -d --build

echo "Waiting for application to start..."
timeout 120 bash -c 'until curl -sf http://localhost:8080/actuator/health/readiness > /dev/null; do sleep 2; done' || {
    echo "ERROR: Application failed to start"
    docker compose logs
    exit 1
}

echo "Testing health endpoints..."
curl -sf http://localhost:8080/actuator/health || exit 1
curl -sf http://localhost:8080/actuator/health/liveness || exit 1
curl -sf http://localhost:8080/actuator/health/readiness || exit 1

echo "Testing API endpoint..."
RESPONSE=$(curl -s -w "\n%{http_code}" http://localhost:8080/api/v1/temperature?latitude=52.52&longitude=13.41)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" != "200" ]; then
    echo "ERROR: API returned status $HTTP_CODE"
    exit 1
fi

if [ -z "$BODY" ]; then
    echo "ERROR: API returned empty response"
    exit 1
fi

echo "Testing Swagger UI..."
curl -sf http://localhost:8080/swagger-ui/index.html > /dev/null || exit 1

echo "Testing Prometheus metrics..."
curl -sf http://localhost:8080/actuator/prometheus > /dev/null || exit 1

echo ""
echo "=========================================="
echo "Local CI Pipeline: SUCCESS"
echo "=========================================="
echo ""
echo "Coverage: ${COVERAGE}%"
echo "API Response: $BODY"
echo ""
echo "Application running at:"
echo "  - API: http://localhost:8080/api/v1/temperature"
echo "  - Swagger: http://localhost:8080/swagger-ui/index.html"
echo "  - Health: http://localhost:8080/actuator/health"
echo "  - Metrics: http://localhost:8080/actuator/prometheus"
echo ""
echo "Stop with: docker compose down"
