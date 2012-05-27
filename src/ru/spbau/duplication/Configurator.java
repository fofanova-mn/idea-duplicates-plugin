package ru.spbau.duplication;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import inspectionDescriptions.Settings;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

/**
 * @author: maria
 */
public class Configurator implements Configurable {
    private JCheckBox findDuplicatesOfPublicCheckBox;
    private JCheckBox findDuplicatesOfStatementsInHierarchyCheckBox;
    private JCheckBox findDuplicatesOfMethodsCheckBox;
    private JCheckBox findDuplicatesOfStatementsInRelativesCheckBox;
    private JSlider minimumSizeOfDuplicateSlider;
    private JPanel mainPanel;

    @Nls
    @Override
    public String getDisplayName() {
        return "Duplications detector";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        Settings.setMinimumSizeOfDuplicate(minimumSizeOfDuplicateSlider.getValue());
        Settings.setFindDuplicatesOfMethods(findDuplicatesOfMethodsCheckBox.isSelected());
        Settings.setFindDuplicatesOfPublic(findDuplicatesOfPublicCheckBox.isSelected());
        Settings.setFindDuplicatesOfStatementsInHierarchy(findDuplicatesOfStatementsInHierarchyCheckBox.isSelected());
        Settings.setFindDuplicatesOfStatementsInRelatives(findDuplicatesOfStatementsInRelativesCheckBox.isSelected());
    }

    @Override
    public void reset() {
        minimumSizeOfDuplicateSlider.setValue(Settings.getMinimumSizeOfDuplicate());
        findDuplicatesOfMethodsCheckBox.setSelected(Settings.isFindDuplicatesOfMethods());
        findDuplicatesOfPublicCheckBox.setSelected(Settings.isFindDuplicatesOfPublic());
        findDuplicatesOfStatementsInHierarchyCheckBox.setSelected(Settings.isFindDuplicatesOfStatementsInHierarchy());
        findDuplicatesOfStatementsInRelativesCheckBox.setSelected(Settings.isFindDuplicatesOfStatementsInRelatives());
    }

    @Override
    public void disposeUIResources() {
    }
}
