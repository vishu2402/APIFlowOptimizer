terraform {
  required_version = ">= 1.3"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 6.20.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}


# VPC (terraform-aws-modules/vpc)
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "6.5.1"

  name = var.name_prefix
  cidr = "10.0.0.0/16"
  azs  = slice(data.aws_availability_zones.available.names, 0, 3)
  public_subnets  = ["10.0.1.0/24","10.0.2.0/24","10.0.3.0/24"]
  private_subnets = ["10.0.11.0/24","10.0.12.0/24","10.0.13.0/24"]

  enable_nat_gateway = false   # ← change this to false
}


data "aws_availability_zones" "available" {}

# EKS (terraform-aws-modules/eks)
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "21.9.0"

  name               = "${var.name_prefix}-eks"
  kubernetes_version = "1.27"

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  eks_managed_node_groups = {
    default = {
      create         = true
      name           = "default"
      instance_types = [var.eks_node_instance_type]
      desired_size   = 2
      min_size       = 1
      max_size       = 3
      subnet_ids     = module.vpc.private_subnets
    }
  }
}



# RDS Postgres
resource "aws_db_subnet_group" "pg" {
  name       = "${var.name_prefix}-pg-subnet"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_db_instance" "postgres" {
  identifier = "${var.name_prefix}-postgres"     # instance identifier (required)
  engine     = "postgres"
  engine_version = "15"
  instance_class = var.rds_instance_class
  allocated_storage = var.rds_allocated_storage

  db_name  = var.postgres_db_name   # <-- changed from name -> db_name
  username = var.postgres_username
  password = var.postgres_password

  skip_final_snapshot = true
  vpc_security_group_ids = [module.vpc.default_security_group_id]
  db_subnet_group_name   = aws_db_subnet_group.pg.name
  publicly_accessible = false
}


# MSK (managed Kafka)
resource "aws_msk_cluster" "kafka" {
  cluster_name = "${var.name_prefix}-msk"
  kafka_version = "3.4.0"
  number_of_broker_nodes = 3

  broker_node_group_info {
    instance_type = var.msk_instance_type
    client_subnets = module.vpc.private_subnets
    security_groups = [module.vpc.default_security_group_id]

    storage_info {
      ebs_storage_info {
        volume_size = 100   # <-- use volume_size here
      }
    }
  }

  encryption_info {
    encryption_in_transit { client_broker = "TLS" }
  }
}

