#!/usr/bin/env bash
set -e

echo "==> Pushing to origin..."
git push origin main

echo "==> Running remote deploy..."
ssh randy_ubuntu@192.168.76.104 /opt/english-tutor/deploy.sh
