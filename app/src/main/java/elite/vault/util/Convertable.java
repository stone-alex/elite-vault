package elite.vault.util;

import elite.vault.json.GsonFactory;
import elite.vault.json.ToJsonConvertible;
import elite.vault.yaml.ToYamlConvertable;
import elite.vault.yaml.YamlFactory;

public class Convertable implements ToJsonConvertible, ToYamlConvertable {

    @Override public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override public String toYaml() {
        return YamlFactory.toYaml(this);
    }
}
