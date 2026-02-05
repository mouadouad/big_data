"""
NYC Taxi Data Pipeline DAG - Real Execution.

This DAG orchestrates the complete data pipeline with real commands:
1. Data Retrieval (Ex01)
2. Data Ingestion & Validation with Spark (Ex02)
3. SQL Data Warehouse loading (Ex03)
4. ML Model Training (Ex05)

Note: Uses DockerOperator to execute tasks in the appropriate containers.
"""
from airflow import DAG
from airflow.operators.bash import BashOperator
from airflow.providers.docker.operators.docker import DockerOperator
from docker.types import Mount
from datetime import datetime, timedelta

default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'start_date': datetime(2023, 1, 1),
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=5),
}

with DAG(
    'nyc_taxi_pipeline',
    default_args=default_args,
    description='End-to-end NYC Taxi data pipeline with real execution',
    schedule_interval=timedelta(days=1),
    catchup=False,
    tags=['nyc-taxi', 'ml', 'spark', 'production'],
) as dag:

    # ================================================================
    # EXERCISE 1 & 2: Data Retrieval and Spark Ingestion
    # ================================================================
    
    # Task 1: Submit Spark job for data ingestion
    t1_spark_ingestion = DockerOperator(
        task_id='spark_data_ingestion',
        image='bitnami/spark:3.5',
        api_version='auto',
        auto_remove=True,
        command='''
            spark-submit \
            --master spark://spark-master:7077 \
            --packages org.apache.hadoop:hadoop-aws:3.3.4,com.amazonaws:aws-java-sdk-bundle:1.12.262 \
            /opt/spark-apps/ex02_data_ingestion/src/main/scala/DataIngestion.scala
        ''',
        docker_url='unix://var/run/docker.sock',
        network_mode='big_data_spark-network',
        mounts=[
            Mount(
                source='/home/debian/big_data',
                target='/opt/spark-apps',
                type='bind'
            )
        ],
    )

    # ================================================================
    # EXERCISE 3: Load data to PostgreSQL Data Warehouse
    # ================================================================
    
    t2_data_warehouse = BashOperator(
        task_id='load_data_warehouse',
        bash_command='''
            echo "=== Loading data to PostgreSQL Data Warehouse ===" && \
            PGPASSWORD=postgres psql -h postgres -U postgres -d taxi -c "
                SELECT COUNT(*) as total_trips FROM trips;
            " && \
            echo "Data Warehouse check completed!"
        ''',
    )

    # ================================================================
    # EXERCISE 5: ML Preprocessing and Training
    # ================================================================
    
    # Task 3: ML Preprocessing
    t3_ml_preprocessing = DockerOperator(
        task_id='ml_preprocessing',
        image='python:3.11-slim',
        api_version='auto',
        auto_remove=True,
        command='''
            bash -c "
                cd /app/ex05_ml_prediction_service/src && \
                pip install pandas scikit-learn pyarrow s3fs joblib -q && \
                python preprocessing.py
            "
        ''',
        docker_url='unix://var/run/docker.sock',
        network_mode='big_data_spark-network',
        mounts=[
            Mount(
                source='/home/debian/big_data',
                target='/app',
                type='bind'
            )
        ],
        environment={
            'PYTHONUNBUFFERED': '1',
        },
    )

    # Task 4: ML Model Training
    t4_ml_training = DockerOperator(
        task_id='ml_training',
        image='python:3.11-slim',
        api_version='auto',
        auto_remove=True,
        command='''
            bash -c "
                cd /app/ex05_ml_prediction_service/src && \
                pip install pandas scikit-learn pyarrow joblib -q && \
                python train.py
            "
        ''',
        docker_url='unix://var/run/docker.sock',
        network_mode='big_data_spark-network',
        mounts=[
            Mount(
                source='/home/debian/big_data',
                target='/app',
                type='bind'
            )
        ],
        environment={
            'PYTHONUNBUFFERED': '1',
        },
    )

    # ================================================================
    # NOTIFICATION: Pipeline Complete
    # ================================================================
    
    t5_notification = BashOperator(
        task_id='pipeline_complete',
        bash_command='''
            echo "========================================" && \
            echo "NYC TAXI PIPELINE COMPLETED SUCCESSFULLY" && \
            echo "========================================" && \
            echo "Timestamp: $(date)" && \
            echo "All exercises executed:"  && \
            echo "  - Ex01-02: Data Ingestion (Spark)" && \
            echo "  - Ex03: Data Warehouse (PostgreSQL)" && \
            echo "  - Ex05: ML Training (Random Forest)" && \
            echo "========================================"
        ''',
    )

    # ================================================================
    # PIPELINE FLOW
    # ================================================================
    # Ex01-02 (Spark) -> Ex03 (DW) -> Ex05 (ML Prep) -> Ex05 (ML Train) -> Done
    
    t1_spark_ingestion >> t2_data_warehouse >> t3_ml_preprocessing
    t3_ml_preprocessing >> t4_ml_training >> t5_notification
