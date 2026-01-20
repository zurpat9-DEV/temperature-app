# Kubernetes Manifests Verification Guide

## Quick Verification (No Cluster Required)

### Option 1: Automated Script

```bash
cd temperature-proxy
chmod +x verify-k8s.sh
./verify-k8s.sh
```

### Option 2: Manual Verification

#### Step 1: YAML Syntax Validation

```bash
cd k8s
for file in *.yaml; do
    echo "Checking $file..."
    python3 -c "import yaml; yaml.safe_load(open('$file'))"
done
```

#### Step 2: Kustomize Build

```bash
# Install kubectl (includes kustomize)
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl

# Build manifests
./kubectl kustomize k8s/ > k8s/built-manifests.yaml

# Verify output
python3 -c "
import yaml
docs = list(yaml.safe_load_all(open('k8s/built-manifests.yaml')))
print(f'Parsed {len(docs)} resources')
for doc in docs:
    if doc:
        print(f'  - {doc[\"kind\"]}: {doc[\"metadata\"][\"name\"]}')
"
```

Expected output:
```
Parsed 9 resources
  - Namespace: temperature-proxy
  - ServiceAccount: temperature-proxy-sa
  - ConfigMap: temperature-proxy-config
  - Service: temperature-proxy
  - Deployment: temperature-proxy
  - PodDisruptionBudget: temperature-proxy-pdb
  - HorizontalPodAutoscaler: temperature-proxy-hpa
  - Ingress: temperature-proxy-ingress
  - NetworkPolicy: temperature-proxy-netpol
```

## Local Cluster Testing

### Option 1: kind (Kubernetes in Docker)

```bash
# Install kind
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind

# Create cluster
kind create cluster --name temperature-proxy

# Build and load image
docker build -t temperature-proxy:latest .
kind load docker-image temperature-proxy:latest --name temperature-proxy

# Deploy
kubectl apply -k k8s/

# Verify deployment
kubectl get all -n temperature-proxy
kubectl wait --for=condition=ready pod -l app=temperature-proxy -n temperature-proxy --timeout=180s

# Test API
kubectl port-forward -n temperature-proxy svc/temperature-proxy 8080:8080 &
sleep 5
curl "http://localhost:8080/actuator/health"
curl "http://localhost:8080/api/v1/weather/current?lat=52.52&lon=13.41"

# Cleanup
kind delete cluster --name temperature-proxy
```

### Option 2: Minikube

```bash
# Install Minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# Start cluster
minikube start --cpus=2 --memory=4096

# Build image in Minikube
eval $(minikube docker-env)
docker build -t temperature-proxy:latest .

# Deploy
kubectl apply -k k8s/

# Test
minikube service temperature-proxy -n temperature-proxy

# Cleanup
minikube delete
```

### Option 3: Docker Desktop Kubernetes

```bash
# Enable Kubernetes in Docker Desktop settings

# Build image
docker build -t temperature-proxy:latest .

# Deploy
kubectl apply -k k8s/

# Test
kubectl port-forward -n temperature-proxy svc/temperature-proxy 8080:8080
curl "http://localhost:8080/api/v1/weather/current?lat=52.52&lon=13.41"
```

## Static Analysis Tools

### kubeval

```bash
# Install
wget https://github.com/instrumenta/kubeval/releases/latest/download/kubeval-linux-amd64.tar.gz
tar xf kubeval-linux-amd64.tar.gz
sudo mv kubeval /usr/local/bin/

# Validate
cd k8s
kubeval *.yaml
```

### kube-linter

```bash
# Install
wget https://github.com/stackrox/kube-linter/releases/download/v0.6.5/kube-linter-linux.tar.gz
tar xf kube-linter-linux.tar.gz
sudo mv kube-linter /usr/local/bin/

# Lint
cd k8s
kube-linter lint .
```

## GitHub Actions CI/CD

The project includes automated Kubernetes validation in GitHub Actions:

- File: `.github/workflows/kubernetes-validate.yml`
- Triggers: Push to main/develop, Pull Requests
- Validates: YAML syntax, Kustomize build, kubeval, kube-linter, kind integration test

To enable:
1. Push code to GitHub repository
2. GitHub Actions will automatically run validation
3. Check Actions tab for results

## Production Deployment

### Prerequisites

1. Kubernetes cluster (1.25+)
2. Ingress controller (NGINX recommended)
3. Metrics server (for HPA)
4. Docker registry (for custom image)

### Steps

```bash
# Build and push image
docker build -t your-registry/temperature-proxy:v1.0.0 .
docker push your-registry/temperature-proxy:v1.0.0

# Update image in deployment
cd k8s
sed -i 's|temperature-proxy:latest|your-registry/temperature-proxy:v1.0.0|g' deployment.yaml

# Deploy
kubectl apply -k .

# Verify
kubectl get all -n temperature-proxy
kubectl get ingress -n temperature-proxy

# Check HPA
kubectl get hpa -n temperature-proxy

# View logs
kubectl logs -n temperature-proxy -l app=temperature-proxy --tail=100 -f
```

## Troubleshooting

### Pods not starting

```bash
kubectl describe pod -n temperature-proxy -l app=temperature-proxy
kubectl logs -n temperature-proxy -l app=temperature-proxy --previous
```

### Image pull errors

```bash
# Check if image exists
docker images | grep temperature-proxy

# For kind/minikube, reload image
kind load docker-image temperature-proxy:latest --name test-cluster
```

### HPA not scaling

```bash
# Check metrics-server
kubectl top nodes
kubectl top pods -n temperature-proxy

# If not available, install metrics-server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

### Ingress not working

```bash
# Check ingress controller
kubectl get pods -n ingress-nginx

# Check ingress resource
kubectl describe ingress temperature-proxy-ingress -n temperature-proxy

# Get ingress IP
kubectl get ingress -n temperature-proxy
```

## Verification Checklist

- [ ] YAML syntax validation passed
- [ ] Kustomize build successful
- [ ] All 9 resources generated
- [ ] Built manifests parseable
- [ ] kubeval validation passed (optional)
- [ ] kube-linter checks passed (optional)
- [ ] Pods running in test cluster (optional)
- [ ] Health probes responding (optional)
- [ ] API endpoints accessible (optional)

## Expected Resources

| Resource Type | Name | Purpose |
|---------------|------|---------|
| Namespace | temperature-proxy | Isolation |
| ServiceAccount | temperature-proxy-sa | Pod identity |
| ConfigMap | temperature-proxy-config | Environment config |
| Service | temperature-proxy | Internal networking |
| Deployment | temperature-proxy | Application pods |
| PodDisruptionBudget | temperature-proxy-pdb | High availability |
| HorizontalPodAutoscaler | temperature-proxy-hpa | Auto-scaling |
| Ingress | temperature-proxy-ingress | External access |
| NetworkPolicy | temperature-proxy-netpol | Network security |

## Configuration Override

Environment variables can be overridden in ConfigMap:

```yaml
# Edit configmap.yaml
data:
  APP_CACHE_TTL: "120s"              # Change cache TTL
  APP_OPEN_METEO_TIMEOUT: "2s"       # Change timeout
  APP_RATE_LIMIT_REQUESTS_PER_MINUTE: "200"  # Increase rate limit
```

Then reapply:
```bash
kubectl apply -k k8s/
kubectl rollout restart deployment/temperature-proxy -n temperature-proxy
```

## Monitoring

### Prometheus Metrics

```bash
# Port-forward to metrics endpoint
kubectl port-forward -n temperature-proxy svc/temperature-proxy 8080:8080

# Scrape metrics
curl http://localhost:8080/actuator/prometheus
```

### Health Checks

```bash
# Liveness
kubectl exec -n temperature-proxy deployment/temperature-proxy -- wget -q -O- http://localhost:8080/actuator/health/liveness

# Readiness
kubectl exec -n temperature-proxy deployment/temperature-proxy -- wget -q -O- http://localhost:8080/actuator/health/readiness
```

## Security

The deployment follows security best practices:

- Non-root user (UID 1001)
- Read-only root filesystem
- No privilege escalation
- Capabilities dropped: ALL
- NetworkPolicy restricting traffic
- ServiceAccount with minimal permissions
- Resource limits enforced
