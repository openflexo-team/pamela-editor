package org.openflexo.pamela.editor.ui.preferences;

import java.util.List;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.CloneableProxyObject;
import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Embedded;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.Setter;

/**
 * Abstract base for every node of the preferences themes tree.
 *
 * <p>It is the <em>structural</em> base: it gives the tree its shape ({@code name},
 * {@code children}/{@code parent}) and the transient display attributes used by the
 * browser ({@code displayLabel}, {@code iconKey}). Concrete themes extend this interface
 * and add their own typed, string-convertible {@code @Getter}/{@code @Setter} properties,
 * which are the editable preference values (see {@code preferences-design.md §1}).</p>
 *
 * <p>{@code children}/{@code parent} form a PAMELA bidirectional embedded relationship
 * (composition): serialization, cloning and deletion cascade through the tree.</p>
 */
@ModelEntity(isAbstract = true)
@ImplementationClass(PreferencesNodeImpl.class)
public interface PreferencesNode extends AccessibleProxyObject, CloneableProxyObject {

    String NAME = "name";
    String CHILDREN = "children";
    String PARENT = "parent";

    /** Stable identifier of this node among its siblings (also the JSON nesting key). */
    @Getter(NAME)
    String getName();

    @Setter(NAME)
    void setName(String name);

    @Getter(value = CHILDREN, cardinality = Cardinality.LIST, inverse = PARENT)
    @Embedded
    List<PreferencesNode> getChildren();

    @Adder(CHILDREN)
    void addToChildren(PreferencesNode child);

    @Remover(CHILDREN)
    void removeFromChildren(PreferencesNode child);

    @Getter(value = PARENT, inverse = CHILDREN)
    PreferencesNode getParent();

    @Setter(PARENT)
    void setParent(PreferencesNode parent);

    // ---------------------------------------------------------------- non-PAMELA

    /** {@code true} when this node has no parent (the serialization root). */
    boolean isRoot();

    /** Slash path of names from the root, e.g. {@code "/general/window"}; root is {@code "/"}. */
    String getPath();

    /** Localized text shown in the browser; transient, set by the tree builder. */
    String getDisplayLabel();

    void setDisplayLabel(String displayLabel);

    /** Icon identifier for the browser; transient, set by the tree builder. */
    String getIconKey();

    void setIconKey(String iconKey);

    /** Finds a direct child by {@link #getName()}, or {@code null}. */
    PreferencesNode getChild(String name);
}
