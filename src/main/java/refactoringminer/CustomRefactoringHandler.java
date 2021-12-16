package refactoringminer;

import data.Globals;
import gr.uom.java.xmi.diff.CodeRange;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;

import java.util.*;

/**
 * Created by Dimitrios Zisis <zdimitris@outlook.com>
 * Date: 18/02/2021
 */
public class CustomRefactoringHandler extends RefactoringHandler {

    private final String filePath;

    public CustomRefactoringHandler(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void handle(String commitId, List<Refactoring> refactorings) {
        if (!refactorings.isEmpty())
            for (Refactoring refactoring : refactorings) {
                for (CodeRange codeRange : refactoring.rightSide()) {
                    if (codeRange.getFilePath().equals(filePath))
                        Globals.setHasRefactoring(true);
                }
            }
    }
}
