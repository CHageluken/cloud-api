#!/usr/bin/env bash

# TODO: remove
CI_COMMIT_SHA=$(uuidgen);

# Get ECR repository URI from SSM
REPOSITORY_URI=$(aws ecr describe-repositories | jq -r '.repositories[] | select(.repositoryName | contains("apiservice")) | .repositoryUri')
# Login to AWS ECR
aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin $REPOSITORY_URI
# Build container image, tag it and push it to the ECR repository
REPOSITORY_NAME=$(echo $REPOSITORY_URI | awk '{split($0,a,"/"); print a[2]}')
LATEST_IMAGE_DIGEST=$(aws ecr list-images --repository-name $REPOSITORY_NAME | jq '.[] | .[] | select(.imageTag=="latest") | .imageDigest')
ECS_CLUSTER_ARN=$(aws ecs list-clusters | jq -r '.clusterArns[0]')
ECS_SERVICE_ARN=$(aws ecs list-services --cluster $ECS_CLUSTER_ARN | jq -r '.serviceArns[0]')
if ! [[ -z "$LATEST_IMAGE_DIGEST" ]] # there exists an image with tag 'latest'
then
    docker pull $REPOSITORY_URI:latest || true # so we pull it first (for caching purposes, see next command flag --cache-from)
fi
mvn package
docker build --platform=linux/amd64 --cache-from $REPOSITORY_URI:latest --tag $REPOSITORY_URI:$CI_COMMIT_SHA --tag $REPOSITORY_URI:latest .
docker push $REPOSITORY_URI:$CI_COMMIT_SHA
docker push $REPOSITORY_URI:latest
# Deployment
ECS_TASK_ARN=$(aws ecs list-tasks --cluster $ECS_CLUSTER_ARN | jq -r '.taskArns[0]')
if ! [[ -z "$ECS_TASK_ARN" ]] # there exists a task
then
    aws ecs stop-task --cluster $ECS_CLUSTER_ARN --task $ECS_TASK_ARN > /dev/null
fi
aws ecs update-service --cluster $ECS_CLUSTER_ARN --service $ECS_SERVICE_ARN --force-new-deployment > /dev/null # trigger new deployment instead of waiting for ECS to discover 0 running tasks
echo "Deployed updated staging image, triggered ECS service deployment."
