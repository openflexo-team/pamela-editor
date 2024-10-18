package org.openflexo.pamela.editor;

import java.io.File;

import org.openflexo.pamela.annotations.ModelEntity;

import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.AnnotationFilter;

public class MainTestSpoon {

	public static void main(String[] args) {
		SpoonAPI spoon = new Launcher();

		System.out.println("Ici: " + System.getProperty("user.dir"));

		File f = new File(System.getProperty("user.dir") + "/src/test/resources/JavaCode/Model1");
		System.out.println("f: " + f + " exists " + f.exists());

		spoon.addInputResource(f.getAbsolutePath());
		CtModel model = spoon.buildModel();
		for (CtPackage ctPackage : model.getAllPackages()) {
			System.out.println("Package: " + ctPackage);
			for (CtType<?> ctType : ctPackage.getTypes()) {
				System.out.println("> " + ctType.getQualifiedName());
				System.out.println(
						"fragment: " + ctType.getOriginalSourceFragment().getStart() + "-" + ctType.getOriginalSourceFragment().getEnd());
				System.out.println("Les methodes:");
				for (CtMethod<?> ctMethod : ctType.getMethods()) {
					System.out.println(">>> " + ctMethod.getShortRepresentation());
					// System.out.println("fragment: " + ctMethod.getOriginalSourceFragment().getStart() + "-"
					// + ctMethod.getOriginalSourceFragment().getEnd() + " : [" + ctMethod.getOriginalSourceFragment() + "]");
					// System.out.println("comment: " + ctMethod.getDocComment());

				}
			}
			for (CtElement ctElement : ctPackage.getElements(null)) {
				System.out.println("-----> " + ctElement + " of " + ctElement.getClass().getSimpleName());
			}
			System.out.println("Prout");
			for (CtElement ctElement : ctPackage.getElements(new AnnotationFilter<CtElement>(ModelEntity.class))) {
				System.out.println("-----> " + ctElement + " of " + ctElement.getClass().getSimpleName());
			}
		}
	}
}
