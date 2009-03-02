/**
 * TimeRecord.java
 * ----------------------------------------------------------------------------------
 * 
 * Copyright (C) 2008 www.integratedmodelling.org
 * Created: Jan 17, 2008
 *
 * ----------------------------------------------------------------------------------
 * This file is part of ThinklabTimePlugin.
 * 
 * ThinklabTimePlugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ThinklabTimePlugin is distributed in the hope that it will be useful,
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
package org.integratedmodelling.time.implementations.observations;

import org.integratedmodelling.corescience.implementations.observations.Observation;
import org.integratedmodelling.corescience.interfaces.cmodel.IConceptualModel;
import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.time.implementations.cmodels.InstantConceptualModel;
import org.integratedmodelling.time.literals.TimeValue;

/**
 * This represents the implementation of a TemporalRecord, an observation of time that can only state one
 * single point in time, and will refuse to be merged with any record of a different point.
 * 
 * @author Ferdinando Villa
 *
 */
public class TimeRecord extends Observation {

	TimeValue value = null;
	
	@Override
	public IConceptualModel createMissingConceptualModel() throws ThinklabException {
		
		return new InstantConceptualModel(value = (TimeValue) getDataSource());
	}

	

}