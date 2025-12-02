Finance Management App 💰

https://img.shields.io/badge/Kotlin-1.9.0-blue.svg
https://img.shields.io/badge/Compose-1.5.0-brightgreen.svg
https://img.shields.io/badge/License-MIT-yellow.svg
https://img.shields.io/badge/minSdk-24-orange.svg
https://img.shields.io/badge/Firebase-Enabled-red.svg

A modern personal finance management application built with Jetpack Compose, featuring expense tracking, budgeting, recurring transactions, and AI-powered financial insights.

📋 Table of Contents

Features
Screenshots
Architecture
Installation
Building
Configuration
Contributing
License
✨ Features

Core Functionality

Transaction Management: Add, edit, delete, and categorize income and expenses
Budget Tracking: Set monthly budgets and monitor spending limits
Recurring Expenses: Automate regular payments with flexible scheduling
Multi-Currency Support: Handle transactions in different currencies
Data Export: Export financial data to CSV, Excel, and PDF formats
Analytics & Insights

Visual Dashboards: Interactive charts and graphs for financial overview
Category Analysis: Breakdown of spending by category
Trend Analysis: Identify spending patterns over time
Financial Reports: Generate detailed weekly, monthly, and yearly reports
Goal Tracking: Set and monitor savings and investment goals
Smart Features

AI-Powered Insights: Get intelligent suggestions for budget optimization
Receipt Scanning: Extract transaction details from receipt images using OCR
Bill Reminders: Never miss a payment with smart notifications
Financial Forecasting: Predict future expenses based on historical data
Spending Alerts: Receive notifications for unusual spending patterns
User Experience

Material Design 3: Modern, intuitive interface following latest design standards
Dark/Light Themes: Automatic theme switching based on system preferences
Biometric Authentication: Secure login with fingerprint or face recognition
Offline Support: Full functionality without internet connection
Multi-language: Vietnamese and English language support
Security & Sync

End-to-End Encryption: Secure financial data protection
Cloud Backup: Automatic synchronization with Firebase
Local Storage: Option to store data locally only
Privacy Focused: No collection of personal financial data
📱 Screenshots

Dashboard & Overview

Dashboard	Statistics	Budget View
https://via.placeholder.com/300x600/4CAF50/FFFFFF?text=Dashboard	https://via.placeholder.com/300x600/2196F3/FFFFFF?text=Statistics	https://via.placeholder.com/300x600/FF9800/FFFFFF?text=Budget
Transaction Management

Add Transaction	Categories	Recurring
https://via.placeholder.com/300x600/9C27B0/FFFFFF?text=Add+Transaction	https://via.placeholder.com/300x600/3F51B5/FFFFFF?text=Categories	https://via.placeholder.com/300x600/00BCD4/FFFFFF?text=Recurring
Settings & AI Features

Settings	AI Assistant	Reports
https://via.placeholder.com/300x600/607D8B/FFFFFF?text=Settings	https://via.placeholder.com/300x600/009688/FFFFFF?text=AI+Assistant	https://via.placeholder.com/300x600/E91E63/FFFFFF?text=Reports
🏗️ Architecture

Tech Stack

Language: Kotlin 1.9.0
UI Framework: Jetpack Compose 1.5.0
Architecture: Clean Architecture with MVVM
Dependency Injection: Dagger Hilt
Local Database: Room
Remote Database: Firebase Firestore
Authentication: Firebase Auth
Image Processing: ML Kit
Notifications: WorkManager + AlarmManager
Project Structure
Design Patterns
Repository Pattern: Abstraction between data sources and business logic
Observer Pattern: Reactive UI updates with StateFlow
Factory Pattern: Object creation for complex entities
Strategy Pattern: Different algorithms for data processing
Builder Pattern: Complex object construction
🚀 Installation

Prerequisites

Android Studio 2022.2.1 or higher
JDK 17 or higher
Android SDK 33 (API Level 33)
Kotlin 1.9.0
Step 1: Clone the Repository

bash
git clone https://github.com/Wendy84205/Expensemanagementapp.git
cd Expensemanagementapp
Step 2: Open in Android Studio

Launch Android Studio
Select "Open an Existing Project"
Navigate to the cloned directory
Click "Open"
Step 3: Configure Firebase

Go to Firebase Console
Create a new project or select existing one
Click "Add app" and select Android
Register your app with package name: com.example.financeapp
Download the google-services.json file
Place it in the app/ directory of your project
Step 4: Configure API Keys

Create a secrets.properties file in the root directory:

properties
# OpenAI API (for AI features)
OPENAI_API_KEY=your_openai_api_key_here

# Currency Exchange API (optional)
CURRENCY_API_KEY=your_exchange_api_key_here

# OCR API (optional for receipt scanning)
OCR_API_KEY=your_ocr_api_key_here
🏗️ Building the Project

Debug Build

bash
./gradlew assembleDebug
Release Build

bash
./gradlew assembleRelease
Run Tests

bash
./gradlew test
Generate APK

bash
./gradlew assembleRelease
⚙️ Configuration

Environment Variables

The following environment variables can be configured:

Variable	Description	Required	Default
ENABLE_CLOUD_SYNC	Enable Firebase sync	No	true
ENABLE_AI_FEATURES	Enable AI-powered features	No	true
ENABLE_OCR	Enable receipt scanning	No	true
ENABLE_BIOMETRICS	Enable biometric authentication	No	true
DEFAULT_CURRENCY	Default currency	No	VND
Build Variants

debug: Development build with debugging enabled
release: Production build with optimization
staging: Pre-production testing build
Product Flavors

free: Basic features without premium functionality
premium: All features unlocked
🧪 Testing

Unit Tests

bash
./gradlew testDebugUnitTest
Instrumentation Tests

bash
./gradlew connectedDebugAndroidTest
UI Tests

The project includes comprehensive UI tests using Espresso and Compose testing frameworks.

📊 Performance Metrics

App Size

APK Size: ~15MB
Install Size: ~25MB
Performance

Cold Start: < 2 seconds
Screen Transitions: < 300ms
Database Operations: < 100ms
Image Processing: < 2 seconds
Battery Impact

Background Usage: Minimal
Sync Operations: Optimized for battery life
Wake Locks: Used only for critical operations
🤝 Contributing

We welcome contributions from the community! Here's how you can help:

Reporting Issues

Check if the issue already exists in the Issues section
Create a new issue with a clear title and description
Include steps to reproduce, expected behavior, and actual behavior
Add screenshots or videos if applicable
Submitting Changes

Fork the repository
Create a feature branch: git checkout -b feature/your-feature-name
Make your changes
Run tests: ./gradlew test
Commit changes: git commit -m 'Add some feature'
Push to branch: git push origin feature/your-feature-name
Create a Pull Request
Code Style Guidelines

Follow Kotlin coding conventions
Use meaningful variable and function names
Add comments for complex logic
Write unit tests for new features
Update documentation as needed
Pull Request Checklist

Code follows project style guidelines
All tests pass
No new warnings introduced
Documentation updated
Screenshots added for UI changes
📈 Development Roadmap

Version 1.0 (Current)

Basic transaction management
Budget tracking
Recurring expenses
Multi-language support
Dark/light theme
Version 1.1 (In Progress)

Bank account integration
Investment tracking
Advanced analytics
Family/shared budgets
Receipt storage
Version 1.2 (Planned)

Tax calculation
Financial planning tools
Export to accounting software
Advanced AI predictions
Web dashboard
Version 2.0 (Future)

Cross-platform (iOS, Web)
Advanced security features
API for developers
Plugin system
Community features
🔧 Troubleshooting

Common Issues

Firebase Connection Issues

Ensure google-services.json is in the correct location
Check Firebase project configuration
Verify package name matches Firebase registration
Check internet connection
Build Failures

Clean project: ./gradlew clean
Invalidate caches in Android Studio
Update dependencies
Check JDK version
App Crashes

Check logcat for error messages
Verify device compatibility
Clear app data
Reinstall the app
Getting Help

Check the Wiki
Search existing Issues
Create a new issue for bugs
Use GitHub Discussions for questions
📚 Documentation

API Documentation

Firebase API Reference
Jetpack Compose Documentation
Room Database Guide
User Guides

Getting Started Guide
User Manual
FAQ
Developer Guides

Architecture Overview
Code Style Guide
Testing Guide
📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

text
MIT License

Copyright (c) 2024 Wendy

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
🙏 Acknowledgments

Jetpack Compose team for the amazing UI framework
Firebase team for backend services
OpenAI for AI capabilities
All contributors who have helped improve this project
📞 Contact

GitHub: Wendy84205
Email: wendy84205@gmail.com
Issues: GitHub Issues
🌟 Support the Project

If you find this project useful, please consider:

Giving it a ⭐ on GitHub
Sharing it with others
Contributing code or documentation
Reporting bugs and suggesting features
Disclaimer: This application is for personal finance management only. It is not a certified financial advisory tool. Always consult with a professional financial advisor for important financial decisions.
