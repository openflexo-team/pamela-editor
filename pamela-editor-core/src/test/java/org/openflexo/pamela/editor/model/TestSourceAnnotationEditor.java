package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the pure text transforms of {@link SourceAnnotationEditor},
 * focused on {@link SourceAnnotationEditor#setAnnotationParameter}.
 */
public class TestSourceAnnotationEditor {

    private int declOf(String src, String declaration) {
        return src.indexOf(declaration);
    }

    @Test
    public void testAddParameterToBareAnnotation() {
        String src = "@ModelEntity\npublic interface Foo {\n}\n";
        String out = SourceAnnotationEditor.setAnnotationParameter(
                src, declOf(src, "public interface"), "ModelEntity", "isAbstract", "true");
        assertEquals("@ModelEntity(isAbstract = true)\npublic interface Foo {\n}\n", out);
    }

    @Test
    public void testRemoveLastParameterDropsParens() {
        String src = "@ModelEntity(isAbstract = true)\npublic interface Foo {\n}\n";
        String out = SourceAnnotationEditor.setAnnotationParameter(
                src, declOf(src, "public interface"), "ModelEntity", "isAbstract", null);
        assertEquals("@ModelEntity\npublic interface Foo {\n}\n", out);
    }

    @Test
    public void testReplaceExistingParameter() {
        String src = "@ModelEntity(isAbstract = false)\npublic interface Foo {\n}\n";
        String out = SourceAnnotationEditor.setAnnotationParameter(
                src, declOf(src, "public interface"), "ModelEntity", "isAbstract", "true");
        assertEquals("@ModelEntity(isAbstract = true)\npublic interface Foo {\n}\n", out);
    }

    @Test
    public void testAppendParameterKeepsExisting() {
        String src = "\t@Getter(value = \"x\")\n\tString getX();\n";
        String out = SourceAnnotationEditor.setAnnotationParameter(
                src, declOf(src, "String getX"), "Getter", "defaultValue", "\"y\"");
        assertEquals("\t@Getter(value = \"x\", defaultValue = \"y\")\n\tString getX();\n", out);
    }

    @Test
    public void testRemoveOneOfSeveralParameters() {
        String src = "\t@Getter(value = \"x\", defaultValue = \"y\")\n\tString getX();\n";
        String out = SourceAnnotationEditor.setAnnotationParameter(
                src, declOf(src, "String getX"), "Getter", "defaultValue", null);
        assertEquals("\t@Getter(value = \"x\")\n\tString getX();\n", out);
    }

    @Test
    public void testListCardinalityArgumentNotConfusedByComma() {
        String src = "\t@Getter(value = \"x\", cardinality = Cardinality.LIST)\n\tList<X> getX();\n";
        String out = SourceAnnotationEditor.setAnnotationParameter(
                src, declOf(src, "List<X> getX"), "Getter", "defaultValue", "\"d\"");
        assertEquals("\t@Getter(value = \"x\", cardinality = Cardinality.LIST, defaultValue = \"d\")\n\tList<X> getX();\n", out);
    }

    @Test
    public void testRemoveAnnotationFromMultiAnnotationBlock() {
        // Mirrors model2 AbstractNode.incomingEdges: @Embedded sits BELOW @Getter,
        // and the declaration position points at the first annotation (@Getter).
        String src = "\t@Getter(value = X, cardinality = Cardinality.LIST)\n"
                + "\t@XMLElement(context = \"Incoming\")\n"
                + "\t@Embedded(closureConditions = { Y })\n"
                + "\t@CloningStrategy(StrategyType.CLONE)\n"
                + "\tpublic List<Edge> getIncomingEdges();\n";
        int declAtFirstAnnotation = src.indexOf("@Getter");
        String out = SourceAnnotationEditor.removeAnnotation(src, declAtFirstAnnotation, "Embedded");
        assertEquals("\t@Getter(value = X, cardinality = Cardinality.LIST)\n"
                + "\t@XMLElement(context = \"Incoming\")\n"
                + "\t@CloningStrategy(StrategyType.CLONE)\n"
                + "\tpublic List<Edge> getIncomingEdges();\n", out);
    }

    @Test
    public void testSetParameterOnFirstAnnotationOfBlock() {
        String src = "\t@Getter(value = X)\n\t@Embedded\n\tList<Edge> getX();\n";
        int declAtFirst = src.indexOf("@Getter");
        String out = SourceAnnotationEditor.setAnnotationParameter(
                src, declAtFirst, "Getter", "isDerived", "true");
        assertEquals("\t@Getter(value = X, isDerived = true)\n\t@Embedded\n\tList<Edge> getX();\n", out);
    }

    @Test
    public void testAnnotationNotFoundReturnsOriginal() {
        String src = "@ModelEntity\npublic interface Foo {\n}\n";
        String out = SourceAnnotationEditor.setAnnotationParameter(
                src, declOf(src, "public interface"), "XMLElement", "primary", "true");
        assertEquals(src, out);
    }

    // -------------------------------------------------------------------------
    // addImportEntry / removeImportEntry (imports-support-design.md §5.1)
    // -------------------------------------------------------------------------

    @Test
    public void testAddImportEntryCreatesNewAnnotation() {
        String src = "package test.model4;\n\npublic interface Foo {\n}\n";
        String out = SourceAnnotationEditor.addImportEntry(
                src, declOf(src, "public interface"), "Bar", null);
        assertEquals(
                "package test.model4;\n\n"
                        + "import org.openflexo.pamela.annotations.Import;\n\n"
                        + "import org.openflexo.pamela.annotations.Imports;\n\n"
                        + "@Imports({ @Import(Bar.class) })\n"
                        + "public interface Foo {\n}\n",
                out);
    }

    @Test
    public void testAddImportEntryEnsuresCrossPackageImport() {
        String src = "package test.model4;\n\npublic interface Foo {\n}\n";
        String out = SourceAnnotationEditor.addImportEntry(
                src, declOf(src, "public interface"), "Bar", "other.pkg.Bar");
        assertTrue(out.contains("import other.pkg.Bar;"));
        assertTrue(out.contains("@Imports({ @Import(Bar.class) })"));
    }

    @Test
    public void testAddImportEntryAppendsToExplicitArrayForm() {
        String src = "\t@Imports({ @Import(A.class) })\n\tpublic interface Foo {\n\t}\n";
        String out = SourceAnnotationEditor.addImportEntry(
                src, declOf(src, "public interface"), "B", null);
        assertEquals("\t@Imports({ @Import(A.class), @Import(B.class) })\n\tpublic interface Foo {\n\t}\n", out);
    }

    @Test
    public void testAddImportEntryNormalizesSugarForm() {
        String src = "\t@Imports(@Import(A.class))\n\tpublic interface Foo {\n\t}\n";
        String out = SourceAnnotationEditor.addImportEntry(
                src, declOf(src, "public interface"), "B", null);
        assertEquals("\t@Imports({ @Import(A.class), @Import(B.class) })\n\tpublic interface Foo {\n\t}\n", out);
    }

    @Test
    public void testAddImportEntryIsIdempotent() {
        String src = "\t@Imports({ @Import(A.class) })\n\tpublic interface Foo {\n\t}\n";
        String out = SourceAnnotationEditor.addImportEntry(
                src, declOf(src, "public interface"), "A", null);
        assertEquals(src, out);
    }

    @Test
    public void testRemoveImportEntryLeavesOtherEntries() {
        String src = "\t@Imports({ @Import(A.class), @Import(B.class) })\n\tpublic interface Foo {\n\t}\n";
        String out = SourceAnnotationEditor.removeImportEntry(
                src, declOf(src, "public interface"), "A");
        assertEquals("\t@Imports({ @Import(B.class) })\n\tpublic interface Foo {\n\t}\n", out);
    }

    @Test
    public void testRemoveImportEntryDownToEmptyRemovesWholeAnnotation() {
        String src = "\t@Imports({ @Import(A.class) })\n\tpublic interface Foo {\n\t}\n";
        String out = SourceAnnotationEditor.removeImportEntry(
                src, declOf(src, "public interface"), "A");
        assertEquals("\tpublic interface Foo {\n\t}\n", out);
    }

    @Test
    public void testRemoveImportEntryMatchesByLastSegmentAgainstQualifiedEntry() {
        // Entry written fully-qualified in source; removal by simple name still matches.
        String src = "\t@Imports({ @Import(other.pkg.A.class), @Import(B.class) })\n\tpublic interface Foo {\n\t}\n";
        String out = SourceAnnotationEditor.removeImportEntry(
                src, declOf(src, "public interface"), "A");
        assertEquals("\t@Imports({ @Import(B.class) })\n\tpublic interface Foo {\n\t}\n", out);
    }

    @Test
    public void testRemoveImportEntryNotPresentReturnsOriginal() {
        String src = "\t@Imports({ @Import(A.class) })\n\tpublic interface Foo {\n\t}\n";
        String out = SourceAnnotationEditor.removeImportEntry(
                src, declOf(src, "public interface"), "NotThere");
        assertEquals(src, out);
    }

    @Test
    public void testRemoveImportEntryNoImportsAnnotationReturnsOriginal() {
        String src = "public interface Foo {\n}\n";
        String out = SourceAnnotationEditor.removeImportEntry(
                src, declOf(src, "public interface"), "A");
        assertEquals(src, out);
    }
}
