package cloud.eppo;

import cloud.eppo.ufc.dto.*;
import java.util.*;
import java.util.stream.Collectors;

public class BanditEvaluator {

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
                  double actionScore = modelData.getDefaultActionScore();

                  // get all coefficients known to the model for this action
                  BanditCoefficients banditCoefficients =
                      modelData.getCoefficients().get(actionName);

                  if (banditCoefficients == null) {
                    // Unknown action; return default score of 0
                    return actionScore;
                  }

                  actionScore += banditCoefficients.getIntercept();
                  actionScore +=
                      scoreContextForCoefficients(
                          actionAttributes.getNumericAttributes(),
                          banditCoefficients.getActionNumericCoefficients());
                  actionScore +=
                      scoreContextForCoefficients(
                          actionAttributes.getCategoricalAttributes(),
                          banditCoefficients.getActionCategoricalCoefficients());
                  actionScore +=
                      scoreContextForCoefficients(
                          subjectAttributes.getNumericAttributes(),
                          banditCoefficients.getSubjectNumericCoefficients());
                  actionScore +=
                      scoreContextForCoefficients(
                          subjectAttributes.getCategoricalAttributes(),
                          banditCoefficients.getSubjectCategoricalCoefficients());

                  return actionScore;
                }));
  }

  private static double scoreContextForCoefficients(
      Attributes attributes, Map<String, ? extends BanditAttributeCoefficients> coefficients) {

    double totalScore = 0.0;

    for (BanditAttributeCoefficients attributeCoefficients : coefficients.values()) {
      EppoValue contextValue = attributes.get(attributeCoefficients.getAttributeKey());
      // The coefficient implementation knows how to score
      double attributeScore = attributeCoefficients.scoreForAttributeValue(contextValue);
      totalScore += attributeScore;
    }

    return totalScore;
  }

  private static Map<String, Double> weighActions(
      Map<String, Double> actionScores, double gamma, double actionProbabilityFloor) {
    Double highestScore = null;
    String highestScoredAction = null;
    for (Map.Entry<String, Double> actionScore : actionScores.entrySet()) {
      if (highestScore == null
          || actionScore.getValue() > highestScore
          || actionScore.getValue().equals(highestScore)
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

      // Compute weight and round to four decimal places
      double unboundedProbability =
          1 / (actionScores.size() + (gamma * (highestScore - actionScore.getValue())));
      double boundedProbability = Math.max(unboundedProbability, actionProbabilityFloor);
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
    // all be shifted to the
    // same new action at the same time.
    List<String> shuffledActionKeys =
        actionWeights.keySet().stream()
            .sorted(
                Comparator.comparingInt(
                        (String actionKey) ->
                            ShardUtils.getShard(
                                flagKey + "-" + subjectKey + "-" + actionKey,
                                BANDIT_ASSIGNMENT_SHARDS))
                    .thenComparing(actionKey -> actionKey))
            .collect(Collectors.toList());

    // Select action from the shuffled actions, based on weight
    double assignedShard =
        ShardUtils.getShard(flagKey + "-" + subjectKey, BANDIT_ASSIGNMENT_SHARDS);
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
