/**
 * Clear.java
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
package org.integratedmodelling.thinklab.command.base;

import org.integratedmodelling.thinklab.KnowledgeManager;
import org.integratedmodelling.thinklab.command.Command;
import org.integratedmodelling.thinklab.command.CommandDeclaration;
import org.integratedmodelling.thinklab.command.CommandPattern;
import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.interfaces.IAction;
import org.integratedmodelling.thinklab.interfaces.ICommandOutputReceptor;
import org.integratedmodelling.thinklab.interfaces.ISession;
import org.integratedmodelling.thinklab.interfaces.IValue;
import org.integratedmodelling.thinklab.session.SingleSessionManager;

/**
 * admin command to clear knowledge base (totally or selectively)
 * @author Ferdinando Villa, Ecoinformatics Collaboratory, UVM
 *
 */
public class Clear extends CommandPattern {

	class ClearAction implements IAction {

		public IValue execute(Command command, ICommandOutputReceptor outputDest, ISession session, KnowledgeManager km) throws ThinklabException {
			// TODO we want arguments and warnings

			if (command.getArgumentAsString("ontology").equals("__all__")) {
				if (km.getSessionManager() instanceof SingleSessionManager) {
					((SingleSessionManager)km.getSessionManager()).clear();
				}
			} else
				km.clear(command.getArgumentAsString("ontology"));
			
			return null;
		}	
	}

	 
	
	public Clear() {
		super(); 
	}

	@Override
	public CommandDeclaration createCommand() {
		CommandDeclaration cd = new CommandDeclaration("clear", "clear current session or specified ontology from knowledge base");
		
		/* we'll have args and options, I promise */
		try {
			cd.addOptionalArgument("ontology", "the ontology to clear", 
					KnowledgeManager.get().getTextType().getSemanticType(), "__all__");
		} catch (ThinklabException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return cd;
	}

	@Override
	public IAction createAction() {
		// TODO Auto-generated method stub
		return new ClearAction();
	}

}