package cloud.eppo.ufc.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BanditActions extends HashMap<String, DiscriminableAttributes> implements Actions {
  public BanditActions() {
    super();
  }

  public BanditActions(Map<String, DiscriminableAttributes> actionsWithContext) {
    super(actionsWithContext);
  }

  public BanditActions(Set<String> actionKeys) {
    this(actionKeys.stream().collect(Collectors.toMap(s -> s, s -> new ContextAttributes())));
  }
}
