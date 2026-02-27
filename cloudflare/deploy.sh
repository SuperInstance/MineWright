#!/bin/bash

# Minecraft Agent Reflex - Deployment Script
# This script handles deployment to Cloudflare Workers

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT=${1:-"production"}

echo -e "${GREEN}=== Minecraft Agent Reflex Deployment ===${NC}"
echo "Environment: $ENVIRONMENT"
echo ""

# Function to print colored output
print_info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
print_info "Checking prerequisites..."

# Check if wrangler is installed
if ! command -v wrangler &> /dev/null; then
    print_error "wrangler CLI is not installed"
    echo "Install it with: npm install -g wrangler"
    exit 1
fi

print_success "Wrangler CLI found"

# Check if logged in
print_info "Checking Cloudflare authentication..."
if ! wrangler whoami &> /dev/null; then
    print_error "Not logged in to Cloudflare"
    echo "Run: wrangler login"
    exit 1
fi

print_success "Authenticated with Cloudflare"

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(development|staging|production)$ ]]; then
    print_error "Invalid environment: $ENVIRONMENT"
    echo "Valid options: development, staging, production"
    exit 1
fi

# Run tests
print_info "Running tests..."
if command -v pytest &> /dev/null; then
    pytest tests/ -v
    print_success "Tests passed"
else
    print_info "pytest not found, skipping tests"
fi

# Check if KV namespaces need to be created
print_info "Checking KV namespaces..."

# Try to get KV namespace info
if ! wrangler kv:namespace list --json 2>/dev/null | grep -q "MISSION_CACHE"; then
    print_info "Creating MISSION_CACHE KV namespace..."
    if [ "$ENVIRONMENT" == "production" ]; then
        wrangler kv:namespace create "MISSION_CACHE"
    else
        wrangler kv:namespace create "MISSION_CACHE" --preview
    fi
fi

if ! wrangler kv:namespace list --json 2>/dev/null | grep -q "KNOWLEDGE_CACHE"; then
    print_info "Creating KNOWLEDGE_CACHE KV namespace..."
    if [ "$ENVIRONMENT" == "production" ]; then
        wrangler kv:namespace create "KNOWLEDGE_CACHE"
    else
        wrangler kv:namespace create "KNOWLEDGE_CACHE" --preview
    fi
fi

# Check for required secrets
print_info "Checking required secrets..."

REQUIRED_SECRETS=("FOREMAN_URL" "FOREMAN_API_KEY")

for secret in "${REQUIRED_SECRETS[@]}"; do
    # Check if secret exists (this is a simple check)
    if ! wrangler secret list --env "$ENVIRONMENT" 2>/dev/null | grep -q "$secret"; then
        print_info "Secret $secret not set. Please set it with:"
        echo "  wrangler secret put $secret --env $ENVIRONMENT"
        read -p "Set $secret now? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            wrangler secret put "$secret" --env "$ENVIRONMENT"
        else
            print_error "Required secret not set. Aborting deployment."
            exit 1
        fi
    fi
done

# Deploy
print_info "Deploying to $ENVIRONMENT..."

if [ "$ENVIRONMENT" == "production" ]; then
    wrangler deploy
else
    wrangler deploy --env "$ENVIRONMENT"
fi

print_success "Deployment complete!"

# Get worker URL
WORKER_URL="https://minecraft-agent-reflex"
if [ "$ENVIRONMENT" != "production" ]; then
    WORKER_URL="${WORKER_URL}-${ENVIRONMENT}"
fi
WORKER_URL="${WORKER_URL}.workers.dev"

echo ""
print_success "Worker deployed at: $WORKER_URL"
echo ""
echo "Test endpoints:"
echo "  Health check: curl $WORKER_URL/agents/test-agent/health"
echo "  Get tactical: curl -X POST $WORKER_URL/agents/test-agent/tactical -H 'Content-Type: application/json' -d '{\"position\":{\"x\":0,\"y\":64,\"z\":0},\"nearbyEntities\":[],\"nearbyBlocks\":[],\"health\":20}'"
echo ""
echo "View logs:"
echo "  wrangler tail --env $ENVIRONMENT"
