import requests
import time
import sys

URL = "http://localhost:8092/analyzer/analyze-work"

def generate_traffic():
    request_count = 0
    print("🚀 Starting BALANCED TRAFFIC GENERATOR...")
    print("   - Rotating: Fast -> Heavy DB -> Heavy Join")
    print("   - 5% Random Errors (Handled by Java)")
    print("Press Ctrl+C to stop.")
    
    scenarios = ["fast", "heavy_db", "heavy_join"]
    
    try:
        while True:
            type_param = scenarios[request_count % len(scenarios)]

            try:
                start = time.time()
                response = requests.get(f"{URL}?type={type_param}", timeout=30)
                duration = (time.time() - start) * 1000
                
                request_count += 1
                
                if response.status_code == 200:
                    print(f"[{request_count}] {type_param.upper()}: {response.text} (Took {duration:.2f}ms)")
                else:
                    print(f"[{request_count}] ERROR (500): {type_param.upper()}")
                
            except Exception as e:
                print(f"Connection Failed: {e}")

            time.sleep(1.0)

    except KeyboardInterrupt:
        print(f"\nStopped. Total Requests: {request_count}")
        sys.exit(0)

if __name__ == "__main__":
    generate_traffic()