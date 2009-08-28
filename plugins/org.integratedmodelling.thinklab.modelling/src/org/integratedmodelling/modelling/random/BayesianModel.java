package org.integratedmodelling.modelling.random;

import java.util.ArrayList;
import java.util.Collection;

import org.integratedmodelling.corescience.CoreScience;
import org.integratedmodelling.modelling.DefaultStatefulAbstractModel;
import org.integratedmodelling.modelling.implementations.observations.BayesianTransformer;
import org.integratedmodelling.modelling.interfaces.IModel;
import org.integratedmodelling.thinklab.KnowledgeManager;
import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.exception.ThinklabValidationException;
import org.integratedmodelling.thinklab.interfaces.applications.ISession;
import org.integratedmodelling.thinklab.interfaces.knowledge.IConcept;
import org.integratedmodelling.thinklab.interfaces.storage.IKBox;
import org.integratedmodelling.utils.Polylist;

public class BayesianModel extends DefaultStatefulAbstractModel {

	String source = null;
	String algorithm = null;
	ArrayList<IConcept> keepers = new ArrayList<IConcept>();
	
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
				keepers.add(KnowledgeManager.get().requireConcept(c.toString()));

		} else super.applyClause(keyword, argument);
			
	}

	@Override
	protected void validateMediatedModel(IModel model)
			throws ThinklabValidationException {
	}

	@Override
	protected Object validateState(Object state)
			throws ThinklabValidationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IConcept getCompatibleObservationType(ISession session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IModel getConfigurableClone() {
		
		BayesianModel ret = new BayesianModel();
		ret.copy(this);

		// TODO the rest
		
		return ret;
	}

	@Override
	public Polylist buildDefinition(IKBox kbox, ISession session) throws ThinklabException {

		ArrayList<Object> arr = new ArrayList<Object>();
		
		arr.add("modeltypes:BayesianTransformer");
		
		if (!isMediating())
			arr.add(Polylist.list(CoreScience.HAS_OBSERVABLE, this.observableSpecs));
				
		if (source != null)
			arr.add(Polylist.list(BayesianTransformer.HAS_NETWORK_SOURCE, source));

		if (algorithm != null)
			arr.add(Polylist.list(BayesianTransformer.HAS_BAYESIAN_ALGORITHM, algorithm));

		for (int i = 0; i < keepers.size(); i++) {
			arr.add(Polylist.list(
						BayesianTransformer.RETAINS_STATES, 
						keepers.get(i).toString()));
		}
		
		return Polylist.PolylistFromArrayList(arr);
	}

	@Override
	public Polylist conceptualize() throws ThinklabException {
		// TODO Auto-generated method stub
		return null;
	}

}