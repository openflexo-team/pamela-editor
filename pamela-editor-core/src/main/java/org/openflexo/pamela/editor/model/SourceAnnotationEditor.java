package org.openflexo.pamela.editor.model;

/**
 * Targeted, text-level editing of Java annotations in source files.
 *
 * <p>Annotations are added and removed by precise textual insertion/deletion in
 * the source rather than by mutating the Spoon AST and re-printing the whole
 * file. This is deliberate: Spoon's {@code SniperJavaPrettyPrinter} has a known
 * defect when <em>inserting</em> a brand-new annotation adjacent to existing
 * modifiers (it omits the separator, producing tokens such as
 * {@code @ModelEntitypublic}), and the default printer reformats the entire
 * file. A targeted text edit is minimal-diff by construction: it preserves the
 * user's formatting, comments and layout everywhere except the single inserted
 * (or removed) line.</p>
 *
 * <p>The methods are pure functions over the source string. Locating the
 * declaration to annotate is the caller's responsibility — the byte offset is
 * typically obtained from Spoon's {@code CtElement.getPosition().getSourceStart()}.
 * Persisting the result is also the caller's responsibility (write the file,
 * then trigger a meta-model rebuild).</p>
 *
 * <p>This helper is reused by every annotation-editing operation: declaring an
 * entity ({@code @ModelEntity}), and the future add/remove of {@code @Embedded},
 * {@code @XMLElement}, {@code @Getter} parameters, etc.</p>
 */
public final class SourceAnnotationEditor {

    private SourceAnnotationEditor() {
        // utility class — not instantiable
    }

    /**
     * Inserts {@code @simpleName} on its own line directly above the declaration
     * that begins at offset {@code declarationStart}, matching the declaration's
     * indentation, and adds {@code import qualifiedName;} after the package
     * statement if it is not already present.
     *
     * <p>Idempotent: if an {@code @simpleName} annotation is already present on
     * the line(s) immediately preceding the declaration, the source is returned
     * unchanged.</p>
     *
     * @param source            the full source text
     * @param declarationStart  offset of the declaration to annotate
     *                          (e.g. {@code position.getSourceStart()})
     * @param simpleName        the annotation simple name, e.g. {@code "ModelEntity"}
     * @param qualifiedName     the annotation fully qualified name, e.g.
     *                          {@code "org.openflexo.pamela.annotations.ModelEntity"};
     *                          may be {@code null} to skip import handling
     * @return the edited source text
     */
    public static String addAnnotation(String source, int declarationStart,
                                       String simpleName, String qualifiedName) {
        if (source == null || declarationStart < 0 || declarationStart > source.length()) {
            throw new IllegalArgumentException("Invalid declaration offset");
        }

        // Start of the declaration's line.
        int lineStart = source.lastIndexOf('\n', declarationStart - 1) + 1;

        // Idempotency: already annotated just above?
        if (isAlreadyAnnotated(source, lineStart, simpleName)) {
            return ensureImport(source, qualifiedName);
        }

        String indent = leadingWhitespace(source, lineStart);
        String annotationLine = indent + "@" + simpleName + "\n";

        String withAnnotation = source.substring(0, lineStart)
                + annotationLine
                + source.substring(lineStart);

        return ensureImport(withAnnotation, qualifiedName);
    }

    /**
     * Removes the {@code @simpleName} annotation line that sits immediately above
     * the declaration starting at {@code declarationStart}, if present. The
     * import is intentionally left in place (it may be used elsewhere, and a
     * later rebuild with auto-imports will tidy it).
     *
     * @return the edited source text, or the original if no such annotation line
     *         was found
     */
    public static String removeAnnotation(String source, int declarationStart, String simpleName) {
        if (source == null || declarationStart < 0 || declarationStart > source.length()) {
            throw new IllegalArgumentException("Invalid declaration offset");
        }
        int lineStart = source.lastIndexOf('\n', declarationStart - 1) + 1;
        String token = "@" + simpleName;

        // Walk backwards over preceding lines looking for the annotation line.
        int cursor = lineStart;
        while (cursor > 0) {
            int prevLineStart = source.lastIndexOf('\n', cursor - 2) + 1;
            String line = source.substring(prevLineStart, cursor).trim();
            if (line.equals(token) || line.startsWith(token + "(") || line.startsWith(token + " ")) {
                return source.substring(0, prevLineStart) + source.substring(cursor);
            }
            if (!line.isEmpty() && !line.startsWith("@")) {
                break; // hit non-annotation content — stop searching
            }
            cursor = prevLineStart;
        }
        return source;
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private static boolean isAlreadyAnnotated(String source, int lineStart, String simpleName) {
        String token = "@" + simpleName;
        int cursor = lineStart;
        while (cursor > 0) {
            int prevLineStart = source.lastIndexOf('\n', cursor - 2) + 1;
            String line = source.substring(prevLineStart, cursor).trim();
            if (line.equals(token) || line.startsWith(token + "(") || line.startsWith(token + " ")) {
                return true;
            }
            if (!line.isEmpty() && !line.startsWith("@")) {
                return false; // reached the previous statement
            }
            cursor = prevLineStart;
        }
        return false;
    }

    private static String leadingWhitespace(String source, int lineStart) {
        int i = lineStart;
        while (i < source.length() && (source.charAt(i) == ' ' || source.charAt(i) == '\t')) {
            i++;
        }
        return source.substring(lineStart, i);
    }

    /**
     * Adds {@code import qualifiedName;} after the {@code package} statement if it
     * is absent. Types in {@code java.lang} or in the default package are skipped.
     */
    private static String ensureImport(String source, String qualifiedName) {
        if (qualifiedName == null || !qualifiedName.contains(".")
                || qualifiedName.startsWith("java.lang.")) {
            return source;
        }
        String importStatement = "import " + qualifiedName + ";";
        if (source.contains(importStatement)) {
            return source;
        }
        int pkgIndex = source.indexOf("package ");
        if (pkgIndex >= 0) {
            int semicolon = source.indexOf(';', pkgIndex);
            if (semicolon >= 0) {
                int after = semicolon + 1;
                return source.substring(0, after) + "\n\n" + importStatement + source.substring(after);
            }
        }
        // No package statement: prepend the import.
        return importStatement + "\n\n" + source;
    }
}
