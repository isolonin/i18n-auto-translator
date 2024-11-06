package i.solonin.model;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@State(name = "i18nPluginSettings", storages = @Storage("i18n-auto-translator.xml"))
public class Settings implements PersistentStateComponent<Settings> {
    public boolean active;
    public int httpPool = 10;
    public long httpTimeout = 1000L;
    public String yandexSecretKey;
    @Transient
    public Map<String, Map<String, String>> local = new HashMap<>();
    public Cache cache = new Cache();

    @Override
    public @Nullable Settings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull Settings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public boolean isEnabled() {
        return active && !StringUtils.isEmpty(this.yandexSecretKey);
    }

    public boolean isModified(JCheckBox yandexEnable, JTextField yandexSecretKeyField) {
        return Optional.ofNullable(yandexEnable).map(AbstractButton::isSelected).map(b -> !b.equals(this.active)).orElse(false) ||
                isModified(yandexSecretKeyField, this.yandexSecretKey);
    }

    public boolean isModified(JTextField filed, String value) {
        return Optional.ofNullable(filed).map(JTextComponent::getText).map(t -> !Objects.equals(t, value)).orElse(false);
    }

    public void putAsLocal(String fileName, String key, String v1, String v2) {
        Map<String, String> values = local.computeIfAbsent(fileName, k -> new HashMap<>());
        values.put(key + "=" + v1, v2);
    }

    public void putAsTranslated(String language, String v1, String v2) {
        Translate translate = cache.data.get(language);
        if (translate == null) {
            translate = new Translate();
            cache.data.put(language, translate);
        }
        translate.put(v1, v2);
    }

    public String getByLocal(String fileName, String key, String v1) {
        return Optional.ofNullable(local.get(fileName)).map(t -> t.get(key + "=" + v1)).orElse(null);
    }

    public String getByTranslated(String language, String value) {
        return Optional.ofNullable(cache.data.get(language)).map(t -> t.get(value)).orElse(null);
    }
}
