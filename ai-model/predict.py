"""Reusable prediction helper for the synthetic MPLAD anomaly model."""

from __future__ import annotations

from pathlib import Path
from typing import Any, Mapping

import joblib
import numpy as np
import pandas as pd

BASE_DIRECTORY = Path(__file__).resolve().parent
MODEL_PATH = BASE_DIRECTORY / "model" / "anomaly_model.pkl"


def load_model_artifact() -> dict[str, Any]:
    """Safely load the trained pipeline and its feature metadata."""
    if not MODEL_PATH.is_file():
        raise FileNotFoundError(f"Model artifact not found: {MODEL_PATH}. Run train_model.py first.")
    try:
        artifact = joblib.load(MODEL_PATH)
    except Exception as error:
        raise RuntimeError(f"Could not load model artifact: {error}") from error
    if not isinstance(artifact, dict) or "pipeline" not in artifact or "feature_columns" not in artifact:
        raise ValueError("Model artifact has an invalid format.")
    return artifact


def determine_risk(anomaly_score: float) -> tuple[str, str]:
    """Map a 0–100 unusualness score to review-friendly labels.

    Scores below 50 are LOW/Normal; 50–74 are MEDIUM/Potential Anomaly;
    scores of 75 and above are HIGH/High Risk Anomaly. These are review
    priorities only and are not fraud determinations.
    """
    if anomaly_score >= 75:
        return "HIGH", "High Risk Anomaly"
    if anomaly_score >= 50:
        return "MEDIUM", "Potential Anomaly"
    return "LOW", "Normal"


def predict_project(project: Mapping[str, Any]) -> dict[str, Any]:
    """Score one project dictionary containing the model's numerical features."""
    if not isinstance(project, Mapping):
        raise TypeError("Project input must be a dictionary-like mapping.")
    artifact = load_model_artifact()
    features: list[str] = artifact["feature_columns"]
    missing = [feature for feature in features if feature not in project]
    if missing:
        raise ValueError(f"Project input is missing required features: {missing}")

    try:
        values = {feature: float(project[feature]) for feature in features}
    except (TypeError, ValueError) as error:
        raise ValueError("All model features must contain numeric values.") from error
    if not np.isfinite(list(values.values())).all():
        raise ValueError("Model features must be finite numeric values.")

    input_frame = pd.DataFrame([values], columns=features)
    pipeline = artifact["pipeline"]
    prediction = int(pipeline.predict(input_frame)[0])
    decision_value = float(pipeline.decision_function(input_frame)[0])

    # Isolation Forest's decision boundary is zero. Negating it turns more
    # unusual observations into higher scores; clipping keeps the result simple.
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


def demo_project() -> dict[str, float]:
    """Return a fictional example with an unusual spend and delay combination."""
    return {
        "sanctioned_amount": 65.0,
        "released_amount": 58.0,
        "actual_expenditure": 79.0,
        "completion_percentage": 35.0,
        "project_duration_days": 420.0,
        "delay_days": 310.0,
        "contractor_previous_projects": 7.0,
        "contractor_avg_cost": 71.0,
        "expenditure_per_completion_percent": 2.257,
        "projects_by_contractor": 9.0,
        "is_completed": 0.0,
    }


if __name__ == "__main__":
    try:
        print(predict_project(demo_project()))
    except (FileNotFoundError, RuntimeError, TypeError, ValueError) as error:
        print(f"Prediction demo failed: {error}")
