# NYC Taxi Fare Prediction Service - Exercise 5

## 🎯 Objectif
Créer un modèle de Machine Learning pour prédire le prix total (`total_amount`) d'une course de taxi NYC avec **RMSE < 10**.

## ✅ Résultats Obtenus
- **RMSE Test:** 6.56 (**✅ < 10**)
- **R²:** 0.91
- **MAE:** 3.50
- **Algorithme:** Random Forest Regressor

---

## 📋 Architecture

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   MinIO     │─────▶│ Preprocessing │─────▶│   Training  │
│ 36.6M rows  │      │  (200k sample)│      │ Random Forest│
└─────────────┘      └──────────────┘      └──────┬──────┘
                                                   │
                                                   ▼
                                            ┌─────────────┐
                                            │ Saved Model │
                                            │   .joblib   │
                                            └──────┬──────┘
                                                   │
                                                   ▼
                                            ┌─────────────┐
                                            │  Streamlit  │
                                            │  Interface  │
                                            └─────────────┘
```

---

## 🐳 Déploiement Docker (Recommandé - Sécurisé)

### Avantages:
- ✅ Isolation complète du serveur
- ✅ Limite mémoire (4GB max)
- ✅ Redémarrage automatique
- ✅ Pas de risque de crash serveur

### Lancer le service:
```bash
cd ~/Projects/projet_big_data_cytech_25/ex05_ml_prediction_service

# Démarrer le service ML en Docker
docker-compose up -d

# Voir les logs
docker-compose logs -f ml-service
```

**Interface disponible:** https://ml-service.haroun-joudi.com/

### Arrêter le service:
```bash
docker-compose down
```

---

## 🖥️ Installation Serveur (Alternative - Sans Docker)

⚠️ **Non recommandé:** Pas d'isolation, risque de surcharge mémoire

### Pré-requis:
1. **Installer uv** (gestionnaire d'environnements Python):
```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
source $HOME/.local/bin/env
```

2. **Créer l'environnement virtuel:**
```bash
cd ~/Projects/projet_big_data_cytech_25/ex05_ml_prediction_service
uv venv
source .venv/bin/activate
uv pip install .
```

---

## 🚀 Exécution (Étape par Étape)

### Étape 1: Prétraitement des Données
```bash
cd src
python preprocessing.py
```

**Durée:** ~5 minutes  
**Sortie:** `../data/cleaned_data.parquet` (200k échantillon)

**Ce que ça fait:**
- Charge données validées depuis MinIO (`s3://nyc-validated/2023/`)
- Nettoie les outliers (total_amount > 500, trip_distance > 100, etc.)
- Feature engineering (hour, day_of_week, is_weekend)
- Échantillonnage stratifié (200k lignes pour protéger le serveur)

### Étape 2: Entraînement du Modèle
```bash
python train.py
```

**Sortie:** `../models/taxi_fare_model.joblib`

**Ce que ça fait:**
- Charge les données nettoyées
- Split 80/20 train/test
- Entraîne Random Forest (100 arbres, max_depth=15)
- Évalue et sauvegarde le modèle

**Métriques affichées:**
```
RMSE Test: 6.56
MAE: 3.50
R²: 0.91
```

### Étape 3: Interface Streamlit
```bash
streamlit run app.py --server.port 8501 --server.address 0.0.0.0
```

**Accès:** https://ml-service.haroun-joudi.com/

**Fonctionnalités:**
- Formulaire interactif pour saisir détails du trajet
- Prédiction instantanée du prix
- Visualisation des inputs
- Interface en français 🇫🇷

### Étape 4: Tests (Optionnel)
```bash
pytest tests/ -v
```

---

## 📊 Structure du Projet

```
ex05_ml_prediction_service/
├── docker-compose.yml       # Déploiement Docker sécurisé
├── pyproject.toml           # Dépendances (géré par uv)
├── README.md                # Ce fichier
├── src/
│   ├── preprocessing.py     # Nettoyage + feature engineering
│   ├── train.py             # Entraînement Random Forest
│   ├── app.py               # Interface Streamlit
│   ├── inference.py         # CLI pour prédictions
│   └── eda.py               # Analyse exploratoire (optionnel)
├── tests/
│   ├── test_preprocessing.py
│   └── test_inference.py
├── data/                    # Données nettoyées (généré)
│   └── cleaned_data.parquet
└── models/                  # Modèles sauvegardés (généré)
    └── taxi_fare_model.joblib
```

---

## 🔬 Détails Techniques

### Features Utilisées (7 features)
| Feature | Type | Description |
|---------|------|-------------|
| `trip_distance` | Float | Distance en miles |
| `passenger_count` | Int | Nombre de passagers (1-6) |
| `PULocationID` | Int | Zone de prise en charge (1-265) |
| `DOLocationID` | Int | Zone de dépose (1-265) |
| `hour` | Int | Heure de départ (0-23) |
| `day_of_week` | Int | Jour (0=Lundi, 6=Dimanche) |
| `is_weekend` | Bool | 1 si weekend, 0 sinon |

### Hyperparamètres Random Forest
```python
RandomForestRegressor(
    n_estimators=100,        # 100 arbres
    max_depth=15,            # Profondeur max (évite overfitting)
    min_samples_split=10,    # Régularisation
    n_jobs=-1,               # Utilise tous les CPU
    random_state=42          # Reproductibilité
)
```

### Échantillonnage Stratifié
Le script utilise **200k échantillons** au lieu de 36.6M pour:
- ✅ Éviter crash serveur (RAM limitée)
- ✅ Accélérer l'entraînement (10 min vs plusieurs heures)
- ✅ Maintenir distribution des prix (stratified sampling)

**Bins de prix:**
- 0-15$ (trajets courts)
- 15-25$ (trajets moyens)
- 25-40$ (trajets longs)
- 40-100$ (trajets très longs)
- 100$+ (aéroports/outliers)

---

## 🐛 Dépannage

### Problème: Erreur de connexion MinIO
```
S3FS Error: Unable to connect to endpoint
```

**Solution:** Vérifier que MinIO est démarré:
```bash
docker ps | grep minio
# Si absent:
cd ~/Projects/projet_big_data_cytech_25
docker-compose up -d minio
```

### Problème: Modèle non trouvé
```
FileNotFoundError: ../models/taxi_fare_model.joblib
```

**Solution:** Entraîner le modèle d'abord:
```bash
cd src
python preprocessing.py  # D'abord
python train.py          # Ensuite
```

### Problème: Serveur crash pendant preprocessing
**Cause:** Trop de données en mémoire

**Solution:** Utiliser Docker avec limite mémoire (déjà configuré dans docker-compose.yml)

---

## 📈 Performance du Modèle

### Métriques Finales
| Métrique | Train | Test | Commentaire |
|----------|-------|------|-------------|
| **RMSE** | 5.89 | 6.56 | ✅ < 10 (objectif atteint) |
| **MAE** | 3.12 | 3.50 | Erreur moyenne acceptable |
| **R²** | 0.93 | 0.91 | Très bon fit |
| **Overfitting** | +11% | | Acceptable |

### Distribution des Erreurs
- **50% des prédictions:** erreur < $3.50
- **90% des prédictions:** erreur < $10
- **Outliers:** Quelques trajets aéroports mal prédits

---

## ✅ Critères de Réussite (PDF)

| Critère | Requis | Obtenu | Statut |
|---------|--------|--------|--------|
| RMSE < 10 | ✅ | 6.56 | ✅ PASS |
| Utilisation `uv` | ✅ | ✅ | ✅ PASS |
| Python natif interdit | ✅ | ✅ | ✅ PASS |
| Interface utilisateur | Bonus | Streamlit | ✅ BONUS |

---

## 🎓 Améliorations Possibles

### Court Terme:
- [ ] Ajouter XGBoost pour comparer performance
- [ ] Implémenter cross-validation (5-fold)
- [ ] Ajouter feature importance visualization

### Moyen Terme:
- [ ] API FastAPI pour intégration externe
- [ ] Déploiement production avec Kubernetes
- [ ] A/B testing de différents modèles

---