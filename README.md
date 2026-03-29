# AtmoSense: Real-Time Weather & AI Insights

## Overview

A feature-rich Android weather application that combines real-time weather data with advanced AI capabilities. This app provides personalized user experiences, intelligent weather insights, and dynamic UI customization, supporting over 40,000 cities worldwide.

## Demo

[Watch the application demo](https://drive.google.com/file/d/1VqYiaP8VnDN_AfZOpJ6qXD9okpAePTVd/view)

## Key Highlights

- **Real-Time Data & Multi-City Tracking**: Leverages the OpenWeather API to provide up-to-date metrics (temperature, humidity, wind, conditions) across a 40,000+ global city SQLite database.
- **Dynamic AI UI Theming**: Integrates **Google Gemini 2.5 Pro** to allow users to describe desired UI themes in natural language, automatically generating and applying WCAG AA compliant color schemes.
- **Intelligent Weather Insights**: Features an AI-powered Q&A system via **Gemini 2.5 Flash** that contextually generates practical questions and answers based on live weather data.
- **Photorealistic Visualizations**: Dynamically synthesizes realistic city images matching current weather conditions and time-of-day utilizing the **Gemini 2.0 Flash Image** model.
- **Robust Quality Assurance**: Achieved **95%+ code coverage** through an extensive Espresso UI testing suite featuring custom `IdlingResource` synchronization for deterministic validation of asynchronous network and AI workflows.

## Technical Stack & Architecture

### Core Technologies

- **Language**: Java 8
- **Platform**: Android SDK (API 29-34)
- **Database**: Room Database (ORM for SQLite)
- **Networking**: OkHttp3 (Asynchronous HTTP client) & Gson
- **AI/LLM Pipelines**: Google Gemini 2.5 Pro, 2.5 Flash, 2.0 Flash Image
- **External APIs**: OpenWeather API, Google Gemini API
- **Testing Frameworks**: Espresso (UI), JUnit (Unit), JaCoCo (Coverage Analysis)

### Architecture Highlights

- **Multi-Model LLM Orchestration**: Coordinated three distinct AI models within a unified mobile experience, implementing prompt engineering, rigorous JSON extraction logic, and robust fallback mechanisms to handle API latency and failure states.
- **Thread-Safe Processing Pipeline**: Engineered efficient background thread management using `ExecutorService` and `Handler` paradigms for seamless execution of network calls, AI generation, and database batch imports (inserting 500 records per batch) without blocking the primary UI thread.
- **Accessibility-First Implementation**: Developed validation logic that mathematically verifies color contrast ratios (4.5:1 minimum) for AI-generated hex codes, ensuring all dynamic UI themes strictly adhere to WCAG AA accessibility standards.
- **Automated Test Synchronization**: Engineered a specialized testing architecture utilizing Espresso's `IdlingResource` to deterministically synchronize assertions with unpredictable API and LLM network latencies, effectively eliminating test flakiness.
- **State Management & Data Persistence**: Implemented Android `AccountManager` for secure, persistent user authentication and isolated tenant-level data storage, encompassing individualized city lists and custom theming profiles.
- **Design Patterns Applied**: Extensively utilized established software engineering patterns, including Singleton (Database Client configuration), DAO (City data access layer), Factory, Observer/Callback (handling LLM API asynchronous responses), and Template Method (automated theming via `BaseActivity`).

## Contributors

Zhen Bi, Divey Anand, Aryaman Nasare, Aaron Lee, Niranjan Kalaiselvan, Sabelle Huang, Satej Sukthankar, Yuang Cai
