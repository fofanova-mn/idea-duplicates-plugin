package ru.spbau.duplication;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.extractMethod.ExtractMethodHandler;
import com.intellij.refactoring.util.duplicates.Match;
import com.intellij.refactoring.util.duplicates.MethodDuplicatesHandler;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author maria
 */
public class DuplicateStatementsQuickFix implements LocalQuickFix {
    private final Match match;
    private final PsiElement[] elements;

    public DuplicateStatementsQuickFix(Match match, PsiElement[] elements) {
        this.match = match;
        this.elements = elements;
    }

    @NotNull
    @Override
    public String getName() {
        return DuplicationBundle.message("extract.and.replace.quick.fix.name");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return DuplicationBundle.message("extract.and.replace.quick.fix.name");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        final PsiFile psiFile = elements[0].getContainingFile();
        final Editor editor = openEditor(project, psiFile);

        final DataContext dataContext = new DataContext() {
            @Override
            public Object getData(@NonNls String dataId) {
                if (dataId.equals(PlatformDataKeys.EDITOR.getName())) {
                    return editor;
                }
                if (dataId.equals(LangDataKeys.PSI_FILE.getName())) {
                    return psiFile;
                }
                return null;
            }
        };
        final int startOffset = elements[0].getTextOffset();
        new ExtractMethodHandler().invoke(project, elements, dataContext);
        final PsiReference referenceAt = psiFile.findReferenceAt(startOffset);
        final PsiElement targetElement = referenceAt == null ? null : referenceAt.resolve();
        if (targetElement instanceof PsiMethod) {
            PsiMethod psiMethod = (PsiMethod) targetElement;
            psiMethod.getModifierList().setModifierProperty("protected", true);

            // find match to update names
            for (Match newMatch : MethodDuplicatesHandler.hasDuplicates(match.getFile(), psiMethod)) {
                new DuplicateQuickFix(newMatch, psiMethod, false).applyFix(project, descriptor);
            }
        }
    }


    @Nullable
    private static Editor openEditor(final Project project, final PsiFile file) {
        final VirtualFile virtualFile = file.getVirtualFile();
        assert virtualFile != null;
        final OpenFileDescriptor fileDescriptor = new OpenFileDescriptor(project, virtualFile);
        return FileEditorManager.getInstance(project).openTextEditor(fileDescriptor, false);
    }
}
