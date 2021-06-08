package org.hyperledger.bpa.client;

import io.micronaut.context.annotation.Requires;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.server.AdminStatusReadiness;

@Singleton
@Requires(beans = HealthEndpoint.class)
@Requires(property = HealthEndpoint.PREFIX + ".acapy.enabled", value = "true")
public class AcaPyHealthCheck extends AbstractHealthIndicator<Map<String, String>> {
  @Inject
  AriesClient ac;

  @Override
  protected Map<String, String> getHealthInformation() {
    try {
      Optional<AdminStatusReadiness> status = ac.statusReady();
      if (status.isPresent() && status.get().isReady()) {
        this.healthStatus = HealthStatus.UP;
      } else {
        this.healthStatus = HealthStatus.DOWN;
      }
    } catch (IOException e) {
      throw new RuntimeException((e));
    }
    return null;
  }

  @Override
  protected String getName() {
    return "aca-py";
  }
}
