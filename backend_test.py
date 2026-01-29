#!/usr/bin/env python3
"""
Backend Testing Script for Workout Timer App
Tests the FastAPI backend endpoints and database connectivity
"""

import requests
import json
import os
import sys
from datetime import datetime

# Get backend URL from frontend .env file
def get_backend_url():
    try:
        with open('/app/frontend/.env', 'r') as f:
            for line in f:
                if line.startswith('EXPO_PUBLIC_BACKEND_URL='):
                    url = line.split('=', 1)[1].strip().strip('"')
                    return f"{url}/api"
    except Exception as e:
        print(f"Error reading frontend .env: {e}")
    
    # Fallback to localhost for testing
    return "http://localhost:8001/api"

BACKEND_URL = get_backend_url()
print(f"Testing backend at: {BACKEND_URL}")

def test_health_check():
    """Test the basic health check endpoint"""
    print("\n=== Testing Health Check Endpoint ===")
    try:
        response = requests.get(f"{BACKEND_URL}/", timeout=10)
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.json()}")
        
        if response.status_code == 200:
            data = response.json()
            if data.get("message") == "Hello World":
                print("✅ Health check passed")
                return True
            else:
                print("❌ Health check failed - unexpected response")
                return False
        else:
            print(f"❌ Health check failed - status code {response.status_code}")
            return False
            
    except requests.exceptions.ConnectionError:
        print("❌ Health check failed - connection refused")
        return False
    except requests.exceptions.Timeout:
        print("❌ Health check failed - request timeout")
        return False
    except Exception as e:
        print(f"❌ Health check failed - {str(e)}")
        return False

def test_database_connection():
    """Test database connectivity by checking environment variables"""
    print("\n=== Testing Database Configuration ===")
    try:
        # Check if backend .env has required variables
        env_path = '/app/backend/.env'
        if not os.path.exists(env_path):
            print("❌ Backend .env file not found")
            return False
            
        with open(env_path, 'r') as f:
            env_content = f.read()
            
        has_mongo_url = 'MONGO_URL=' in env_content
        has_db_name = 'DB_NAME=' in env_content
        
        print(f"MONGO_URL configured: {has_mongo_url}")
        print(f"DB_NAME configured: {has_db_name}")
        
        if has_mongo_url and has_db_name:
            print("✅ Database configuration found")
            return True
        else:
            print("❌ Database configuration incomplete")
            return False
            
    except Exception as e:
        print(f"❌ Database configuration check failed - {str(e)}")
        return False

def test_status_endpoints():
    """Test the status check endpoints"""
    print("\n=== Testing Status Endpoints ===")
    
    # Test POST /api/status
    try:
        test_data = {
            "client_name": "workout_timer_test"
        }
        
        print("Testing POST /api/status...")
        response = requests.post(f"{BACKEND_URL}/status", 
                               json=test_data, 
                               timeout=10)
        print(f"POST Status Code: {response.status_code}")
        
        if response.status_code == 200:
            created_status = response.json()
            print(f"Created status: {created_status}")
            print("✅ POST /api/status passed")
            post_success = True
        else:
            print(f"❌ POST /api/status failed - status code {response.status_code}")
            print(f"Response: {response.text}")
            post_success = False
            
    except Exception as e:
        print(f"❌ POST /api/status failed - {str(e)}")
        post_success = False
    
    # Test GET /api/status
    try:
        print("\nTesting GET /api/status...")
        response = requests.get(f"{BACKEND_URL}/status", timeout=10)
        print(f"GET Status Code: {response.status_code}")
        
        if response.status_code == 200:
            status_list = response.json()
            print(f"Retrieved {len(status_list)} status records")
            print("✅ GET /api/status passed")
            get_success = True
        else:
            print(f"❌ GET /api/status failed - status code {response.status_code}")
            print(f"Response: {response.text}")
            get_success = False
            
    except Exception as e:
        print(f"❌ GET /api/status failed - {str(e)}")
        get_success = False
    
    return post_success and get_success

def check_backend_service():
    """Check if backend service is running"""
    print("\n=== Checking Backend Service Status ===")
    try:
        # Check supervisor status
        import subprocess
        result = subprocess.run(['sudo', 'supervisorctl', 'status', 'backend'], 
                              capture_output=True, text=True, timeout=10)
        print(f"Supervisor status: {result.stdout.strip()}")
        
        if 'RUNNING' in result.stdout:
            print("✅ Backend service is running")
            return True
        else:
            print("❌ Backend service is not running")
            return False
            
    except Exception as e:
        print(f"❌ Could not check backend service status - {str(e)}")
        return False

def main():
    """Run all backend tests"""
    print("🚀 Starting Backend Tests for Workout Timer App")
    print("=" * 60)
    
    results = {}
    
    # Check if backend service is running
    results['service_running'] = check_backend_service()
    
    # Test basic health check
    results['health_check'] = test_health_check()
    
    # Test database configuration
    results['database_config'] = test_database_connection()
    
    # Test status endpoints (optional for MVP)
    results['status_endpoints'] = test_status_endpoints()
    
    # Summary
    print("\n" + "=" * 60)
    print("🏁 TEST SUMMARY")
    print("=" * 60)
    
    total_tests = len(results)
    passed_tests = sum(1 for result in results.values() if result)
    
    for test_name, result in results.items():
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"{test_name.replace('_', ' ').title()}: {status}")
    
    print(f"\nOverall: {passed_tests}/{total_tests} tests passed")
    
    if results['health_check']:
        print("\n✅ Backend is healthy and ready for use")
        return True
    else:
        print("\n❌ Backend has critical issues")
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)