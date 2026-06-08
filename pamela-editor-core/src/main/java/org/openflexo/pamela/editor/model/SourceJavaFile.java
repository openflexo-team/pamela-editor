package org.openflexo.pamela.editor.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Lightweight representation of a {@code .java} source file that exists in a
 * source directory but is <em>not</em> a discovered {@code @ModelEntity}.
 *
 * <p>No Spoon parsing is performed. The qualified name and package name are
 * derived purely from the file's position relative to its source directory.
 * The file content is read lazily on the first call to {@link #getContent()}.
 * </p>
 */
public class SourceJavaFile implements SourceElement {

    private final File file;
    private final File sourceDirectory;
    private final SourceMetaModel metaModel;

    /** Cached content — null until first read. */
    private String content;

    /** Cached qualified name — derived lazily from the package declaration. */
    private String qualifiedName;

    /**
     * Cached result of the {@code @ModelEntity} scan.
     * {@code null} means not yet scanned.
     */
    private Boolean potentialModelEntity;

    SourceJavaFile(File file, File sourceDirectory, SourceMetaModel metaModel) {
        this.file          = file;
        this.sourceDirectory = sourceDirectory;
        this.metaModel     = metaModel;
    }

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** The {@code .java} file on disk. */
    public File getFile() {
        return file;
    }

    /** Simple class name derived from the file name (without the {@code .java} extension). */
    public String getSimpleName() {
        String name = file.getName();
        return name.endsWith(".java") ? name.substring(0, name.length() - 5) : name;
    }

    /**
     * Fully qualified class name: {@code <packageDeclaration>.<simpleName>}.
     *
     * <p>The package is read from the {@code package} statement in the source file
     * (first 50 lines). Falls back to the simple name alone if no package declaration
     * is found (default package) or if the file cannot be read.</p>
     */
    public String getQualifiedName() {
        if (qualifiedName == null) {
            String pkg = readPackageDeclaration();
            qualifiedName = pkg.isEmpty() ? getSimpleName() : pkg + "." + getSimpleName();
        }
        return qualifiedName;
    }

    /**
     * Package name read from the source file's {@code package} statement.
     * Returns an empty string for the default package.
     */
    public String getPackageName() {
        String qn = getQualifiedName();
        int dot = qn.lastIndexOf('.');
        return dot >= 0 ? qn.substring(0, dot) : "";
    }

    private String readPackageDeclaration() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int linesRead = 0;
            while ((line = reader.readLine()) != null && linesRead < 50) {
                linesRead++;
                line = line.trim();
                if (line.startsWith("package ") && line.endsWith(";")) {
                    return line.substring("package ".length(), line.length() - 1).trim();
                }
                if (line.startsWith("public ") || line.startsWith("class ")
                        || line.startsWith("interface ") || line.startsWith("enum ")) {
                    break;
                }
            }
        } catch (IOException e) {
            // ignore — return default package
        }
        return "";
    }

    /**
     * Returns {@code true} if the file contains an {@code @ModelEntity} annotation,
     * suggesting it is a PAMELA model entity that has not yet been registered as a
     * root type.
     *
     * <p>Detection is purely textual — the file is scanned line by line for the
     * string {@code "@ModelEntity"}, without any Java parsing. The result is
     * cached after the first scan.</p>
     */
    public boolean isPotentialModelEntity() {
        if (potentialModelEntity == null) {
            potentialModelEntity = scanForModelEntityAnnotation();
        }
        return potentialModelEntity;
    }

    private boolean scanForModelEntityAnnotation() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("@ModelEntity")) {
                    return true;
                }
            }
        } catch (IOException e) {
            // ignore — assume not a model entity
        }
        return false;
    }

    /** The source directory this file belongs to. */
    public File getSourceDirectory() {
        return sourceDirectory;
    }

    /** The {@link SourceMetaModel} that owns this file. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    // -------------------------------------------------------------------------
    // Lazy content
    // -------------------------------------------------------------------------

    /**
     * Returns the full text content of the {@code .java} file.
     * The content is read from disk on the first call and cached for subsequent calls.
     *
     * @throws IOException if the file cannot be read
     */
    public String getContent() throws IOException {
        if (content == null) {
            content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        }
        return content;
    }

    /** Clears all caches so the next reads re-scan the file from disk. */
    public void invalidateContent() {
        content = null;
        qualifiedName = null;
        potentialModelEntity = null;
    }

    @Override
    public String toString() {
        return "SourceJavaFile(" + getQualifiedName() + ")";
    }
}
