# MPLAD AI Fraud Detection: Project Report

## 1. Abstract

MPLAD AI Fraud Detection is a local, full-stack prototype for monitoring fictional MPLAD-style project records. The system combines a browser dashboard, a Java Spring Boot REST API, a MySQL database, and a Python FastAPI machine-learning service. It provides an on-demand anomaly review for a selected project by passing eleven project-related numerical features to an Isolation Forest model.

The returned result contains an anomaly status, anomaly score, risk level, and model output to help a reviewer prioritize attention. The system is designed to identify potential anomalies and risk indicators for human review. It does not establish, prove, or confirm fraud.

## 2. Introduction

Project-monitoring data often combines financial, delivery, completion, and contractor information. Reviewing a large set of records manually can make it difficult to identify projects whose combination of indicators is unusual. This project demonstrates a local architecture in which project data can be listed in a dashboard and passed through an anomaly-detection service on demand.

The implementation uses fictional demo data at every data layer. The MySQL seed records and the model-training dataset are MPLAD-style examples only; no real constituency, contractor, or project information is represented.

## 3. Problem Statement

There is a need for a structured way to prioritize project records for review when expenditure, completion, delay, or contractor-related patterns appear unusual. A manual process alone may not consistently surface records whose values are atypical relative to the available dataset.

The problem addressed by this project is therefore not the automatic determination of fraud. It is the generation and presentation of potential anomaly signals that support a human review process.

## 4. Motivation

The project is motivated by the value of combining a clear project-monitoring interface with a lightweight, explainable workflow for anomaly review. A dashboard makes project data accessible, while an unsupervised model can provide a consistent numerical signal for further examination. Keeping the output as a review indicator preserves the role of human judgment and avoids treating a model result as a factual allegation.

## 5. Objectives

- Store and retrieve fictional MPLAD-style project records through a MySQL-backed REST API.
- Provide a browser dashboard for viewing, filtering, and inspecting project records.
- Integrate the Spring Boot backend with a local FastAPI AI service.
- Submit the model's exact eleven numerical inputs for a selected project.
- Return an anomaly status, anomaly score, risk level, and model output to the frontend.
- Handle a missing project and an unavailable AI service with appropriate HTTP responses.
- Keep analysis results transient; the current implementation does not save them to MySQL.
- Present all AI outputs as potential anomalies or risk indicators requiring human review.

## 6. Proposed Solution

The proposed solution is a four-part local application:

1. A frontend dashboard loads project data from Spring Boot and presents project-level actions.
2. A Spring Boot application manages MySQL access and exposes `/api/projects` endpoints.
3. A FastAPI service loads the existing Joblib model artifact and scores validated numeric input at `/predict`.
4. MySQL stores project records in the `mplad_db.projects` table.

When the user chooses **Analyze risk**, the frontend calls the Spring endpoint for the selected database record. The backend retrieves the record, maps it to the model contract, calls FastAPI, and returns the model response. No analysis table or persistence path exists in the current implementation.

## 7. System Architecture

```text
┌──────────────────────────────────────────────────────────────┐
│ Frontend: HTML, CSS, vanilla JavaScript                       │
│ dashboard.html → GET /api/projects                            │
│                  → POST /api/projects/{id}/analyze            │
└───────────────────────────┬──────────────────────────────────┘
                            │ HTTP, localhost:8080
                            v
┌──────────────────────────────────────────────────────────────┐
│ Spring Boot: com.mplad.fraud_detection                        │
│ ProjectController / ProjectService / ProjectAnalysisService   │
└───────────────┬──────────────────────────────┬───────────────┘
                │ JPA                          │ HTTP POST /predict
                v                              v
┌─────────────────────────────┐    ┌───────────────────────────┐
│ MySQL 8                     │    │ FastAPI, 127.0.0.1:8000    │
│ mplad_db.projects            │    │ Joblib Isolation Forest    │
└─────────────────────────────┘    └───────────────────────────┘
```

The backend AI client uses the local base address `http://127.0.0.1:8000`. The frontend uses `http://localhost:8080/api/projects`.

## 8. Technology Stack

| Area | Implemented technology |
| --- | --- |
| Frontend | HTML, CSS, vanilla JavaScript |
| Frontend visuals | Custom DOM/CSS-rendered charts and summaries |
| Backend | Java 17, Spring Boot 3.5.0, Spring Web, Spring Data JPA |
| Build and tests | Maven and Maven Wrapper |
| Database | MySQL 8 and MySQL Connector/J |
| AI service | Python, FastAPI, Uvicorn, Pydantic |
| Data and ML | Pandas, NumPy, scikit-learn, Joblib |
| Anomaly model | scikit-learn `IsolationForest` in a pipeline with `SimpleImputer` |

The current source does not include a Chart.js dependency; dashboard visuals are generated directly in JavaScript and CSS.

## 9. Project Modules

### 9.1 Frontend

The frontend is contained in `frontend/` and includes `index.html`, `dashboard.html`, `style.css`, and `script.js`.

Implemented dashboard functionality includes:

- Loading projects from `GET http://localhost:8080/api/projects`.
- Transforming API fields for display of project identity, location, allocation, expenditure, and progress state.
- Search and filters for project text, status, risk level, and category.
- Summary cards and custom visual summaries for project status, risk distribution, and budget versus expenditure.
- A project detail modal.
- **Analyze risk** actions in the project table and detail modal.
- Display of `anomaly_status`, `anomaly_score`, `risk_level`, and `model_output` after an analysis request.

The dashboard explicitly describes AI results as review signals and not fraud determinations.

### 9.2 Spring Boot Backend

The backend is in `backend/mplad-backend/`. Its package root is `com.mplad.fraud_detection`.

The implementation contains:

- `ProjectController` under `/api/projects`.
- `ProjectService` and `ProjectRepository` for JPA-backed project operations.
- `ProjectAnalysisService` to coordinate database lookup and AI analysis.
- `PythonAnomalyClient` to call the local FastAPI `/predict` endpoint.
- `Project` as the JPA entity mapped to the `projects` table.
- `ApiExceptionHandler` for analysis-specific 404 and 503 responses.

At startup, `MpladBackendApplication` opens a database connection through the configured data source to make local configuration problems visible. JPA schema generation is disabled; the schema is managed separately by SQL.

### 9.3 MySQL Database

The database schema is in `database/schema.sql`. It creates the `mplad_db` database and a `projects` table, then inserts ten fictional demo records.

The table is designed around a generated numeric primary key, a unique project identifier, project context, financial and delivery data, model inputs, and timestamps. The SQL schema includes indexes for district, constituency, and project type, as well as checks for completion percentage and completion state.

### 9.4 Python AI/ML Service

The AI service is in `ai-model/`.

- `app.py` provides the FastAPI service and loads the existing model once at application startup.
- `predict.py` provides prediction helpers and risk interpretation.
- `train_model.py` contains synthetic data generation and Isolation Forest training logic.
- `data/mplad_data.csv` is the fictional/demo training dataset.
- `model/anomaly_model.pkl` is the saved Joblib model artifact used by the service.

The FastAPI input model rejects extra fields and validates that every numerical value is finite before scoring.

## 10. Database Design

The application uses database `mplad_db` and table `projects`.

| Column group | Fields |
| --- | --- |
| Primary and external identity | `id`, `project_id` |
| Project context | `constituency`, `district`, `project_type` |
| Financial fields | `sanctioned_amount`, `released_amount`, `actual_expenditure` |
| Delivery fields | `completion_percentage`, `project_duration_days`, `delay_days`, `is_completed` |
| Contractor and derived fields | `contractor_previous_projects`, `contractor_avg_cost`, `expenditure_per_completion_percent`, `projects_by_contractor` |
| Audit fields | `created_at`, `updated_at` |

`id` is an auto-incrementing primary key. `project_id` is unique and serves as the stable external project identifier. Financial values in the demo schema are described as lakh rupees. The model analysis reads project values but does not add or update database columns.

## 11. AI/ML Methodology

### 11.1 Dataset

The training dataset is `ai-model/data/mplad_data.csv`. It is explicitly fictional and MPLAD-style. The training module can generate reproducible synthetic records; it creates no fraud labels and describes unusual combinations only for model variation. The standard synthetic generator is configured for 800 rows and injects unusual cost, delay, or contractor patterns into approximately every tenth generated record.

No real-world accuracy metric, validation metric, or fraud-classification metric is present in the current implementation.

### 11.2 Features used by the model

Spring Boot and FastAPI use the following eleven numerical features in the exact order stored with the model artifact:

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

The backend maps the database Boolean `is_completed` to integer `1` or `0` before sending the prediction request. FastAPI accepts numeric values for this feature, like every other field in its request model.

### 11.3 Isolation Forest

The project uses an unsupervised `IsolationForest`. This choice fits the prototype because the synthetic dataset has no labels indicating confirmed fraud. The model is intended to identify observations that are unusual relative to the supplied feature distribution.

The training pipeline applies `SimpleImputer(strategy="median")` before the Isolation Forest. The configured model parameters are `contamination=0.10`, `random_state=42`, and `n_estimators=200`.

### 11.4 Training process

`train_model.py` performs the following process:

1. Optionally generates synthetic data when `--generate-data` is supplied.
2. Loads the CSV and verifies the required columns.
3. Converts the eleven model fields to numeric values.
4. Fits the imputer and Isolation Forest pipeline.
5. Stores the pipeline, feature-column order, model type, and random state in `ai-model/model/anomaly_model.pkl` using Joblib.

The existing saved model artifact is used for service operation. Retraining or regenerating data is not required for normal application use and would change project artifacts.

### 11.5 Prediction process

At FastAPI startup, the model artifact is loaded into application state. For `POST /predict`, the service creates a one-row Pandas data frame in the stored feature order, calls the pipeline's `predict` and `decision_function` methods, and transforms the decision value into a clipped 0–100 anomaly score.

The response is:

```json
{
  "anomaly_status": "...",
  "anomaly_score": 0.0,
  "risk_level": "...",
  "model_output": "..."
}
```

`model_output` is `unusual` when the Isolation Forest prediction is `-1`; otherwise it is `within_expected_pattern`.

### 11.6 Risk and anomaly interpretation

The code maps the anomaly score to review-oriented categories:

| Score range | Risk Level | Anomaly Status |
| --- | --- | --- |
| Below 50 | `LOW` | `Normal` |
| 50 to 74 | `MEDIUM` | `Potential Anomaly` |
| 75 and above | `HIGH` | `High Risk Anomaly` |

If the model marks a record unusual but the calculated score would otherwise be low, the service promotes the result to `MEDIUM` / `Potential Anomaly`. These are review priorities, not confirmed fraud classifications.

## 12. Backend REST API

`ProjectController` is mounted at `/api/projects` on the Spring Boot server at `localhost:8080`.

| Method | Endpoint | Implemented purpose |
| --- | --- | --- |
| `GET` | `/api/projects` | Return all project records |
| `GET` | `/api/projects/{id}` | Return a project by database ID |
| `POST` | `/api/projects/{id}/analyze` | Retrieve a project and return its AI analysis |
| `GET` | `/api/projects/district/{district}` | Return projects by district |
| `GET` | `/api/projects/type/{projectType}` | Return projects by project type |
| `GET` | `/api/projects/completed` | Return completed projects |
| `POST` | `/api/projects` | Create a project record |
| `DELETE` | `/api/projects/{id}` | Delete a project record |

The FastAPI service runs locally at `127.0.0.1:8000` and exposes:

| Method | Endpoint | Implemented purpose |
| --- | --- | --- |
| `GET` | `/health` | Report that the service and model are loaded |
| `POST` | `/predict` | Return an anomaly-review result for the eleven validated features |

## 13. Frontend Dashboard

The dashboard is served from `frontend/dashboard.html`. It uses the projects API rather than the static demo adapter for its loaded project list. It derives presentation-level status and initial display risk values from project fields, then replaces the selected project's in-session risk display with the returned AI analysis after an **Analyze risk** action.

The project modal displays:

- Project ID and location.
- Financial position relative to allocation.
- Current risk signal.
- Anomaly status.
- Anomaly score out of 100.
- Risk level.
- Model output.

The analysis display remains client-side for the current browser session; it is not written to MySQL.

## 14. End-to-End Data Flow

1. The browser loads `dashboard.html` and requests the project list from `GET /api/projects`.
2. Spring Boot uses Spring Data JPA to read `projects` from MySQL.
3. The frontend renders project information, filtering controls, summaries, and action buttons.
4. The reviewer selects **Analyze risk** for a project.
5. The frontend calls `POST /api/projects/{id}/analyze`.
6. `ProjectAnalysisService` retrieves the project by database ID and creates `AiPredictionRequest` from the entity fields.
7. `PythonAnomalyClient` sends that request as JSON to `POST http://127.0.0.1:8000/predict`.
8. FastAPI validates the input, scores it with the loaded pipeline, and returns the four analysis values.
9. Spring Boot returns that response to the browser.
10. The dashboard displays the result as a potential anomaly/risk signal for review.

## 15. Testing and Validation

The following results were verified against the implemented local application:

| Validation activity | Verified result |
| --- | --- |
| Maven build and test | `mvnw.cmd clean test` completed with `BUILD SUCCESS` |
| Spring Boot to MySQL | Spring Boot connected successfully to `mplad_db` |
| Project API | `GET /api/projects` returned 10 demo records |
| FastAPI health check | `GET /health` returned HTTP `200` |
| FastAPI prediction | `POST /predict` returned HTTP `200` for valid input |
| End-to-end analysis | `POST /api/projects/1/analyze` returned HTTP `200` |
| Missing project | An analysis request for a missing project returned HTTP `404` |
| AI unavailable | With FastAPI unavailable, the analysis endpoint returned HTTP `503` |
| Frontend JavaScript | `node --check frontend/script.js` passed |
| Frontend integration | Static verification confirmed the projects API and analysis action references |

The backend test class currently verifies that the Spring application context loads. The listed HTTP checks were completed against the local services and fictional demo data. No model accuracy or real-world fraud-detection performance is claimed.

## 16. Error Handling

The implemented analysis flow provides explicit error handling:

- When a database ID does not identify a project, `ProjectAnalysisService` raises a project-not-found exception and `ApiExceptionHandler` returns HTTP `404` with the message `Project not found.`
- When the FastAPI service cannot be reached or does not return a body, `PythonAnomalyClient` maps the client error to an AI-service-unavailable exception. `ApiExceptionHandler` returns HTTP `503` with a safe client-facing message.
- FastAPI rejects extra prediction fields and rejects non-finite numerical values through its Pydantic request model.
- The frontend catches failed analysis requests and displays the returned safe message in the analysis area.

## 17. Security and Privacy Considerations

- The repository uses fictional/demo data and does not claim to contain real project or personal data.
- Database credentials are not documented in this report. Local application property files are excluded by `.gitignore` to reduce the risk of committing credentials.
- The backend is configured for local development and permits the explicitly listed local frontend origins through CORS.
- There is no implemented authentication, authorization, encryption-in-transit configuration, audit log, or role-based access control. These are important requirements before any non-demo use.
- A model output should not be treated as evidence of misconduct. Reviewers would need authorized data, supporting documents, and appropriate governance procedures for any real deployment.

## 18. Limitations

- The dataset and seeded records are fictional, so the application provides no evidence of real-world effectiveness.
- The Isolation Forest is an anomaly detector rather than a fraud classifier and has no confirmed-fraud labels or evaluation metrics in this project.
- The FastAPI URL is hard-coded in the backend client to the local loopback address `127.0.0.1:8000`.
- Analysis results are not persisted, versioned, or auditable in the database.
- The project has no user authentication or reviewer workflow.
- The frontend's dashboard-level risk display includes client-side presentation calculations that are separate from the FastAPI analysis response.
- The system has only been validated as a local application; no production deployment is implemented or claimed.

## 19. Future Enhancements

The following items are future work and are not implemented in the current project:

- Persist analysis history, model version, timestamp, reviewer decision, and supporting notes.
- Add a structured Review Required workflow and audit trail.
- Externalize the AI service URL and database settings through secure environment-specific configuration.
- Add authentication, role-based access control, and stronger API validation.
- Add automated API, service, repository, and frontend tests.
- Add monitoring, model evaluation, model governance, and version management.
- Add authorized data-ingestion, privacy, and data-quality controls before considering any real data source.
- Improve accessibility and add richer dashboard reporting where it remains clear that model signals require human review.

## 20. Conclusion

MPLAD AI Fraud Detection demonstrates a complete local path from project data to a human-readable anomaly-review result. The frontend, Spring Boot API, MySQL database, and FastAPI service are integrated so a reviewer can request an analysis for a selected fictional project and inspect the returned Anomaly Score, Risk Level, and anomaly status.

The prototype's principal contribution is an implementation pattern for AI-assisted review prioritization, not automated fraud judgment. Its current limitations and fictional data make it suitable for learning, demonstration, and further controlled development rather than real-world decision-making.

## 21. Disclaimer

This project uses fictional/demo MPLAD-style data. Terms such as `Potential Anomaly`, `High Risk Anomaly`, `Risk Level`, `Anomaly Score`, and `Review Required` describe model-assisted review priority only. They do not confirm fraud, wrongdoing, legal liability, or facts about any real person, project, constituency, contractor, or organization.
