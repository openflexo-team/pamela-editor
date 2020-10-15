package qdox;

import com.thoughtworks.qdox.model.*;
import com.thoughtworks.qdox.writer.ModelWriter;
import com.thoughtworks.qdox.writer.impl.DefaultModelWriter;
import com.thoughtworks.qdox.writer.impl.IndentBuffer;

import java.util.*;

/**
 * Created by adria on 27/01/2017.
 */
public class OrderedModelWriter extends DefaultModelWriter {

    private IndentBuffer buffer = getBuffer();

    public OrderedModelWriter() {
    }

    @Override
    public ModelWriter writeSource(JavaSource source) {
        // package statement
        writePackage(source.getPackage());

        // import statement
        for (String imprt : source.getImports()) {
            buffer.write("import ");
            buffer.write(imprt);
            buffer.write(';');
            buffer.newline();
        }
        if (source.getImports().size() > 0) {
            buffer.newline();
        }

        // classes
        for (ListIterator<JavaClass> iter = source.getClasses().listIterator(); iter.hasNext(); ) {
            JavaClass cls = iter.next();
            writeClass(cls);
            if (iter.hasNext()) {
                buffer.newline();
            }
        }
        return this;
    }

    @Override
    public ModelWriter writeClass(JavaClass cls) {
        commentHeader(cls);

        writeAccessibilityModifier(cls.getModifiers());
        writeNonAccessibilityModifiers(cls.getModifiers());

        buffer.write(cls.isEnum() ? "enum " : cls.isInterface() ? "interface " : cls.isAnnotation() ? "@interface "
                : "class ");
        buffer.write(cls.getName());

        // subclass
        if (cls.getSuperClass() != null) {
            String className = cls.getSuperClass().getFullyQualifiedName();
            if (!"java.lang.Object".equals(className) && !"java.lang.Enum".equals(className)) {
                buffer.write(" extends ");
                buffer.write(cls.getSuperClass().getGenericCanonicalName());
            }
        }

        // implements
        if (cls.getImplements().size() > 0) {
            buffer.write(cls.isInterface() ? " extends " : " implements ");

            for (ListIterator<JavaType> iter = cls.getImplements().listIterator(); iter.hasNext(); ) {
                buffer.write(iter.next().getGenericCanonicalName());
                if (iter.hasNext()) {
                    buffer.write(", ");
                }
            }
        }

        return writeClassBody(cls);
    }

    private ModelWriter writeClassBody(JavaClass cls) {
        Set<JavaModel> elements = new TreeSet<>(new Comparator<JavaModel>() {
            @Override
            public int compare(JavaModel o1, JavaModel o2) {
                if (o1.getLineNumber() < o2.getLineNumber()) return -1;
                if (o1.getLineNumber() > o2.getLineNumber()) return 1;
                else return 0;
            }
        });
        List<JavaModel> newElements = new ArrayList<>();
        buffer.write(" {");
        buffer.newline();
        buffer.indent();

        // fields
        for (JavaField javaField : cls.getFields()) {
            if (javaField.getLineNumber() == -1) newElements.add(javaField);
            else elements.add(javaField);
        }

        // constructors
        for (JavaConstructor javaConstructor : cls.getConstructors()) {
            if (javaConstructor.getLineNumber() == -1) newElements.add(javaConstructor);
            else elements.add(javaConstructor);
        }

        // methods
        for (JavaMethod javaMethod : cls.getMethods()) {
            if (javaMethod.getLineNumber() == -1) newElements.add(javaMethod);
            else elements.add(javaMethod);
        }

        // inner-classes
        for (JavaClass innerCls : cls.getNestedClasses()) {
            if (innerCls.getLineNumber() == -1) newElements.add(innerCls);
            else elements.add(innerCls);
        }

        for (JavaModel elt : elements) {
            if (elt instanceof JavaField) {
                buffer.newline();
                writeField((JavaField) elt);
            }
            if (elt instanceof JavaConstructor) {
                buffer.newline();
                writeConstructor((JavaConstructor) elt);
            }
            if (elt instanceof JavaMethod) {
                buffer.newline();
                writeMethod((JavaMethod) elt);
            }
            if (elt instanceof JavaClass) {
                buffer.newline();
                writeClass((JavaClass) elt);
            }
        }

        for (JavaModel elt : newElements) {
            if (elt instanceof JavaField) {
                buffer.newline();
                writeField((JavaField) elt);
            }
            if (elt instanceof JavaConstructor) {
                buffer.newline();
                writeConstructor((JavaConstructor) elt);
            }
            if (elt instanceof JavaMethod) {
                buffer.newline();
                writeMethod((JavaMethod) elt);
            }
            if (elt instanceof JavaClass) {
                buffer.newline();
                writeClass((JavaClass) elt);
            }
        }

        buffer.deindent();
        buffer.newline();
        buffer.write('}');
        buffer.newline();
        return this;
    }

    private void writeNonAccessibilityModifiers(Collection<String> modifiers) {
        for (String modifier : modifiers) {
            if (!modifier.startsWith("p")) {
                buffer.write(modifier);
                buffer.write(' ');
            }
        }
    }

    private void writeAccessibilityModifier(Collection<String> modifiers) {
        for (String modifier : modifiers) {
            if (modifier.startsWith("p")) {
                buffer.write(modifier);
                buffer.write(' ');
            }
        }
    }
}
