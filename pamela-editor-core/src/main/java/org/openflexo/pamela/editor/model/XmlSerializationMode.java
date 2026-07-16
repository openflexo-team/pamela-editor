package org.openflexo.pamela.editor.model;

/**
 * How a {@link SourceModelProperty} is serialized in XML — the three mutually-exclusive
 * states of PAMELA's {@code @XMLAttribute} / {@code @XMLElement} annotations on a getter.
 *
 * <p>A property is serialized as an XML attribute <em>or</em> an XML element, never both;
 * this enum makes that exclusivity explicit for the inspector's 3-state selector (see
 * {@code xml-serialization-design.md}).</p>
 */
public enum XmlSerializationMode {
    /** Neither {@code @XMLAttribute} nor {@code @XMLElement}. */
    NONE,
    /** {@code @XMLAttribute} on the getter. */
    ATTRIBUTE,
    /** {@code @XMLElement} on the getter. */
    ELEMENT
}
