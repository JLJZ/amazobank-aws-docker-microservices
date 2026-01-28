#!/bin/bash

# --- Configuration ---
# Base URL of the API endpoint (replace with your actual API endpoint)
# For local testing, this might be http://localhost:8080/users
# For production, this might be a deployed API Gateway URL
# API_BASE_URL="http://localhost:8080/api/users" # Example: replace with your actual API endpoint
API_BASE_URL="https://api.itsag2t2.com/api/users" # Example: replace with your actual API endpoint

# --- Prerequisites Check ---
if ! command -v curl &> /dev/null; then
    echo "Error: curl is not installed. Please install it."
    exit 1
fi

if [ ! -f "access_token.txt" ]; then
    echo "Error: access_token.txt not found. Please run ./get_cognito_token.sh first to obtain tokens."
    exit 1
fi

# --- Token Loading ---
ACCESS_TOKEN=$(cat access_token.txt)

# --- API Call ---
echo "--- Calling API ---"
echo "Using Access Token: [REDACTED]" # Avoid printing token directly for security

# Example API call: GET request to retrieve user data
# You can modify this to POST, PUT, DELETE, etc. as needed.
# The Authorization header is crucial for authenticated requests.
curl -k "$API_BASE_URL" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -v # Verbose output to see request/response details

# Check the exit status of curl
if [ $? -eq 0 ]; then
  echo ""
  echo "API call completed successfully."
else
  echo ""
  echo "API call failed."
  exit 1
fi

# --- Optional: Refresh Token ---
# If you need to refresh the token, you would use the refresh_token.txt
# and call the Cognito initiate-auth command with REFRESH_TOKEN_AUTH flow.
# This is a more advanced step and not included in this basic script.
# Example (conceptual):
# REFRESH_TOKEN=$(cat refresh_token.txt)
# aws cognito-idp initiate-auth --auth-flow REFRESH_TOKEN_AUTH --client-id "$CLIENT_ID" --auth-parameters "REFRESH_TOKEN=$REFRESH_TOKEN"
