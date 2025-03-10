#!/bin/bash

# Image Recovery Tool script
echo "Starting GSM Arena Image Recovery Tool..."

# Check if jar file exists
if [ ! -f "target/gsm-arena-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar" ]; then
    echo "Building the project first..."
    mvn clean package
fi

# Check if directories exist
echo "Checking if directory structure exists..."
if [ ! -d "interactive_data/phones" ]; then
    echo "Warning: Directory interactive_data/phones does not exist"
fi

if [ ! -d "interactive_data/images" ]; then
    echo "Warning: Directory interactive_data/images does not exist"
fi

if [ ! -d "final_data/images" ]; then
    echo "Warning: Directory final_data/images does not exist"
fi

# List JSON files in phones directory if it exists
if [ -d "interactive_data/phones" ]; then
    echo "JSON files in interactive_data/phones:"
    ls -la interactive_data/phones/*.json 2>/dev/null || echo "  No JSON files found"
fi

# List image directories
echo -e "\nImage directories in interactive_data/images:"
if [ -d "interactive_data/images" ]; then
    ls -la interactive_data/images/ 2>/dev/null || echo "  No subdirectories found"
fi

echo -e "\nImage directories in final_data/images:"
if [ -d "final_data/images" ]; then
    ls -la final_data/images/ 2>/dev/null || echo "  No subdirectories found"
fi

# Run the recovery tool with verbose output
echo -e "\nRunning Image Recovery Tool..."
java -cp target/gsm-arena-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar org.example.utils.ImageRecoveryTool

echo -e "\nRecovery process complete."
