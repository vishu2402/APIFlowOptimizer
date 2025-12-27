output "eks_cluster_id" {
  value = module.eks.cluster_id
}

output "eks_endpoint" {
  value = try(module.eks.cluster_endpoint, "")   # cluster_endpoint if available
}

output "eks_cluster_ca" {
  value = try(module.eks.cluster_certificate_authority_data, "")
}
