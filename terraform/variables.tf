variable "aws_region" {
  description = "Região AWS onde os recursos serão criados"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Nome do projeto, usado para nomear os recursos"
  type        = string
  default     = "isobar-fm-api"
}

variable "image_tag" {
  description = "Tag da imagem Docker no ECR a ser implantada (ex: latest, v1)"
  type        = string
  default     = "latest"
}

variable "cpu" {
  description = "CPU alocada para o App Runner (unidades vCPU App Runner: 0.25, 0.5, 1, 2, 4)"
  type        = string
  default     = "0.25 vCPU"
}

variable "memory" {
  description = "Memória alocada para o App Runner (0.5, 1, 2, 3, 4, 6, 8, 10, 12 GB)"
  type        = string
  default     = "0.5 GB"
}

variable "port" {
  description = "Porta em que a aplicação Spring Boot escuta"
  type        = number
  default     = 8080
}

variable "cors_allowed_origin" {
  description = "Origem permitida para CORS (ex: URL do frontend, se houver)"
  type        = string
  default     = "http://localhost:3000"
}
