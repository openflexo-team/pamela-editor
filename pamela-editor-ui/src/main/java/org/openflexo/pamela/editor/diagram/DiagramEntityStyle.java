package org.openflexo.pamela.editor.diagram;

import java.awt.Color;
import java.awt.Font;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

/**
 * The entity-look style embedded in a {@link PamelaClassDiagram} (one block per diagram, applied
 * to all its boxes). A snapshot of the preference defaults at diagram creation, kept in the
 * {@code .diagram} file so the diagram stays stable when the preference defaults later change
 * (see {@code preferences-design.md §6bis}). Mirrors the fields of {@code EntityStylePreferences}.
 */
@ModelEntity
@ImplementationClass(DiagramEntityStyle.DiagramEntityStyleImpl.class)
public interface DiagramEntityStyle extends AccessibleProxyObject {

    String HEADER_BACKGROUND_COLOR = "headerBackgroundColor";
    String BODY_BACKGROUND_COLOR = "bodyBackgroundColor";
    String BORDER_COLOR = "borderColor";
    String TITLE_FONT = "titleFont";
    String MEMBER_FONT = "memberFont";

    @Getter(value = HEADER_BACKGROUND_COLOR, defaultValue = "210,225,245")
    Color getHeaderBackgroundColor();

    @Setter(HEADER_BACKGROUND_COLOR)
    void setHeaderBackgroundColor(Color color);

    @Getter(value = BODY_BACKGROUND_COLOR, defaultValue = "255,255,255")
    Color getBodyBackgroundColor();

    @Setter(BODY_BACKGROUND_COLOR)
    void setBodyBackgroundColor(Color color);

    @Getter(value = BORDER_COLOR, defaultValue = "105,105,105")
    Color getBorderColor();

    @Setter(BORDER_COLOR)
    void setBorderColor(Color color);

    @Getter(value = TITLE_FONT, defaultValue = "SansSerif,1,12")
    Font getTitleFont();

    @Setter(TITLE_FONT)
    void setTitleFont(Font font);

    @Getter(value = MEMBER_FONT, defaultValue = "SansSerif,0,11")
    Font getMemberFont();

    @Setter(MEMBER_FONT)
    void setMemberFont(Font font);

    abstract class DiagramEntityStyleImpl implements DiagramEntityStyle {
    }
}
