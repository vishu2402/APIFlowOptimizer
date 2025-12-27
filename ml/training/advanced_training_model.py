import pandas as pd
import numpy as np
import tensorflow as tf
from tensorflow.keras import layers, models, Input
from sqlalchemy import create_engine, text
import os
import shutil

DB_CONNECTION = 'postgresql://admin:root@localhost:5435/apiflow'
MODEL_DIR = "apiflow_tf_model/1"

def get_data():
    print("📡 Fetching Multi-Modal Data...")
    engine = create_engine(DB_CONNECTION)
    
    # Fetch SQL (Text), Duration (Numeric), and Status (Categorical/Numeric)
    sql = """
        SELECT 
            t.duration_ms,
            CASE WHEN t.status_code = 'STATUS_CODE_ERROR' THEN 1 ELSE 0 END as is_error,
            attr -> 'value' ->> 'stringValue' as sql_text
        FROM 
            traces t
        JOIN 
            raw_events r ON t.trace_id = r.trace_id AND t.span_id = r.span_id,
            jsonb_array_elements(r.payload -> 'attributes') attr
        WHERE 
            attr ->> 'key' = 'db.statement'
            AND t.operation_name LIKE 'SELECT%'
    """
    with engine.connect() as conn:
        df = pd.read_sql(text(sql), conn)
    return df

def label_data(row):
    # Classification Rules (Ground Truth)
    sql = row['sql_text'].upper()
    if "JOIN" in sql: return 2          # Complex Join
    if row['is_error'] == 1: return 3   # Database Failure
    if "AVG" in sql: return 1           # Heavy Aggregation
    return 0                            # Normal

def train_tier1_model():
    df = get_data()
    if len(df) < 50: return print("⚠️ Not enough data.")

    df['label'] = df.apply(label_data, axis=1)
    
    # --- PREPARE INPUTS ---
    # Input A: Text Data (SQL)
    X_text = tf.constant(df['sql_text'].values, dtype=tf.string)
    
    # Input B: Numerical Data (Duration + Error Flag)
    # We normalize duration roughly (div by 1000) to keep it in 0-1 range mostly
    stats_data = df[['duration_ms', 'is_error']].values
    stats_data[:,0] = stats_data[:,0] / 1000.0 
    X_stats = tf.constant(stats_data, dtype=tf.float32)

    y_labels = tf.constant(df['label'].values, dtype=tf.int32)

    # --- BUILD MULTI-INPUT MODEL ---
    
    # Branch 1: Text Processing (The "Language" Brain)
    text_input = Input(shape=(), dtype=tf.string, name='text_input')
    vectorize_layer = layers.TextVectorization(max_tokens=2000, output_mode='int', output_sequence_length=50)
    vectorize_layer.adapt(X_text)
    
    text_features = vectorize_layer(text_input)
    text_features = layers.Embedding(2001, 16)(text_features)
    text_features = layers.GlobalAveragePooling1D()(text_features)

    # Branch 2: Stats Processing (The "Math" Brain)
    stats_input = Input(shape=(2,), dtype=tf.float32, name='stats_input')
    stats_features = layers.Dense(8, activation='relu')(stats_input)

    # Merge Branches
    x = layers.concatenate([text_features, stats_features])
    x = layers.Dense(16, activation='relu')(x)
    output = layers.Dense(4, activation='softmax')(x) # 4 Classes

    model = models.Model(inputs=[text_input, stats_input], outputs=output)

    model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])

    print("\n🧠 Training Multi-Modal Model...")
    model.fit({'text_input': X_text, 'stats_input': X_stats}, y_labels, epochs=15, batch_size=16, verbose=1)

    # --- EXPORT ---
    print(f"\n💾 Saving Tier 1 Model to {MODEL_DIR}...")
    if os.path.exists(MODEL_DIR): shutil.rmtree(MODEL_DIR)
    model.export(MODEL_DIR)
    print("✅ SavedModel Exported! Ready for TF Serving.")

if __name__ == "__main__":
    train_tier1_model()