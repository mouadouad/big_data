"""
Streamlit frontend for NYC Taxi fare prediction.

This application provides a user-friendly interface to:
- Input trip details
- Get fare predictions from the trained model
- Visualize prediction results

Usage:
    streamlit run app.py
"""
import streamlit as st
import joblib
import pandas as pd
import os

# Page configuration
st.set_page_config(
    page_title="NYC Taxi Fare Predictor",
    page_icon="🚕",
    layout="centered"
)

# Custom CSS for styling
st.markdown("""
<style>
    .main-header {
        font-size: 2.5rem;
        font-weight: bold;
        text-align: center;
        color: #FFD700;
        margin-bottom: 2rem;
    }
    .prediction-box {
        background-color: #1E1E1E;
        padding: 2rem;
        border-radius: 10px;
        text-align: center;
        border: 2px solid #FFD700;
    }
    .prediction-value {
        font-size: 3rem;
        font-weight: bold;
        color: #00FF00;
    }
</style>
""", unsafe_allow_html=True)


@st.cache_resource
def load_model():
    """
    Load the trained model with caching.

    Returns
    -------
    sklearn.Pipeline
        Trained model pipeline.
    """
    model_path = "../models/taxi_fare_model.joblib"
    if os.path.exists(model_path):
        return joblib.load(model_path)
    return None


def predict_fare(model, trip_distance, passenger_count, pickup_id,
                 dropoff_id, hour, day_of_week, is_weekend):
    """
    Make a fare prediction.

    Parameters
    ----------
    model : sklearn.Pipeline
        Trained model.
    trip_distance : float
        Trip distance in miles.
    passenger_count : int
        Number of passengers.
    pickup_id : int
        Pickup location ID.
    dropoff_id : int
        Dropoff location ID.
    hour : int
        Hour of pickup (0-23).
    day_of_week : int
        Day of week (0-6).
    is_weekend : int
        Weekend flag (0 or 1).

    Returns
    -------
    float
        Predicted fare in dollars.
    """
    data = pd.DataFrame([{
        'trip_distance': trip_distance,
        'passenger_count': passenger_count,
        'PULocationID': pickup_id,
        'DOLocationID': dropoff_id,
        'hour': hour,
        'day_of_week': day_of_week,
        'is_weekend': is_weekend
    }])

    prediction = model.predict(data)[0]
    return round(prediction, 2)


def main():
    """Main Streamlit application."""
    # Header
    st.markdown('<p class="main-header">🚕 NYC Taxi Fare Predictor</p>',
                unsafe_allow_html=True)

    st.markdown("""
    Prédisez le prix d'une course de taxi à New York en utilisant
    notre modèle de Machine Learning (Random Forest).
    """)

    st.divider()

    # Load model
    model = load_model()

    if model is None:
        st.error("❌ Modèle non trouvé ! Lancez d'abord `python train.py`")
        return

    st.success("✅ Modèle chargé avec succès !")

    # Input form
    st.subheader("📝 Détails de la course")

    col1, col2 = st.columns(2)

    with col1:
        trip_distance = st.number_input(
            "Distance (miles)",
            min_value=0.1,
            max_value=100.0,
            value=5.0,
            step=0.5,
            help="Distance du trajet en miles"
        )

        passenger_count = st.number_input(
            "Nombre de passagers",
            min_value=1,
            max_value=6,
            value=2,
            step=1
        )

        pickup_id = st.number_input(
            "Zone de départ (ID)",
            min_value=1,
            max_value=265,
            value=132,
            help="ID de la zone de prise en charge (1-265)"
        )

    with col2:
        dropoff_id = st.number_input(
            "Zone d'arrivée (ID)",
            min_value=1,
            max_value=265,
            value=161,
            help="ID de la zone de dépose (1-265)"
        )

        hour = st.slider(
            "Heure de départ",
            min_value=0,
            max_value=23,
            value=12,
            help="Heure de la course (0-23)"
        )

        day_of_week = st.selectbox(
            "Jour de la semaine",
            options=[
                (0, "Lundi"),
                (1, "Mardi"),
                (2, "Mercredi"),
                (3, "Jeudi"),
                (4, "Vendredi"),
                (5, "Samedi"),
                (6, "Dimanche")
            ],
            format_func=lambda x: x[1],
            index=2
        )

    is_weekend = 1 if day_of_week[0] >= 5 else 0

    st.divider()

    # Prediction button
    if st.button("🔮 Prédire le prix", type="primary", use_container_width=True):
        with st.spinner("Calcul en cours..."):
            fare = predict_fare(
                model,
                trip_distance,
                passenger_count,
                pickup_id,
                dropoff_id,
                hour,
                day_of_week[0],
                is_weekend
            )

        # Display result
        st.markdown("### 💵 Prix estimé")

        col1, col2, col3 = st.columns([1, 2, 1])
        with col2:
            st.markdown(f"""
            <div class="prediction-box">
                <p class="prediction-value">${fare}</p>
                <p>Prix total estimé</p>
            </div>
            """, unsafe_allow_html=True)

        # Additional info
        st.markdown("---")
        st.markdown("#### 📊 Récapitulatif")

        recap_col1, recap_col2 = st.columns(2)
        with recap_col1:
            st.metric("Distance", f"{trip_distance} miles")
            st.metric("Passagers", passenger_count)
            st.metric("Heure", f"{hour}h00")

        with recap_col2:
            st.metric("Départ", f"Zone {pickup_id}")
            st.metric("Arrivée", f"Zone {dropoff_id}")
            st.metric("Jour", day_of_week[1])

    # Footer
    st.divider()
    st.caption("""
    🔬 Modèle: Random Forest Regressor |
    📊 RMSE: ~6.5 |
    📈 R²: 0.91
    """)


if __name__ == "__main__":
    main()
