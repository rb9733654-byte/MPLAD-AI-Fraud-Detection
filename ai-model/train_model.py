"""Train a lightweight anomaly model using synthetic MPLAD-style project data.

This module uses no fraud labels. It identifies project records that are unusual
relative to the rest of the synthetic dataset and should be reviewed by a person.
"""

from __future__ import annotations

import argparse
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline

BASE_DIRECTORY = Path(__file__).resolve().parent
DATA_PATH = BASE_DIRECTORY / "data" / "mplad_data.csv"
MODEL_PATH = BASE_DIRECTORY / "model" / "anomaly_model.pkl"

FEATURE_COLUMNS = [
    "sanctioned_amount",
    "released_amount",
    "actual_expenditure",
    "completion_percentage",
    "project_duration_days",
    "delay_days",
    "contractor_previous_projects",
    "contractor_avg_cost",
    "expenditure_per_completion_percent",
    "projects_by_contractor",
    "is_completed",
]

REQUIRED_COLUMNS = [
    "project_id",
    "constituency",
    "district",
    "project_type",
    *FEATURE_COLUMNS,
]


def generate_synthetic_data(row_count: int = 800, random_state: int = 42) -> None:
    """Create a reproducible, entirely fictional MPLAD-style dataset.

    A small subset has unusual cost, delay, or completion combinations so an
    unsupervised model has meaningful variation to learn from. No fraud label is
    created and none of the names or locations identify real projects.
    """
    if row_count < 500:
        raise ValueError("Synthetic dataset must contain at least 500 rows.")

    rng = np.random.default_rng(random_state)
    project_types = np.array(["Road", "Water", "Education", "Health", "Lighting", "Community"])
    rows: list[dict[str, object]] = []

    for index in range(1, row_count + 1):
        sanctioned = float(np.clip(rng.normal(58, 22), 12, 150))
        completion = float(np.clip(rng.normal(67, 26), 3, 100))
        released = float(np.clip(sanctioned * rng.uniform(0.55, 1.0), 5, sanctioned))
        expected_spend = released * (completion / 100) * rng.normal(1.03, 0.08)
        expenditure = float(np.clip(expected_spend, 1, released * 1.08))
        duration = int(rng.integers(90, 730))
        delay = int(max(0, rng.normal(24, 26)))
        previous_projects = int(rng.integers(1, 25))
        contractor_average = float(np.clip(sanctioned * rng.normal(0.98, 0.20), 8, 180))
        contractor_project_count = int(rng.integers(1, 12))

        # Approximately 10% of rows receive a synthetic unusual pattern.
        if index % 10 == 0:
            pattern = index % 3
            if pattern == 0:
                expenditure = released * rng.uniform(1.18, 1.48)
                completion = float(rng.uniform(12, 55))
            elif pattern == 1:
                delay = int(rng.integers(260, 620))
                completion = float(rng.uniform(18, 65))
            else:
                contractor_project_count = int(rng.integers(28, 65))
                contractor_average = sanctioned * rng.uniform(2.1, 3.2)

        rows.append({
            "project_id": f"DEMO-MPL-{index:04d}",
            "constituency": f"Demo Constituency {(index - 1) % 20 + 1:02d}",
            "district": f"Sample District {(index - 1) % 35 + 1:02d}",
            "project_type": str(rng.choice(project_types)),
            "sanctioned_amount": round(sanctioned, 2),
            "released_amount": round(released, 2),
            "actual_expenditure": round(expenditure, 2),
            "completion_percentage": round(completion, 2),
            "project_duration_days": duration,
            "delay_days": delay,
            "contractor_previous_projects": previous_projects,
            "contractor_avg_cost": round(contractor_average, 2),
            "expenditure_per_completion_percent": round(expenditure / max(completion, 1), 3),
            "projects_by_contractor": contractor_project_count,
            "is_completed": int(completion >= 99),
        })

    DATA_PATH.parent.mkdir(parents=True, exist_ok=True)
    pd.DataFrame(rows).to_csv(DATA_PATH, index=False)
    print(f"Created {row_count} rows of synthetic demo data: {DATA_PATH}")


def load_and_validate_data() -> pd.DataFrame:
    """Load the CSV and confirm it contains usable training columns."""
    if not DATA_PATH.is_file():
        raise FileNotFoundError(f"Dataset not found: {DATA_PATH}")
    try:
        data = pd.read_csv(DATA_PATH)
    except (OSError, pd.errors.ParserError) as error:
        raise ValueError(f"Could not read dataset: {error}") from error

    missing_columns = set(REQUIRED_COLUMNS) - set(data.columns)
    if missing_columns:
        raise ValueError(f"Dataset is missing required columns: {sorted(missing_columns)}")
    if data.empty:
        raise ValueError("Dataset contains no records.")

    prepared = data.copy()
    for column in FEATURE_COLUMNS:
        prepared[column] = pd.to_numeric(prepared[column], errors="coerce")
    if prepared[FEATURE_COLUMNS].dropna(how="all").empty:
        raise ValueError("No valid numerical feature values were found.")
    return prepared


def train_and_save_model(data: pd.DataFrame) -> int:
    """Train Isolation Forest and save model plus metadata needed for prediction."""
    pipeline = Pipeline([
        ("imputer", SimpleImputer(strategy="median")),
        ("model", IsolationForest(contamination=0.10, random_state=42, n_estimators=200)),
    ])
    feature_data = data[FEATURE_COLUMNS]
    pipeline.fit(feature_data)
    predictions = pipeline.predict(feature_data)
    anomaly_count = int((predictions == -1).sum())

    artifact = {
        "pipeline": pipeline,
        "feature_columns": FEATURE_COLUMNS,
        "model_type": "IsolationForest",
        "random_state": 42,
    }
    try:
        MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
        joblib.dump(artifact, MODEL_PATH)
    except (OSError, ValueError) as error:
        raise RuntimeError(f"Could not save model artifact: {error}") from error
    return anomaly_count


def main() -> None:
    parser = argparse.ArgumentParser(description="Train the synthetic MPLAD anomaly model.")
    parser.add_argument("--generate-data", action="store_true", help="Regenerate the synthetic demo dataset first.")
    arguments = parser.parse_args()

    if arguments.generate_data:
        generate_synthetic_data()
    data = load_and_validate_data()
    anomaly_count = train_and_save_model(data)

    print(f"Training records: {len(data)}")
    print(f"Features used: {', '.join(FEATURE_COLUMNS)}")
    print("Model type: IsolationForest")
    print(f"Detected training anomalies: {anomaly_count}")
    print(f"Model artifact saved to: {MODEL_PATH}")


if __name__ == "__main__":
    main()
