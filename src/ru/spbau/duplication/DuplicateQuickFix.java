package ru.spbau.duplication;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.refactoring.util.duplicates.Match;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Class of quick fix. Needed to replace duplicated code with  a call of duplicated method.
 *
 * @author: Maria Fofanova
 */
public class DuplicateQuickFix implements LocalQuickFix {
    private final Match match;
    private final PsiMethod method;
    private final boolean appendClassName;

    /**
     * Constructs a new quick fix.
     *
     * @param match Lines of duplicate.
     * @param method Method that was duplicated.
     * @param appendClassName True if it is needed to add the name of class before the method invocation.
     */
    public DuplicateQuickFix(Match match, PsiMethod method, boolean appendClassName) {
        this.match = match;
        this.method = method;
        this.appendClassName = appendClassName;
    }

    @NotNull
    @Override
    public String getName() {
        return DuplicationBundle.message("replace.quick.fix.name");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return DuplicationBundle.message("replace.quick.fix.name");
    }

    /**
     * Replaces lines with the invoking of method.
     *
     * @param project Project where duplicate is.
     * @param descriptor The description of duplication.
     */
    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        final PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        final PsiClass containingClass = method.getContainingClass();
        assert containingClass != null;
        final StringBuilder callText = new StringBuilder();
        if (appendClassName) {
            callText.append(containingClass.getName()).append(".");
        }
        callText.append(method.getName()).append("(");
        int i = 0;
        for (PsiVariable parameter : method.getParameterList().getParameters()) {
            if (i > 0) {
                callText.append(", ");
            }
            List<PsiElement> parameterValues = match.getParameterValues(parameter);
            callText.append(parameterValues == null || parameterValues.isEmpty() ?
                    "null" :
                    parameterValues.get(0).getText());
            ++i;
        }
        callText.append(")");
        PsiMethodCallExpression methodCallExpression =
                (PsiMethodCallExpression) factory.createExpressionFromText(callText.toString(), null);
        methodCallExpression = (PsiMethodCallExpression) CodeStyleManager.getInstance(method.getManager())
                .reformat(methodCallExpression);

        final PsiFile matchFile = match.getFile();
        match.replace(method, methodCallExpression, null);
        if (methodCallExpression.getMethodExpression().resolve() == null) {
            final PsiClass aClass = method.getContainingClass();
            assert aClass != null;
            JavaCodeStyleManager.getInstance(project).addImport((PsiJavaFile) matchFile, aClass);
        }
    }
}
