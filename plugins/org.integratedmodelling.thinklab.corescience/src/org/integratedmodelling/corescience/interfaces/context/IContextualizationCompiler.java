/**
 * IContextualizationWorkflow.java
 * ----------------------------------------------------------------------------------
 * 
 * Copyright (C) 2008 www.integratedmodelling.org
 * Created: Jan 17, 2008
 *
 * ----------------------------------------------------------------------------------
 * This file is part of ThinklabCoreSciencePlugin.
 * 
 * ThinklabCoreSciencePlugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ThinklabCoreSciencePlugin is distributed in the hope that it will be useful,
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
 * @date      Jan 17, 2008
 * @license   http://www.gnu.org/licenses/gpl.txt GNU General Public License v3
 * @link      http://www.integratedmodelling.org
 **/
package org.integratedmodelling.corescience.interfaces.context;

import org.integratedmodelling.corescience.observation.IObservation;
import org.integratedmodelling.corescience.observation.Observation;
import org.integratedmodelling.thinklab.exception.ThinklabException;

/**
 * Testing new approach to contextualization. Not used anywhere except for development.
 */
public interface IContextualizationCompiler {

	/**
	 * Notify an observation that will need to be part of the workflow.
	 * @param observation
	 */
	public abstract void addObservation(IObservation observation);
	
	/**
	 * Notify that destination observation depends on source observation.
	 * @param destination
	 * @param source
	 */
	public abstract void addObservationDependency(IObservation destination, IObservation source);
	
	/**
	 * Notify that the state of destination observation will be taken from the state of 
	 * source destination, involving possible mediation of conceptual models and extents so
	 * that the state of source is seen by destination under its own viewpoint.
	 *  
	 * @param destination
	 * @param source
	 */
	public abstract void addMediatedDependency(IObservation destination, IObservation source);

	/*
	 * Produce a compiled class that can be instantiated and run to produce the states.
	 */
	public abstract Class<?> compile(IObservation observation, IObservationContext context) throws ThinklabException;
	
	
}