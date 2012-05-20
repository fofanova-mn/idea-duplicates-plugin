package ru.spbau.duplication;

import com.intellij.testFramework.InspectionTestCase;

/**
 * @author maria
 */
public class DuplicationGlobalInspectionToolTest extends InspectionTestCase {
    @Override
    protected String getTestDataPath() {
        return "testData/inspection";
    }

    private void doTest() throws Exception {
        doTest(getTestName(true), new DuplicationGlobalInspectionTool());
    }

    public void testUtil1() throws Throwable {
        doTest();
    }

    public void testInheritence1() throws Throwable {
        doTest();
    }

    public void testRelatives1() throws Throwable {
        doTest();
    }
}
