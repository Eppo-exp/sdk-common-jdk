package cloud.eppo.api;

import java.util.Map;

/** Interface for BanditCoefficients allowing downstream SDKs to provide custom implementations. */
public interface IBanditCoefficients {
  String getActionKey();

  Double getIntercept();

  Map<String, ? extends IBanditNumericAttributeCoefficients> getSubjectNumericCoefficients();

  Map<String, ? extends IBanditCategoricalAttributeCoefficients>
      getSubjectCategoricalCoefficients();

  Map<String, ? extends IBanditNumericAttributeCoefficients> getActionNumericCoefficients();

  Map<String, ? extends IBanditCategoricalAttributeCoefficients> getActionCategoricalCoefficients();
}
