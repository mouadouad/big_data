from airflow import DAG
from airflow.operators.bash import BashOperator
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
    description='End-to-end NYC Taxi data pipeline',
    schedule_interval=timedelta(days=1),
    catchup=False,
) as dag:

    # Task 1: Upload Data (Calls Ex01 via sbt)
    # Note: We use ssh to execute on the host or assume airflow has access. 
    # For simplicity in this Docker setup, we'll placeholder a simpler task 
    # or assume the user runs Airflow to trigger these.
    # A robust way is to use DockerOperator, but let's stick to BashOperator printing for now
    # to demonstrate the flow as requested by the exercise structure.
    
    t1 = BashOperator(
        task_id='data_retrieval',
        bash_command='echo "Simulating Data Retrieval (Ex01)..." && sleep 5',
    )

    t2 = BashOperator(
        task_id='data_ingestion',
        bash_command='echo "Simulating Data Ingestion (Ex02)..." && sleep 5',
    )

    t3 = BashOperator(
        task_id='model_training',
        bash_command='echo "Simulating Model Training (Ex05)..." && sleep 5',
    )

    t1 >> t2 >> t3
