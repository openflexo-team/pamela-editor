/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openflexo.gina.model.widget.FIBCustom.FIBCustomComponent.CustomComponentParameter;
import org.openflexo.pamela.editor.model.SourceCompilationUnit;
import org.openflexo.pamela.editor.model.SourceFolder;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/**
 * Selects a Java <b>class or interface</b> as a Spoon {@link CtType} (a {@code CtClass} or
 * {@code CtInterface}), browsing either a whole metamodel or a single package. Surfacing Spoon
 * values is the documented encapsulation exception already used by the Spoon outline
 * ({@code SourceCompilationUnit.getRootTypes()}, ui-design.md §19.3).
 *
 * <p>Two presentation modes (the {@code hierarchical} flag, also bound from the FIB):</p>
 * <ul>
 * <li><b>structural</b> (default): when scoped to a whole metamodel, every registered
 * {@link SourceFolder} (source directory) at the root, each with its packages nested underneath
 * (mirroring the {@code MetaModelBrowser} tree, ui-design.md §4.1); when scoped to a single
 * package, that package directly at the root;</li>
 * <li><b>hierarchical</b>: top-level types (no in-scope super-type) with their sub-types as children,
 * computed from Spoon's {@code getSuperclass()} / {@code getSuperInterfaces()}.</li>
 * </ul>
 *
 * <p>Optional constraint {@code classKind} = {@code ANY} | {@code CLASS} | {@code INTERFACE}.</p>
 *
 * @author sylvain
 */
@SuppressWarnings("serial")
public class JavaClassSelector extends FIBPamelaObjectSelector<CtType<?>> {

	public static final Resource FIB_FILE = ResourceLocator.locateResource("Fib/Selector/JavaClassSelector.fib");

	public enum ClassKind {
		ANY, CLASS, INTERFACE
	}

	private SourceMetaModel metaModel;
	private SourcePackage sourcePackage;
	private boolean hierarchical = false;
	private ClassKind classKind = ClassKind.ANY;

	public JavaClassSelector(CtType<?> editedObject) {
		super(editedObject);
	}

	@Override
	public Resource getFIBResource() {
		return FIB_FILE;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Class<CtType<?>> getRepresentedType() {
		return (Class) CtType.class;
	}

	@Override
	public String renderedString(CtType<?> editedObject) {
		return editedObject != null ? editedObject.getSimpleName() : "";
	}

	/** The selector itself is the browser root; the tree is exposed through its getters. */
	@Override
	public Object getRootObject() {
		return this;
	}

	@Override
	protected Collection<CtType<?>> computeAllSelectableValues() {
		return allClassesInScope();
	}

	// --- context ------------------------------------------------------------

	public SourceMetaModel getMetaModel() {
		if (metaModel != null) {
			return metaModel;
		}
		return sourcePackage != null ? sourcePackage.getMetaModel() : null;
	}

	@CustomComponentParameter(name = "metaModel", type = CustomComponentParameter.Type.OPTIONAL)
	public void setMetaModel(SourceMetaModel metaModel) {
		if (this.metaModel != metaModel) {
			SourceMetaModel old = this.metaModel;
			this.metaModel = metaModel;
			getPropertyChangeSupport().firePropertyChange("metaModel", old, metaModel);
			refresh();
		}
	}

	public SourcePackage getSourcePackage() {
		return sourcePackage;
	}

	@CustomComponentParameter(name = "sourcePackage", type = CustomComponentParameter.Type.OPTIONAL)
	public void setSourcePackage(SourcePackage sourcePackage) {
		if (this.sourcePackage != sourcePackage) {
			SourcePackage old = this.sourcePackage;
			this.sourcePackage = sourcePackage;
			getPropertyChangeSupport().firePropertyChange("sourcePackage", old, sourcePackage);
			refresh();
		}
	}

	public boolean isHierarchical() {
		return hierarchical;
	}

	@CustomComponentParameter(name = "hierarchical", type = CustomComponentParameter.Type.OPTIONAL)
	public void setHierarchical(boolean hierarchical) {
		if (this.hierarchical != hierarchical) {
			this.hierarchical = hierarchical;
			getPropertyChangeSupport().firePropertyChange("hierarchical", !hierarchical, hierarchical);
			refresh();
		}
	}

	public ClassKind getClassKind() {
		return classKind;
	}

	public void setClassKind(ClassKind classKind) {
		ClassKind k = classKind != null ? classKind : ClassKind.ANY;
		if (this.classKind != k) {
			ClassKind old = this.classKind;
			this.classKind = k;
			getPropertyChangeSupport().firePropertyChange("classKind", old, k);
			refresh();
		}
	}

	@CustomComponentParameter(name = "classKind", type = CustomComponentParameter.Type.OPTIONAL)
	public void setClassKind(String classKind) {
		setClassKind(classKind != null ? ClassKind.valueOf(classKind.trim().toUpperCase()) : ClassKind.ANY);
	}

	// --- constraint ---------------------------------------------------------

	@Override
	public boolean isAcceptableValue(Object o) {
		if (!(o instanceof CtType)) {
			return false;
		}
		if (!isAmongRestricted(o)) {
			return false;
		}
		switch (classKind) {
			case CLASS:
				return o instanceof CtClass;
			case INTERFACE:
				return o instanceof CtInterface;
			default:
				return true;
		}
	}

	// --- tree exposed to the FIB browser ------------------------------------

	/** Structural mode: the source folders shown at top level (whole-project scope only). */
	public List<SourceFolder> getSourceFoldersInScope() {
		if (sourcePackage != null) {
			return new ArrayList<>();
		}
		SourceMetaModel mm = getMetaModel();
		return mm != null ? new ArrayList<>(mm.getSourceFolders()) : new ArrayList<>();
	}

	/** Structural mode: the single package shown at top level when scoped to one package. */
	public List<SourcePackage> getPackagesInScope() {
		if (sourcePackage != null) {
			List<SourcePackage> single = new ArrayList<>();
			single.add(sourcePackage);
			return single;
		}
		return new ArrayList<>();
	}

	/** Structural mode: the classes declared in {@code pkg} (within scope). */
	public List<CtType<?>> classesInPackage(SourcePackage pkg) {
		List<CtType<?>> result = new ArrayList<>();
		if (pkg == null) {
			return result;
		}
		String pkgName = pkg.getQualifiedName();
		for (CtType<?> type : allClassesInScope()) {
			if (pkgName.equals(packageNameOf(type))) {
				result.add(type);
			}
		}
		return result;
	}

	/** Hierarchical mode: types with no in-scope super-type. */
	public List<CtType<?>> getRootClasses() {
		Set<String> inScopeNames = new LinkedHashSet<>();
		Collection<CtType<?>> all = allClassesInScope();
		for (CtType<?> t : all) {
			inScopeNames.add(t.getQualifiedName());
		}
		List<CtType<?>> roots = new ArrayList<>();
		for (CtType<?> t : all) {
			boolean hasInScopeSuper = false;
			for (String superQN : directSuperNames(t)) {
				if (inScopeNames.contains(superQN)) {
					hasInScopeSuper = true;
					break;
				}
			}
			if (!hasInScopeSuper) {
				roots.add(t);
			}
		}
		return roots;
	}

	/** Hierarchical mode: in-scope direct sub-types of {@code type}. */
	public List<CtType<?>> subClassesOf(CtType<?> type) {
		List<CtType<?>> result = new ArrayList<>();
		if (type == null) {
			return result;
		}
		String qn = type.getQualifiedName();
		for (CtType<?> t : allClassesInScope()) {
			if (directSuperNames(t).contains(qn)) {
				result.add(t);
			}
		}
		return result;
	}

	// --- enumeration helpers ------------------------------------------------

	private Collection<CtType<?>> allClassesInScope() {
		SourceMetaModel mm = getMetaModel();
		if (mm == null) {
			return new ArrayList<>();
		}
		String pkgFilter = sourcePackage != null ? sourcePackage.getQualifiedName() : null;
		// De-duplicate by qualified name (a CU may be reachable more than once)
		LinkedHashSet<CtType<?>> result = new LinkedHashSet<>();
		for (SourceCompilationUnit cu : mm.getAllCompilationUnits()) {
			for (CtType<?> type : cu.getRootTypes()) {
				if (pkgFilter == null || pkgFilter.equals(packageNameOf(type))) {
					result.add(type);
				}
			}
		}
		return new ArrayList<>(result);
	}

	private static String packageNameOf(CtType<?> type) {
		if (type.getPackage() != null) {
			return type.getPackage().getQualifiedName();
		}
		String qn = type.getQualifiedName();
		int dot = qn.lastIndexOf('.');
		return dot > 0 ? qn.substring(0, dot) : "";
	}

	private static Set<String> directSuperNames(CtType<?> type) {
		Set<String> names = new LinkedHashSet<>();
		CtTypeReference<?> superClass = type.getSuperclass();
		if (superClass != null) {
			names.add(superClass.getQualifiedName());
		}
		for (CtTypeReference<?> superItf : type.getSuperInterfaces()) {
			names.add(superItf.getQualifiedName());
		}
		return names;
	}
}
