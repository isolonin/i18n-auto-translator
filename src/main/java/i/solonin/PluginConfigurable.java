package i.solonin;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.ui.JBUI;
import i.solonin.model.Settings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class PluginConfigurable extends BaseConfigurable implements Configurable {
    private JCheckBox yandexActive;
    private JTextField yandexSecretKeyField;
    private final Settings settings;

    public PluginConfigurable() {
        this.settings = ServiceManager.getService(Settings.class);
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Yandex API Settings";
    }

    @Override
    public @Nullable JComponent createComponent() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = JBUI.insets(5);

        constraints.gridx = 0;
        constraints.gridy = 0;
        yandexActive = new JCheckBox();
        yandexActive.setSelected(settings.active);
        panel.add(new JLabel("Enable:"), constraints);
        constraints.gridx = 1;
        panel.add(yandexActive, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        yandexSecretKeyField = new JTextField(settings.yandexSecretKey, 30);
        panel.add(new JLabel("Yandex Secret Key:"), constraints);
        constraints.gridx = 1;
        panel.add(yandexSecretKeyField, constraints);

        return panel;
    }

    @Override
    public boolean isModified() {
        return settings.isModified(yandexActive, yandexSecretKeyField);
    }

    @Override
    public void apply() {
        settings.active = yandexActive.isSelected();
        settings.yandexSecretKey = yandexSecretKeyField.getText();
    }
}
