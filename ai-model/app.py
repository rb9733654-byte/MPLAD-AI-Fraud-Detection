"""Local FastAPI service for the synthetic MPLAD anomaly model.

Start with the existing environment:
    .\\.venv\\Scripts\\uvicorn.exe app:app --host 127.0.0.1 --port 8000

The service returns anomaly-review signals only; it does not make fraud
determinations. The model artifact is loaded once during application startup.
"""

from __future__ import annotations

from contextlib import asynccontextmanager
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI, Request
from pydantic import BaseModel, ConfigDict, field_validator

from predict import MODEL_PATH, determine_risk


class ProjectFeatures(BaseModel):
    """Numerical inputs in the exact order and names stored with the model."""

    model_config = ConfigDict(extra="forbid")

    sanctioned_amount: float
    released_amount: float
    actual_expenditure: float
    completion_percentage: float
    project_duration_days: float
    delay_days: float
    contractor_previous_projects: float
    contractor_avg_cost: float
    expenditure_per_completion_percent: float
    projects_by_contractor: float
    is_completed: float

    @field_validator("*")
    @classmethod
    def values_must_be_finite(cls, value: float) -> float:
        if not np.isfinite(value):
            raise ValueError("must be a finite numeric value")
        return value


def load_model_once() -> dict:
    """Load and validate the existing joblib artifact without retraining it."""
    if not MODEL_PATH.is_file():
        raise RuntimeError(f"Model artifact not found: {Path(MODEL_PATH).name}")

    artifact = joblib.load(MODEL_PATH)
    if not isinstance(artifact, dict) or {"pipeline", "feature_columns"} - artifact.keys():
        raise RuntimeError("Model artifact has an invalid format.")
    return artifact


@asynccontextmanager
async def lifespan(application: FastAPI):
    artifact = load_model_once()
    application.state.pipeline = artifact["pipeline"]
    application.state.feature_columns = artifact["feature_columns"]
    yield


app = FastAPI(
    title="MPLAD Synthetic Anomaly Service",
    version="1.0.0",
    description="Local anomaly-review scoring for synthetic MPLAD-style project data.",
    lifespan=lifespan,
)


@app.get("/health")
def health(request: Request) -> dict[str, str]:
    """Confirm the service is ready and the existing model is in memory."""
    return {
        "status": "ok",
        "model_status": "loaded",
        "model_type": type(request.app.state.pipeline.named_steps["model"]).__name__,
    }


@app.post("/predict")
def predict(features: ProjectFeatures, request: Request) -> dict[str, str | float]:
    """Return the existing Isolation Forest's anomaly-review result for one project."""
    feature_columns: list[str] = request.app.state.feature_columns
    values = features.model_dump()
    input_frame = pd.DataFrame(
        [{feature: values[feature] for feature in feature_columns}],
        columns=feature_columns,
    )
    pipeline = request.app.state.pipeline
    prediction = int(pipeline.predict(input_frame)[0])
    decision_value = float(pipeline.decision_function(input_frame)[0])
    anomaly_score = round(float(np.clip(50 - (decision_value * 100), 0, 100)), 2)
    risk_level, anomaly_status = determine_risk(anomaly_score)
    if prediction == -1 and risk_level == "LOW":
        risk_level, anomaly_status = "MEDIUM", "Potential Anomaly"

    return {
        "anomaly_status": anomaly_status,
        "anomaly_score": anomaly_score,
        "risk_level": risk_level,
        "model_output": "unusual" if prediction == -1 else "within_expected_pattern",
    }
