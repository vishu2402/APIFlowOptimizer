variable "aws_region" {
  type    = string
  default = "ap-south-1"
}

variable "name_prefix" {
  type    = string
  default = "apiflow"
}

variable "postgres_db_name" {
  type    = string
  default = "apiflow_db"
}

variable "postgres_username" {
  type    = string
  default = "apiflow"
}

variable "postgres_password" {
  type    = string
  default = "ChangeMe123!"
}

variable "rds_instance_class" {
  type    = string
  default = "db.t3.medium"
}

variable "rds_allocated_storage" {
  type    = number
  default = 20
}

variable "eks_node_instance_type" {
  type    = string
  default = "t3.medium"
}

variable "msk_instance_type" {
  type    = string
  default = "kafka.m5.large"
}
