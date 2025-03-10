#!/bin/bash

# Run Main Menu with error handling
echo "Starting GSM Arena Scraper Menu..."

# Check if jar file exists
if [ ! -f "target/gsm-arena-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar" ]; then
    echo "Error: JAR file not found! Please run 'mvn clean package' first."
    exit 1
fi

# Run the menu
java -cp target/gsm-arena-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar org.example.Main "$@"
