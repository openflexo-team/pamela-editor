package org.openflexo.pamela.editor.ui.preferences;

import java.awt.Color;
import java.awt.Font;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

/**
 * Visual style of {@code SourceModelEntity} boxes on a class diagram
 * ({@code /classDiagramDesign/entities}): header / body / border colours and title / member
 * fonts. {@link Color} and {@link Font} are string-convertible (global converters), so the
 * properties serialize transparently and carry string {@code defaultValue}s
 * ({@code "r,g,b"} for colours, {@code "name,style,size"} for fonts).
 */
@ModelEntity
@ImplementationClass(EntityStylePreferences.EntityStylePreferencesImpl.class)
public interface EntityStylePreferences extends PreferencesNode {

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

    abstract class EntityStylePreferencesImpl extends PreferencesNodeImpl
            implements EntityStylePreferences {
    }
}
