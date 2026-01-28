#!/bin/bash

# --- Configuration ---
# Cognito User Pool ID (inferred from application-prod.properties)
USER_POOL_ID="ap-southeast-1_W7C683l6w"
# Cognito Client ID (provided by user)
CLIENT_ID="5jqse721vu1hk4dmf1qvlml7j"

# --- Prerequisites Check ---
if ! command -v aws &> /dev/null; then
    echo "Error: AWS CLI is not installed. Please install it and configure it."
    exit 1
fi

if ! command -v jq &> /dev/null; then
    echo "Error: jq is not installed. Please install it (e.g., 'sudo apt-get install jq' or 'brew install jq')."
    exit 1
fi

# --- User Input ---
echo "--- Cognito Token Acquisition ---"
read -p "Enter your Cognito username: " USERNAME
read -sp "Enter your Cognito password: " PASSWORD
echo # Newline after password input

# --- Authentication ---
echo "Attempting to authenticate with Cognito..."
AUTH_RESPONSE=$(aws cognito-idp initiate-auth \
  --auth-flow USER_PASSWORD_AUTH \
  --client-id "$CLIENT_ID" \
  --auth-parameters "USERNAME=$USERNAME,PASSWORD=$PASSWORD" \
  --region ap-southeast-1) # Assuming region from issuer-uri

# --- Token Handling ---
if echo "$AUTH_RESPONSE" | jq -e '.AuthenticationResult' > /dev/null; then
  echo "Authentication successful!"
  
  ID_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.AuthenticationResult.IdToken')
  ACCESS_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.AuthenticationResult.AccessToken')
  REFRESH_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.AuthenticationResult.RefreshToken')

  echo "$ID_TOKEN" > id_token.txt
  echo "$ACCESS_TOKEN" > access_token.txt
  echo "$REFRESH_TOKEN" > refresh_token.txt

  echo "Tokens saved to:"
  echo "- id_token.txt"
  echo "- access_token.txt"
  echo "- refresh_token.txt"
else
  echo "Authentication failed."
  echo "AWS CLI Response:"
  echo "$AUTH_RESPONSE" | jq '.' # Pretty print the error response
  exit 1
fi
