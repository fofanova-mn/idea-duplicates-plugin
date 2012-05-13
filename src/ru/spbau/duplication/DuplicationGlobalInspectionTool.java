package ru.spbau.duplication;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.refactoring.util.duplicates.Match;
import org.jetbrains.annotations.Nullable;

/**
 * @author maria
 */
public class DuplicationGlobalInspectionTool extends GlobalInspectionTool {
    @Override
    public void runInspection(AnalysisScope scope, final InspectionManager manager, final GlobalInspectionContext globalContext, final ProblemDescriptionsProcessor problemDescriptionsProcessor) {
        final DuplicationGlobalInspectionContext duplicationInspectionContext = globalContext.getExtension(DuplicationGlobalInspectionContext.KEY);
        if (duplicationInspectionContext == null) {
            return;
        }

        for (final Pair<Match, PsiMethod> problem : duplicationInspectionContext.getUtilMatches()) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    final Match match = problem.getFirst();
                    final PsiMethod method = problem.getSecond();

                    final ProblemDescriptor descriptor = computeMethodProblemDescriptor(match, method, manager, true);
                    if (descriptor != null) {
                        problemDescriptionsProcessor.addProblemElement(globalContext.getRefManager().getReference(match.getFile()), descriptor);
                    }
                }
            });
        }

        for (final Pair<Match, PsiMethod> problem : duplicationInspectionContext.getHierarchyMatches()) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    final Match match = problem.getFirst();
                    final PsiMethod method = problem.getSecond();

                    final ProblemDescriptor descriptor = computeMethodProblemDescriptor(match, method, manager, false);
                    if (descriptor != null) {
                        problemDescriptionsProcessor.addProblemElement(globalContext.getRefManager().getReference(match.getFile()), descriptor);
                    }
                }
            });
        }

        for (final Pair<Match, PsiElement[]> problem : duplicationInspectionContext.getStatementsMatches()) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    final Match matchInClass = problem.getFirst();
                    final PsiElement[] elementsInSuperClass = problem.getSecond();

                    final ProblemDescriptor descriptor = computeElementsProblemDescriptor(matchInClass, elementsInSuperClass, manager);
                    if (descriptor != null) {
                        problemDescriptionsProcessor.addProblemElement(globalContext.getRefManager().getReference(matchInClass.getFile()), descriptor);
                    }
                }
            });
        }
    }

    @Nullable
    private ProblemDescriptor computeMethodProblemDescriptor(Match match, PsiMethod method, InspectionManager manager, boolean appendClassNameInFix) {
        final PsiClass psiClass = method.getContainingClass();
        return manager.createProblemDescriptor(
                match.getMatchStart(),
                match.getMatchEnd(),
                DuplicationBundle.message("duplicated.method.from.class",
                        psiClass == null ? "" : psiClass.getQualifiedName(),
                        method.getName(),
                        method.getContainingFile().getVirtualFile().getUrl(),
                        method.getTextOffset()),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                false,
                new DuplicateQuickFix(match, method, appendClassNameInFix)
        );
    }

    @Nullable
    private ProblemDescriptor computeElementsProblemDescriptor(Match matchInClass, PsiElement[] elementsInSuperClass, InspectionManager manager) {
        final PsiFile psiFileSuper = elementsInSuperClass[0].getContainingFile();
        final PsiFile psiFile = matchInClass.getFile();
        int startOffset = elementsInSuperClass[0].getTextRange().getStartOffset();
        int endOffset = elementsInSuperClass[elementsInSuperClass.length - 1].getTextRange().getEndOffset();
        return manager.createProblemDescriptor(
                matchInClass.getMatchStart(),
                matchInClass.getMatchEnd(),
                DuplicationBundle.message("duplicated.statements.from.class",
                        psiFileSuper.getName(),
                        psiFileSuper.getOriginalFile().getVirtualFile().getUrl(),
                        startOffset,
                        endOffset,
                        psiFile.getName(),
                        psiFile.getOriginalFile().getVirtualFile().getUrl(),
                        matchInClass.getMatchStart().getTextRange().getStartOffset(),
                        matchInClass.getMatchEnd().getTextRange().getEndOffset()
                ),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                false,
                new DuplicateStatementsQuickFix(matchInClass, elementsInSuperClass)
        );
    }
}
