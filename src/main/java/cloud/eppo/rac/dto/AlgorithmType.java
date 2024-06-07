package cloud.eppo.rac.dto;

import java.util.Arrays;

public enum AlgorithmType {
  CONSTANT,
  CONTEXTUAL_BANDIT,
  OVERRIDE;

  AlgorithmType() {}

  public static AlgorithmType forValues(String value) {
    return Arrays.stream(AlgorithmType.values())
        .filter(a -> a.name().equalsIgnoreCase(value))
        .findFirst()
        .orElse(null);
  }
}
