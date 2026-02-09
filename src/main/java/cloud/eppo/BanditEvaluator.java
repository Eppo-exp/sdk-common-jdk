package cloud.eppo;

import static cloud.eppo.Utils.getShard;

import cloud.eppo.api.Actions;
import cloud.eppo.api.Attributes;
import cloud.eppo.api.DiscriminableAttributes;
import cloud.eppo.api.EppoValue;
import cloud.eppo.api.dto.BanditCategoricalAttributeCoefficients;
import cloud.eppo.api.dto.BanditCoefficients;
import cloud.eppo.api.dto.BanditModelData;
import cloud.eppo.api.dto.BanditNumericAttributeCoefficients;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BanditEvaluator {
  private static final Logger logger = LoggerFactory.getLogger(BanditEvaluator.class);
  private static final int BANDIT_ASSIGNMENT_SHARDS = 10000; // hard-coded for now

  public static BanditEvaluationResult evaluateBandit(
      String flagKey,
      String subjectKey,
      DiscriminableAttributes subjectAttributes,
      Actions actions,
      BanditModelData modelData) {
    Map<String, Double> actionScores = scoreActions(subjectAttributes, actions, modelData);
    Map<String, Double> actionWeights =
        weighActions(actionScores, modelData.getGamma(), modelData.getActionProbabilityFloor());
    String selectedActionKey = selectAction(flagKey, subjectKey, actionWeights);

    // Compute optimality gap in terms of score
    double topScore =
        actionScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
    double optimalityGap = topScore - actionScores.get(selectedActionKey);

    return new BanditEvaluationResult(
        flagKey,
        subjectKey,
        subjectAttributes,
        selectedActionKey,
        actions.get(selectedActionKey),
        actionScores.get(selectedActionKey),
        actionWeights.get(selectedActionKey),
        modelData.getGamma(),
        optimalityGap);
  }

  private static Map<String, Double> scoreActions(
      DiscriminableAttributes subjectAttributes, Actions actions, BanditModelData modelData) {
    return actions.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                  String actionName = e.getKey();
                  DiscriminableAttributes actionAttributes = e.getValue();

                  // get all coefficients known to the model for this action
                  BanditCoefficients banditCoefficients =
                      modelData.getCoefficients().get(actionName);

                  if (banditCoefficients == null) {
                    // Unknown action; return the default action score
                    return modelData.getDefaultActionScore();
                  }

                  // Score the action using the provided attributes
                  double actionScore = banditCoefficients.getIntercept();
                  actionScore +=
                      scoreNumericAttributes(
                          actionAttributes.getNumericAttributes(),
                          banditCoefficients.getActionNumericCoefficients());
                  actionScore +=
                      scoreCategoricalAttributes(
                          actionAttributes.getCategoricalAttributes(),
                          banditCoefficients.getActionCategoricalCoefficients());
                  actionScore +=
                      scoreNumericAttributes(
                          subjectAttributes.getNumericAttributes(),
                          banditCoefficients.getSubjectNumericCoefficients());
                  actionScore +=
                      scoreCategoricalAttributes(
                          subjectAttributes.getCategoricalAttributes(),
                          banditCoefficients.getSubjectCategoricalCoefficients());

                  return actionScore;
                }));
  }

  private static double scoreNumericAttributes(
      Attributes attributes,
      Map<String, ? extends BanditNumericAttributeCoefficients> coefficients) {
    double totalScore = 0.0;

    for (BanditNumericAttributeCoefficients attributeCoefficients : coefficients.values()) {
      EppoValue attributeValue = attributes.get(attributeCoefficients.getAttributeKey());
      double attributeScore =
          scoreNumericAttributeValue(
              attributeValue,
              attributeCoefficients.getAttributeKey(),
              attributeCoefficients.getCoefficient(),
              attributeCoefficients.getMissingValueCoefficient());
      totalScore += attributeScore;
    }

    return totalScore;
  }

  private static double scoreNumericAttributeValue(
      EppoValue attributeValue,
      String attributeKey,
      double coefficient,
      double missingValueCoefficient) {
    if (attributeValue == null || attributeValue.isNull()) {
      return missingValueCoefficient;
    }
    if (!attributeValue.isNumeric()) {
      logger.warn("Unexpected categorical attribute value for attribute {}", attributeKey);
    }
    return coefficient * attributeValue.doubleValue();
  }

  private static double scoreCategoricalAttributes(
      Attributes attributes,
      Map<String, ? extends BanditCategoricalAttributeCoefficients> coefficients) {
    double totalScore = 0.0;

    for (BanditCategoricalAttributeCoefficients attributeCoefficients : coefficients.values()) {
      EppoValue attributeValue = attributes.get(attributeCoefficients.getAttributeKey());
      double attributeScore =
          scoreCategoricalAttributeValue(
              attributeValue,
              attributeCoefficients.getAttributeKey(),
              attributeCoefficients.getValueCoefficients(),
              attributeCoefficients.getMissingValueCoefficient());
      totalScore += attributeScore;
    }

    return totalScore;
  }

  private static double scoreCategoricalAttributeValue(
      EppoValue attributeValue,
      String attributeKey,
      Map<String, Double> valueCoefficients,
      double missingValueCoefficient) {
    if (attributeValue == null || attributeValue.isNull()) {
      return missingValueCoefficient;
    }
    if (attributeValue.isNumeric()) {
      logger.warn("Unexpected numeric attribute value for attribute {}", attributeKey);
      return missingValueCoefficient;
    }

    String valueKey = attributeValue.toString();
    Double coefficient = valueCoefficients.get(valueKey);

    // Categorical attributes are treated as one-hot booleans, so it's just the coefficient * 1
    // when present
    return coefficient != null ? coefficient : missingValueCoefficient;
  }

  private static Map<String, Double> weighActions(
      Map<String, Double> actionScores, double gamma, double actionProbabilityFloor) {
    Double highestScore = null;
    String highestScoredAction = null;
    for (Map.Entry<String, Double> actionScore : actionScores.entrySet()) {
      if (highestScore == null
          || actionScore.getValue() > highestScore
          || actionScore
                  .getValue()
                  .equals(highestScore) // note: we break ties for scores by action name
              && actionScore.getKey().compareTo(highestScoredAction) < 0) {
        highestScore = actionScore.getValue();
        highestScoredAction = actionScore.getKey();
      }
    }

    // Weigh all the actions using their score
    Map<String, Double> actionWeights = new HashMap<>();
    double totalNonHighestWeight = 0.0;
    for (Map.Entry<String, Double> actionScore : actionScores.entrySet()) {
      if (actionScore.getKey().equals(highestScoredAction)) {
        // The highest scored action is weighed at the end
        continue;
      }

      // Compute weight (probability)
      double unboundedProbability =
          1 / (actionScores.size() + (gamma * (highestScore - actionScore.getValue())));
      double minimumProbability = actionProbabilityFloor / actionScores.size();
      double boundedProbability = Math.max(unboundedProbability, minimumProbability);
      totalNonHighestWeight += boundedProbability;

      actionWeights.put(actionScore.getKey(), boundedProbability);
    }

    // Weigh the highest scoring action (defensively preventing a negative probability)
    double weightForHighestScore = Math.max(1 - totalNonHighestWeight, 0);
    actionWeights.put(highestScoredAction, weightForHighestScore);
    return actionWeights;
  }

  private static String selectAction(
      String flagKey, String subjectKey, Map<String, Double> actionWeights) {
    // Deterministically "shuffle" the actions
    // This way as action weights shift, a bunch of users who were on the edge of one action won't
    // all be shifted to the same new action at the same time.
    List<String> shuffledActionKeys =
        actionWeights.keySet().stream()
            .sorted(
                Comparator.comparingInt(
                        (String actionKey) ->
                            getShard(
                                flagKey + "-" + subjectKey + "-" + actionKey,
                                BANDIT_ASSIGNMENT_SHARDS))
                    .thenComparing(actionKey -> actionKey))
            .collect(Collectors.toList());

    // Select action from the shuffled actions, based on weight
    double assignedShard = getShard(flagKey + "-" + subjectKey, BANDIT_ASSIGNMENT_SHARDS);
    double assignmentWeightThreshold = assignedShard / (double) BANDIT_ASSIGNMENT_SHARDS;
    double cumulativeWeight = 0;
    String assignedAction = null;
    for (String actionKey : shuffledActionKeys) {
      cumulativeWeight += actionWeights.get(actionKey);
      if (cumulativeWeight > assignmentWeightThreshold) {
        assignedAction = actionKey;
        break;
      }
    }
    return assignedAction;
  }
}
