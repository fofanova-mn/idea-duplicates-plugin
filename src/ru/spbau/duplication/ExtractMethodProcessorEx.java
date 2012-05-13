package ru.spbau.duplication;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.refactoring.extractMethod.ExtractMethodProcessor;

/**
 * @author maria
 */
public class ExtractMethodProcessorEx extends ExtractMethodProcessor {
    public ExtractMethodProcessorEx(Project project, Editor editor, PsiElement[] elements, PsiType forcedReturnType, String refactoringName, String initialMethodName, String helpId) {
        super(project, editor, elements, forcedReturnType, refactoringName, initialMethodName, helpId);
    }

    public PsiVariable getOutputVariable() {
        return myOutputVariable;
    }

    public PsiVariable[] getOutputVariables() {
        return myOutputVariables;
    }
}
