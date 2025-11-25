AI Doctor Starter Project (backend + frontend)

Backend:
- Spring Boot 3.1.1, Java 17
- Run:
  cd backend
  mvn clean package
  mvn spring-boot:run

Frontend (dev):
- React (create-react-app style)
- Run:
  cd frontend
  npm install
  npm start

Notes:
- Backend uses H2 in-memory DB so it starts out-of-the-box.
- OpenAI, OCR, and Transcription implementations are placeholders (mock).
- app.upload-dir property is configured in backend/src/main/resources/application.properties
