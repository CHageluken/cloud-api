no-cache-dir = True
# Base image is a lightweight (alpine-based) docker image as we will need to build, tag and push an image to our container registry (ECR).
FROM gliderlabs/alpine:3.4
# Install necessary libraries: PIP (Python package manager), jq (json query library).
RUN apk add --update-cache py-pip jq
# Install AWS CLI tools using PIP (Python package).
RUN pip install awscli
# Upgrade pip
RUN pip install --upgrade pip