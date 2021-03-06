package org.integratedmodelling.modelling.commands;

import java.util.HashMap;

import org.integratedmodelling.corescience.context.ObservationContext;
import org.integratedmodelling.corescience.interfaces.IContext;
import org.integratedmodelling.corescience.interfaces.IObservation;
import org.integratedmodelling.corescience.interfaces.IObservationContext;
import org.integratedmodelling.corescience.interfaces.IState;
import org.integratedmodelling.corescience.interfaces.internal.Topology;
import org.integratedmodelling.corescience.listeners.IContextualizationListener;
import org.integratedmodelling.geospace.Geospace;
import org.integratedmodelling.geospace.implementations.observations.RasterGrid;
import org.integratedmodelling.geospace.interfaces.IGazetteer;
import org.integratedmodelling.geospace.literals.ShapeValue;
import org.integratedmodelling.modelling.ModelMap;
import org.integratedmodelling.modelling.ObservationFactory;
import org.integratedmodelling.modelling.context.Context;
import org.integratedmodelling.modelling.interfaces.IDataset;
import org.integratedmodelling.modelling.interfaces.IVisualization;
import org.integratedmodelling.modelling.literals.ContextValue;
import org.integratedmodelling.modelling.model.DefaultAbstractModel;
import org.integratedmodelling.modelling.model.Model;
import org.integratedmodelling.modelling.model.ModelFactory;
import org.integratedmodelling.modelling.model.Scenario;
import org.integratedmodelling.modelling.storage.FileArchive;
import org.integratedmodelling.modelling.storage.NetCDFArchive;
import org.integratedmodelling.modelling.visualization.FileVisualization;
import org.integratedmodelling.modelling.visualization.ObservationListing;
import org.integratedmodelling.thinklab.command.Command;
import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.exception.ThinklabResourceNotFoundException;
import org.integratedmodelling.thinklab.exception.ThinklabValidationException;
import org.integratedmodelling.thinklab.interfaces.annotations.ThinklabCommand;
import org.integratedmodelling.thinklab.interfaces.applications.ISession;
import org.integratedmodelling.thinklab.interfaces.commands.ICommandHandler;
import org.integratedmodelling.thinklab.interfaces.knowledge.IConcept;
import org.integratedmodelling.thinklab.interfaces.knowledge.IInstance;
import org.integratedmodelling.thinklab.interfaces.literals.IValue;
import org.integratedmodelling.thinklab.interfaces.query.IQueryResult;
import org.integratedmodelling.thinklab.interfaces.storage.IKBox;
import org.integratedmodelling.thinklab.kbox.KBoxManager;
import org.integratedmodelling.time.TimeFactory;
import org.integratedmodelling.utils.Polylist;

@ThinklabCommand(
		name="model",
		description="build a model observation of the given concept and return it",
		argumentNames="model",
		argumentTypes="thinklab-core:Text",
		argumentDescriptions="the concept to build a model for or the model id",
		optionalArgumentNames="context,context1",
		optionalArgumentDefaultValues="_NONE_,_NONE_",
		optionalArgumentDescriptions="spatial or temporal context,spatial or temporal context",
		optionalArgumentTypes="thinklab-core:Text,thinklab-core:Text",
		optionArgumentLabels="all kboxes,,,none,256, , , , ",
		optionLongNames="kbox,visualize,dump,outfile,resolution,clear,scenario,write,map",
		optionNames="k,v,d,o,r,c,s,w,map",
		optionTypes="thinklab-core:Text,owl:Nothing,owl:Nothing,thinklab-core:Text,thinklab-core:Integer,owl:Nothing,thinklab-core:Text,owl:Nothing,owl:Nothing",
		optionDescriptions="kbox,visualize after modeling,dump results to console,NetCDF file to export results to,max linear resolution for raster grid,clear cache before computing,scenario to apply before computing,store results to standard workspace,show the model map (required dot installed)",
		returnType="observation:Observation")
public class ModelCommand implements ICommandHandler {

	IObservationContext ctx = null;
	HashMap<IConcept, IState> states = new HashMap<IConcept, IState>();
	
	class Listener implements IContextualizationListener {

		@Override
		public void onContextualization(IObservation original, ObservationContext context) {
			ctx = context;
		}

		@Override
		public void postTransformation(IObservation original, ObservationContext context) {
		}

		@Override
		public void preTransformation(IObservation original, ObservationContext context) {
		}
	}

	// TODO use elsewhere to create contexts from strings
	private IContext getTopology(Command command, String argid, ISession session) throws ThinklabException {
		
		IContext ret = null;
		
		String arg = command.getArgumentAsString(argid);
		if (arg != null && !arg.equals("_NONE_")) {
			
			if (Character.isDigit(arg.charAt(0))) {
				
				Polylist pls = TimeFactory.parseTimeTopology(arg);
				
				if (pls == null) {
					throw new ThinklabValidationException(
							"temporal extent specification invalid or unsupported: " + 
							arg);
				}
				IInstance when = 
					session.createObject(pls);
				ret = Context.getContext(((Topology)ObservationFactory.getObservation(when)).getExtent());
				
			} else {
				
				int res = 
					(int)command.getOptionAsDouble("resolution", 256.0);	
				ShapeValue roi = null;
				IQueryResult result = 
					Geospace.get().lookupFeature(arg);
				
				if (result.getTotalResultCount() > 0)
					roi = (ShapeValue) result.getResultField(0, IGazetteer.SHAPE_FIELD);
					
				if (roi != null) {
				
					IInstance where = 
						session.createObject(RasterGrid.createRasterGrid(roi, res));
					ret = Context.getContext(((Topology)ObservationFactory.getObservation(where)).getExtent());
					
					// TODO this should be part of the instance definition but it's very expensive
					((RasterGrid) ObservationFactory.getObservation(where)).mask(roi);

				} else { 
					throw new ThinklabResourceNotFoundException(
							"region name " + 
							arg +
							" cannot be resolved");
				}
				
			}
			
		}
		
		return ret;
	}
	
	@Override
	public IValue execute(Command command, ISession session)
			throws ThinklabException {
		
		String concept = command.getArgumentAsString("model");
		String ctxname = command.getArgumentAsString("context");
		
		IKBox kbox = KBoxManager.get();
		if (command.hasOption("kbox"))
			kbox = KBoxManager.get().requireGlobalKBox(command.getOptionAsString("kbox"));
		
		Model model = ModelFactory.get().requireModel(concept);
		
		IContext context = ModelFactory.get().requireContext(ctxname);

	
		if (command.hasOption("clear")) {
			ModelFactory.get().clearCache();
		}
		
		if (command.hasOption("map")) {
			ModelMap.show();
		}
			
		if (command.hasOption("scenario")) {

			// remove
			((DefaultAbstractModel)model).dump(System.out);

			
			String sc = command.getOptionAsString("scenario");
			Scenario scenario = ModelFactory.get().requireScenario(sc);
			model = (Model) model.applyScenario(scenario);
			
			// remove
			((DefaultAbstractModel)model).dump(System.out);
		}
		
		IQueryResult r = 
			ModelFactory.get().run(model, kbox, session, null, context);
		
		if (session.getOutputStream() != null) {
			session.getOutputStream().println(
					r.getTotalResultCount() + " possible observation(s) found");
		}
		
		IValue ret = null;
		
		if (r.getTotalResultCount() > 0) {
			
			IValue res = r.getResult(0, session);
			IContext result = ((ContextValue)res).getObservationContext();

			if (command.hasOption("write")) {
				IDataset archive = new FileArchive(result);
				archive.persist();
			}
			
			if (command.hasOption("visualize")) {
				IVisualization visualization = new FileVisualization(result);
				visualization.visualize();
			}
			
			// check if a listener has set ctx, which means we're visualizing
			if (command.hasOption("outfile")) {

				/*
				 * save to netcdf
				 */
				String outfile = command.getOptionAsString("outfile");

				NetCDFArchive out = new NetCDFArchive();
				out.setContext(result);
				out.write(outfile);
				session.print(
					"result of " + concept + " model written to " + outfile);
			}
			
			if (command.hasOption("dump")) {
				ObservationListing lister = new ObservationListing(result);
				lister.dump(session.getOutputStream());
			}

			ret = res;
		}
			
		return null;
	}

}
