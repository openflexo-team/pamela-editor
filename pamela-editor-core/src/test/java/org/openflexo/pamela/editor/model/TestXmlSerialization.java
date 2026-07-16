package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;

/**
 * Tests for the XML-serialization mutation support ({@code xml-serialization-design.md}):
 * entity {@code @XMLElement}, property 3-state {@code @XMLAttribute}/{@code @XMLElement},
 * and the metamodel-wide fill-only-missing auto-annotation.
 *
 * <p>Uses {@code test/model1} (root {@code Foo1} → {@code Foo2}): {@code Foo2.name} is a
 * SINGLE {@code String} (string-convertible → attribute); {@code Foo1.foo2} is a SINGLE
 * model-entity reference (non-embedded → skipped by the auto-action).</p>
 */
public class TestXmlSerialization {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final File MODEL1_SRC = new File(
            System.getProperty("user.dir") + "/src/test/java/test/model1");

    private File copyDir(File src, File destParent) throws IOException {
        File dest = new File(destParent, src.getName());
        dest.mkdirs();
        File[] files = src.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    Files.copy(f.toPath(), new File(dest, f.getName()).toPath());
                }
            }
        }
        return dest;
    }

    private void writePamelaFile(File pamelaFile, File sourceDir, String... rootTypes) throws IOException {
        String relPath = pamelaFile.getParentFile().toPath().relativize(sourceDir.toPath())
                .toString().replace(File.separatorChar, '/');
        StringBuilder sb = new StringBuilder("{\n  \"name\": \"xml-test\",\n  \"sourceDirectories\": [\"")
                .append(relPath).append("\"],\n  \"rootTypes\": [");
        for (int i = 0; i < rootTypes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(rootTypes[i]).append('"');
        }
        sb.append("]\n}\n");
        try (FileWriter fw = new FileWriter(pamelaFile)) {
            fw.write(sb.toString());
        }
    }

    private SourceMetaModel load(String folder) throws IOException {
        File workDir = tmp.newFolder(folder);
        File srcCopy = copyDir(MODEL1_SRC, workDir);
        File pamelaFile = new File(workDir, "test.pamela");
        writePamelaFile(pamelaFile, srcCopy, "test.model1.Foo1");
        return SourceMetaModelSerializer.load(pamelaFile);
    }

    private String read(SourceMetaModel mm, String qn) throws IOException {
        return new String(Files.readAllBytes(
                mm.getEntity(qn).getCompilationUnit().getFile().toPath()));
    }

    // ---------------------------------------------------------------------

    @Test
    public void testEntityXmlElement() throws IOException {
        SourceMetaModel mm = load("entityXml");
        assertFalse(mm.getEntity("test.model1.Foo1").isXmlElement());

        // Each editable-inspector setter is followed by a rebuild in the running app
        // (the app reacts to the fired event) — so Spoon positions are fresh between edits.
        mm.getEntity("test.model1.Foo1").setXmlTag("myFoo");
        mm.flushAll();
        String src = read(mm, "test.model1.Foo1");
        assertTrue("@XMLElement inserted", src.contains("@XMLElement"));
        assertTrue("xmlTag written", src.contains("myFoo"));
        mm.rebuildMetaModel();
        assertTrue(mm.getEntity("test.model1.Foo1").isXmlElement());
        assertEquals("myFoo", mm.getEntity("test.model1.Foo1").getXmlTag());

        mm.getEntity("test.model1.Foo1").setXmlPrimary(true);
        mm.flushAll();
        assertTrue("primary written", read(mm, "test.model1.Foo1").contains("primary"));
        mm.rebuildMetaModel();
        assertTrue(mm.getEntity("test.model1.Foo1").isXmlPrimary());

        mm.getEntity("test.model1.Foo1").setXmlContext("ctx");
        mm.flushAll();
        assertTrue("context written", read(mm, "test.model1.Foo1").contains("context"));
        mm.rebuildMetaModel();
        assertEquals("ctx", mm.getEntity("test.model1.Foo1").getXmlContext());

        // Remove
        mm.getEntity("test.model1.Foo1").setXmlElement(false);
        mm.flushAll();
        assertFalse("@XMLElement removed", read(mm, "test.model1.Foo1").contains("@XMLElement"));
        mm.rebuildMetaModel();
        assertFalse(mm.getEntity("test.model1.Foo1").isXmlElement());
    }

    @Test
    public void testPropertyModeTransitions() throws IOException {
        SourceMetaModel mm = load("propXml");
        SourceModelProperty foo2 = mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("foo2");
        assertEquals(XmlSerializationMode.NONE, foo2.getXmlSerialization());

        // NONE -> ATTRIBUTE
        foo2.setXmlSerialization(XmlSerializationMode.ATTRIBUTE);
        mm.flushAll();
        String src = read(mm, "test.model1.Foo1");
        assertTrue(src.contains("@XMLAttribute"));
        assertFalse(src.contains("@XMLElement"));
        mm.rebuildMetaModel();
        assertEquals(XmlSerializationMode.ATTRIBUTE,
                mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("foo2").getXmlSerialization());

        // ATTRIBUTE -> ELEMENT (attribute must be dropped)
        SourceModelProperty p = mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("foo2");
        p.setXmlSerialization(XmlSerializationMode.ELEMENT);
        mm.flushAll();
        src = read(mm, "test.model1.Foo1");
        assertTrue(src.contains("@XMLElement"));
        assertFalse(src.contains("@XMLAttribute"));
        mm.rebuildMetaModel();

        // ELEMENT -> NONE
        p = mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("foo2");
        p.setXmlSerialization(XmlSerializationMode.NONE);
        mm.flushAll();
        src = read(mm, "test.model1.Foo1");
        assertFalse(src.contains("@XMLElement"));
        assertFalse(src.contains("@XMLAttribute"));
        mm.rebuildMetaModel();
        assertEquals(XmlSerializationMode.NONE,
                mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("foo2").getXmlSerialization());
    }

    @Test
    public void testSerializationChoicesRestrictedByType() throws IOException {
        SourceMetaModel mm = load("choices");
        // Foo2.name is a String (string-convertible) → NONE/ATTRIBUTE/ELEMENT.
        XmlSerializationMode[] nameChoices =
                mm.getEntity("test.model1.Foo2").getDeclaredProperties().get("name").getXmlSerializationChoices();
        assertEquals(3, nameChoices.length);
        // Foo1.foo2 is a model-entity reference (not string-convertible) → NONE/ELEMENT only.
        XmlSerializationMode[] refChoices =
                mm.getEntity("test.model1.Foo1").getDeclaredProperties().get("foo2").getXmlSerializationChoices();
        assertEquals(2, refChoices.length);
        assertEquals(XmlSerializationMode.NONE, refChoices[0]);
        assertEquals(XmlSerializationMode.ELEMENT, refChoices[1]);
    }

    @Test
    public void testAutoAnnotateFillsOnlyMissing() throws IOException {
        SourceMetaModel mm = load("autoXml");

        int added = mm.autoAnnotateXmlSerialization();
        // Foo1 @XMLElement + Foo2 @XMLElement + Foo2.name @XMLAttribute = 3.
        // Foo1.foo2 is a non-embedded reference → skipped.
        assertEquals(3, added);
        mm.flushAll();
        mm.rebuildMetaModel();

        SourceModelEntity foo1 = mm.getEntity("test.model1.Foo1");
        SourceModelEntity foo2 = mm.getEntity("test.model1.Foo2");
        assertTrue("Foo1 is an XML element", foo1.isXmlElement());
        assertTrue("Foo2 is an XML element", foo2.isXmlElement());
        assertEquals("string SINGLE → attribute", XmlSerializationMode.ATTRIBUTE,
                foo2.getDeclaredProperties().get("name").getXmlSerialization());
        assertEquals("non-embedded reference → skipped", XmlSerializationMode.NONE,
                foo1.getDeclaredProperties().get("foo2").getXmlSerialization());

        // Idempotent: a second run adds nothing.
        assertEquals(0, mm.autoAnnotateXmlSerialization());
    }
}
