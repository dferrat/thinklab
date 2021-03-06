/**
 * OPALWriter.java
 * ----------------------------------------------------------------------------------
 * 
 * Copyright (C) 2008 www.integratedmodelling.org
 * Created: Jan 17, 2008
 *
 * ----------------------------------------------------------------------------------
 * This file is part of ThinklabOPALPlugin.
 * 
 * ThinklabOPALPlugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ThinklabOPALPlugin is distributed in the hope that it will be useful,
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
package org.integratedmodelling.opal;

import java.io.File;
import java.util.HashSet;

import org.integratedmodelling.opal.profile.OPALProfile;
import org.integratedmodelling.opal.profile.OPALProfileFactory;
import org.integratedmodelling.thinklab.KnowledgeManager;
import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.interfaces.knowledge.IInstance;
import org.integratedmodelling.thinklab.interfaces.knowledge.IRelationship;
import org.integratedmodelling.utils.MiscUtilities;
import org.integratedmodelling.utils.xml.XMLDocument;
import org.w3c.dom.Node;

/**
 * @author villa
 *
 */
public class OPALWriter {
	
	
	private static void writeInstance(IInstance i, XMLDocument document, OPALProfile profile, HashSet<String> refs) throws ThinklabException {
		
		if (refs == null)
			refs = new HashSet<String>();
		
		writeInstanceInternal(i, document, document.root(), profile, refs);
		
	}
	
	
	private static Node writeInstanceInternal(IInstance instance, XMLDocument document, Node parent,
			OPALProfile profile, HashSet<String> refs) throws ThinklabException {

		
		/* 
		 * if this has been written before, just write out a ref for it. Make sure
		 * we have all prefixes right.
		 */
		if (refs.contains(instance.getLocalName())) { 
			
			/* we just do nothing if it's being appended to the root node and we 
			 * already have it. 
			 */
			if (parent.getNodeName().equals(profile.getDefaultRootNodeID())) {
				return null;
			}
			
			/*
			 * write the ref if the parent is a relationship
			 */
			return document.appendTextNode(
					profile.getDefaultReferenceTag(),
					instance.getLocalName(), 
					parent);
		}
		
		/* have profile determine node name, ID etc. appropriately */
		Node ret = 
			document.createNode(
					profile.getOPALConceptID(instance.getDirectType().getSemanticType(),document),
					parent);
		
		document.addAttribute(
				ret, 
				profile.getDefaultNameTag(), 
				instance.getLocalName());
		
		refs.add(instance.getLocalName());
		
		/* write labels and comments if any, plus any annotations */
		String label = instance.getLabel();
		
		if (label != null && !label.equals(""))
			document.appendTextNode(profile.getDefaultLabelTag(), label, ret);
		
		label = instance.getDescription();
		
		if (label != null && !label.equals(""))
			document.appendTextNode(profile.getDefaultDescriptionTag(), label, ret);
		
		/* scan properties */
		for (IRelationship r : instance.getRelationships()) {

			/* 
			 * see the relationship id, and ignore the whole thing if it's one of those 
			 * that don't appear in the KM, such as protege metadata.
			 */
			String cid = profile.getOPALConceptID(r.getProperty().getSemanticType(), document);
			
			if (cid == null)
				continue;

			/*
			 * honor anything that has been blacklisted in the knowledge manager
			 */
			if (KnowledgeManager.get().isPropertyBlacklisted(r.getProperty().toString()))
				continue;
			
			/*
			 * object relationship: write rel node unless implicit, and recurse
			 */
			if (r.isObject()) {
				
				/* see if we can default the relationship */
				if (profile.getDefaultRelationship(
						instance.getDirectType(),
						r.getValue().asObjectReference().getObject().getDirectType())
						== null) {
				
					/* nope, write it up as is */
					Node reln = document.createNode(cid, ret);
				
					writeInstanceInternal(
						r.getValue().asObjectReference().getObject(),
						document,
						reln,
						profile,
						refs);
					
				} else {
					
					/* 
					 * default relationship, just write the instance directly
					 * within the main instance.
					 */
					writeInstanceInternal(
							r.getValue().asObjectReference().getObject(),
							document,
							ret,
							profile,
							refs);
				}
				
				
			} else if (r.isClassification()) {
				
				document.appendTextNode(
						cid,
						r.getValue().getConcept().toString(), 
						ret);
				
			} else {

				document.appendTextNode(
						cid,
						r.getValue().toString(), 
						ret);
			}
		}
		
		return ret;
		
	}

	/**
	 * Serialize the passed instances to the given file using the default profile or
	 * any profile that corresponds to the outfile extension if it is not .xml.
	 * 
	 * @param outfile
	 * @param instances
	 * @throws ThinklabException
	 */
	public static void writeInstances(File outfile, IInstance ... instances) throws ThinklabException {
		
		String profile = null;
		if (!outfile.toString().endsWith(".xml")) {
			profile = MiscUtilities.getFileExtension(outfile.toString());
		}
		writeInstances(outfile, profile, instances);
	}
	
	/**
	 * Serialize the passed instances to the given outfile using the specified
	 * OPAL profile. If null is passed for the profile name, use the default
	 * profile. 
	 * 
	 * @param outfile
	 * @param profile
	 * @param instances
	 * @throws ThinklabException
	 */
	public static void writeInstances(File outfile, String profile, IInstance ... instances) throws ThinklabException {
		
		/* 
		 * fetch profile
		 */
		OPALProfile prof = OPALProfileFactory.get().getProfile(profile, true);
		
		/* 
		 * create xml document; if file name ends with .xml, add profile processing
		 * instruction unless profile is null.
		 */
		XMLDocument doc = new XMLDocument(prof.getDefaultRootNodeID());
		
		HashSet<String> refs = new HashSet<String>();
		
		/* write down the instances */
		for (IInstance inst : instances) {
			writeInstance(inst, doc, prof, refs);
		}

		if (!prof.isDefault()) {
			/* add processing instruction to set profile - do it anyway even if
			 * we have given a different extension. */
			doc.addProcessingInstruction("OPAL", "profile=" + prof.getName());
		}
		
		/* serialize document to file */
		doc.writeToFile(outfile);
	}
	
}
