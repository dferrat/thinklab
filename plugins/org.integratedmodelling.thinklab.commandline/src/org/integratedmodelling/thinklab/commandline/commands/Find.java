/**
 * Is.java
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
package org.integratedmodelling.thinklab.commandline.commands;

import org.integratedmodelling.thinklab.KnowledgeManager;
import org.integratedmodelling.thinklab.command.Command;
import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.extensions.CommandHandler;
import org.integratedmodelling.thinklab.interfaces.applications.ISession;
import org.integratedmodelling.thinklab.interfaces.commands.ICommandInputProvider;
import org.integratedmodelling.thinklab.interfaces.commands.ICommandOutputReceptor;
import org.integratedmodelling.thinklab.interfaces.knowledge.IConcept;
import org.integratedmodelling.thinklab.interfaces.knowledge.IOntology;
import org.integratedmodelling.thinklab.interfaces.literals.IValue;

/**
 * Find command will search for knowledge. For now just finds a concept by
 * (exact) name.
 * 
 * @author Ferdinando
 * 
 */
public class Find implements CommandHandler {

	public IValue execute(Command command, ICommandInputProvider inputSource,
			ICommandOutputReceptor outputDest, ISession session, KnowledgeManager km) throws ThinklabException {

		// TODO this should figure out what the semantic type is for, cross
		// check properly, and
		// call the appropriate methods. So far it only handles concepts.
		String s1 = command.getArgumentAsString("c1");

		for (IOntology o : km.getKnowledgeRepository().retrieveAllOntologies()) {

			IConcept c = o.getConcept(s1);

			if (c != null) {
				System.out.println("\t" + c);
			}
		}
		return null;
	}

}
