# -----------------------------------------------------------------------------
# Lambda: roda o mesmo container Spring Boot via AWS Lambda Web Adapter,
# exposto publicamente via Function URL (sem API Gateway, sem custo extra).
#
# Pré-requisito: o Dockerfile precisa incluir o Lambda Web Adapter e a imagem
# precisa ser reenviada ao ECR com a tag definida em var.lambda_image_tag.
# Veja DEPLOY.md para o passo a passo completo.
# -----------------------------------------------------------------------------

resource "aws_iam_role" "lambda_execution" {
  name = "${var.project_name}-lambda-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_basic_execution" {
  role       = aws_iam_role.lambda_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_lambda_function" "api" {
  function_name = "${var.project_name}-fn"
  role          = aws_iam_role.lambda_execution.arn

  package_type = "Image"
  image_uri    = "${aws_ecr_repository.api.repository_url}:${var.lambda_image_tag}"

  # JVM + Spring Boot precisam de memória e timeout maiores que o padrão de Lambda.
  # Mais memória também dá mais CPU proporcional, o que ajuda o cold start.
  memory_size = var.lambda_memory_size
  timeout     = var.lambda_timeout

  environment {
    variables = {
      CORS_ALLOWED_ORIGIN_1 = var.cors_allowed_origin
      # Porta em que o Spring Boot escuta dentro do container.
      # O Lambda Web Adapter (configurado no Dockerfile) faz o proxy pra cá.
      AWS_LWA_PORT = tostring(var.port)
      PORT         = tostring(var.port)

      # Usa o endpoint de health real como readiness check, evitando o
      # NoResourceFoundException que aparecia nos logs (GET / não mapeado).
      AWS_LWA_READINESS_CHECK_PATH = "/actuator/health"
    }
  }

  tags = {
    Project = var.project_name
  }
}

resource "aws_lambda_function_url" "api" {
  function_name      = aws_lambda_function.api.function_name
  authorization_type = "NONE"

  cors {
    allow_origins = [var.cors_allowed_origin]
    allow_methods = ["*"]
    allow_headers = ["Accept", "Accept-Language", "Content-Type"]
  }
}

# Permissão explícita para invocação pública via Function URL.
resource "aws_lambda_permission" "public_invoke" {
  statement_id            = "AllowPublicInvokeFunctionUrl"
  action                  = "lambda:InvokeFunctionUrl"
  function_name           = aws_lambda_function.api.function_name
  principal                = "*"
  function_url_auth_type  = "NONE"
}

# A partir de nov/2025 a AWS passou a exigir também permissão explícita para
# lambda:InvokeFunction em Function URLs com AuthType NONE. O provider Terraform
# ainda não suporta a condição "lambda:InvokedViaFunctionUrl" nativamente
# (ver https://github.com/hashicorp/terraform-provider-aws/issues/44829),
# então essa segunda permissão é adicionada via AWS CLI dentro do próprio apply.
resource "null_resource" "public_invoke_function" {
  triggers = {
    function_name = aws_lambda_function.api.function_name
  }

  provisioner "local-exec" {
    command = <<-EOT
      aws lambda add-permission \
        --function-name ${aws_lambda_function.api.function_name} \
        --statement-id AllowPublicInvokeFunction \
        --action lambda:InvokeFunction \
        --principal "*" \
        --region ${var.aws_region} \
        || true
    EOT
  }

  depends_on = [aws_lambda_function_url.api]
}
