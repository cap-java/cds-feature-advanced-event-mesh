package com.sap.cds.feature.messaging.aem.client.binding;

record AemClientIdentity(String clientId, String clientSecret)
    implements com.sap.cloud.security.config.ClientIdentity {

  @Override
  public String getId() {
    return this.clientId;
  }

  @Override
  public String getSecret() {
    return this.clientSecret;
  }
}
