#!/bin/sh

# Traps these exit codes to run our cleanup method.
trap cleanup 1 2 3 6

# Cleanup method for when process is exited.
cleanup() {
  echo "\nStopping containers..."
  docker-compose -f docker-compose.dev.yml down
}

# Build the API Service jar.
rm -rf target/
echo "Building application..."
mvn clean install
# Uncomment if you want to run the integration tests as well.
# mvn integration-test
# Create docker network 'smartfloor' if it does not exist yet.
docker network create smartfloor || true
# Build services from compose file.
echo "Setting up Docker containers..."
docker-compose -f docker-compose.dev.yml down
docker-compose -f docker-compose.dev.yml up -d --build
# Tail the API Service container to see if it runs correctly.
docker logs -f smartfloor-api
read