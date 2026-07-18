# Deploy — isobar-fm-api no AWS App Runner

Este módulo Terraform cria:

- Um repositório **ECR** para guardar a imagem Docker da API.
- Uma role **IAM** que permite o App Runner puxar a imagem do ECR.
- Um serviço **App Runner** que roda o container e expõe a API publicamente via HTTPS.

`auto_deployments_enabled = false`: o serviço só atualiza quando você rodar `terraform apply`
de novo com uma tag de imagem nova — evita redeploys automáticos e custo surpresa.

---

## Pré-requisitos

- Terraform >= 1.5
- AWS CLI configurado (`aws configure` ou variáveis de ambiente)
- Docker instalado
- Um `Dockerfile` na raiz do projeto Spring Boot (multi-stage, expondo a porta 8080)

---

## Passo 1 — Criar a infraestrutura base (ECR + IAM)

Na primeira execução, o App Runner ainda não tem imagem pra puxar. Então primeiro
criamos só o ECR e a role, depois fazemos o push da imagem, e só então o App Runner
consegue subir com sucesso.

```bash
cd terraform-isobar-fm-api
terraform init
terraform apply -target=aws_ecr_repository.api -target=aws_iam_role.apprunner_ecr_access -target=aws_iam_role_policy_attachment.apprunner_ecr_access
```

Confirme com `yes` quando solicitado.

Anote a URL do repositório que aparece no output `ecr_repository_url`.

---

## Passo 2 — Build e push da imagem Docker

```bash
# Autentica o Docker no ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <ECR_REPOSITORY_URL_SEM_TAG>

# Build da imagem (rode na raiz do projeto Spring Boot, onde está o Dockerfile)
docker build -t isobar-fm-api .

# Tag apontando pro ECR
docker tag isobar-fm-api:latest <ECR_REPOSITORY_URL>:latest

# Push
docker push <ECR_REPOSITORY_URL>:latest
```

Substitua `<ECR_REPOSITORY_URL>` pelo valor do output do Passo 1
(algo como `123456789012.dkr.ecr.us-east-1.amazonaws.com/isobar-fm-api`).

---

## Passo 3 — Criar o serviço App Runner

Agora que a imagem já existe no ECR, aplique o restante:

```bash
terraform apply
```

O App Runner leva de 2 a 5 minutos pra provisionar e passar no health check.

Ao final, o output `app_runner_service_url` mostra a URL pública HTTPS.

Teste:

```bash
curl "$(terraform output -raw app_runner_service_url)/actuator/health"
curl "$(terraform output -raw app_runner_service_url)/api/v1/bands"
```

---

## Passo 4 — Derrubar tudo depois de usar

Como o objetivo é uma demo pontual pro processo seletivo, destrua os recursos
assim que o recrutador confirmar o teste, pra não deixar nada consumindo crédito:

```bash
terraform destroy
```

O ECR tem `force_delete = true`, então o `destroy` remove o repositório mesmo
com imagens dentro — não precisa limpar manualmente antes.

---

## Custo estimado

Com o tier mínimo (0.25 vCPU / 0.5 GB), o App Runner cobra por segundo enquanto
o serviço está provisionado — na faixa de poucos centavos por hora de uso ativo,
bem dentro do seu saldo de créditos atual. Não há custo fixo de ALB ou control
plane como em ECS/EKS.
