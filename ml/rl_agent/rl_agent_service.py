from flask import Flask, request, jsonify
import numpy as np
import random

app = Flask(__name__)

# --- REINFORCEMENT LEARNING STATE ---
# Actions: 0=Index, 1=Cache, 2=Pool
ACTIONS = ["APPLY_INDEX", "ENABLE_CACHE", "OPTIMIZE_POOL"]
# Q-Table: Stores the "Value" of each action. Initialize with zeros.
# Simple mapping: just learning global best actions for now.
q_values = {action: 0.5 for action in ACTIONS}
action_counts = {action: 0 for action in ACTIONS}

# Hyperparameters
EPSILON = 0.2  # 20% exploration (Try random things), 80% exploitation (Do what works)
ALPHA = 0.1    # Learning Rate

@app.route('/predict', methods=['POST'])
def predict_action():
    """
    Contextual Bandit: Given the state (Query Type), chose an action.
    """
    data = request.json
    query_type = data.get("query_type", "unknown")

    # Epsilon-Greedy Policy
    if random.random() < EPSILON:
        # Explore: Pick random action
        action = random.choice(ACTIONS)
        print(f"🎲 Exploring: {action}")
    else:
        # Exploit: Pick best known action
        action = max(q_values, key=q_values.get)
        print(f"🧠 Exploiting ({q_values[action]:.2f}): {action}")

    return jsonify({"suggested_action": action})

@app.route('/feedback', methods=['POST'])
def feedback():
    """
    Receive Reward: Did the latency go down?
    """
    data = request.json
    action = data.get("action")
    reward = data.get("reward") # Positive (Good), Negative (Bad)

    if action in q_values:
        # Q-Learning Update Rule
        # NewValue = OldValue + LearningRate * (Reward - OldValue)
        old_val = q_values[action]
        q_values[action] = old_val + ALPHA * (reward - old_val)
        action_counts[action] += 1
        
        print(f"📉 Learning: Action {action} Reward {reward} -> New Q-Value: {q_values[action]:.2f}")

    return jsonify({"status": "updated"})

if __name__ == '__main__':
    print("🤖 RL Agent Service running on Port 5000...")
    app.run(port=5000)