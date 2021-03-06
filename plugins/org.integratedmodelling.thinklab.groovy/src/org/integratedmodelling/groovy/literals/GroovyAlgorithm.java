/**
 * GroovyAlgorithm.java
 * ----------------------------------------------------------------------------------
 * 
 * Copyright (C) 2008 www.integratedmodelling.org
 * Created: Jan 21, 2008
 *
 * ----------------------------------------------------------------------------------
 * This file is part of ThinklabGroovyPlugin.
 * 
 * ThinklabGroovyPlugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ThinklabGroovyPlugin is distributed in the hope that it will be useful,
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
package org.integratedmodelling.groovy.literals;

import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.interfaces.annotations.LiteralImplementation;
import org.integratedmodelling.thinklab.interfaces.knowledge.IConcept;
import org.integratedmodelling.thinklab.literals.AlgorithmValue;

/**
 * @author UVM Affiliate
 */
@LiteralImplementation(concept="groovy:GroovyCode")
public class GroovyAlgorithm extends AlgorithmValue {

	final static String declPattern = 
		"([a-zA-Z\\-_]+):([a-zA-Z\\-_]+) ([a-zA-Z\\-_]+) = '(.*?)'";
	final static String replPattern = 
		"org.integratedmodelling.ima.core.interfaces.IValue $3 = KM.validateLiteral(KM.requireConcept(\"$1:$2\"), \"$4\")";

	/**
	 * @throws ThinklabException
	 */
	public GroovyAlgorithm() throws ThinklabException {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param c
	 * @param s
	 * @throws ThinklabException
	 */
	public GroovyAlgorithm(IConcept c, String code) throws ThinklabException {
		super(c);
		
		/* Transform semantically typed literals in the code into constructors for their correspondent
		 * IValues. 
		 * 
		 * FIXME there may be advantage in declaring this as the actual type returned by
		 * validation and not just IValue. This may be done by preconstructing the literals
		 * here, (using find() and parsing the substitution fields) and setting them in the context,
		 * instead of calling the constructor in the code.
		 * 
		 */
		String newcode = code.replaceAll(declPattern, replPattern);
				
		/* set processed code into string value */
		value = newcode;
	}
	
	// just for testing, remove
	public static void main(String[] args) {
		
		String testalg = "ima:Measurement dio = '10 m/sec'; print(dio); " +
				"geospace:Polygon p = 'POLYGON(21 32 45)'; print(p); return dio + p;";
		
		System.out.println("before: " + testalg);
		System.out.println("after: " + testalg.replaceAll(declPattern, replPattern));
	}

}
