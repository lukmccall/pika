#!/usr/bin/env bash

# Test all supported Kotlin versions
# Usage: ./test-all-versions.sh [--quick]
#   --quick: Only run sample, skip tests

VERSIONS="2.1.20 2.2.0 2.2.10 2.2.20 2.2.21 2.3.0 2.3.10 2.3.20"
QUICK_MODE=false

if [[ "$1" == "--quick" ]]; then
  QUICK_MODE=true
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Results tracking
RESULTS=""

echo "========================================"
echo "Testing Kotlin Compiler Plugin"
echo "========================================"
echo ""

for version in $VERSIONS; do
  echo -e "${YELLOW}Testing Kotlin $version${NC}"
  echo "----------------------------------------"

  sample_status="FAIL"
  test_status="SKIP"

  # Test sample project
  echo -n "  Running sample... "
  if ./gradlew :sample:run -PkotlinVersion="$version" --quiet 2>/dev/null; then
    echo -e "${GREEN}OK${NC}"
    sample_status="PASS"
  else
    echo -e "${RED}FAILED${NC}"
  fi

  # Run tests (unless quick mode)
  if [[ "$QUICK_MODE" == false ]]; then
    echo -n "  Running tests... "
    if ./gradlew :pika-compiler:test -PkotlinVersion="$version" --quiet 2>/dev/null; then
      echo -e "${GREEN}OK${NC}"
      test_status="PASS"
    else
      echo -e "${RED}FAILED${NC}"
      test_status="FAIL"
    fi
  fi

  RESULTS="$RESULTS$version:$sample_status:$test_status\n"
  echo ""
done

# Print summary
echo "========================================"
echo "Summary"
echo "========================================"
echo ""
printf "%-10s | %-10s | %-10s\n" "Version" "Sample" "Tests"
printf "%-10s-+-%-10s-+-%-10s\n" "----------" "----------" "----------"

FAILED=0
echo -e "$RESULTS" | while IFS=: read -r version sample test; do
  [[ -z "$version" ]] && continue

  if [[ "$sample" == "PASS" ]]; then
    sample_display="${GREEN}PASS${NC}"
  else
    sample_display="${RED}FAIL${NC}"
    FAILED=1
  fi

  if [[ "$test" == "PASS" ]]; then
    test_display="${GREEN}PASS${NC}"
  elif [[ "$test" == "SKIP" ]]; then
    test_display="${YELLOW}SKIP${NC}"
  else
    test_display="${RED}FAIL${NC}"
    FAILED=1
  fi

  printf "%-10s | %-19b | %-19b\n" "$version" "$sample_display" "$test_display"
done

echo ""

# Check for failures
if echo -e "$RESULTS" | grep -q "FAIL"; then
  echo -e "${RED}Some tests failed!${NC}"
  exit 1
else
  echo -e "${GREEN}All tests passed!${NC}"
  exit 0
fi
