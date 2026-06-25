package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;

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
}
