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
        return addAnnotation(source, declarationStart, simpleName, qualifiedName, null);
    }

    /**
     * Like {@link #addAnnotation(String, int, String, String)} but with annotation
     * arguments, e.g. {@code argsText = "value = \"name\""} inserts
     * {@code @Getter(value = "name")}.
     *
     * @param argsText the annotation arguments (without the enclosing parentheses),
     *                 or {@code null}/empty for a marker annotation
     */
    public static String addAnnotation(String source, int declarationStart,
                                       String simpleName, String qualifiedName, String argsText) {
        if (source == null || declarationStart < 0 || declarationStart > source.length()) {
            throw new IllegalArgumentException("Invalid declaration offset");
        }

        // Start of the declaration's line.
        int lineStart = source.lastIndexOf('\n', declarationStart - 1) + 1;

        // Idempotency: already annotated just above?
        if (isAlreadyAnnotated(source, lineStart, simpleName)) {
            return ensureImport(source, qualifiedName);
        }

        String annotationText = "@" + simpleName
                + (argsText == null || argsText.isEmpty() ? "" : "(" + argsText + ")");
        String withAnnotation = insertAnnotationLine(source, declarationStart, annotationText);
        return ensureImport(withAnnotation, qualifiedName);
    }

    /**
     * Inserts {@code annotationText} (e.g. {@code "@Getter(value = \"name\")"}) on
     * its own line directly above the declaration starting at
     * {@code declarationStart}, matching its indentation. Does <b>not</b> touch
     * imports and does <b>not</b> check idempotency — the caller controls both.
     *
     * <p>When several annotations are inserted into the same source for different
     * declarations, apply them in <b>descending offset order</b> so each insertion
     * leaves the smaller (not-yet-processed) offsets valid.</p>
     */
    public static String insertAnnotationLine(String source, int declarationStart, String annotationText) {
        if (source == null || declarationStart < 0 || declarationStart > source.length()) {
            throw new IllegalArgumentException("Invalid declaration offset");
        }
        int lineStart = source.lastIndexOf('\n', declarationStart - 1) + 1;
        String indent = leadingWhitespace(source, lineStart);
        String line = indent + annotationText + "\n";
        return source.substring(0, lineStart) + line + source.substring(lineStart);
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
        int annAt = findAnnotation(source, declarationStart, simpleName);
        if (annAt < 0) {
            return source;
        }
        int lineStart = source.lastIndexOf('\n', annAt - 1) + 1;
        int lineEnd = source.indexOf('\n', annAt);
        if (lineEnd < 0) {
            lineEnd = source.length();
        }
        int removeEnd = lineEnd < source.length() ? lineEnd + 1 : lineEnd;
        return source.substring(0, lineStart) + source.substring(removeEnd);
    }

    /**
     * Sets, replaces, or removes a named parameter of an annotation already
     * present on the declaration at {@code declarationStart}.
     *
     * <p>{@code valueExpression} is the raw Java expression for the value
     * (e.g. {@code "true"}, {@code "\"abc\""}, {@code "Cardinality.LIST"});
     * {@code null} removes the parameter. Handles an annotation with no
     * parentheses ({@code @ModelEntity} → {@code @ModelEntity(isAbstract = true)})
     * and existing parameters (the parameter is replaced in place or appended;
     * removing the last one drops the parentheses). Minimal-diff text edit — no
     * AST re-print.</p>
     *
     * @return the edited source, or the original if the annotation is not found
     */
    public static String setAnnotationParameter(String source, int declarationStart,
            String annotationSimpleName, String parameterName, String valueExpression) {
        if (source == null || declarationStart < 0 || declarationStart > source.length()) {
            throw new IllegalArgumentException("Invalid declaration offset");
        }
        int annStart = findAnnotation(source, declarationStart, annotationSimpleName);
        if (annStart < 0) {
            return source;
        }
        int afterName = annStart + 1 + annotationSimpleName.length();
        // Skip whitespace after the annotation name.
        int i = afterName;
        while (i < source.length() && Character.isWhitespace(source.charAt(i))) {
            i++;
        }

        String argsInner = "";
        int annEnd; // exclusive end of the whole annotation in source
        if (i < source.length() && source.charAt(i) == '(') {
            int close = matchingParen(source, i);
            if (close < 0) {
                return source;
            }
            argsInner = source.substring(i + 1, close).trim();
            annEnd = close + 1;
        } else {
            annEnd = afterName;
        }

        java.util.List<String> segments = splitTopLevel(argsInner);
        int found = -1;
        for (int s = 0; s < segments.size(); s++) {
            if (segmentMatchesParameter(segments.get(s), parameterName)) {
                found = s;
                break;
            }
        }
        if (valueExpression != null) {
            String seg = parameterName + " = " + valueExpression;
            if (found >= 0) {
                segments.set(found, seg);
            } else {
                segments.add(seg);
            }
        } else if (found >= 0) {
            segments.remove(found);
        }

        // Java forbids the single-element shorthand once an annotation has 2+
        // elements: a bare implicit-value segment (e.g. "TEXT" in @Getter(TEXT))
        // must be expanded to "value = TEXT" as soon as another parameter is added.
        // At most one bare segment can exist (the implicit value element).
        if (segments.size() > 1) {
            for (int s = 0; s < segments.size(); s++) {
                if (!isNamedArgument(segments.get(s))) {
                    segments.set(s, "value = " + segments.get(s));
                    break;
                }
            }
        }

        StringBuilder newAnn = new StringBuilder("@").append(annotationSimpleName);
        if (!segments.isEmpty()) {
            newAnn.append('(').append(String.join(", ", segments)).append(')');
        }
        return source.substring(0, annStart) + newAnn + source.substring(annEnd);
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    /**
     * Locates {@code @annotationSimpleName} in the annotation block at/above the
     * declaration starting at {@code declarationStart}. Returns the offset of the
     * {@code @}, or {@code -1}.
     */
    private static int findAnnotation(String source, int declarationStart, String simpleName) {
        String token = "@" + simpleName;
        int lineStart = source.lastIndexOf('\n', declarationStart - 1) + 1;

        // The Spoon position of an annotated declaration may point at the FIRST
        // annotation (so the target annotation can sit BELOW it, between it and the
        // declaration) or at a preceding Javadoc/block comment (Spoon includes the
        // attached comment in the element's source range) or at the declaration itself
        // (annotations ABOVE). So scan the whole contiguous annotation block: walk up
        // to its top, then down to the declaration, examining each line and skipping
        // over any /** ... */ or /* ... */ comment encountered along the way (never
        // searching for the token inside one — a Javadoc may itself mention
        // "@Something" in prose, e.g. {@code @Imports}, which must not false-match).
        int top = lineStart;
        while (top > 0) {
            int prevStart = source.lastIndexOf('\n', top - 2) + 1;
            String prevLine = source.substring(prevStart, Math.max(prevStart, top - 1)).trim();
            if (prevLine.isEmpty() || prevLine.startsWith("@")) {
                top = prevStart;
            } else {
                break;
            }
        }
        int cur = top;
        boolean inBlockComment = false;
        while (cur < source.length()) {
            int lineEnd = source.indexOf('\n', cur);
            if (lineEnd < 0) {
                lineEnd = source.length();
            }
            String line = source.substring(cur, lineEnd);
            String trimmed = line.trim();

            if (inBlockComment) {
                if (trimmed.contains("*/")) {
                    inBlockComment = false;
                }
                cur = lineEnd + 1;
                continue;
            }
            if (trimmed.startsWith("/*")) {
                // Opens a block/Javadoc comment ("/**", "/*"); stays open past this
                // line unless it also closes on the same line.
                inBlockComment = !trimmed.contains("*/");
                cur = lineEnd + 1;
                continue;
            }
            if (trimmed.startsWith("*") || trimmed.equals("*/")) {
                // Javadoc continuation line ("* ...") or a lone closing line.
                cur = lineEnd + 1;
                continue;
            }

            int idx = line.indexOf(token);
            if (idx >= 0) {
                int after = idx + token.length();
                // Whole annotation name (not @ModelEntityXxx) — next char must not
                // continue the identifier.
                if (after >= line.length() || !Character.isJavaIdentifierPart(line.charAt(after))) {
                    return cur + idx;
                }
            }
            if (!trimmed.isEmpty() && !trimmed.startsWith("@")) {
                break; // reached the declaration line
            }
            cur = lineEnd + 1;
        }
        return -1;
    }

    /** Index of the {@code )} matching the {@code (} at {@code openIndex}, or -1. */
    private static int matchingParen(String source, int openIndex) {
        int depth = 0;
        boolean inString = false, inChar = false;
        for (int i = openIndex; i < source.length(); i++) {
            char c = source.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (inChar) {
                if (c == '\\') {
                    i++;
                } else if (c == '\'') {
                    inChar = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '\'') {
                inChar = true;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Splits annotation arguments on top-level commas (ignoring nested brackets/strings). */
    private static java.util.List<String> splitTopLevel(String args) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (args.isEmpty()) {
            return result;
        }
        int depth = 0;
        boolean inString = false, inChar = false;
        int start = 0;
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (inChar) {
                if (c == '\\') {
                    i++;
                } else if (c == '\'') {
                    inChar = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '\'') {
                inChar = true;
            } else if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            } else if (c == ',' && depth == 0) {
                result.add(args.substring(start, i).trim());
                start = i + 1;
            }
        }
        result.add(args.substring(start).trim());
        return result;
    }

    /** True if {@code segment} is {@code paramName = …}. */
    private static boolean segmentMatchesParameter(String segment, String paramName) {
        if (!segment.startsWith(paramName)) {
            return false;
        }
        int rest = paramName.length();
        while (rest < segment.length() && Character.isWhitespace(segment.charAt(rest))) {
            rest++;
        }
        return rest < segment.length() && segment.charAt(rest) == '=';
    }

    /**
     * True if {@code segment} is a named annotation argument {@code identifier = …}
     * (as opposed to a bare implicit-value expression such as {@code TEXT} or
     * {@code "foo"}). A top-level {@code =} preceded by a Java identifier marks a
     * named argument; annotation element values never contain a bare top-level
     * {@code =} otherwise.
     */
    private static boolean isNamedArgument(String segment) {
        int i = 0;
        while (i < segment.length() && Character.isWhitespace(segment.charAt(i))) {
            i++;
        }
        int idStart = i;
        while (i < segment.length() && Character.isJavaIdentifierPart(segment.charAt(i))) {
            i++;
        }
        if (i == idStart) {
            return false; // no leading identifier
        }
        while (i < segment.length() && Character.isWhitespace(segment.charAt(i))) {
            i++;
        }
        // A single '=' (not '==') right after the identifier marks a named argument.
        return i < segment.length() && segment.charAt(i) == '='
                && (i + 1 >= segment.length() || segment.charAt(i + 1) != '=');
    }

    private static boolean isAlreadyAnnotated(String source, int lineStart, String simpleName) {
        // Scan the whole annotation block (handles multi-annotation declarations).
        return findAnnotation(source, lineStart, simpleName) >= 0;
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
    public static String ensureImport(String source, String qualifiedName) {
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

    /**
     * Pattern matching one {@code @Import(<ref>.class)} entry within the parenthesized span of
     * an {@code @Imports(...)} annotation. Works uniformly whether the entries are wrapped in
     * {@code { ... }} (explicit array, single or multiple) or written as the bare single-element
     * sugar {@code @Imports(@Import(X.class))} — the regex does not care about braces.
     */
    private static final java.util.regex.Pattern IMPORT_ENTRY_PATTERN =
            java.util.regex.Pattern.compile("@Import\\s*\\(\\s*([\\w.]+)\\s*\\.class\\s*\\)");

    /**
     * Adds one {@code @Import(<referenceText>.class)} entry to the {@code @Imports(...)}
     * annotation on the declaration starting at {@code declarationStart}, creating the
     * {@code @Imports} annotation (and ensuring the two annotations' own imports) if absent.
     * Idempotent — an entry whose last dot-segment already equals {@code referenceText}'s last
     * dot-segment is left untouched (besides ensuring {@code qualifiedNameToImport}).
     *
     * <p>Always normalizes to the explicit array form {@code @Imports({ @Import(...), ... })},
     * even if the existing annotation used the single-element sugar or a {@code value = ...}
     * form — same rewrite-the-whole-annotation-text philosophy as
     * {@link #setAnnotationParameter}.</p>
     *
     * @param referenceText        the text to reference the imported type by, e.g. {@code "Circle"}
     *                             (same package) or {@code "other.pkg.Circle"} (fully qualified,
     *                             if the caller chooses not to add an import statement)
     * @param qualifiedNameToImport the qualified name to {@code import}, or {@code null} if no
     *                              import statement is needed (same package, or the caller
     *                              already used a fully-qualified {@code referenceText})
     * @return the edited source text
     */
    public static String addImportEntry(String source, int declarationStart,
            String referenceText, String qualifiedNameToImport) {
        if (source == null || declarationStart < 0 || declarationStart > source.length()) {
            throw new IllegalArgumentException("Invalid declaration offset");
        }
        int annStart = findAnnotation(source, declarationStart, "Imports");
        String edited;
        if (annStart < 0) {
            String annotationText = "@Imports({ @Import(" + referenceText + ".class) })";
            edited = insertAnnotationLine(source, declarationStart, annotationText);
            edited = ensureImport(edited, "org.openflexo.pamela.annotations.Imports");
            edited = ensureImport(edited, "org.openflexo.pamela.annotations.Import");
        } else {
            int[] span = annotationParenSpan(source, annStart, "Imports");
            if (span == null) {
                return source; // malformed / marker-only @Imports — leave untouched
            }
            String inner = source.substring(span[0] + 1, span[1]);
            java.util.List<String> refs = new java.util.ArrayList<>(extractImportReferences(inner));
            if (!containsReference(refs, referenceText)) {
                refs.add(referenceText);
            }
            edited = source.substring(0, annStart) + buildImportsAnnotationText(refs)
                    + source.substring(span[1] + 1);
        }
        return ensureImport(edited, qualifiedNameToImport);
    }

    /**
     * Removes the {@code @Import} entry whose last dot-segment equals {@code targetSimpleName}
     * from the {@code @Imports(...)} annotation on the declaration starting at
     * {@code declarationStart}. If the array becomes empty, removes the whole {@code @Imports}
     * annotation line (rather than leaving a pointless {@code @Imports({})}).
     *
     * @return the edited source, or the original if {@code @Imports} is absent or has no
     *         matching entry
     */
    public static String removeImportEntry(String source, int declarationStart, String targetSimpleName) {
        if (source == null || declarationStart < 0 || declarationStart > source.length()) {
            throw new IllegalArgumentException("Invalid declaration offset");
        }
        int annStart = findAnnotation(source, declarationStart, "Imports");
        if (annStart < 0) {
            return source;
        }
        int[] span = annotationParenSpan(source, annStart, "Imports");
        if (span == null) {
            return source;
        }
        String inner = source.substring(span[0] + 1, span[1]);
        java.util.List<String> refs = new java.util.ArrayList<>(extractImportReferences(inner));
        if (!removeReference(refs, targetSimpleName)) {
            return source; // not present
        }
        if (refs.isEmpty()) {
            return removeAnnotation(source, declarationStart, "Imports");
        }
        return source.substring(0, annStart) + buildImportsAnnotationText(refs)
                + source.substring(span[1] + 1);
    }

    /**
     * Sets or replaces the value of a single-{@code Class}-valued annotation such as
     * {@code @ImplementationClass(Foo.class)}, creating the annotation (with its own import
     * ensured) if absent. Handles both the bare shorthand form ({@code @Foo(Bar.class)}, used
     * throughout this codebase) and the explicit {@code @Foo(value = Bar.class)} form when
     * replacing an existing value — {@link #setAnnotationParameter} cannot be reused here since
     * it only recognizes a segment starting with {@code "<paramName> ="}, so a bare-shorthand
     * existing value would not be found and a second, conflicting {@code value = ...} segment
     * would be appended instead. Always normalizes to the bare shorthand form on write.
     *
     * @param annotationSimpleName    e.g. {@code "ImplementationClass"}
     * @param annotationQualifiedName e.g. {@code "org.openflexo.pamela.annotations.ImplementationClass"},
     *                                used to ensure the annotation's own import when the
     *                                annotation must be created
     * @param referenceText           the text to reference the class by, e.g. {@code "FooImpl"}
     *                                (same package) or {@code "other.pkg.FooImpl"} (fully
     *                                qualified, if the caller chooses not to add an import)
     * @param qualifiedNameToImport   the qualified name to {@code import} for the referenced
     *                                class, or {@code null} if no import statement is needed
     * @return the edited source text
     */
    public static String setClassAnnotationValue(String source, int declarationStart,
            String annotationSimpleName, String annotationQualifiedName,
            String referenceText, String qualifiedNameToImport) {
        if (source == null || declarationStart < 0 || declarationStart > source.length()) {
            throw new IllegalArgumentException("Invalid declaration offset");
        }
        int annStart = findAnnotation(source, declarationStart, annotationSimpleName);
        String newAnnotationText = "@" + annotationSimpleName + "(" + referenceText + ".class)";
        String edited;
        if (annStart < 0) {
            edited = insertAnnotationLine(source, declarationStart, newAnnotationText);
            edited = ensureImport(edited, annotationQualifiedName);
        } else {
            int[] span = annotationParenSpan(source, annStart, annotationSimpleName);
            if (span == null) {
                // Marker annotation (no parentheses at all) — insert the value directly
                // after the annotation name.
                int afterName = annStart + 1 + annotationSimpleName.length();
                edited = source.substring(0, annStart) + newAnnotationText + source.substring(afterName);
            } else {
                edited = source.substring(0, annStart) + newAnnotationText + source.substring(span[1] + 1);
            }
        }
        return ensureImport(edited, qualifiedNameToImport);
    }

    /**
     * Locates the {@code (...)} span of the annotation named {@code simpleName} starting at
     * {@code annStart}. Returns {@code {openIndex, closeIndex}} (both inclusive of the
     * parentheses themselves), or {@code null} if there are no parentheses (marker annotation)
     * or they are unbalanced.
     */
    private static int[] annotationParenSpan(String source, int annStart, String simpleName) {
        int afterName = annStart + 1 + simpleName.length();
        int i = afterName;
        while (i < source.length() && Character.isWhitespace(source.charAt(i))) {
            i++;
        }
        if (i >= source.length() || source.charAt(i) != '(') {
            return null;
        }
        int close = matchingParen(source, i);
        if (close < 0) {
            return null;
        }
        return new int[] { i, close };
    }

    private static java.util.List<String> extractImportReferences(String annotationInner) {
        java.util.List<String> refs = new java.util.ArrayList<>();
        java.util.regex.Matcher m = IMPORT_ENTRY_PATTERN.matcher(annotationInner);
        while (m.find()) {
            refs.add(m.group(1));
        }
        return refs;
    }

    private static String buildImportsAnnotationText(java.util.List<String> refs) {
        StringBuilder sb = new StringBuilder("@Imports({ ");
        for (int i = 0; i < refs.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("@Import(").append(refs.get(i)).append(".class)");
        }
        sb.append(" })");
        return sb.toString();
    }

    /** Last {@code .}-separated segment of a (possibly fully-qualified) reference text. */
    private static String lastSegment(String reference) {
        int dot = reference.lastIndexOf('.');
        return dot < 0 ? reference : reference.substring(dot + 1);
    }

    private static boolean containsReference(java.util.List<String> refs, String referenceText) {
        String target = lastSegment(referenceText);
        for (String r : refs) {
            if (lastSegment(r).equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean removeReference(java.util.List<String> refs, String targetSimpleName) {
        String target = lastSegment(targetSimpleName);
        java.util.Iterator<String> it = refs.iterator();
        while (it.hasNext()) {
            if (lastSegment(it.next()).equals(target)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Inserts {@code public static final String <constantName> = "<value>";} at the top of the body
     * of the type whose declaration starts at {@code typeDeclarationStart}, if a constant of that
     * name is not already declared. Idempotent (safe to call once per generated accessor when
     * several reference the same constant). Targeted text edit — no AST re-print. See
     * {@code property-identifier-constant-design.md §6}.
     *
     * @return the edited source, or the original if the constant is already declared or the body
     *         brace cannot be located
     */
    public static String ensureConstantField(String source, int typeDeclarationStart,
            String constantName, String value) {
        if (source == null || typeDeclarationStart < 0 || typeDeclarationStart > source.length()) {
            throw new IllegalArgumentException("Invalid declaration offset");
        }
        // Already declared? (a "String <name>" field — accept any modifier ordering).
        java.util.regex.Matcher existing = java.util.regex.Pattern
                .compile("\\bString\\s+" + java.util.regex.Pattern.quote(constantName) + "\\b")
                .matcher(source);
        if (existing.find()) {
            return source;
        }
        int brace = source.indexOf('{', typeDeclarationStart);
        if (brace < 0) {
            return source;
        }
        int insertAt = brace + 1;
        String field = "\n\tpublic static final String " + constantName + " = \"" + value + "\";\n";
        return source.substring(0, insertAt) + field + source.substring(insertAt);
    }
}
