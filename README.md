# Transfer System

## Prerequisites

- Docker (version 27.5.1)
- Git (version 2.37.1)

## Setup Instructions

### Download and Install Docker

- For Windows: https://docs.docker.com/desktop/setup/install/windows-install/
- For Linux: https://docs.docker.com/desktop/setup/install/linux/
- For Mac: https://docs.docker.com/desktop/setup/install/mac-install/

### Clone Application

Clone the application repository by running the following commands:

```sh
cd ~
git clone https://github.com/rohit-agg/transfer-system
```

### Execute Application

Execute the following to run the application:

```sh
cd ~/transfer-system
docker compose up --build --force-recreate
```

## APIs

### Create Account

```sh
curl --location 'localhost:8080/accounts' \
--header 'Content-Type: application/json' \
--data '{
    "account_id": 1,
    "name": "Rohit Aggarwal",
    "initial_balance": 1000
}'
```

### Fetch Account

```sh
curl --location 'localhost:8080/accounts/5'
```

### Submit Transaction

```sh
curl --location 'localhost:8080/transactions' \
--header 'Content-Type: application/json' \
--data '{
    "source_account_id": 6,
    "destination_account_id": 3,
    "amount": 121.12
}'
```