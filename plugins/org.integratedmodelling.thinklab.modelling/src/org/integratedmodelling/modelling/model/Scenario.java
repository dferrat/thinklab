package org.integratedmodelling.modelling.model;

import java.util.ArrayList;
import java.util.Map;

import org.integratedmodelling.modelling.corescience.ObservationModel;
import org.integratedmodelling.modelling.interfaces.IModel;
import org.integratedmodelling.modelling.interfaces.IModelForm;

/**
 * A scenario is an identification model containing observables that can be
 * swapped for others in a model. The form allows a simpler specification
 * of dependencies, which are used differently.
 * 
 * @author Ferdinando Villa
 *
 */
public class Scenario extends ObservationModel implements IModelForm {

	public Scenario(String namespace) {
		super(namespace);
		// TODO Auto-generated constructor stub
	}

	ArrayList<IModel> models = new ArrayList<IModel>();
	ArrayList<Object> editableData = new ArrayList<Object>();

	public void addModel(IModel model, Map<?,?> metadata, Object editableDesc) {
		
		models.add(model);
		editableData.add(editableDesc);
	}
	
	/**
	 * Add observables that were not defined, substitute those
	 * that were with the incoming ones.
	 * 
	 * @param scenario
	 */
	public void merge(Scenario scenario) {

		for (IModel m : scenario.models) {
			int i = 0;
			for (IModel om : models) {
				if (m.getObservableClass().is(om.getObservableClass())) {
					models.set(i, m);
					break;
				}
				i++;
			}
			if (i== models.size()) {
				models.add(m);
				editableData.add(((Model)m).editable);
			}
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		return 
			obj instanceof Scenario ? 
				getName().equals(((IModelForm)obj).getName()) : false;
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

}
