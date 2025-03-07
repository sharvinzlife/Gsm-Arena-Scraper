# 📱 GSM Arena Phone Scraper

![Java](https://img.shields.io/badge/Java-17-orange)
![Maven](https://img.shields.io/badge/Maven-3.8-blue)
![License](https://img.shields.io/badge/License-MIT-green)

> An elegant Java application for extracting comprehensive phone specifications from GSM Arena with high-resolution images.

<p align="center">
  <img src="https://i.imgur.com/sEWOnnm.jpeg" width="650">
</p>

## ✨ Features

- 🔄 **Dual Scraping Modes**:
  - 📋 **Batch Scraper** - Process a predefined list of 55+ phones in one go
  - 🔍 **Interactive Scraper** - Manually search and select phones to scrape

- 🖼️ **High-Resolution Images**:
  - Smart image URL pattern matching for different phone brands
  - Automatic fallback mechanisms for reliable image retrieval

- 📊 **Comprehensive Data**:
  - Detailed technical specifications
  - Organized into logical categories
  - Clean JSON format for easy integration

- 🚀 **Performance**:
  - Optimized HTTP requests with connection pooling
  - Rate limiting to avoid overloading the server
  - Robust error handling and recovery

## 🛠️ Installation

### Prerequisites

- Java JDK 11+
- Maven 3.6+

### Setup

```bash
# Clone the repository
git clone https://github.com/sharvinzlife/Gsm-Arena-Scraper.git
cd Gsm-Arena-Scraper

# Build the project
mvn clean package
```

## 📋 Usage

### Menu Interface

Run the main menu to choose between interactive or batch mode:

```bash
java -cp target/gsm-arena-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar org.example.Main
```

### Interactive Mode

Manually search for specific phone models:

```bash
java -cp target/gsm-arena-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar org.example.InteractiveScraper
```

<details>
<summary>Interactive Mode Demo</summary>

```
=== GSM Arena Phone Data Scraper ===
Enter phone names to scrape (one at a time)
Examples: 'Samsung S24 Ultra', 'iPhone 15 Pro Max', 'Pixel 8'

Enter phone name (or type 'done' to finish, 'exit' to quit): Samsung S24 Ultra
Added: Samsung S24 Ultra (Total phones: 1)

Enter phone name (or type 'done' to finish, 'exit' to quit): iPhone 15 Pro
Added: iPhone 15 Pro (Total phones: 2)

Enter phone name (or type 'done' to finish, 'exit' to quit): done

Summary of phones to scrape:
1. Samsung S24 Ultra
2. iPhone 15 Pro

Proceed with scraping these phones? (y/n): y
```
</details>

### Batch Mode

Automatically scrape a predefined list of phone models:

```bash
java -cp target/gsm-arena-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar org.example.BatchScraper
```

## 📂 Project Structure

```
gsm-arena-scraper/
├── 📁 src/main/java/org/example/     # Main application code
│   ├── BatchScraper.java             # Batch scraping functionality
│   ├── InteractiveScraper.java       # Interactive scraping mode
│   ├── Main.java                     # Main menu interface
│   └── 📁 core/                      # Core utilities
│       ├── ImageUtils.java           # Image URL generation & download
│       ├── OxylabsClient.java        # HTTP client with proxy support
│       └── PhoneDataParser.java      # HTML parsing utilities
├── 📁 batch_data/                    # Batch mode output
│   ├── phones/                       # JSON specification files
│   └── images/                       # High-resolution phone images
├── 📁 interactive_data/              # Interactive mode output
│   ├── phones/                       # JSON specification files
│   └── images/                       # High-resolution phone images
└── 📄 pom.xml                        # Maven configuration
```

## 🧩 Core Components

### 🔍 PhoneDataParser

Extracts structured data from HTML using JSoup, organizing specifications into categories:
- Display
- Platform (CPU, RAM)
- Camera
- Battery
- Network
- General specifications

### 🖼️ ImageUtils

Generates high-resolution image URLs based on phone models with smart pattern matching for different brands:
- Samsung
- Apple
- Motorola
- Google
- Realme
- Xiaomi
- Vivo/iQOO

### 🌐 OxylabsClient

Handles HTTP communication with GSM Arena through a proxy service to avoid rate limiting and ensure reliable scraping.

## 📊 Output Format

The scraper generates comprehensive JSON files with organized phone specifications:

<details>
<summary>Show JSON Example</summary>

```json
{
  "name": "Samsung Galaxy S24 Ultra",
  "image": "https://fdn2.gsmarena.com/vv/bigpic/samsung-galaxy-s24-ultra-5g.jpg",
  "highResImage": "https://fdn2.gsmarena.com/vv/pics/samsung/samsung-galaxy-s24-ultra-5g-sm-s928-0.jpg",
  "specifications": {
    "Display": {
      "Size": "6.8 inches",
      "Resolution": "1440 x 3120 pixels",
      "Type": "Dynamic LTPO AMOLED 2X, 120Hz"
    },
    "Platform": {
      "CPU": "Octa-core (1x3.39 GHz + 3x3.1 GHz + 4x2.9 GHz)",
      "Chipset": "Qualcomm Snapdragon 8 Gen 3",
      "GPU": "Adreno 750",
      "RAM": "12GB"
    },
    "Camera": {
      "Main Camera": "200 MP (f/1.7) + 50 MP (f/3.4) + 12 MP (f/2.2) + 10 MP (f/2.4)",
      "Features": "LED flash, panorama, HDR, auto-HDR",
      "Video": "8K@30fps, 4K@30/60/120fps, 1080p@30/60/240fps, 1080p@960fps"
    },
    "Battery": {
      "Capacity": "5000 mAh",
      "Charging": "45W wired, PD3.0, 15W wireless, 4.5W reverse wireless"
    },
    "Network": {
      "Technology": "GSM / CDMA / HSPA / EVDO / LTE / 5G"
    },
    "General": {
      "Announced": "2024, January 17"
    }
  }
}
```
</details>

## 🔧 Technology Stack

- **Java** - Core programming language
- **Maven** - Dependency management and build automation
- **JSoup** - HTML parsing and data extraction
- **OkHttp** - HTTP client for web requests
- **JSON** - Data storage format

## 📜 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👏 Acknowledgements

- [GSM Arena](https://www.gsmarena.com/) - Source of phone specifications data
- [Jsoup](https://jsoup.org/) - Excellent HTML parsing library
- [OkHttp](https://square.github.io/okhttp/) - Efficient HTTP client

---

<p align="center">
  Created with ❤️ by <a href="https://github.com/sharvinzlife">Sharvin</a>
</p>
