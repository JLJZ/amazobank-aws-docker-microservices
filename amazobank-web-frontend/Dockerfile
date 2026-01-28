# Use Node.js LTS
FROM node:18-alpine

WORKDIR /app

# Install dependencies
COPY package.json package-lock.json* ./
RUN npm install

# Copy app code
COPY . .

# Build Next.js
RUN npm run build

# Expose frontend port
EXPOSE 3000

# Start server
CMD ["npm", "start"]