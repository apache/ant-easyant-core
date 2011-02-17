package org.apache.easyant.tasks.menu;

import org.apache.easyant.core.menu.MenuGenerator;
import org.apache.tools.ant.BuildException;

import java.io.IOException;
import java.util.List;

/**
 * Create a new menu using any registered generators.
 */
public class StartMenuTask extends AbstractMenuGeneratorTask {

	private String file;

	@Override
	public void execute() throws BuildException {
		if (getFile() == null) {
			throw new BuildException("file argument is required !");
		}

		//TODO: this isn't quite right.  we shouldn't be passing the same file argument to every generator.
		List<MenuGenerator> generators = getMenuGeneratorForContext(getContext()).getMenuGenerators();
		for (MenuGenerator generator : generators) {
			try {
			    generator.startMenu(getContext(), getFile());
			} catch (IOException ioe) {
			    throw new BuildException("Error writing menu file " + getFile() + ": " + ioe.getMessage(), ioe);
			}
		}
	}

	/**
	 * Get the file associated to this generator
	 * @return a file
	 */
	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

}
