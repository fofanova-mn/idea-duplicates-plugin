package ru.spbau.duplication;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Trinity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.refactoring.util.duplicates.Match;
import org.jetbrains.annotations.Nullable;

/**
 * Inspection for detecting code duplicates.
 *
 * @author Maria Fofanova
 */
public class DuplicationGlobalInspectionTool extends GlobalInspectionTool {

    /**
     * Runs inspection for detecting code duplicates.
     *
     * @param scope The scope where to detect duplicates in.
     * @param manager The inspection manager.
     * @param globalContext The context of running.
     * @param problemDescriptionsProcessor The processor that collects results of running the inspection.
     */
    @Override
    public void runInspection(AnalysisScope scope, final InspectionManager manager,
                              final GlobalInspectionContext globalContext,
                              final ProblemDescriptionsProcessor problemDescriptionsProcessor) {
        final DuplicationGlobalInspectionContext duplicationInspectionContext =
                globalContext.getExtension(DuplicationGlobalInspectionContext.KEY);
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

                    final ProblemDescriptor descriptor = computeElementsProblemDescriptor(null, matchInClass, elementsInSuperClass, manager);
                    if (descriptor != null) {
                        problemDescriptionsProcessor.addProblemElement(globalContext.getRefManager().getReference(matchInClass.getFile()), descriptor);
                    }
                }
            });
        }

        for (final Trinity<PsiClass, Match, PsiElement[]> problem : duplicationInspectionContext.getStatementsMatchesInRelatives()) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    final Match matchInClass = problem.getSecond();
                    final PsiElement[] elementsInSuperClass = problem.getThird();

                    final ProblemDescriptor descriptor = computeElementsProblemDescriptor(problem.getFirst(), matchInClass, elementsInSuperClass, manager);
                    if (descriptor != null) {
                        problemDescriptionsProcessor.addProblemElement(globalContext.getRefManager().getReference(matchInClass.getFile()), descriptor);
                    }
                }
            });
        }
    }

    /**
     * Creates a description of a method duplication.
     *
     * @param match Statements that duplicate the specified method.
     * @param method The specified method.
     * @param manager The manager of inspections.
     * @param appendClassNameInFix True if it is needed to specify class name before the method invocation.
     * @return The problem descriptor.
     */
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

    /**
     * Creates a description of lines duplication (part of methods).
     *
     * @param clazz Class where duplication was found.
     * @param matchInClass Lines of duplication.
     * @param elementsInSuperClass Elements in super class that were duplicated.
     * @param manager The inspection manager.
     * @return The descriptor of duplication.
     */
    @Nullable
    private ProblemDescriptor computeElementsProblemDescriptor(@Nullable PsiClass clazz, Match matchInClass,
                                                        PsiElement[] elementsInSuperClass, InspectionManager manager) {
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
                new DuplicateStatementsQuickFix(clazz, matchInClass, elementsInSuperClass)
        );
    }
}
