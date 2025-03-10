#!/bin/bash

# Run Image Embedder with error handling
echo "Starting GSM Arena Image Embedder..."

# Check if jar file exists
if [ ! -f "target/gsm-arena-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar" ]; then
    echo "Error: JAR file not found! Please run 'mvn clean package' first."
    exit 1
fi

# Check if directory argument is provided
if [ $# -eq 0 ]; then
    echo "Usage: ./embed-images.sh <json_directory>"
    echo "Example: ./embed-images.sh interactive_data/phones"
    exit 1
fi

# Check if the directory exists
if [ ! -d "$1" ]; then
    echo "Error: Directory $1 does not exist"
    exit 1
fi

echo "Rebuilding project to ensure all classes are up to date..."
mvn clean package

# Run the image embedder
echo "Running image embedder on $1..."
java -cp target/gsm-arena-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar org.example.utils.ImageEmbedder "$1"
