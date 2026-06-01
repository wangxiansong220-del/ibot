# ibot frontend

This frontend talks to the Java backend for authentication, Qwen chat, RAG history, and admin operations.

## Run locally

1. Install dependencies:
   `npm install`
2. Start the backend in `cd C:\Users\86136\Desktop\ibot\backend`:
   `mvn spring-boot:run`
3. Start the frontend in `cd C:\Users\86136\Desktop\ibot\front`:
   `npm run dev`

The app will open on [http://localhost:3000](http://localhost:3000) and proxy API requests to the backend on `http://127.0.0.1:8080`.

## Default admin account

The backend seeds an admin account when none exists:

- email: `admin@ibot.local`
- password: `Admin@123456`

Change these values in `C:\Users\86136\Desktop\ibot\backend\src\main\resources\application.properties` before using the app outside local development.
