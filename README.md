Finance Management App 💰

https://img.shields.io/badge/Kotlin-1.9.0-blue.svg
https://img.shields.io/badge/Compose-1.5.0-brightgreen.svg
https://img.shields.io/badge/License-MIT-yellow.svg

A smart personal finance management application with a modern interface, supporting expense tracking, budgeting, and recurring transactions.

🎯 Key Features

💳 Transaction Management

Add/Edit/Delete income and expenses
Custom category classification
Bill scanning from images (AI-powered)
Recurring expenses automation
Export data to Excel/PDF
📊 Statistics & Reports

Visual charts with time-based analysis
Spending analysis by category
Weekly/Monthly/Yearly reports
Real-time income vs. expense comparison
🎨 Smart Interface

Modern Material Design 3
Dark/Light theme support
Multi-language (Vietnamese/English)
Smart notifications and reminders
🔒 Security & Sync

Multi-platform login (Google, Facebook, Phone)
Cloud sync with Firebase
Backup & Restore functionality
Encrypted sensitive data
🚀 Technology Stack

Frontend

Jetpack Compose - Modern UI toolkit
Material Design 3 - Design system
Compose Navigation - Navigation
MVVM Architecture - Clean architecture
Backend & Database

Firebase Firestore - NoSQL database
Firebase Authentication - User authentication
Firebase Storage - File storage
Room Database - Local database
AI & ML

ML Kit - Text recognition
OpenAI API - Expense analysis
TensorFlow Lite - Image processing
Utilities

Coroutines - Asynchronous programming
Flow/StateFlow - State management
Dagger/Hilt - Dependency injection
DataStore - Preferences storage
📱 Main Screens

1. Dashboard

text
📊 Financial Overview
├── Current balance
├── Monthly income/expense
├── Remaining budget
└── Highlighted transactions
2. Transaction Management

text
💳 Add Transaction
├── Type (Income/Expense)
├── Amount
├── Category
├── Date & Time
└── Notes + Attachments
3. Recurring Expenses

text
🔄 Recurring Expenses
├── Frequency (Daily/Weekly/Monthly/Yearly)
├── Start/End date
├── Automatic recording
└── Notification reminders
4. Statistics & Reports

text
📈 Analytics Dashboard
├── Time-based analysis
├── Category breakdown
├── Period comparison
└── Report export
5. Settings

text
⚙️ App Configuration
├── Account settings
├── Language selection
├── Theme customization
├── Backup management
└── About app
🏗️ Project Structure

text
financeapp/
├── 📁 screen/                    # Application screens
│   ├── auth/                    # Authentication
│   ├── main/                    # Main screens
│   │   ├── dashboard/          # Dashboard
│   │   ├── transaction/        # Transactions
│   │   ├── budget/             # Budgeting
│   │   └── statistics/         # Statistics
│   ├── settings/               # Settings
│   └── features/               # Features
│       ├── ai/                 # AI Assistant
│       ├── recurring/          # Recurring expenses
│       └── category/           # Categories
│
├── 📁 viewmodel/               # ViewModels
│   ├── auth/                   # Authentication
│   ├── transaction/            # Transactions
│   ├── budget/                 # Budget
│   ├── user/                   # User
│   └── ai/                     # AI
│
├── 📁 data/                    # Data layer
│   ├── models/                 # Data classes
│   ├── repository/             # Repositories
│   ├── local/                  # Local database
│   └── remote/                 # Remote data
│
├── 📁 components/              # UI Components
│   ├── ui/                     # Reusable components
│   ├── theme/                  # Theme configuration
│   └── utils/                  # Component utilities
│
├── 📁 utils/                   # Utilities
│   ├── language/               # Multi-language
│   └── notification/           # Notifications
│
└── 📁 navigation/              # Navigation
🛠️ Installation & Setup

System Requirements

Android Studio Flamingo (2022.2.1) or higher
JDK 17
Android SDK 33+
Kotlin 1.9.0
Step 1: Clone the repository

bash
git clone https://github.com/Wendy84205/Expensemanagementapp.git
cd Expensemanagementapp
Step 2: Configure Firebase

Create a project on Firebase Console
Add an Android app
Download the google-services.json file
Place it in the app/ directory
Step 3: Configure API keys

Create a secrets.properties file in the root directory:

properties
# OpenAI API
OPENAI_API_KEY=your_openai_api_key_here

# Bank integration (optional)
BANK_API_KEY=your_bank_api_key_here
Step 4: Build & Run

bash
./gradlew assembleDebug
# Or open in Android Studio and run
📸 Screenshots

Login	Dashboard	Add Transaction
https://screenshots/login.png	https://screenshots/dashboard.png	https://screenshots/add_transaction.png
Statistics	Recurring Expenses	Settings
https://screenshots/statistics.png	https://screenshots/recurring.png	https://screenshots/settings.png
🤖 AI Features

1. AI Butler Assistant

Financial chatbot assistant
Smart spending analysis
Savings recommendations
Income/expense forecasting
2. Bill Scanning

Text recognition from images
Automatic transaction data filling
Smart category classification
Image attachment storage
3. AI Analysis

Anomaly detection in spending
Optimal budget recommendations
Financial risk alerts
Smart savings goals
📈 Roadmap

V1.0 (Current)

✅ Basic transaction management
✅ Chart statistics
✅ Recurring expenses
✅ Multi-language support
V1.1 (Upcoming)

🚧 Bank integration
🚧 PDF/Excel report generation
🚧 Payment reminders
🚧 Automatic backup
V1.2 (Future)

🔄 Investment & Stock tracking
🔄 Financial goals
🔄 Community features
🔄 Web Dashboard
👥 Contributing

Contributions are welcome! Please:

Fork the project
Create a new branch (git checkout -b feature/AmazingFeature)
Commit your changes (git commit -m 'Add some AmazingFeature')
Push to the branch (git push origin feature/AmazingFeature)
Open a Pull Request
Commit Convention

text
feat:     Add new feature
fix:      Fix bug
docs:     Update documentation
style:    Format code (no logic change)
refactor: Refactor code
test:     Add/update tests
chore:    Update build, dependencies
📝 License

This project is licensed under the MIT License. See the LICENSE file for details.

📞 Contact & Support

Author: Wendy

GitHub: @Wendy84205
Email: wendy84205@gmail.com
Support:

📖 Documentation
🐛 Report Issues
💡 Feature Requests
🌟 Star History

https://api.star-history.com/svg?repos=Wendy84205/Expensemanagementapp&type=Date

⭐ If you find this project useful, please give it a star on GitHub!

"Smart Finance Management - Secure Future"
