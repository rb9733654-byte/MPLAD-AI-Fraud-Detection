# MPLAD AI Fraud Detection

MPLAD AI Fraud Detection is a local prototype for monitoring fictional MPLAD-style project records. It combines a browser dashboard, a Spring Boot REST API, MySQL project data, and a FastAPI-based machine-learning service to surface potential anomalies and risk indicators for human review.

> The included dataset and database seed records are fictional/demo data. Anomaly outputs are review signals only; they do not prove, confirm, or determine fraud.

## Project overview

The application presents project allocation, expenditure, completion, delay, and contractor-pattern information in a dashboard. A reviewer can request an on-demand AI analysis for a project. The backend maps the project record to the model's required numerical features, calls the local AI service, and returns an anomaly status, anomaly score, risk level, and model output without persisting the result to MySQL.

## Problem statement

Project-monitoring teams need a practical way to prioritize records for review when spending, progress, delay, and contractor patterns look unusual. Reviewing every record manually can be slow and inconsistent. This prototype demonstrates how a data-driven anomaly signal can help focus review attention while keeping human judgment central.

## Objectives

- Present MPLAD-style project information in a single dashboard.
- Store and retrieve project records through a Spring Boot API and MySQL.
- Score a selected project with an unsupervised anomaly model.
- Return understandable review indicators: **Potential Anomaly**, **Anomaly Score**, **Risk Level**, and model output.
- Keep AI results transient; the current implementation does not write them back to MySQL.
- Avoid fraud determinations: potential anomalies require review and supporting evidence.

## Key features

- Project list, project details, filters, portfolio summary cards, and DOM-rendered visual summaries.
- MySQL-backed `projects` API with list, lookup, filter, create, and delete operations.
- On-demand `POST /api/projects/{id}/analyze` integration from Spring Boot to FastAPI.
- FastAPI health endpoint and validated prediction input contract.
- Isolation Forest anomaly scoring with eleven numerical project features.
- Dashboard **Analyze risk** actions in both the table and project detail modal.
- Clear 404 response for an unknown project and 503 response when the AI service is unavailable.
- CORS support for local frontend development on ports 3000, 5173, and 5500 (plus the listed loopback origins).

## System architecture

```text
Browser dashboard (HTML, CSS, JavaScript)
          |
          | GET /api/projects
          | POST /api/projects/{id}/analyze
          v
Spring Boot API :8080  <----------------->  MySQL 8 / mplad_db / projects
          |
          | POST /predict (11 numerical features)
          v
FastAPI AI service :8000  --->  Joblib model artifact
                                  Isolation Forest pipeline
```

## Technology stack

| Layer | Technology used |
| --- | --- |
| Frontend | HTML, CSS, and vanilla JavaScript |
| Dashboard visuals | Custom DOM/CSS-rendered bar and comparison charts (Chart.js is not a current dependency) |
| Backend | Java 17, Spring Boot 3.5.0, Spring Web, Spring Data JPA, Maven |
| Database | MySQL 8, `mplad_db`, `projects` table |
| AI service | Python, FastAPI, Uvicorn, Pydantic |
| ML/data | Pandas, NumPy, scikit-learn Isolation Forest, Joblib |

## Project structure

```text
MPLAD-AI-Fraud-Detection/
├── ai-model/
│   ├── app.py                    # FastAPI /health and /predict service
│   ├── predict.py                # Shared prediction helpers and risk mapping
│   ├── train_model.py            # Synthetic-data generator and model trainer
│   ├── requirements.txt
│   ├── data/mplad_data.csv       # Fictional MPLAD-style training data
│   └── model/anomaly_model.pkl   # Saved Isolation Forest artifact
├── backend/mplad-backend/
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd
│   └── src/
│       ├── main/java/com/mplad/fraud_detection/
│       │   ├── controller/
│       │   ├── dto/
│       │   ├── entity/
│       │   ├── repository/
│       │   └── service/
│       └── main/resources/application.properties
├── database/schema.sql           # MySQL schema and ten fictional seed records
├── frontend/
│   ├── index.html
│   ├── dashboard.html
│   ├── script.js
│   └── style.css
├── .gitignore
└── README.md
```

## How the system works

1. MySQL stores project records in `mplad_db.projects`.
2. The Spring Boot application (`com.mplad.fraud_detection`) exposes those records at `http://localhost:8080/api/projects`.
3. The dashboard loads project records from that API.
4. When a reviewer chooses **Analyze risk**, the dashboard sends `POST /api/projects/{id}/analyze` to Spring Boot.
5. Spring Boot loads the requested project, maps its fields to the eleven model inputs, and calls `http://127.0.0.1:8000/predict`.
6. FastAPI returns an anomaly-review result. Spring Boot passes it back to the dashboard; no analysis result is stored in MySQL.
7. The dashboard displays the anomaly status, anomaly score, risk level, and model output for review.

## AI/ML approach

The existing saved model is an unsupervised scikit-learn `IsolationForest` pipeline. Training uses a median imputer followed by an Isolation Forest configured with `contamination=0.10`, `random_state=42`, and `n_estimators=200`. The model artifact is stored with its feature-column metadata in `ai-model/model/anomaly_model.pkl` and is loaded once when FastAPI starts.

The model uses these eleven numerical features, in this exact order:

1. `sanctioned_amount`
2. `released_amount`
3. `actual_expenditure`
4. `completion_percentage`
5. `project_duration_days`
6. `delay_days`
7. `contractor_previous_projects`
8. `contractor_avg_cost`
9. `expenditure_per_completion_percent`
10. `projects_by_contractor`
11. `is_completed`

The service converts the Isolation Forest decision value into a 0–100 **Anomaly Score** and maps it to review-oriented labels:

| Anomaly Score | Risk Level | Anomaly Status |
| --- | --- | --- |
| Below 50 | `LOW` | `Normal` |
| 50–74 | `MEDIUM` | `Potential Anomaly` |
| 75 and above | `HIGH` | `High Risk Anomaly` |

An Isolation Forest result marked unusual is never downgraded to `LOW`; it is presented as at least `MEDIUM` / `Potential Anomaly`. These labels are priorities for review, not fraud findings.

## Backend REST API

The backend package root is `com.mplad.fraud_detection`. `ProjectController` is mounted at `/api/projects`.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/projects` | List all projects |
| `GET` | `/api/projects/{id}` | Get one project by database ID |
| `POST` | `/api/projects/{id}/analyze` | Request AI anomaly analysis for one project |
| `GET` | `/api/projects/district/{district}` | Filter projects by district |
| `GET` | `/api/projects/type/{projectType}` | Filter projects by project type |
| `GET` | `/api/projects/completed` | List completed projects |
| `POST` | `/api/projects` | Create a project record |
| `DELETE` | `/api/projects/{id}` | Delete a project record |

For the analysis endpoint:

- An unknown project ID returns `404` with `{"message":"Project not found."}`.
- An unavailable FastAPI service returns `503` with a safe client-facing message.

## FastAPI AI service

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `http://127.0.0.1:8000/health` | Confirm the service and model are loaded |
| `POST` | `http://127.0.0.1:8000/predict` | Score one validated set of eleven numerical features |

## Example AI analysis response

`POST /api/projects/1/analyze` returns the same response contract as the AI service:

```json
{
  "anomaly_status": "Normal",
  "anomaly_score": 38.26,
  "risk_level": "LOW",
  "model_output": "within_expected_pattern"
}
```

Scores can vary when the model artifact or input project changes. A response with `Potential Anomaly` or `High Risk Anomaly` indicates that review is required; it is not a confirmed fraud result.

## Database

The MySQL schema is in `database/schema.sql`. It creates the `mplad_db` database and the `projects` table, then inserts ten fictional demo records.

Important `projects` columns include:

- Identity and context: `id`, `project_id`, `constituency`, `district`, `project_type`
- Financial/progress fields: `sanctioned_amount`, `released_amount`, `actual_expenditure`, `completion_percentage`, `project_duration_days`, `delay_days`
- Contractor and model inputs: `contractor_previous_projects`, `contractor_avg_cost`, `expenditure_per_completion_percent`, `projects_by_contractor`, `is_completed`
- Audit timestamps: `created_at`, `updated_at`

Financial sample values are expressed in lakh rupees. JPA schema generation is disabled (`spring.jpa.hibernate.ddl-auto=none`), so the schema is managed through the SQL file.

## Frontend dashboard

Open `frontend/dashboard.html` through a local static server. The dashboard:

- Fetches projects from `http://localhost:8080/api/projects`.
- Maps database fields into presentation fields for status, allocation, expenditure, and dashboard summaries.
- Provides search and status, risk-level, and category filters.
- Provides **Details** and **Analyze risk** actions for each project.
- Displays the returned anomaly status, anomaly score, risk level, and model output in the project modal.

## Installation and setup

### Prerequisites

- MySQL 8 with the `mysql` command-line client available.
- Java Development Kit 17.
- Python with `venv` support.
- A modern browser.

### 1. Create and seed the database

From the repository root, run the schema script. Enter your MySQL password only when prompted; do not place it in a command or commit it.

```powershell
Get-Content database\schema.sql | mysql -u root -p
```

The script creates `mplad_db`, creates `projects`, and inserts the demo records. If those records already exist, do not rerun the seed insert unchanged because `project_id` is unique.

### 2. Configure the backend database connection

Create or update `backend/mplad-backend/src/main/resources/application.properties` locally with your MySQL connection details. Keep credentials local and out of version control. The application requires properties for the JDBC URL, username, password, and MySQL driver; the database name is `mplad_db`.

The repository's `.gitignore` excludes this local property file to help prevent credentials from being committed.

### 3. Install Python dependencies

```powershell
Set-Location ai-model
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
Set-Location ..
```

The saved artifact at `ai-model/model/anomaly_model.pkl` is already used by the service. Retraining is not required to run the application. `train_model.py` can regenerate synthetic data and overwrite the artifact, so run it only when intentionally retraining.

## How to run

Start the services in separate terminals, in this order.

### MySQL

Ensure the local MySQL server is running and that `mplad_db` has been created with `database/schema.sql`.

### FastAPI AI service (port 8000)

```powershell
Set-Location ai-model
.\.venv\Scripts\uvicorn.exe app:app --host 127.0.0.1 --port 8000
```

Confirm readiness at `http://127.0.0.1:8000/health`.

### Spring Boot backend (port 8080)

```powershell
Set-Location backend\mplad-backend
.\mvnw.cmd spring-boot:run
```

The backend verifies its MySQL connection at startup and calls the AI service at `http://127.0.0.1:8000` when analysis is requested.

### Frontend (port 5500)

From the repository root:

```powershell
Set-Location frontend
py -m http.server 5500
```

Then open [http://localhost:5500/dashboard.html](http://localhost:5500/dashboard.html). Port 5500 is included in the backend CORS configuration.

## Important API endpoints

```text
GET  http://127.0.0.1:8000/health
POST http://127.0.0.1:8000/predict

GET  http://localhost:8080/api/projects
GET  http://localhost:8080/api/projects/{id}
POST http://localhost:8080/api/projects/{id}/analyze
GET  http://localhost:8080/api/projects/district/{district}
GET  http://localhost:8080/api/projects/type/{projectType}
GET  http://localhost:8080/api/projects/completed
POST http://localhost:8080/api/projects
DELETE http://localhost:8080/api/projects/{id}
```

## Testing and validation

The following checks are available for local validation:

```powershell
# Spring Boot compile and test suite
Set-Location backend\mplad-backend
.\mvnw.cmd clean test

# Frontend JavaScript syntax check (run from repository root)
node --check frontend\script.js
```

With the MySQL server, FastAPI service, and Spring Boot service running, validate:

- `GET /health` returns `200` and reports a loaded model.
- `POST /predict` returns `200` for the eleven valid numerical features.
- `GET /api/projects` returns the seeded project records.
- `POST /api/projects/{id}/analyze` returns `200` and the four analysis fields.
- A missing project analysis request returns `404`.
- Stopping FastAPI causes the backend analysis request to return `503`.

## Limitations

- All included data is fictional/demo MPLAD-style data.
- The model is an unsupervised anomaly detector, not a fraud classifier or investigative system.
- Analysis results are transient and are not stored in MySQL.
- The AI service base URL is currently hard-coded to local loopback address `127.0.0.1:8000` in the backend client.
- The prototype has no authentication, authorization, audit workflow, or production deployment configuration.
- The dashboard's portfolio-level risk display includes client-side presentation calculations; it is distinct from the on-demand AI result.

## Future improvements

- Persist analysis history, timestamps, and reviewer decisions with an audit trail.
- Add authentication, role-based access control, and secure configuration management.
- Externalize the AI service URL and other environment-specific settings.
- Add API, service, repository, and frontend automated tests.
- Add reviewer notes, evidence attachments, and a structured Review Required workflow.
- Introduce model monitoring, versioning, validation, and governance before any real-world use.
- Replace fictional demo data only through authorized, privacy-aware, and validated data pipelines.

## Disclaimer

This is a demonstration project built with fictional MPLAD-style data. Its **Anomaly Score**, **Risk Level**, `Potential Anomaly`, and `High Risk Anomaly` labels indicate where a reviewer may need to look more closely. They do not establish misconduct, fraud, legal liability, or facts about any real person, contractor, constituency, or project.

## Author and project information

- **Project:** MPLAD AI Fraud Detection
- **Application name:** MPLAD Monitor
- **Purpose:** AI-assisted prioritization of fictional project records for human review
- **Status:** Local development prototype

Author details are not specified in the repository source.
