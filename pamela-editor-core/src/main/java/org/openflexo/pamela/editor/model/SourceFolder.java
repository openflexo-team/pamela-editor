package org.openflexo.pamela.editor.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one source directory registered on a {@link SourceMetaModel}
 * ({@link SourceMetaModel#addSourceDirectory(File)}).
 *
 * <p>A {@code SourceFolder} is a thin, stable wrapper around a {@link File}: it
 * exists purely so the browser can show source directories as first-class nodes
 * and nest their {@linkplain #getPackages() packages} underneath, mirroring how
 * {@link SourcePackage} nests its {@linkplain SourcePackage#getMembers() members}.
 * It carries no state of its own beyond the directory and the owning meta-model —
 * everything else ({@link #getPackages()}) is computed on demand.</p>
 *
 * <p>One instance is created per registered directory and kept for the lifetime
 * of the {@link SourceMetaModel} (see {@link SourceMetaModel#addSourceDirectory}
 * / {@link SourceMetaModel#removeSourceDirectory}), so identity is stable across
 * calls to {@link SourceMetaModel#getSourceFolders()}.</p>
 */
public class SourceFolder implements SourceElement {

    private final File directory;
    private final SourceMetaModel metaModel;

    SourceFolder(File directory, SourceMetaModel metaModel) {
        this.directory = directory;
        this.metaModel = metaModel;
    }

    /** The source directory on disk. */
    public File getDirectory() {
        return directory;
    }

    /** The owning meta-model. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    /** Absolute path of the source directory, for display/tooltip purposes. */
    public String getPath() {
        return directory.getPath();
    }

    /**
     * Display name: the directory's own name (last path segment), falling back
     * to the full path when the directory is a filesystem root.
     */
    public String getName() {
        String name = directory.getName();
        return name.isEmpty() ? directory.getPath() : name;
    }

    /**
     * Packages containing at least one {@code .java} file scanned from this
     * source directory, in the same order as {@link SourceMetaModel#getAllPackages()}.
     *
     * <p>A package whose files span several source directories (an unusual but
     * possible case — several roots sharing one package name) appears under
     * every directory that contributes at least one of its files.</p>
     */
    public List<SourcePackage> getPackages() {
        return metaModel.getPackagesForSourceDirectory(directory);
    }

    /**
     * All {@code @ModelEntity} types discovered across every {@linkplain #getPackages() package}
     * contributing to this source directory, in package order then per-package discovery order.
     * Computed lazily on every call — no separate state is maintained.
     */
    public List<SourceModelEntity> getEntities() {
        List<SourceModelEntity> result = new ArrayList<>();
        for (SourcePackage pkg : getPackages()) {
            result.addAll(pkg.getEntities());
        }
        return result;
    }

    /** Number of {@code @ModelEntity} types across every package of this source directory. */
    public int getEntitiesCount() {
        return getEntities().size();
    }

    /** Number of abstract entities across every package of this source directory. */
    public int getAbstractEntitiesCount() {
        int count = 0;
        for (SourceModelEntity e : getEntities()) {
            if (e.isAbstract()) count++;
        }
        return count;
    }

    /** Total number of declared properties across every entity of this source directory. */
    public int getTotalPropertiesCount() {
        int count = 0;
        for (SourceModelEntity e : getEntities()) {
            count += e.getDeclaredProperties().size();
        }
        return count;
    }

    @Override
    public String toString() {
        return "SourceFolder(" + directory.getPath() + ")";
    }
}
