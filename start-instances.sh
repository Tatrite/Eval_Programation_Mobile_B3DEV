#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: ./start-relais.sh <nombre_de_relais>"
  exit 1
fi

NUM_RELAIS=$1

echo "==> Compilation du projet et création de l'image Docker..."
./gradlew bootJar
docker build -t tor-app:latest .

echo "==> Démarrage du Chef et du Registre..."
docker-compose up -d

sleep 2

echo "==> Lancement et démarrage des $NUM_RELAIS relais..."
for i in $(seq 1 $NUM_RELAIS)
do
  CONTAINER_NAME="relay-$i"
  docker rm -f $CONTAINER_NAME 2>/dev/null

  docker run -d \
    --name "$CONTAINER_NAME" \
    --network tor-network \
    -e ROLE=RELAIS \
    -e CONTAINER_NAME="$CONTAINER_NAME" \
    tor-app:latest

  echo "✅ Conteneur $CONTAINER_NAME démarré."
done

echo "==> Tout est prêt ! Tu peux tester dans ton navigateur."