package ru.spbau.duplication;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.GlobalInspectionContext;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.ex.Tools;
import com.intellij.codeInspection.lang.GlobalInspectionContextExtension;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.extractMethod.PrepareFailedException;
import com.intellij.refactoring.util.RefactoringUtil;
import com.intellij.refactoring.util.duplicates.DuplicatesFinder;
import com.intellij.refactoring.util.duplicates.Match;
import com.intellij.refactoring.util.duplicates.MethodDuplicatesHandler;
import com.intellij.refactoring.util.duplicates.VariableReturnValue;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import inspectionDescriptions.Settings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Class of the duplication inspection context.
 *
 * @author: Maria Fofanova
 */
public class DuplicationGlobalInspectionContext
        implements GlobalInspectionContextExtension<DuplicationGlobalInspectionContext> {

    public static final Key<DuplicationGlobalInspectionContext> KEY = Key.create("DuplicationGlobalInspectionContext");

    private final List<Pair<Match, PsiMethod>> utilMatches = new ArrayList<Pair<Match, PsiMethod>>();
    private final List<Pair<Match, PsiMethod>> hierarchyMatches = new ArrayList<Pair<Match, PsiMethod>>();
    private final List<Pair<Match, PsiElement[]>> statementsMatches = new ArrayList<Pair<Match, PsiElement[]>>();
    private final List<Trinity<PsiClass, Match, PsiElement[]>> statementsMatchesInRelatives = new ArrayList<Trinity<PsiClass, Match, PsiElement[]>>();

    @Override
    public Key<DuplicationGlobalInspectionContext> getID() {
        return KEY;
    }

    public List<Pair<Match, PsiMethod>> getUtilMatches() {
        return utilMatches;
    }

    public List<Pair<Match, PsiMethod>> getHierarchyMatches() {
        return hierarchyMatches;
    }

    public List<Pair<Match, PsiElement[]>> getStatementsMatches() {
        return statementsMatches;
    }

    public List<Trinity<PsiClass, Match, PsiElement[]>> getStatementsMatchesInRelatives() {
        return statementsMatchesInRelatives;
    }

    @Override
    public void performPostRunActivities(List<InspectionProfileEntry> inspections, GlobalInspectionContext context) {
    }

    @Override
    public void cleanup() {
        utilMatches.clear();
        hierarchyMatches.clear();
        statementsMatches.clear();
        statementsMatchesInRelatives.clear();
    }

    @Override
    public void performPreRunActivities(List<Tools> globalTools,
                                        List<Tools> localTools,
                                        final GlobalInspectionContext context) {
        final GlobalSearchScope libraryScope = GlobalSearchScope.EMPTY_SCOPE;

        final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        if (Settings.isFindDuplicatesOfPublic())
            findDuplicates(context, findUtilMethods(context, libraryScope, indicator), indicator);
        final Set<PsiClass> classes = findClasses(context, libraryScope, indicator);
        if (Settings.isFindDuplicatesOfMethods())
            findDuplicatesInHierarchy(context, classes, indicator);
        if (Settings.isFindDuplicatesOfStatementsInHierarchy())
            findStatementsDuplicates(context, classes, indicator);
        if (Settings.isFindDuplicatesOfStatementsInRelatives())
            findStatementsDuplicatesInRelatives(context, classes, indicator);


    }

    private void findDuplicates(final GlobalInspectionContext context,
                                final Set<PsiMethod> utilMethods,
                                final ProgressIndicator indicator) {
        final SearchScope searchScope = context.getRefManager().getScope().toSearchScope();
        for (final VirtualFile virtualFile : FileTypeIndex
                .getFiles(JavaFileType.INSTANCE, (GlobalSearchScope) searchScope.union(GlobalSearchScope.EMPTY_SCOPE))) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    final PsiFile psiFile = PsiManager.getInstance(context.getProject()).findFile(virtualFile);
                    if (psiFile == null) {
                        return;
                    }
                    if (indicator != null) {
                        ProgressWrapper.unwrap(indicator)
                                .setText(DuplicationBundle.message("finding.duplicates.in.file", psiFile.getName()));
                    }

                    for (PsiMethod psiMethod : utilMethods) {
                        if (psiMethod.getBody() == null) {
                            continue;
                        }
                        for (Match match : MethodDuplicatesHandler.hasDuplicates(psiFile, psiMethod)) {
                            utilMatches.add(new Pair<Match, PsiMethod>(match, psiMethod));
                        }
                    }
                }
            });
        }
    }

    private Set<PsiMethod> findUtilMethods(final GlobalInspectionContext context,
                                           GlobalSearchScope libraryScope,
                                           @Nullable final ProgressIndicator indicator) {
        final Set<PsiMethod> utilMethods = new THashSet<PsiMethod>();
        processAllPsiFiles(context, libraryScope, new PsiElementProcessor<PsiFile>() {
            @Override
            public boolean execute(@NotNull PsiFile psiFile) {
                psiFile.accept(new PsiElementVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {
                        if (element instanceof PsiMethod && isPublicStatic((PsiMethod) element)) {
                            utilMethods.add((PsiMethod) element);
                        }
                        element.acceptChildren(this);
                    }
                });
                return true;
            }
        });
        return utilMethods;
    }

    /**
     * @return All classes from <code>context<code/>
     */
    private Set<PsiClass> findClasses(final GlobalInspectionContext context,
                                      GlobalSearchScope libraryScope, @Nullable final ProgressIndicator indicator) {
        final Set<PsiClass> classes = new THashSet<PsiClass>();
        processAllPsiFiles(context, libraryScope, new PsiElementProcessor<PsiFile>() {
            @Override
            public boolean execute(@NotNull PsiFile psiFile) {
                psiFile.accept(new PsiElementVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {
                        if (element instanceof PsiClass && isNormalClass((PsiClass) element)) {
                            classes.add((PsiClass) element);
                        }
                        element.acceptChildren(this);
                    }
                });
                return true;
            }
        });
        return classes;
    }

    private boolean isNormalClass(PsiClass clazz) {
        return !clazz.isAnnotationType() && !clazz.isEnum() && !clazz.isInterface();
    }

    private boolean isPublicStatic(PsiMethod method) {
        return method.getModifierList().hasExplicitModifier(PsiModifier.PUBLIC) && isStatic(method);
    }

    private boolean isStatic(PsiMethod method) {
        return method.getModifierList().hasExplicitModifier(PsiModifier.STATIC);
    }

    private boolean isPrivate(PsiMethod method) {
        return method.getModifierList().hasExplicitModifier(PsiModifier.PRIVATE);
    }

    private void findDuplicatesInEachClass(GlobalInspectionContext context,
                                           Set<PsiClass> classes,
                                           final ProgressIndicator indicator) {
        for (final PsiClass clazz : classes) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    if (indicator != null) {
                        ProgressWrapper.unwrap(indicator).setText(
                                DuplicationBundle.message("finding.duplicates.in.class", clazz.getQualifiedName()));
                    }
                    final List<PsiMethod> methods = new ArrayList<PsiMethod>(Arrays.asList(clazz.getMethods()));
                    for (PsiMethod psiMethod : methods) {
                        if (psiMethod.getBody() == null) {
                            continue;
                        }
                        for (Match match : MethodDuplicatesHandler
                                .hasDuplicates(clazz.getContainingFile(), psiMethod)) {
                            final TextRange matchRange = TextRange
                                    .create(match.getMatchStart().getTextOffset(), match.getMatchEnd().getTextOffset());
                            if (clazz.getTextRange().contains(matchRange)) {
                                hierarchyMatches.add(new Pair<Match, PsiMethod>(match, psiMethod));
                            }
                        }
                    }
                }
            });
        }

    }

    private void findDuplicatesInHierarchy(GlobalInspectionContext context,
                                           Set<PsiClass> classes,
                                           final ProgressIndicator indicator) {
        for (final PsiClass clazz : classes) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    if (indicator != null) {
                        ProgressWrapper.unwrap(indicator).setText(
                                DuplicationBundle.message("finding.duplicates.in.class", clazz.getQualifiedName()));
                    }
                    final List<PsiMethod> superMethods = new ArrayList<PsiMethod>(Arrays.asList(clazz.getAllMethods()));
                    superMethods.removeAll(Arrays.asList(clazz.getMethods()));
                    for (PsiMethod psiMethod : superMethods) {
                        if (psiMethod.getBody() == null || isPrivate(psiMethod) || isStatic(psiMethod)) {
                            continue;
                        }
                        for (Match match : MethodDuplicatesHandler
                                .hasDuplicates(clazz.getContainingFile(), psiMethod)) {
                            final TextRange matchRange = TextRange
                                    .create(match.getMatchStart().getTextOffset(), match.getMatchEnd().getTextOffset());
                            if (clazz.getTextRange().contains(matchRange)) {
                                hierarchyMatches.add(new Pair<Match, PsiMethod>(match, psiMethod));
                            }
                        }
                    }
                }
            });
        }
    }

    private void processAllPsiFiles(final GlobalInspectionContext context,
                                    GlobalSearchScope libraryScope, final PsiElementProcessor<PsiFile> processor) {
        final SearchScope searchScope = context.getRefManager().getScope().toSearchScope();
        for (final VirtualFile virtualFile : FileTypeIndex
                .getFiles(JavaFileType.INSTANCE, (GlobalSearchScope) searchScope.union(libraryScope))) {
            if (!ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
                @Override
                public Boolean compute() {
                    final PsiFile psiFile = PsiManager.getInstance(context.getProject()).findFile(virtualFile);
                    return psiFile == null || processor.execute(psiFile);

                }
            })) {
                break;
            }
        }
    }

    private void findStatementsDuplicates(final GlobalInspectionContext context,
                                          Set<PsiClass> classes,
                                          final ProgressIndicator indicator) {
        final AnalysisScope searchScope = context.getRefManager().getScope();
        for (final PsiClass clazz : classes) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    if (indicator != null) {
                        ProgressWrapper.unwrap(indicator).setText(DuplicationBundle
                                .message("finding.statements.duplicates.in.class", clazz.getQualifiedName()));
                    }
                    suggestDuplication(context, clazz, clazz,  new MatchHandler() {
                        @Override
                        public void handle(Match match, PsiElement[] elements) {
                            statementsMatches.add(new Pair<Match, PsiElement[]>(match, elements));
                        }
                    });
                    for (PsiClass superClazz : clazz.getSupers()) {
                        if (superClazz.isInterface() || superClazz.isEnum()) {
                            continue;
                        }
                        if (searchScope.contains(superClazz)) {
                            suggestDuplication(context, clazz, superClazz, new MatchHandler() {
                                @Override
                                public void handle(Match match, PsiElement[] elements) {
                                    statementsMatches.add(new Pair<Match, PsiElement[]>(match, elements));
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    private void findStatementsDuplicatesInRelatives(final GlobalInspectionContext context,
                                                     final Set<PsiClass> classes,
                                                     final ProgressIndicator indicator) {
        final AnalysisScope searchScope = context.getRefManager().getScope();
        for (final PsiClass clazz1 : classes) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    if (indicator != null) {
                        ProgressWrapper.unwrap(indicator).setText(DuplicationBundle
                                .message("finding.statements.duplicates.in.class", clazz1.getQualifiedName()));
                    }
                    for (final PsiClass clazz2 : classes) {
                        final PsiClass commonParent = findCommonParent(clazz1, clazz2);
                        if (commonParent != null && searchScope.contains(commonParent)) {
                            suggestDuplication(context, clazz1, clazz2, new MatchHandler() {
                                @Override
                                public void handle(Match match, PsiElement[] elements) {
                                    statementsMatchesInRelatives
                                            .add(new Trinity<PsiClass, Match, PsiElement[]>(commonParent, match,
                                                    elements));
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    @Nullable
    private PsiClass findCommonParent(PsiClass clazz1, PsiClass clazz2) {
        final List<PsiClass> supers1 = getSupers(clazz1);
        final List<PsiClass> supers2 = getSupers(clazz2);
        Collections.reverse(supers1);
        Collections.reverse(supers2);
        PsiClass result = null;
        int i = 0;
        while (i < supers1.size() && i < supers2.size()) {
            PsiClass aClass = supers1.get(i);
            PsiClass bClass = supers2.get(i);
            if (bClass.equals(aClass)) {
                ++i;
                result = aClass;
                continue;
            }
            return result;
        }
        return result;
    }

    private List<PsiClass> getSupers(PsiClass clazz1) {
        final List<PsiClass> result = new ArrayList<PsiClass>();
        PsiClass parent = clazz1.getSuperClass();
        while (parent != null) {
            result.add(parent);
            parent = parent.getSuperClass();
        }
        return result;
    }

    private boolean canBeDuplication(Match match, PsiElement elements[]) {
        final PsiClass childPsiClass = PsiTreeUtil.getParentOfType(match.getMatchStart(), PsiClass.class);
        final PsiClass parentPsiClass = PsiTreeUtil.getParentOfType(elements[0], PsiClass.class);
        final PsiMethod childPsiMethod = PsiTreeUtil.getParentOfType(match.getMatchStart(), PsiMethod.class);
        final PsiMethod parentPsiMethod = PsiTreeUtil.getParentOfType(elements[0], PsiMethod.class);

        if(childPsiClass.equals(parentPsiClass)) {
            return true;
        }
        if(childPsiClass.isInheritorDeep(parentPsiClass, null)) {
            return !isPrivate(parentPsiMethod) || (isStatic(childPsiMethod) && !isStatic(parentPsiMethod));
        }
        if(findCommonParent(childPsiClass, parentPsiClass) != null) return true;
        return false;
    }

    private void suggestDuplication(final GlobalInspectionContext context,
                                    final PsiClass clazz,
                                    final PsiClass superClazz,
                                    final MatchHandler handler) {
        superClazz.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitCodeBlock(PsiCodeBlock block) {
                final List<PsiElement> childrenList = ContainerUtil
                        .filter(block.getChildren(), new Condition<PsiElement>() {
                            @Override
                            public boolean value(PsiElement psiElement) {
                                return !(psiElement instanceof PsiWhiteSpace || psiElement instanceof PsiComment);
                            }
                        });
                PsiElement[] children = childrenList.toArray(new PsiElement[childrenList.size()]);
                children = Arrays.copyOfRange(children, 1, children.length - 1);
                for (int len = children.length; len > Settings.getMinimumSizeOfDuplicate() - 1; --len) {
                    for (int i = 0; i < children.length - len + 1; ++i) {
                        final PsiElement[] subChildren = Arrays.copyOfRange(children, i, i + len);
                        if (!processStatements(context, clazz, subChildren, handler)) {
                            return;
                        }
                    }
                }
                super.visitCodeBlock(block);
            }
        });
    }

    private boolean processStatements(GlobalInspectionContext context,
                                      PsiClass targetClass,
                                      PsiElement[] elements,
                                      MatchHandler handler) {
        // from com.intellij.refactoring.extractMethod.ExtractMethodHandler#getProcessor
        for (PsiElement element : elements) {
            if (element instanceof PsiStatement && RefactoringUtil
                    .isSuperOrThisCall((PsiStatement) element, true, true)) {
                return false;
            }
        }

        final ExtractMethodProcessorEx processor = new ExtractMethodProcessorEx(context.getProject(), null, elements,
                null, "", "", null);
        processor.setShowErrorDialogs(false);
        try {
            if (!processor.prepare(null)) {
                return true;
            }
        } catch (PrepareFailedException e) {
            return true;
        }
        final PsiVariable[] myOutputVariables = processor.getOutputVariables();
        final PsiVariable myOutputVariable = processor.getOutputVariable();
        final DuplicatesFinder duplicatesFinder = new DuplicatesFinder(elements, processor.getInputVariables(),
                myOutputVariable != null ? new VariableReturnValue(myOutputVariable) : null,
                Arrays.asList(myOutputVariables));
        final List<Match> duplicates = duplicatesFinder.findDuplicates(targetClass);
        for (Match match : duplicates) {
            final PsiMethod psiMethod = PsiTreeUtil.getParentOfType(match.getMatchStart(), PsiMethod.class);
            final PsiMethod parentPsiMethod = PsiTreeUtil.getParentOfType(elements[0], PsiMethod.class);
            if (psiMethod == null || psiMethod.equals(parentPsiMethod)
                    || (isStatic(psiMethod) && !isStatic(parentPsiMethod))) {
                continue;
            }
            handler.handle(match, elements);
        }
        return duplicates.isEmpty();
    }

    private interface MatchHandler {
        void handle(Match match, PsiElement[] elements);
    }
}
