.PHONY: help clean compile test coverage verify format format-check docker docker-test ci security deps-check deps-update

help:
	@echo "Available targets:"
	@echo "  clean         - Clean build artifacts"
	@echo "  compile       - Compile source code"
	@echo "  test          - Run unit tests"
	@echo "  coverage      - Generate coverage report"
	@echo "  verify        - Run full verification"
	@echo "  format        - Apply code formatting"
	@echo "  format-check  - Check code formatting"
	@echo "  docker        - Build and start Docker"
	@echo "  docker-test   - Test Docker endpoints"
	@echo "  ci            - Run full local CI pipeline"
	@echo "  security      - Run security scan"
	@echo "  deps-check    - Check dependency updates"
	@echo "  deps-update   - Update dependencies"

clean:
	mvn clean

compile:
	mvn compile -B

test:
	mvn test -B

coverage:
	mvn test jacoco:report -B
	@echo "Coverage report: target/site/jacoco/index.html"

verify:
	mvn verify -B

format:
	mvn spotless:apply

format-check:
	mvn spotless:check

docker:
	docker compose up --build

docker-down:
	docker compose down

docker-test:
	@echo "Testing health endpoints..."
	curl -f http://localhost:8080/actuator/health
	@echo "\nTesting API..."
	curl -f "http://localhost:8080/api/v1/temperature?latitude=52.52&longitude=13.41"
	@echo "\nTesting Swagger..."
	curl -f http://localhost:8080/swagger-ui/index.html > /dev/null && echo "Swagger OK"

ci:
	./ci-local.sh

security:
	mvn org.owasp:dependency-check-maven:check
	@echo "Security report: target/dependency-check-report.html"

deps-check:
	@echo "Checking dependencies..."
	mvn versions:display-dependency-updates
	@echo "\nChecking plugins..."
	mvn versions:display-plugin-updates

deps-update:
	mvn versions:use-latest-releases -DallowMajorUpdates=false
	mvn versions:use-latest-versions -DallowMajorUpdates=false
