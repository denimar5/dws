output "ecr_repository_url" {
  description = "URL do repositório ECR para onde fazer push da imagem Docker"
  value       = aws_ecr_repository.api.repository_url
}

output "lambda_function_name" {
  description = "Nome da função Lambda"
  value       = aws_lambda_function.api.function_name
}

output "lambda_function_url" {
  description = "URL pública HTTPS do Lambda (Function URL)"
  value       = aws_lambda_function_url.api.function_url
}
