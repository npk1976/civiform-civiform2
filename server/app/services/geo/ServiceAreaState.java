package services.geo;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The result of checking an address for membership in a service area. */
public enum ServiceAreaState {
  // The address is in the service area.
  IN_AREA("InArea"),
  // The address is not in the service area.
  NOT_IN_AREA("NotInArea"),
  // The check failed for technical reasons.
  FAILED("Failed");

  private static final Logger logger = LoggerFactory.getLogger(ServiceAreaState.class);

  private final String serializationFormat;

  ServiceAreaState(String serializationFormat) {
    this.serializationFormat = checkNotNull(serializationFormat);
  }

  /** The string representation of this state for storage in the database. */
  public String getSerializationFormat() {
    return this.serializationFormat;
  }

  public static ServiceAreaState getEnumFromSerializedFormat(String serializedFormat) {
    for (ServiceAreaState e : ServiceAreaState.values()) {
      if (e.getSerializationFormat().equals(serializedFormat)) {
        return ServiceAreaState.valueOf(e.name());
      }
    }

    logger.error("Error getting enum from serialized format: {}", serializedFormat);
    throw new RuntimeException("Error getting enum from serialized format: " + serializedFormat);
  }
}
