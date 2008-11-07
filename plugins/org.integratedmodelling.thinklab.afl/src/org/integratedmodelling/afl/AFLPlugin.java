/**
 * SearchEnginePlugin.java
 * ----------------------------------------------------------------------------------
 * 
 * Copyright (C) 2008 www.integratedmodelling.org
 * Created: Jan 21, 2008
 *
 * ----------------------------------------------------------------------------------
 * This file is part of ThinklabSearchEnginePlugin.
 * 
 * ThinklabSearchEnginePlugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ThinklabSearchEnginePlugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * ----------------------------------------------------------------------------------
 * 
 * @copyright 2008 www.integratedmodelling.org
 * @author    Ferdinando Villa (fvilla@uvm.edu)
 * @date      Jan 21, 2008
 * @license   http://www.gnu.org/licenses/gpl.txt GNU General Public License v3
 * @link      http://www.integratedmodelling.org
 **/
package org.integratedmodelling.afl;

import java.util.ArrayList;
import java.util.Properties;

import org.integratedmodelling.afl.Application;
import org.integratedmodelling.afl.library.ListLib;
import org.integratedmodelling.afl.library.MathLib;
import org.integratedmodelling.afl.library.StringLib;
import org.integratedmodelling.afl.library.ThinkLib;
import org.integratedmodelling.thinklab.KnowledgeManager;
import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.exception.ThinklabValidationException;
import org.integratedmodelling.thinklab.plugin.ThinklabPlugin;
import org.integratedmodelling.utils.MalformedListException;
import org.integratedmodelling.utils.Polylist;
import org.java.plugin.registry.Extension;

public class AFLPlugin extends ThinklabPlugin {

	static final String PLUGIN_ID = "org.integratedmodelling.thinklab.afl";

	Interpreter rootInterpreter = null;
	
	ArrayList<Application> applications = new ArrayList<Application>();
	
	@Override
	protected void loadExtensions() throws Exception {
		// TODO Auto-generated method stub
		super.loadExtensions();
		for (Extension ext : getAllExtensions(PLUGIN_ID, "application")) {
			applications.add(createApplication(ext, getProperties()));			
		}
	}


	public static AFLPlugin get() {
		return (AFLPlugin) getPlugin(PLUGIN_ID );
	}
	/**
	 * Get your engine here, passing the necessary configuration properties. 
	 * @param properties 
	 * 
	 * @param id
	 * @param properties
	 * @return
	 * @throws ThinklabException
	 */
	public Application createApplication(Extension ext, Properties properties) throws ThinklabException {
		
		String id = getParameter(ext, "id");
		
		log.info("registering application " + id);

		String wflow = getParameter(ext, "workflow", "()");
		String mclass = getParameter(ext, "model-class");
		String[] packages = getParameters(ext, "step-package");
		
		log.info("application " + id + " registered");
		
		try {
			return new Application(id, Polylist.parse(wflow), mclass, packages);
		} catch (MalformedListException e) {
			throw new ThinklabValidationException(
					"error creating application " + id + "; workflow is not a well-formed list");
		}
	}
	
	public Application getApplication(String id) {
		
		Application ret = null;
		
		for (Application app : applications) {
			if (app.getId().equals(id)) {
				ret = app;
				break;
			}
		}
		
		return ret;
	}

	/**
	 * The root AFL interpreter. Any user code should execute in a child of this one.
	 * @return
	 */
	public Interpreter getRootInterpreter() {
		return rootInterpreter;
	}
	
	@Override
	protected void load(KnowledgeManager km) throws ThinklabException {
		
		/*
		 * create the root interpreter
		 */
		rootInterpreter = new Interpreter();
		
		/*
		 * preload all static content, initialize heap.
		 */
		new MathLib().installLibrary(rootInterpreter);
		new StringLib().installLibrary(rootInterpreter);
		new ListLib().installLibrary(rootInterpreter);
		new ThinkLib().installLibrary(rootInterpreter);
		
	}

	@Override
	protected void unload() throws ThinklabException {
		
		rootInterpreter.cleanup();
	}

}