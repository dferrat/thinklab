/**
 * Ranking.java
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
package org.integratedmodelling.corescience.implementations.observations;

import org.integratedmodelling.corescience.CoreScience;
import org.integratedmodelling.corescience.implementations.cmodels.RankingModel;
import org.integratedmodelling.corescience.implementations.datasources.MemValueContextualizedDatasource;
import org.integratedmodelling.corescience.interfaces.cmodel.IConceptualModel;
import org.integratedmodelling.corescience.interfaces.data.IContextualizedState;
import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.interfaces.annotations.InstanceImplementation;
import org.integratedmodelling.thinklab.interfaces.knowledge.IConcept;
import org.integratedmodelling.thinklab.interfaces.knowledge.IConceptualizable;
import org.integratedmodelling.thinklab.interfaces.knowledge.IInstance;
import org.integratedmodelling.thinklab.interfaces.literals.IValue;
import org.integratedmodelling.thinklab.literals.BooleanValue;
import org.integratedmodelling.utils.Polylist;

/**
 * A ranking is the simplest of quantifications, defining the observable through
 * a numeric state that may or may not be bounded. Bounded ranks of different
 * scales can be mediated if they have been defined to represent scales. 
 * 
 * Ranks are double by default but can be constrained to
 * integers. Rankings are useful in providing an immediate translation for
 * nonsemantic "variables", e.g. in legacy models seen as observation
 * structures.
 * 
 * For ease of specification, rankings contain all their conceptual model
 * parameters in their own properties, and create and configure the conceptual
 * model automatically during validation.
 * 
 * @author Ferdinando Villa
 *
 */
@InstanceImplementation(concept="measurement:Ranking")
public class Ranking extends Observation implements IConceptualizable {

	private static final String MINVALUE_PROPERTY = "measurement:minValue";
	private static final String MAXVALUE_PROPERTY = "measurement:maxValue";
	private static final String ISINTEGER_PROPERTY = "measurement:isInteger";
	private static final String ISSCALE_PROPERTY = "measurement:isScale";
	
	double minV = 0.0;
	double maxV = -1.0;
	boolean integer = false;
	boolean isScale = false;

	@Override
	public IConceptualModel createMissingConceptualModel() throws ThinklabException {
		
		// read in scale attributes and pass to CM
		IValue min = getObservationInstance().get(MINVALUE_PROPERTY);
		IValue max = getObservationInstance().get(MAXVALUE_PROPERTY);
		IValue isi = getObservationInstance().get(ISINTEGER_PROPERTY);
		IValue iss = getObservationInstance().get(ISSCALE_PROPERTY);

		if (min != null)
			minV = min.asNumber().asDouble();
		if (max != null) 
			maxV = max.asNumber().asDouble();
		if (isi != null)
			integer = BooleanValue.parseBoolean(isi.toString());
		if (iss != null)
			isScale = BooleanValue.parseBoolean(iss.toString());

		return new RankingModel(minV, maxV, integer, min != null, max != null, isScale);

	}

	@Override
	public Polylist conceptualize() throws ThinklabException {

		return Polylist.list(
				CoreScience.RANKING,
				Polylist.list(CoreScience.HAS_OBSERVABLE,
						(getObservable() instanceof IConceptualizable) ? 
								((IConceptualizable)getObservable()).conceptualize() :
								getObservable().toList(null)),
						Polylist.list(MINVALUE_PROPERTY, minV+""),
						Polylist.list(MAXVALUE_PROPERTY, maxV+""),
						Polylist.list(ISINTEGER_PROPERTY, integer ? "true" : "false"),
						Polylist.list(ISSCALE_PROPERTY, isScale ? "true" : "false"));
	}

}
