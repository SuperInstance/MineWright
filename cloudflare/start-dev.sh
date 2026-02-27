#!/bin/bash

# Quick start script for local development

echo "=== Minecraft Agent Reflex - Local Development ==="
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "Creating .env from .env.example..."
    cp .env.example .env
    echo "Please edit .env with your configuration"
    echo ""
fi

# Load environment variables
export $(cat .env | grep -v '^#' | xargs)

# Check if wrangler is installed
if ! command -v wrangler &> /dev/null; then
    echo "Installing Wrangler CLI..."
    npm install -g wrangler
fi

# Start development server
echo "Starting local development server..."
echo "Worker will be available at: http://localhost:8787"
echo ""
echo "Press Ctrl+C to stop"
echo ""

wrangler dev --local
