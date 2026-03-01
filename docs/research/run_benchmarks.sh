#!/bin/bash

# run_benchmarks.sh
# Automated benchmark execution script for Steve AI evaluation
# This script runs all benchmark scenarios and generates analysis reports

set -e  # Exit on error

# ============================================================================
# CONFIGURATION
# ============================================================================

STEVE_DIR="${STEVE_DIR:-/path/to/steve}"
RESULTS_DIR="${RESULTS_DIR:-$STEVE_DIR/benchmark_results}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RUN_DIR="$RESULTS_DIR/run_$TIMESTAMP"

# Number of trials per scenario
TRIALS="${TRIALS:-30}"

# Minecraft server settings
MC_MEMORY="4G"
MC_PORT=25565

# LLM API key (set as environment variable)
if [ -z "$OPENAI_API_KEY" ]; then
    echo "Error: OPENAI_API_KEY environment variable not set"
    echo "Export your API key: export OPENAI_API_KEY='sk-...'"
    exit 1
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ============================================================================
# FUNCTIONS
# ============================================================================

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# ============================================================================
# SETUP
# ============================================================================

log_info "Steve AI Benchmark Runner"
log_info "========================="
log_info "Timestamp: $TIMESTAMP"
log_info "Results directory: $RUN_DIR"
log_info "Trials per scenario: $TRIALS"
echo ""

# Create results directory
mkdir -p "$RUN_DIR"
mkdir -p "$RUN_DIR/logs"
mkdir -p "$RUN_DIR/metrics"
mkdir -p "$RUN_DIR/screenshots"

# ============================================================================
# BUILD PROJECT
# ============================================================================

log_info "Building Steve AI..."
cd "$STEVE_DIR"

if [ ! -f "build.gradle" ]; then
    log_error "build.gradle not found. Please set STEVE_DIR to the project root."
    exit 1
fi

./gradlew clean build

if [ $? -ne 0 ]; then
    log_error "Build failed"
    exit 1
fi

log_info "Build successful"

# Copy built mod to mods directory
mkdir -p "$HOME/.minecraft/mods"
cp build/libs/steve-*.jar "$HOME/.minecraft/mods/"

# ============================================================================
# RUN BENCHMARKS
# ============================================================================

# Array of benchmark scenarios
declare -a SCENARIOS=(
    "Simple: Mine 10 Stone"
    "Medium: Build 5x5 House"
    "Complex: Automated Wheat Farm"
    "Multi-Agent: Village with 3 Workers"
)

# Run each scenario
for scenario in "${SCENARIOS[@]}"; do
    log_info ""
    log_info "=========================================="
    log_info "Running Scenario: $scenario"
    log_info "=========================================="

    # Create scenario-specific directory
    scenario_dir=$(echo "$scenario" | tr '[:upper:]' '[:lower:]' | tr ' ' '_')
    mkdir -p "$RUN_DIR/$scenario_dir"

    # Run trials
    for trial in $(seq 1 $TRIALS); do
        log_info "Trial $trial/$TRIALS"

        # Start Minecraft server in background
        # (This would be replaced with actual server startup command)
        # Example: java -Xms$MC_MEMORY -Xmx$MC_MEMORY -jar forge-server.jar nogui &

        # Wait for server to start
        sleep 10

        # Submit benchmark command
        # (This would be done via the evaluation framework)
        # Example: echo "submit_command \"$scenario\"" >> server_input_pipe

        # Wait for benchmark completion or timeout
        timeout=600  # 10 minutes max
        elapsed=0
        while [ $elapsed -lt $timeout ]; do
            # Check if benchmark is complete
            # (This would check for completion signal)
            sleep 5
            elapsed=$((elapsed + 5))
        done

        # Collect metrics
        # (This would move metrics from the Minecraft instance to results)
        # cp "$HOME/.minecraft/steve_metrics.json" "$RUN_DIR/$scenario_dir/trial_$trial.json"

        # Reset world for next trial
        # (This would reset the Minecraft world)
        # rm -rf "$HOME/.minecraft/saves/benchmark_world"
        # cp -r "$HOME/.minecraft/saves/benchmark_world_template" "$HOME/.minecraft/saves/benchmark_world"

        log_info "Trial $trial complete"

        # Small delay between trials
        sleep 5
    done

    log_info "Scenario '$scenario' complete"
done

# ============================================================================
# COLLECT RESULTS
# ============================================================================

log_info ""
log_info "Collecting results..."

# Combine all trial results into single JSON
# (This would merge all individual trial JSONs)
# python3 "$STEVE_DIR/scripts/merge_results.py" "$RUN_DIR" "$RUN_DIR/all_results.json"

# Export metrics from all runs
find "$RUN_DIR" -name "metrics_*.json" -exec cat {} \; > "$RUN_DIR/all_metrics.json"

# ============================================================================
# GENERATE REPORTS
# ============================================================================

log_info ""
log_info "Generating analysis reports..."

# Run R script for statistical analysis
if command -v Rscript &> /dev/null; then
    Rscript "$STEVE_DIR/docs/research/analyze_benchmarks.R" \
        "$RUN_DIR/all_metrics.json" \
        "$RUN_DIR/reports"
else
    log_warn "R not found. Skipping statistical analysis."
    log_warn "Install R to generate reports: https://www.r-project.org/"
fi

# Generate CSV export
# (This would convert JSON to CSV for spreadsheet analysis)
# python3 "$STEVE_DIR/scripts/json_to_csv.py" "$RUN_DIR/all_metrics.json" > "$RUN_DIR/results.csv"

# ============================================================================
# SUMMARY
# ============================================================================

log_info ""
log_info "=========================================="
log_info "BENCHMARK COMPLETE"
log_info "=========================================="
log_info "Results saved to: $RUN_DIR"
log_info ""
log_info "Next steps:"
log_info "  1. Review results in: $RUN_DIR/reports/"
log_info "  2. Check logs in: $RUN_DIR/logs/"
log_info "  3. Analyze metrics in: $RUN_DIR/all_metrics.json"
log_info ""
log_info "To view the report:"
log_info "  cat $RUN_DIR/reports/benchmark_report.txt"
log_info ""

# ============================================================================
# CLEANUP (Optional)
# ============================================================================

# Uncomment to clean up temporary files
# log_info "Cleaning up temporary files..."
# rm -rf /tmp/steve_benchmark_*
# log_info "Cleanup complete"

exit 0
