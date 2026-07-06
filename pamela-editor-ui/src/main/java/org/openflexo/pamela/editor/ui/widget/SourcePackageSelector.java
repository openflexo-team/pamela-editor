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
import java.util.Collections;
import java.util.List;

import org.openflexo.gina.model.widget.FIBCustom.FIBCustomComponent.CustomComponentParameter;
import org.openflexo.pamela.editor.model.SourceFolder;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Selects a {@link SourcePackage}, browsing either a whole metamodel (every {@link SourceFolder}
 * with its packages nested underneath, mirroring the {@code MetaModelBrowser} tree — same
 * structural shape as {@link JavaClassSelector}) or a single {@link SourceFolder} (only that
 * folder's own packages, listed flat).
 *
 * <p>Only existing packages are offered — this selector never creates a new package.</p>
 *
 * @author sylvain
 */
@SuppressWarnings("serial")
public class SourcePackageSelector extends FIBPamelaObjectSelector<SourcePackage> {

	public static final Resource FIB_FILE = ResourceLocator.locateResource("Fib/Selector/SourcePackageSelector.fib");

	private SourceMetaModel metaModel;
	private SourceFolder sourceFolder;

	public SourcePackageSelector(SourcePackage editedObject) {
		super(editedObject);
	}

	@Override
	public Resource getFIBResource() {
		return FIB_FILE;
	}

	@Override
	public Class<SourcePackage> getRepresentedType() {
		return SourcePackage.class;
	}

	@Override
	public String renderedString(SourcePackage editedObject) {
		if (editedObject == null) {
			return "";
		}
		return editedObject.isDefault() ? "(default package)" : editedObject.getQualifiedName();
	}

	/** The selector itself is the browser root; the tree is exposed through its getters. */
	@Override
	public Object getRootObject() {
		return this;
	}

	@Override
	protected Collection<SourcePackage> computeAllSelectableValues() {
		if (sourceFolder != null) {
			return new ArrayList<>(sourceFolder.getPackages());
		}
		SourceMetaModel mm = getMetaModel();
		return mm != null ? new ArrayList<>(mm.getAllPackages()) : null;
	}

	// --- context ------------------------------------------------------------

	public SourceMetaModel getMetaModel() {
		if (metaModel != null) {
			return metaModel;
		}
		return sourceFolder != null ? sourceFolder.getMetaModel() : null;
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

	public SourceFolder getSourceFolder() {
		return sourceFolder;
	}

	/** Scopes the browser to a single folder's own packages; {@code null} browses the whole metamodel. */
	@CustomComponentParameter(name = "sourceFolder", type = CustomComponentParameter.Type.OPTIONAL)
	public void setSourceFolder(SourceFolder sourceFolder) {
		if (this.sourceFolder != sourceFolder) {
			SourceFolder old = this.sourceFolder;
			this.sourceFolder = sourceFolder;
			getPropertyChangeSupport().firePropertyChange("sourceFolder", old, sourceFolder);
			refresh();
		}
	}

	// --- tree exposed to the FIB browser (structural mode, mirrors JavaClassSelector) ---

	/** Whole-metamodel scope only: every source folder shown at top level. */
	public List<SourceFolder> getSourceFoldersInScope() {
		if (sourceFolder != null) {
			return Collections.emptyList();
		}
		SourceMetaModel mm = getMetaModel();
		return mm != null ? new ArrayList<>(mm.getSourceFolders()) : Collections.emptyList();
	}

	/** Single-folder scope only: that folder's own packages shown directly at top level. */
	public List<SourcePackage> getPackagesInScope() {
		if (sourceFolder == null) {
			return Collections.emptyList();
		}
		return new ArrayList<>(sourceFolder.getPackages());
	}
}
