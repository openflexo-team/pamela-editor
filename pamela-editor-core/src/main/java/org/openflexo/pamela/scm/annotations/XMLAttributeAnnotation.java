package org.openflexo.pamela.scm.annotations;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.expression.Constant;

/**
 * Created by adria on 27/02/2017.
 */
public class XMLAttributeAnnotation extends PamelaAnnotation {
    public XMLAttributeAnnotation(JavaAnnotation annotationSource) {
        super(annotationSource);
    }

    public String getXmlTag() {
        return ((Constant) annotationSource.getProperty("xmlTag")).getImage();
    }

    public XMLAttributeAnnotation setXmlTag(String xmlTag) {
        annotationSource.getPropertyMap().remove("xmlTag");
        annotationSource.getPropertyMap().put("xmlTag", Constant.newStringLiteral(xmlTag));
        return this;
    }

    public String getNamespace() {
        return ((Constant) annotationSource.getProperty("namespace")).getImage();
    }

    public XMLAttributeAnnotation setNamespace(String namespace) {
        annotationSource.getPropertyMap().remove("namespace");
        annotationSource.getPropertyMap().put("namespace", Constant.newStringLiteral(namespace));
        return this;
    }
}
