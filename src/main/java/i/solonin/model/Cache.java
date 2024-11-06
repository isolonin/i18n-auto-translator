package i.solonin.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Cache {
    public Map<String, Translate> data = new HashMap<>();
}
