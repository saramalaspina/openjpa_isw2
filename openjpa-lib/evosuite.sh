#!/usr/bin/env bash
set -e

CLASSES=(
  "org.apache.openjpa.lib.util.ClassUtil"
)

# Jar EvoSuite
EVOSUITE_JAR="${EVOSUITE_JAR:-$(pwd)/tools/evosuite-1.2.0.jar}"

SEARCH_BUDGET="${SEARCH_BUDGET:-180}"

echo "▶ Build OpenJPA..."
mvn -q -DskipTests package

echo "▶ Build classpath..."
mvn -q dependency:build-classpath \
  -DincludeScope=test \
  -Dmdep.outputFile=evosuite.classpath

echo "$(pwd)/target/test-classes:$(pwd)/target/classes:$(cat evosuite.classpath)" > full.classpath

CP="$(cat full.classpath)"

mkdir -p evosuite-tests evosuite-report

for CLASS in "${CLASSES[@]}"; do
  echo "▶ EvoSuite on $CLASS"

  "$JAVA_HOME/bin/java" -Xmx4g -jar "$EVOSUITE_JAR" \
    -class "$CLASS" \
    -projectCP "$CP" \
    -Djunit=4 \
    -Dsandbox=false \
    -Dcriterion=BRANCH \
    -Dtest_dir=evosuite-tests \
    -Dreport_dir=evosuite-report \
    -Dsearch_budget="$SEARCH_BUDGET"

done

echo "Test generated in evosuite-tests/"
