#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="$SCRIPT_DIR/k8s"

echo "=== Kubernetes Manifests Verification ==="
echo ""

echo "[1/5] Checking YAML syntax..."
cd "$K8S_DIR"
for file in *.yaml; do
    if [ "$file" = "built-manifests.yaml" ]; then
        continue
    fi
    echo -n "  Checking $file... "
    if python3 -c "import yaml; yaml.safe_load(open('$file'))" 2>/dev/null; then
        echo "✓"
    else
        echo "✗ FAILED"
        exit 1
    fi
done
echo ""

echo "[2/5] Checking kustomization.yaml completeness..."
YAML_FILES=$(find . -maxdepth 1 -name "*.yaml" ! -name "kustomization.yaml" ! -name "built-manifests.yaml" | wc -l)
KUSTOMIZE_RESOURCES=$(grep -c "^  -" kustomization.yaml || echo 0)
echo "  YAML files found: $YAML_FILES"
echo "  Resources in kustomization.yaml: $KUSTOMIZE_RESOURCES"
if [ "$YAML_FILES" -eq "$KUSTOMIZE_RESOURCES" ]; then
    echo "  ✓ All files included"
else
    echo "  ✗ Mismatch detected"
    exit 1
fi
echo ""

echo "[3/5] Installing kubectl (if not exists)..."
cd "$SCRIPT_DIR"
if ! command -v kubectl &> /dev/null; then
    if [ ! -f "./kubectl" ]; then
        echo "  Downloading kubectl..."
        KUBECTL_VERSION=$(curl -L -s https://dl.k8s.io/release/stable.txt)
        curl -LO "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl"
        chmod +x kubectl
        echo "  ✓ kubectl downloaded (local)"
    else
        echo "  ✓ kubectl already exists (local)"
    fi
    KUBECTL="./kubectl"
else
    echo "  ✓ kubectl found in PATH"
    KUBECTL="kubectl"
fi
echo ""

echo "[4/5] Building manifests with Kustomize..."
$KUBECTL kustomize "$K8S_DIR" > "$K8S_DIR/built-manifests.yaml"
MANIFEST_LINES=$(wc -l < "$K8S_DIR/built-manifests.yaml")
echo "  Generated manifest: $MANIFEST_LINES lines"
echo "  ✓ Kustomize build successful"
echo ""

echo "[5/5] Validating built manifests..."
RESOURCE_COUNT=$(python3 -c "
import yaml
with open('$K8S_DIR/built-manifests.yaml') as f:
    docs = list(yaml.safe_load_all(f))
    docs = [d for d in docs if d is not None]
    print(len(docs))
    for doc in docs:
        kind = doc.get('kind', 'Unknown')
        name = doc.get('metadata', {}).get('name', 'Unknown')
        print(f'  - {kind}: {name}')
")
echo "  Resources parsed: $RESOURCE_COUNT"
echo "  ✓ All resources valid"
echo ""

echo "=== Verification Summary ==="
echo "✓ YAML syntax: OK"
echo "✓ Kustomization: OK"
echo "✓ Kustomize build: OK ($MANIFEST_LINES lines)"
echo "✓ Resource validation: OK"
echo ""
echo "All Kubernetes manifests are valid and ready for deployment!"
echo ""
echo "To deploy to a cluster, run:"
echo "  kubectl apply -k $K8S_DIR"
