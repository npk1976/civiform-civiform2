locals {
  # The hard-coded zero indexes here are necessary to access the fqdn from the record set associated with it
  # because the private_dns_zone_configs and record_sets blocks expose lists, even if we only have one dns zone
  # and record set configured.
  postgres_private_link = azurerm_private_endpoint.endpoint.private_dns_zone_configs[0].record_sets[0].fqdn
  generated_hostname    = "${var.application_name}-${random_pet.server.id}.azurewebsites.net"

  postgres_password_secret_name = "postgres-password"
  app_key_secret_name = "app-secret-key"
}
