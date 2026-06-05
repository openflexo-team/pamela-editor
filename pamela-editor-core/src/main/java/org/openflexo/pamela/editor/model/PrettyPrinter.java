package org.openflexo.pamela.editor.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.openflexo.pamela.annotations.Getter;

/**
 * Package-private helper that produces a readable, indented tree-style ASCII
 * dump of a {@link SourceMetaModel}.
 *
 * <p>This class is the single implementation point for
 * {@link SourceMetaModel#prettyPrint()}. All other {@code Source*} classes
 * expose package-private {@code appendTo(PrettyPrinter)} methods so that
 * the public API surface stays minimal.</p>
 *
 * <p>The output format uses box-drawing characters (├──, └──, │) and is
 * grouped by package (packages sorted alphabetically, entities sorted
 * alphabetically within each package).</p>
 */
class PrettyPrinter {

    /** The PAMELA "not set" sentinel returned by {@code @Getter.defaultValue()}. */
    private static final String PAMELA_UNDEFINED = Getter.UNDEFINED;

    private final StringBuilder sb = new StringBuilder();

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    /**
     * Builds and returns the pretty-printed representation of the given
     * meta-model.
     *
     * @param metaModel the model to print (must not be {@code null})
     * @return the human-readable string
     */
    String print(SourceMetaModel metaModel) {
        sb.setLength(0);
        appendMetaModel(metaModel);
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // SourceMetaModel
    // -------------------------------------------------------------------------

    private void appendMetaModel(SourceMetaModel mm) {
        // Header
        String displayName = mm.getName() != null ? mm.getName() : "<unnamed>";
        sb.append("SourceMetaModel '").append(displayName).append("'\n");

        // Source directories
        sb.append("  Directories : ");
        List<File> dirs = mm.getSourceDirectories();
        if (dirs.isEmpty()) {
            sb.append("(none)\n");
        } else {
            sb.append("[");
            for (int i = 0; i < dirs.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(dirs.get(i).getAbsolutePath());
            }
            sb.append("]\n");
        }

        // Root types
        sb.append("  Root types  : ");
        List<String> roots = mm.getRootTypeNames();
        if (roots.isEmpty()) {
            sb.append("(none)\n");
        } else {
            sb.append("[").append(String.join(", ", roots)).append("]\n");
        }

        // Global issues
        sb.append("  Issues      : ");
        List<Issue> mmIssues = mm.getIssues();
        if (mmIssues.isEmpty()) {
            sb.append("(none)\n");
        } else {
            sb.append("\n");
            for (Issue issue : mmIssues) {
                sb.append("    [").append(issue.getClass().getSimpleName()).append("] ")
                  .append(issue.getMessage()).append("\n");
            }
        }

        // Packages, sorted alphabetically
        List<SourcePackage> packages = new ArrayList<>(mm.getAllPackages());
        packages.sort(Comparator.comparing(SourcePackage::getQualifiedName));

        for (SourcePackage pkg : packages) {
            sb.append("\n");
            appendPackage(pkg);
        }
    }

    // -------------------------------------------------------------------------
    // SourcePackage
    // -------------------------------------------------------------------------

    private void appendPackage(SourcePackage pkg) {
        // Package header separator
        String pkgName = pkg.isDefault() ? "<default package>" : pkg.getQualifiedName();
        sb.append("  ── Package ").append(pkgName).append(" ");
        // Fill to 72 chars
        int fillerStart = 11 + pkgName.length() + 1;
        for (int i = fillerStart; i < 72; i++) {
            sb.append('─');
        }
        sb.append("\n  │\n");

        // Entities, sorted alphabetically by simple name
        List<SourceModelEntity> entities = new ArrayList<>(pkg.getEntities());
        entities.sort(Comparator.comparing(SourceModelEntity::getSimpleName));

        for (int i = 0; i < entities.size(); i++) {
            boolean last = (i == entities.size() - 1);
            appendEntity(entities.get(i), last);
        }
    }

    // -------------------------------------------------------------------------
    // SourceModelEntity
    // -------------------------------------------------------------------------

    private void appendEntity(SourceModelEntity entity, boolean lastInPackage) {
        String branch = lastInPackage ? "  └──" : "  ├──";
        String childPrefix = lastInPackage ? "       " : "  │    ";

        // Entity header line
        sb.append(branch).append(" [interface] ").append(entity.getSimpleName()).append("\n");

        // File name
        String fileName = "(unknown)";
        if (entity.getCompilationUnit() != null && entity.getCompilationUnit().getFile() != null) {
            fileName = entity.getCompilationUnit().getFile().getName();
        }
        sb.append(childPrefix).append("  File          : ").append(fileName).append("\n");

        // Abstract + InitPolicy
        sb.append(childPrefix).append("  Abstract      : ").append(entity.isAbstract())
          .append("   InitPolicy: ").append(entity.getInitPolicy()).append("\n");

        // Super entities
        sb.append(childPrefix).append("  Super entities: ");
        List<SourceModelEntity> supers = entity.getDirectSuperEntities();
        if (supers.isEmpty()) {
            sb.append("(none)\n");
        } else {
            List<String> names = new ArrayList<>();
            for (SourceModelEntity s : supers) {
                names.add(s.getSimpleName());
            }
            sb.append(String.join(", ", names)).append("\n");
        }

        // Implementation class
        sb.append(childPrefix).append("  Impl class    : ");
        SourceImplementationClass impl = entity.getImplementationClass();
        if (impl == null) {
            sb.append("(none)\n");
        } else {
            sb.append(impl.getSimpleName());
            if (impl.isAbstract()) {
                sb.append(" (abstract)");
            }
            sb.append("\n");
            // Custom methods
            List<SourceCustomMethod> methods = impl.getCustomMethods();
            if (!methods.isEmpty()) {
                sb.append(childPrefix).append("    Custom methods:\n");
                for (int i = 0; i < methods.size(); i++) {
                    boolean lastMethod = (i == methods.size() - 1);
                    SourceCustomMethod m = methods.get(i);
                    String methodBranch = lastMethod
                            ? childPrefix + "      └── "
                            : childPrefix + "      ├── ";
                    sb.append(methodBranch).append(m.getMethodName()).append("()");
                    if (m.isOverride()) {
                        sb.append(" [@Override]");
                    }
                    sb.append("\n");
                }
            }
        }

        // Initializers
        sb.append(childPrefix).append("  Initializers  : ");
        List<SourceModelInitializer> initializers = entity.getInitializers();
        if (initializers.isEmpty()) {
            sb.append("(none)\n");
        } else {
            sb.append("\n");
            for (SourceModelInitializer init : initializers) {
                sb.append(childPrefix).append("    ").append(init.getMethodName())
                  .append(formatInitializerParams(init)).append("\n");
            }
        }

        // Declared properties
        sb.append(childPrefix).append("  Properties (declared):\n");
        Map<String, SourceModelProperty> props = entity.getDeclaredProperties();
        if (props.isEmpty()) {
            sb.append(childPrefix).append("    (none)\n");
        } else {
            List<SourceModelProperty> propList = new ArrayList<>(props.values());
            for (int i = 0; i < propList.size(); i++) {
                boolean lastProp = (i == propList.size() - 1);
                appendProperty(propList.get(i), childPrefix, lastProp);
            }
        }

        // Entity-level issues
        List<Issue> entityIssues = entity.getIssues();
        if (!entityIssues.isEmpty()) {
            sb.append(childPrefix).append("  Issues:\n");
            for (Issue issue : entityIssues) {
                sb.append(childPrefix).append("    [")
                  .append(issue.getClass().getSimpleName()).append("] ")
                  .append(issue.getMessage()).append("\n");
            }
        }
    }

    // -------------------------------------------------------------------------
    // SourceModelProperty
    // -------------------------------------------------------------------------

    private void appendProperty(SourceModelProperty prop, String entityPrefix, boolean lastProp) {
        String propBranch = lastProp
                ? entityPrefix + "    └── "
                : entityPrefix + "    ├── ";
        String propChildPrefix = lastProp
                ? entityPrefix + "          "
                : entityPrefix + "    │     ";

        // Property header: id  [CARDINALITY]  qualifiedTypeName  ◀tag▶  @Embedded
        sb.append(propBranch)
          .append(prop.getPropertyIdentifier())
          .append("  [").append(prop.getCardinality()).append("]  ")
          .append(prop.getType().getQualifiedName());

        // Type annotation
        SourceType type = prop.getType();
        if (type.getModelEntity() != null) {
            sb.append("  ◀entity▶");
        } else if (type.isPrimitive()) {
            sb.append("  ◀primitive▶");
        }

        // @Embedded flag
        if (prop.isEmbedded()) {
            sb.append("  @Embedded");
        }

        // @Derived flag
        if (prop.isDerived()) {
            sb.append("  @Derived");
        }

        sb.append("\n");

        // Getter
        sb.append(propChildPrefix).append("getter  : ").append(prop.getGetterMethodName()).append("()\n");

        // Setter (SINGLE)
        if (prop.getSetterMethodName() != null) {
            sb.append(propChildPrefix).append("setter  : ").append(prop.getSetterMethodName()).append("()\n");
        }

        // Adder (LIST)
        if (prop.getAdderMethodName() != null) {
            sb.append(propChildPrefix).append("adder   : ").append(prop.getAdderMethodName()).append("()\n");
        }

        // Remover (LIST)
        if (prop.getRemoverMethodName() != null) {
            sb.append(propChildPrefix).append("remover : ").append(prop.getRemoverMethodName()).append("()\n");
        }

        // Reindexer
        if (prop.getReindexerMethodName() != null) {
            sb.append(propChildPrefix).append("reindexer: ").append(prop.getReindexerMethodName()).append("()\n");
        }

        // Updater
        if (prop.getUpdaterMethodName() != null) {
            sb.append(propChildPrefix).append("updater : ").append(prop.getUpdaterMethodName()).append("()\n");
        }

        // Embedded flag (for embedding constraints: already shown inline; skip closure/deletion details)
        sb.append(propChildPrefix).append("embedded: ").append(prop.isEmbedded()).append("\n");

        // Inverse
        String inverseId = prop.getInversePropertyIdentifier();
        if (inverseId != null && !inverseId.isEmpty() && !PAMELA_UNDEFINED.equals(inverseId)) {
            sb.append(propChildPrefix).append("inverse : ").append(inverseId).append("\n");
        }

        // Default value (skip PAMELA's UNDEFINED sentinel and empty string)
        String defaultVal = prop.getDefaultValue();
        if (defaultVal != null && !defaultVal.isEmpty() && !PAMELA_UNDEFINED.equals(defaultVal)) {
            sb.append(propChildPrefix).append("default : ").append(defaultVal).append("\n");
        }

        // Property-level issues
        List<Issue> propIssues = prop.getIssues();
        if (!propIssues.isEmpty()) {
            sb.append(propChildPrefix).append("Issues:\n");
            for (Issue issue : propIssues) {
                sb.append(propChildPrefix).append("  [")
                  .append(issue.getClass().getSimpleName()).append("] ")
                  .append(issue.getMessage()).append("\n");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Formats the parameter list of an initializer for display,
     * e.g. {@code "(name, flexoId)"} or {@code "()"}.
     */
    private static String formatInitializerParams(SourceModelInitializer init) {
        List<String> params = init.getParameters();
        if (params.isEmpty()) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            String p = params.get(i);
            sb.append(p.isEmpty() ? "?" : p);
        }
        sb.append(")");
        return sb.toString();
    }
}
