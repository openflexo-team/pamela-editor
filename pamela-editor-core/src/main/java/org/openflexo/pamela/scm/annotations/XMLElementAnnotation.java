package org.openflexo.pamela.scm.annotations;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.expression.Constant;

/**
 * Created by adria on 27/02/2017.
 */
public class XMLElementAnnotation extends PamelaAnnotation {
    public XMLElementAnnotation(JavaAnnotation annotationSource) {
        super(annotationSource);
    }

    public String getXmlTag() {
        return ((Constant) annotationSource.getProperty("xmlTag")).getImage();
    }

    public XMLElementAnnotation setXmlTag(String xmlTag) {
        annotationSource.getPropertyMap().remove("xmlTag");
        annotationSource.getPropertyMap().put("xmlTag", Constant.newStringLiteral(xmlTag));
        return this;
    }

    public String getDeprecatedXMLTags() {
        return ((Constant) annotationSource.getProperty("deprecatedXMLTags")).getImage();
    }

    public XMLElementAnnotation setDeprecatedXMLTags(String deprecatedXMLTags) {
        annotationSource.getPropertyMap().remove("deprecatedXMLTags");
        annotationSource.getPropertyMap().put("deprecatedXMLTags", Constant.newStringLiteral(deprecatedXMLTags));
        return this;
    }

    public String getContext() {
        return ((Constant) annotationSource.getProperty("context")).getImage();
    }

    public XMLElementAnnotation setContext(String context) {
        annotationSource.getPropertyMap().remove("context");
        annotationSource.getPropertyMap().put("context", Constant.newStringLiteral(context));
        return this;
    }

    public String getNamespace() {
        return ((Constant) annotationSource.getProperty("namespace")).getImage();
    }

    public XMLElementAnnotation setNamespace(String namespace) {
        annotationSource.getPropertyMap().remove("namespace");
        annotationSource.getPropertyMap().put("namespace", Constant.newStringLiteral(namespace));
        return this;
    }

    public boolean getPrimary() {
        return annotationSource.getProperty("primary") != null
                && Boolean.valueOf(((Constant) annotationSource.getProperty("primary")).getImage());
    }

    public XMLElementAnnotation setPrimary(boolean primary) {
        annotationSource.getPropertyMap().remove("primary");
        annotationSource.getPropertyMap().put("primary", Constant.newBooleanLiteral(Boolean.toString(primary)));
        return this;
    }
}
