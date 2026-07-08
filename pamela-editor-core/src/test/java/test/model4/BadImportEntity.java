package test.model4;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Import;
import org.openflexo.pamela.annotations.Imports;
import org.openflexo.pamela.annotations.ModelEntity;

/**
 * Root entity whose {@code @Imports} declares two invalid targets: {@link NotAnEntity} (an
 * in-source type that resolves but is not {@code @ModelEntity}) and {@code java.lang.String}
 * (an external type, unresolved under {@code setNoClasspath(true)} with no configured
 * classpath). Both must fire the "does not resolve to a known @ModelEntity" {@link Warning}
 * and neither must end up in {@link #getImportedEntities()}.
 */
@ModelEntity(isAbstract = true)
@Imports({ @Import(NotAnEntity.class), @Import(String.class) })
public interface BadImportEntity extends AccessibleProxyObject {

	@Getter(value = "name")
	String getName();
}
