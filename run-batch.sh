#!/bin/bash

# Run Batch Scraper with error handling
echo "Starting GSM Arena Batch Scraper..."

# Check if jar file exists
if [ ! -f "target/gsm-arena-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar" ]; then
    echo "Error: JAR file not found! Please run 'mvn clean package' first."
    exit 1
fi

# Run the scraper
java -cp target/gsm-arena-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar org.example.BatchScraper "$@"
