package inspectionDescriptions;

import com.intellij.ide.util.PropertiesComponent;

/**
 * Class for saving settings to the properties component.
 *
 * @author: Maria Fofanova
 */
public class Settings {
    private static final String minimumSizeOfDuplicate = "minimumSizeOfDuplicate";
    private static final String findDuplicatesOfMethods = "findDuplicatesOfMethods";
    private static final String findDuplicatesOfPublic = "findDuplicatesOfPublic";
    private static final String findDuplicatesOfStatementsInHierarchy = "findDuplicatesOfStatementsInHierarchy";
    private static final String findDuplicatesOfStatementsInRelatives = "findDuplicatesOfStatementsInRelatives";

    public static boolean isFindDuplicatesOfPublic() {
        return PropertiesComponent.getInstance().getBoolean(findDuplicatesOfPublic, true);
    }

    public static boolean isFindDuplicatesOfStatementsInHierarchy() {
        return PropertiesComponent.getInstance().getBoolean(findDuplicatesOfStatementsInHierarchy, true);
    }

    public static boolean isFindDuplicatesOfMethods() {
        return PropertiesComponent.getInstance().getBoolean(findDuplicatesOfMethods, true);
    }

    public static boolean isFindDuplicatesOfStatementsInRelatives() {
        return PropertiesComponent.getInstance().getBoolean(findDuplicatesOfStatementsInRelatives, true);
    }

    public static int getMinimumSizeOfDuplicate() {
        return Integer.parseInt(PropertiesComponent.getInstance().getValue(minimumSizeOfDuplicate, "2"));
    }


    public static void setFindDuplicatesOfPublic(boolean value) {
        PropertiesComponent.getInstance().setValue(findDuplicatesOfPublic, String.valueOf(value));
    }

    public static void setFindDuplicatesOfStatementsInHierarchy(boolean value) {
        PropertiesComponent.getInstance().setValue(findDuplicatesOfStatementsInHierarchy, String.valueOf(value));
    }

    public static void setFindDuplicatesOfMethods(boolean value) {
        PropertiesComponent.getInstance().setValue(findDuplicatesOfMethods, String.valueOf(value));
    }

    public static void setFindDuplicatesOfStatementsInRelatives(boolean value) {
        PropertiesComponent.getInstance().setValue(findDuplicatesOfStatementsInRelatives, String.valueOf(value));
    }

    public static void setMinimumSizeOfDuplicate(int value) {
        PropertiesComponent.getInstance().setValue(minimumSizeOfDuplicate, String.valueOf(value));
    }
}
