# NYC Taxi Airflow Orchestration (Exercise 6)

## Description
Orchestration automatisée du pipeline Big Data avec Apache Airflow.

## Structure
```
ex06_airflow/
├── dags/
│   └── nyc_taxi_pipeline.py  # DAG principal
├── logs/                      # Logs Airflow (auto-généré)
├── plugins/                   # Plugins personnalisés
└── README.md
```

## Pipeline DAG
```
[Data Retrieval] → [Data Ingestion] → [Data Warehouse] → [ML Preprocessing] → [ML Training] → [Complete]
     Ex01              Ex02               Ex03                Ex05              Ex05
```

## Accès
- **URL**: https://airflow.haroun-joudi.com
- **Login**: airflow
- **Password**: airflow

## Commandes
```bash
# Démarrer Airflow
cd ~/big_data
docker-compose up -d airflow-postgres airflow-webserver airflow-scheduler

# Voir les logs
docker-compose logs -f airflow-webserver

# Arrêter
docker-compose down
```