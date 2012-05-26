package ru.spbau.duplication;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.refactoring.extractMethod.ExtractMethodProcessor;

/**
 * Class of processor that extracts lines into new methods.
 * @author Maria Fofanova
 */
public class ExtractMethodProcessorEx extends ExtractMethodProcessor {
    /**
     * Constructs a processor for extracting lines into methods.
     *
     * @param project The project that contains lines.
     * @param editor The editor that shows extracted lines.
     * @param elements The lines to extract.
     * @param forcedReturnType The return type.
     * @param refactoringName The refactoring name.
     * @param initialMethodName The name of method.
     * @param helpId The id of help.
     */
    public ExtractMethodProcessorEx(Project project, Editor editor, PsiElement[] elements, PsiType forcedReturnType, String refactoringName, String initialMethodName, String helpId) {
        super(project, editor, elements, forcedReturnType, refactoringName, initialMethodName, helpId);
    }

    /**
     * Getter for the only output variable.
     *
     * @return The only output variable.
     */
    public PsiVariable getOutputVariable() {
        return myOutputVariable;
    }

    /**
     * Getter for output variables.
     *
     * @return Output variables.
     */
    public PsiVariable[] getOutputVariables() {
        return myOutputVariables;
    }
}
