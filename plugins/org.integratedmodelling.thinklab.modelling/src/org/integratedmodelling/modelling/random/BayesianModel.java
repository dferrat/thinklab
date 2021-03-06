package org.integratedmodelling.modelling.random;

import java.util.ArrayList;
import java.util.Collection;

import org.integratedmodelling.corescience.CoreScience;
import org.integratedmodelling.corescience.ObservationFactory;
import org.integratedmodelling.corescience.interfaces.IContext;
import org.integratedmodelling.corescience.interfaces.internal.IndirectObservation;
import org.integratedmodelling.modelling.corescience.ClassificationModel;
import org.integratedmodelling.modelling.implementations.observations.BayesianTransformer;
import org.integratedmodelling.modelling.interfaces.IContextOptional;
import org.integratedmodelling.modelling.interfaces.IModel;
import org.integratedmodelling.modelling.model.DefaultAbstractModel;
import org.integratedmodelling.modelling.model.DefaultStatefulAbstractModel;
import org.integratedmodelling.modelling.model.Model;
import org.integratedmodelling.modelling.model.ModelFactory;
import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.exception.ThinklabRuntimeException;
import org.integratedmodelling.thinklab.exception.ThinklabValidationException;
import org.integratedmodelling.thinklab.interfaces.applications.ISession;
import org.integratedmodelling.thinklab.interfaces.knowledge.IConcept;
import org.integratedmodelling.thinklab.interfaces.storage.IKBox;
import org.integratedmodelling.utils.Polylist;

public class BayesianModel extends DefaultStatefulAbstractModel implements IContextOptional {

	public BayesianModel(String namespace) {
		super(namespace);
	}

	String source = null;
	String algorithm = null;
	ArrayList<IConcept> keepers = new ArrayList<IConcept>();
	ArrayList<IConcept> required = new ArrayList<IConcept>();
	IModel resultModel = null;
	
	@Override
	protected void copy(DefaultAbstractModel model) {
		super.copy(model);
		algorithm = ((BayesianModel)model).algorithm;
		keepers = ((BayesianModel)model).keepers;
		source = ((BayesianModel)model).source;
		resultModel = ((BayesianModel)model).resultModel;
		required = ((BayesianModel)model).required;
	}

	@Override
	public void applyClause(String keyword, Object argument)
			throws ThinklabException {
		
		if (keyword.equals(":import")) {
			this.source = argument.toString();
		} else if (keyword.equals(":algorithm")) {
			this.algorithm = argument.toString();
		} else if (keyword.equals(":keep")) {
			
			Collection<?> p = (Collection<?>) argument;
			for (Object c : p)
				keepers.add(ModelFactory.annotateConcept(namespace, c.toString()));

		} else if (keyword.equals(":required")) {
			
			Collection<?> p = (Collection<?>) argument;
			for (Object c : p)
				required.add(ModelFactory.annotateConcept(namespace, c.toString()));

		} else if (keyword.equals(":result")) {
			
			resultModel = (IModel)argument;
			this.metadata.putAll(((DefaultAbstractModel)resultModel).getMetadata());
			
		} else super.applyClause(keyword, argument);
			
	}

	@Override
	public boolean isStateful() {
		return resultModel != null;
	}
	
	@Override
	public void addObservedModel(IModel model) {
		
		if (! (((Model)model).getDefinition() instanceof ClassificationModel)) {
			throw new ThinklabRuntimeException(
					"bayesian node " + model.getName() + 
					" should be a classification");
		}

		// anything that is specifically modeled becomes a keeper automatically
		keepers.add(((DefaultAbstractModel)((Model)model).getDefinition()).getObservableClass());
		super.addObservedModel(model);
	}
	
	@Override
	public void validateMediatedModel(IModel model)
			throws ThinklabValidationException {
		super.validateMediatedModel(model);
	}


	@Override
	public IConcept getCompatibleObservationType(ISession session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IModel getConfigurableClone() {
		
		BayesianModel ret = new BayesianModel(namespace);
		ret.algorithm = algorithm;
		ret.required  = required;
		ret.keepers = keepers;
		ret.resultModel = resultModel;
		ret.source = source;
		ret.copy(this);
		return ret;
	}

	@Override
	public Polylist buildDefinition(IKBox kbox, ISession session, IContext context, int flags) throws ThinklabException {

		ArrayList<Object> arr = new ArrayList<Object>();
		IndirectObservation resultObservation = null;
		
		if (resultModel != null) {
			Polylist ls = 
				((Model)resultModel).getDefinition().buildDefinition(kbox, session, context, flags);
			resultObservation = 
				(IndirectObservation) ObservationFactory.getObservation(session.createObject(ls));	
		}
		
		arr.add("modeltypes:BayesianTransformer");
		
		if (!isMediating())
			arr.add(Polylist.list(CoreScience.HAS_OBSERVABLE, this.observableSpecs));
				
		if (source != null)
			arr.add(Polylist.list(BayesianTransformer.HAS_NETWORK_SOURCE, source));

		if (algorithm != null)
			arr.add(Polylist.list(BayesianTransformer.HAS_BAYESIAN_ALGORITHM, algorithm));

		for (int i = 0; i < keepers.size(); i++) {
			arr.add(Polylist.list(
						ModelFactory.RETAINS_STATES, 
						keepers.get(i).toString()));
			
		}
		
		for (int i = 0; i < required.size(); i++) {
			arr.add(Polylist.list(
						ModelFactory.REQUIRES_STATES, 
						required.get(i).toString()));
		}
				
		/*
		 * communicate how to model specific nodes that had their
		 * model specified by passing a prototype observation.
		 * 
		 * FIXME: this will create observations that will not be resolved, so when the
		 * models are mediating, they won't have their mediated counterpart so they must
		 * include an observable. The whole observable transmission strategy should be revised.
		 * For now we add the stupid flags and pass it to buildDefinition, but the function
		 * shouldn't need any flags.
		 */
		for (IModel c : observed) {
			arr.add(Polylist.list(
					BayesianTransformer.HAS_PROTOTYPE_MODEL,
					((Model)c).getDefinition().buildDefinition(kbox, session, null, FORCE_OBSERVABLE)));
		}

		Polylist ret = Polylist.PolylistFromArrayList(arr);

		ret = ObservationFactory.addReflectedField(ret, "observed", observed);
		
		if (resultObservation != null) {
			ret = ObservationFactory.addReflectedField(ret, "outputObservation", resultObservation);
		}
		
		return addDefaultFields(ret);
	}

	@Override
	public Polylist conceptualize() throws ThinklabException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void validateSemantics(ISession session) throws ThinklabException {
	}

	@Override
	protected Object validateState(Object state)
			throws ThinklabValidationException {
		// TODO Auto-generated method stub
		return null;
	}

}
