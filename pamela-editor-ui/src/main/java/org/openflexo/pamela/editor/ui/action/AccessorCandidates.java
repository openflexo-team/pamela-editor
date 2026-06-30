/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor.
 *
 */

package org.openflexo.pamela.editor.ui.action;

import java.util.ArrayList;
import java.util.List;

import org.openflexo.pamela.editor.model.SourceModelEntity;

import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

/**
 * Signature-based candidate-method lookups shared by the property-creation / promotion dialogs
 * (model-editing-design.md §3.3a). All operate over the methods of an entity's primary interface
 * and feed a {@link org.openflexo.pamela.editor.ui.widget.JavaMethodSelector}'s restricted set.
 */
final class AccessorCandidates {

    private AccessorCandidates() {
    }

    /** Methods of the entity's primary interface (the candidate pool). */
    static List<CtMethod<?>> entityMethods(SourceModelEntity entity) {
        List<CtMethod<?>> result = new ArrayList<>();
        if (entity == null || entity.getCompilationUnit() == null) {
            return result;
        }
        for (CtType<?> type : entity.getCompilationUnit().getRootTypes()) {
            if (entity.getSimpleName().equals(type.getSimpleName())) {
                result.addAll(type.getMethods());
            }
        }
        return result;
    }

    /** No-argument methods whose return type matches the property type (getter candidates). */
    static List<CtMethod<?>> noArgReturning(List<CtMethod<?>> all, String typeQN, boolean list) {
        List<CtMethod<?>> result = new ArrayList<>();
        if (typeQN == null) {
            return result;
        }
        for (CtMethod<?> m : all) {
            if (!m.getParameters().isEmpty() || m.getType() == null) {
                continue;
            }
            if (list) {
                // List<typeQN>
                if (!"java.util.List".equals(m.getType().getQualifiedName())) {
                    continue;
                }
                if (m.getType().getActualTypeArguments().isEmpty()
                        || !typeQN.equals(m.getType().getActualTypeArguments().get(0).getQualifiedName())) {
                    continue;
                }
                result.add(m);
            } else if (typeQN.equals(m.getType().getQualifiedName())) {
                result.add(m);
            }
        }
        return result;
    }

    /** Methods with exactly one parameter whose type matches {@code paramTypeQN}. */
    static List<CtMethod<?>> oneParam(List<CtMethod<?>> all, String paramTypeQN) {
        List<CtMethod<?>> result = new ArrayList<>();
        if (paramTypeQN == null) {
            return result;
        }
        for (CtMethod<?> m : all) {
            if (m.getParameters().size() == 1 && m.getParameters().get(0).getType() != null
                    && paramTypeQN.equals(m.getParameters().get(0).getType().getQualifiedName())) {
                result.add(m);
            }
        }
        return result;
    }

    /** Methods {@code (T, int)} where {@code T} matches {@code elementTypeQN}. */
    static List<CtMethod<?>> reindexer(List<CtMethod<?>> all, String elementTypeQN) {
        List<CtMethod<?>> result = new ArrayList<>();
        if (elementTypeQN == null) {
            return result;
        }
        for (CtMethod<?> m : all) {
            if (m.getParameters().size() == 2 && m.getParameters().get(0).getType() != null
                    && elementTypeQN.equals(m.getParameters().get(0).getType().getQualifiedName())
                    && isInt(m.getParameters().get(1).getType() == null ? null
                            : m.getParameters().get(1).getType().getQualifiedName())) {
                result.add(m);
            }
        }
        return result;
    }

    private static boolean isInt(String qn) {
        return "int".equals(qn) || "java.lang.Integer".equals(qn);
    }
}
