/**
 * Session.java
 * ----------------------------------------------------------------------------------
 * 
 * Copyright (C) 2008 www.integratedmodelling.org
 * Created: Jan 17, 2008
 *
 * ----------------------------------------------------------------------------------
 * This file is part of Thinklab.
 * 
 * Thinklab is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Thinklab is distributed in the hope that it will be useful,
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
 * @author    Ioannis N. Athanasiadis (ioannis@athanasiadis.info)
 * @date      Jan 17, 2008
 * @license   http://www.gnu.org/licenses/gpl.txt GNU General Public License v3
 * @link      http://www.integratedmodelling.org
 **/
package org.integratedmodelling.thinklab.owlapi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

import org.integratedmodelling.thinklab.KnowledgeManager;
import org.integratedmodelling.thinklab.SemanticType;
import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.exception.ThinklabIOException;
import org.integratedmodelling.thinklab.exception.ThinklabResourceNotFoundException;
import org.integratedmodelling.thinklab.exception.ThinklabUnknownResourceException;
import org.integratedmodelling.thinklab.extensions.KnowledgeLoader;
import org.integratedmodelling.thinklab.interfaces.applications.ISession;
import org.integratedmodelling.thinklab.interfaces.applications.IThinklabSessionListener;
import org.integratedmodelling.thinklab.interfaces.applications.IUserModel;
import org.integratedmodelling.thinklab.interfaces.knowledge.IConcept;
import org.integratedmodelling.thinklab.interfaces.knowledge.IInstance;
import org.integratedmodelling.thinklab.interfaces.knowledge.IOntology;
import org.integratedmodelling.thinklab.interfaces.storage.IKBox;
import org.integratedmodelling.thinklab.kbox.KBoxManager;
import org.integratedmodelling.thinklab.kbox.VirtualSessionKBox;
import org.integratedmodelling.thinklab.session.TTYUserModel;
import org.integratedmodelling.utils.MiscUtilities;
import org.integratedmodelling.utils.NameGenerator;
import org.integratedmodelling.utils.Polylist;

/**
 * The basic Thinklab session, implemented on top of an Ontology.
 * <p><b>NOTE:</b> you're not supposed to create a Session. Ask the Knowledge manager for one instead.</p>
 * @author Ferdinando Villa, Ecoinformatics Collaboratory, UVM
 * @see KnowledgeManager#requestNewSession()
 */
public class Session implements ISession {

	IOntology ontology;

	HashMap<String, IInstance> importedObjects = new HashMap<String, IInstance>();
	HashMap<String, String> refs = new HashMap<String, String>();
	HashMap<String, Object> objects = new HashMap<String, Object>();
	
	/*
	 * virtual kboxes for all loaded object sources, so we can always refer to the objects loaded from each
	 * particular source. These are local to the session.
	 */
	HashMap<String, IKBox> vKboxes = new HashMap<String, IKBox>();
	
	
	Properties properties = new Properties();
	
	ArrayList<IThinklabSessionListener> listeners = new ArrayList<IThinklabSessionListener>();

	private IUserModel userModel;
	
	public Session() throws ThinklabException {
		
		/* create the new Ontology with a temp name */
		ontology = KnowledgeManager.get().getKnowledgeRepository().createTemporaryOntology(NameGenerator.newName("JS"));
		/* we want to be able to reload stuff fresh and give it different names if so */
		ontology.allowDuplicateInstanceIDs();
		
		userModel = createUserModel();
	}

	public void finalize() {
		/* notify KM just in case, so it can keep track and log if requested */
		try {
			KnowledgeManager.get().notifySessionDeletion(this);
		} catch (ThinklabException e) {
		}
	}
	
	/**
	 * Used internally to find concepts: can see the internal concepts in the session as well
	 * as the KM public ones.
	 * 
	 * @return
	 * @throws ThinklabResourceNotFoundException
	 */
	private IConcept getConcept(String id) throws ThinklabException {
		
		IConcept ret = null;
		if (id.startsWith(ontology.getConceptSpace()))
			ret = ontology.getConcept(id.substring(id.indexOf(":") + 1));
		
		if (ret == null)
			ret = KnowledgeManager.get().requireConcept(id);
		
		if (ret == null)
			throw new ThinklabResourceNotFoundException("concept " + id + " unknown to session");
		
		return ret;
	}
	
	/* (non-Javadoc)
	 * @see org.integratedmodelling.ima.core.ISession#getSessionID()
	 */
	public String getSessionID() {
		return ontology.getConceptSpace();
	}

	/* (non-Javadoc)
	 * 
	 * @see org.integratedmodelling.ima.core.ISession#makePermanent(java.lang.String)
	 */
	public void makePermanent(String name) throws ThinklabException {

		/* go over all individuals and delete those that have not been validated */
		ArrayList<String> blacklist = new ArrayList<String>();
		for (IInstance i : ontology.getInstances()) {
			if (!i.isValidated())
				blacklist.add(i.getURI());
		}
		for (String uri : blacklist) {
			ontology.removeInstance(uri);
		}
		/* TBC fix namespaces and URIs? Imports? */
		
		/* serialize to OWL and import the temporary doc into the knowledge base */
		try {
			File f = File.createTempFile("jimt", ".owl");
			ontology.write(f.toURI());
			KnowledgeManager.get().getKnowledgeRepository().importOntology(f.toURL(), name, false);
		} catch (IOException e) {
			throw new ThinklabIOException("can't create temporary ontology in filesystem");
		}
	}

	/* (non-Javadoc)
	 * @see org.integratedmodelling.ima.core.ISession#makePermanent()
	 */
	public String makePermanent() throws ThinklabException {
		String newn = NameGenerator.newName("TLSESSION_" + ontology.getConceptSpace());
		makePermanent(newn);
		return newn;
	}

	/* (non-Javadoc)
	 * @see org.integratedmodelling.ima.core.ISession#createObject(java.lang.String, org.integratedmodelling.ima.core.IConcept)
	 */
	public IInstance createObject(String name, IConcept parent) throws ThinklabException {
		return ontology.createInstance(name, parent);
	}

	/* (non-Javadoc)
	 * @see org.integratedmodelling.ima.core.ISession#createObject(java.lang.String, org.integratedmodelling.utils.Polylist)
	 */
	public IInstance createObject(String name, Polylist definition) throws ThinklabException {
	    return ontology.createInstance(name, definition);
    }
        
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.integratedmodelling.ima.core.ISession#loadObjects(java.net.URL)
	 *      TODO fix for general IOntology
	 */
	public Collection<IInstance> loadObjects(URL url) throws ThinklabException {

		boolean loaded = false;
		Collection<IInstance> ret = new ArrayList<IInstance>();

		// see if we have a plugin to load this
		String format = MiscUtilities.getFileExtension(url.toString());

		// see if we have a kbox protocol for this thing
		IKBox kbox = KBoxManager.get().retrieveGlobalKBox(url.toString());

		if (kbox != null) {
			String id = url.getRef();
			if (id != null && !id.equals(""))
				ret.add(kbox.getObjectFromID(id, this));
			return ret;
		}

		if (format != null) {

			/* find the plugin that handles these */
			KnowledgeLoader plu = KnowledgeManager.get().getKnowledgeLoader(format);

			if (plu != null) {

				String sname = MiscUtilities.getURLBaseName(url.toString());
				IKBox kb = vKboxes.get(sname);
				if (kb == null)
					kb = new VirtualSessionKBox(this);
				else if (kb instanceof VirtualSessionKBox) {
					/*
					 * TODO should erase all objects in kbox if it is virtual,
					 * so we just substitute the sessions' contents?
					 */
				}

				ret = plu.loadKnowledge(url, this, kb);

				vKboxes.put(sname, kb);

				loaded = true;
			}
		}
		if (!loaded)
			throw new ThinklabIOException("don't know how to handle format: "
					+ format);

		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.integratedmodelling.ima.core.ISession#deleteObject(java.lang.String)
	 */
	public void deleteObject(String name) throws ThinklabException {
		
		IInstance i = null;
		for (IThinklabSessionListener listener : listeners) {
			if (i == null)
				i = retrieveObject(name);
			listener.objectDeleted(i);
		}
		
		ontology.removeInstance(ontology.getURI()+name);
	}

	/* (non-Javadoc)
	 * @see org.integratedmodelling.ima.core.ISession#listObjects()
	 */
	public Collection<IInstance> listObjects() throws ThinklabException {
		return ontology.getInstances();
	}

	/* (non-Javadoc)
	 * @see org.integratedmodelling.ima.core.ISession#retrieveObject(java.lang.String)
	 */
	public IInstance retrieveObject(String name) {
		return ontology.getInstance(name);
	}

	/* (non-Javadoc)
	 * @see org.integratedmodelling.ima.core.ISession#requireObject(java.lang.String)
	 */
	public IInstance requireObject(String name)
			throws ThinklabResourceNotFoundException {

		IInstance ret = ontology.getInstance(name);
		if (ret == null)
			throw new ThinklabResourceNotFoundException("instance " + name + " does not match any in session");
		return ret;
	}

    public void write(String file) throws ThinklabException {

    	/* go over all individuals and delete those that have not been validated */
        ArrayList<String> blacklist = new ArrayList<String>();
        for (IInstance i : ontology.getInstances()) {
            if (!i.isValidated())
                blacklist.add(i.getURI());
        }
        for (String uri : blacklist) {
            ontology.removeInstance(uri);
        }
        /* TBC fix namespaces and URIs? Imports? */
        
        /* serialize to OWL and import the temporary doc into the knowledge base */
        try {
            File f = new File(file);
            ontology.write(f.toURI());
        } catch (Exception e) {
            throw new ThinklabIOException("can't create ontology in " + file + ": " + e.getMessage());
        }        
    }

	public IInstance createObject(String concept) throws ThinklabException {
		return createObject(ontology.getUniqueObjectName("JI"), getConcept(concept));
	}

	public IInstance createObject(SemanticType concept) throws ThinklabException {
		return createObject(ontology.getUniqueObjectName("JI"), getConcept(concept.toString()));
	}

	public IInstance createObject(String name, String concept) throws ThinklabException {
		return createObject(name, getConcept(concept));
	}

	public IInstance createObject(String name, SemanticType concept) throws ThinklabException {
		return createObject(name, getConcept(concept.toString()));
	}

	public Collection<IInstance> loadObjects(String source) throws ThinklabException {
		
		URL url = MiscUtilities.getURLForResource(source);
		if (url != null)
			return loadObjects(url);
		
		return null;
	}

	public IInstance createObject(Polylist polylist) throws ThinklabException {

		IInstance ret = ontology.createInstance(polylist);

		/* TODO see if we want to use OWL validation, maybe connected to a parameter or preference */
		ret.validate();

		for (IThinklabSessionListener listener : listeners) {
			listener.objectCreated(ret);
		}
		
		
		return ret;
	}

    public IInstance createObject(IInstance ii) throws ThinklabException {
        return createObject(ii.toList(null));
    }

	public IInstance importObject(String kboxURI) throws ThinklabException {

		IInstance ret = importedObjects.get(kboxURI);
		
		if (ret == null) {
		
			IKBox kb = KBoxManager.get().requireGlobalKBox(kboxURI);
			String id = null;
		
			int dot = kboxURI.indexOf("#");
			if (dot >= 0) {
				id = kboxURI.substring(dot + 1);
			}
	
			if (id == null)
				throw new ThinklabUnknownResourceException(kboxURI + " does not specify an object in a kbox");
		
			/*
			 * use the same ref table every time, so we never have to duplicate stuff.
			 */
			ret = kb.getObjectFromID(id, this, new HashMap<String, String>());

			for (IThinklabSessionListener listener : listeners) {
				listener.objectCreated(ret);
			}
		}
		
		return ret;
	}

	public void addListener(IThinklabSessionListener listener) throws ThinklabException {
		listeners.add(listener);
	}

	public Collection<IThinklabSessionListener> getListeners() {
		return listeners;
	}

	public Properties getProperties() {
		return properties;
	}

	public IKBox retrieveKBox(String string) throws ThinklabException {

		IKBox ret = vKboxes.get(string);
		if (ret == null)
			ret = KBoxManager.get().retrieveGlobalKBox(string);
		return ret;
	}

	public Collection<String> getLocalKBoxes() {
		
		ArrayList<String> ret = new ArrayList<String>();
		
		for (String kb : vKboxes.keySet()) {
			ret.add(kb);
		}
		
		return ret;
	}

	public IKBox requireKBox(String string) throws ThinklabException {
		IKBox ret = retrieveKBox(string);
		if (ret == null)
			throw new ThinklabResourceNotFoundException("kbox " + string + " not found");
		return ret;
	}

	public void clearUserData(String id) {
		if (objects.containsKey(id))
			objects.remove(id);
	}

	public void registerUserData(String id, Object object) {
		objects.put(id, object);
	}

	public Object requireUserData(String id) throws ThinklabResourceNotFoundException {
		Object ret = objects.get(id);
		if (ret == null)
			throw new ThinklabResourceNotFoundException("session: user object " + id + " not registered");
		return ret;
	}

	public Object retrieveUserData(String id) {
		return objects.get(id);
	}

	@Override
	public IUserModel getUserModel() {
		// TODO Auto-generated method stub
		return this.userModel;
	}

	@Override
	public void appendOutput(String string) {
		
		if (userModel != null && userModel.getOutputStream() != null) {
			userModel.getOutputStream().print(string);
		}
		
	}

	@Override
	public void displayOutput(String string) {

		if (userModel != null && userModel.getOutputStream() != null) {
			userModel.getOutputStream().println(string);
		}
	}

	@Override
	public InputStream getInputStream() {
		return userModel == null ? null : userModel.getInputStream();
	}

	@Override
	public PrintStream getOutputStream() {
		return userModel == null ? null : userModel.getOutputStream();
	}

	@Override
	public String readLine() throws ThinklabIOException {
		String ret = null;
		if (userModel != null && userModel.getOutputStream() != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(userModel.getInputStream()));
			try {
				ret = reader.readLine();
			} catch (IOException e) {
				throw new ThinklabIOException(e);
			}
		}
		return ret;
	}
	
	protected IUserModel createUserModel() {
		return new TTYUserModel();
	}

	@Override
	public IConcept createConcept(Polylist list) throws ThinklabException {
		return ontology.createConcept(list);
	}

}