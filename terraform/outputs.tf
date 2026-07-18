output "ecr_repository_url" {
  description = "URL do repositório ECR para onde fazer push da imagem Docker"
  value       = aws_ecr_repository.api.repository_url
}

output "app_runner_service_url" {
  description = "URL pública do serviço no App Runner (HTTPS)"
  value       = "https://${aws_apprunner_service.api.service_url}"
}

output "app_runner_service_arn" {
  description = "ARN do serviço App Runner"
  value       = aws_apprunner_service.api.arn
}

output "app_runner_service_status" {
  description = "Status atual do serviço App Runner"
  value       = aws_apprunner_service.api.status
}
