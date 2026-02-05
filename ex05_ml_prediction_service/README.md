# NYC Taxi Fare Prediction Service

## Description
Modèle de Machine Learning pour prédire le prix d'une course de taxi à New York.
- **Algorithme** : Random Forest Regressor
- **RMSE** : ~6.5 (< 10 ✅)
- **R²** : 0.91

## Structure du Projet
```
ex05_ml_prediction_service/
├── pyproject.toml          # Dépendances (géré par uv)
├── src/
│   ├── eda.py              # Analyse exploratoire des données
│   ├── preprocessing.py    # Nettoyage et feature engineering
│   ├── train.py            # Entraînement du modèle
│   ├── inference.py        # Prédiction CLI
│   └── app.py              # Interface Streamlit
├── tests/
│   ├── test_preprocessing.py
│   └── test_inference.py
├── data/                   # Données nettoyées (générées)
└── models/                 # Modèles sauvegardés
```

## Installation (sur le serveur)
```bash
cd ~/big_data/ex05_ml_prediction_service

# Installer uv
curl -LsSf https://astral.sh/uv/install.sh | sh
source $HOME/.local/bin/env

# Créer et activer l'environnement
uv venv
source .venv/bin/activate

# Installer les dépendances
uv pip install .
```

## Exécution (dans l'ordre)

### 1. Prétraitement et nettoyage
```bash
cd src
python preprocessing.py
```

### 2. Entraînement du modèle
```bash
python train.py
```

### 3. Inférence CLI
```bash
python inference.py --distance 5.2 --passengers 2 --pickup 132 --dropoff 161
```

### 4. Interface Streamlit (Optionnel)
```bash
streamlit run app.py --server.port 8501
```
Accès : http://localhost:8501

## Tests et Qualité
```bash
# Tests unitaires
pytest tests/ -v

# Vérification PEP 8
flake8 src/ --max-line-length=100
```

## Métriques du Modèle
| Métrique | Valeur |
|----------|--------|
| RMSE Test | 6.56 |
| MAE | 3.50 |
| R² | 0.91 |
| Overfitting | +11% ✅ |