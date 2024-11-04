package i.solonin.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
public class Cache {
    public Map<String, Translate> data = new HashMap<>();

    public void putAsKey(String fileName, String key, String v1, String v2) {
        Translate translate = data.get(fileName);
        if (translate == null) {
            translate = new Translate();
            data.put(fileName, translate);
        }
        translate.putLocal(key, v1, v2);
    }

    public void putAsValue(String fileName, String v1, String v2) {
        Translate translate = data.get(fileName);
        if (translate == null) {
            translate = new Translate();
            data.put(fileName, translate);
        }
        translate.putTranslate(v1, v2);
    }

    public String getByKey(String fileName, String key, String v1) {
        return Optional.ofNullable(data.get(fileName)).map(t -> t.getByKey(key, v1)).orElse(null);
    }

    public String getByValue(String fileName, String value) {
        return Optional.ofNullable(data.get(fileName)).map(t -> t.getByValue(value)).orElse(null);
    }
}
