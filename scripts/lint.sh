#!/bin/bash

# Script to run all linters

echo "Running ktlint..."
./gradlew ktlintCheck

echo "Running detekt..."
./gradlew detekt

echo "All linters completed!"