package org.apache.easyant.tasks.menu;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.menu.MenuGeneratorRegistry;
import org.apache.tools.ant.Project;

public class MenuGeneratorUtils {

	public static MenuGeneratorRegistry createRegistryForContext(Project project, String context) {
		MenuGeneratorRegistry menuGeneratorRegistry = new MenuGeneratorRegistry(context);
		project.addReference(getReferenceId(context), menuGeneratorRegistry);
        return menuGeneratorRegistry;
	}

	public static MenuGeneratorRegistry getRegistryForContext(Project project, String context, boolean create) {
		MenuGeneratorRegistry registry = (MenuGeneratorRegistry) project.getReference(getReferenceId(context));
		if (registry != null) {
			return registry;
		} else if (create) {
			return createRegistryForContext(project, context);
		} else {
			throw new IllegalArgumentException("No menu generators are registered for " + context);
		}
	}

	private static final String getReferenceId(String context) {
		return context + EasyAntMagicNames.MENU_GENERATOR_REGISTRY_REF;
	}

}
