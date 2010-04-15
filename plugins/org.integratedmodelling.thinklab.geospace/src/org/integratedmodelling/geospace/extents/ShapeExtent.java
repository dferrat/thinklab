/**
 * ShapeExtent.java
 * ----------------------------------------------------------------------------------
 * 
 * Copyright (C) 2008 www.integratedmodelling.org
 * Created: Feb 18, 2008
 *
 * ----------------------------------------------------------------------------------
 * This file is part of ThinklabGeospacePlugin.
 * 
 * ThinklabGeospacePlugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ThinklabGeospacePlugin is distributed in the hope that it will be useful,
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
 * @date      Feb 18, 2008
 * @license   http://www.gnu.org/licenses/gpl.txt GNU General Public License v3
 * @link      http://www.integratedmodelling.org
 **/
package org.integratedmodelling.geospace.extents;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.integratedmodelling.corescience.interfaces.IEntifiable;
import org.integratedmodelling.corescience.interfaces.IExtent;
import org.integratedmodelling.corescience.interfaces.internal.IDatasourceTransformation;
import org.integratedmodelling.geospace.Geospace;
import org.integratedmodelling.geospace.coverage.VectorCoverage;
import org.integratedmodelling.geospace.feature.LazyShapeCollection;
import org.integratedmodelling.geospace.literals.ShapeValue;
import org.integratedmodelling.geospace.transformations.Rasterize;
import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.exception.ThinklabRuntimeException;
import org.integratedmodelling.thinklab.interfaces.knowledge.IConcept;
import org.integratedmodelling.thinklab.interfaces.literals.IValue;
import org.integratedmodelling.utils.Polylist;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Shape extents are generated by vector-oriented contextualization strategies. Raster 
 * CMs should be capable of dealing with them too.
 * 
 * Because shapes are usually the spatial representation of physical entities, we implement
 * IEntifiable so that the shapes can be retrieved.
 * 
 * @author Ferdinando
 *
 */
public class ShapeExtent extends ArealExtent implements IEntifiable {

	// we either have one shape or a feature collection. If we see space as a collection of features, the
	// shape should be the convex hull of all features, but we don't compute it unless necessary.
	Geometry shape = null;
	FeatureCollection<?,?> features = null;
	private String featureURL;
	// only used for lineage so far
	private VectorCoverage coverage = null;
	
	public ShapeExtent(ReferencedEnvelope envelope, CoordinateReferenceSystem crs) {
		super(crs, envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
		// our overall shape is the bounding box
		this.shape = new ShapeValue(envelope).getGeometry();
	}
	
	public ShapeExtent(Geometry shape, Envelope envelope, CoordinateReferenceSystem crs) {
		super(crs, envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
		this.shape = shape;
	}

	public ShapeExtent(FeatureCollection<?,?> features, String sourceURL, ReferencedEnvelope envelope, CoordinateReferenceSystem crs) {
		super(crs, envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
		this.features = features;
		this.featureURL = sourceURL;
		// our overall shape is the bounding box
		this.shape = new ShapeValue(envelope).getGeometry();
	}

	// this is used to create lineages so the original shapes can be reconstructed.
	public ShapeExtent(ArealExtent ex, VectorCoverage vectorCoverage) {
		super(ex.getCRS(),
			  ex.envelope.getMinX(), ex.envelope.getMinY(), ex.envelope.getMaxX(), ex.envelope.getMaxY());
		this.coverage  = vectorCoverage;
	}
	
	public ShapeExtent(ShapeValue shape) {
		super(shape.getCRS(), 
			  shape.getEnvelope().getMinX(), 
			  shape.getEnvelope().getMinY(), 
			  shape.getEnvelope().getMaxX(), 
			  shape.getEnvelope().getMaxY());
		this.shape = shape.getGeometry();
	}

	@Override
	public IValue getFullExtentValue() {
		return new ShapeValue(shape, getCRS());
	}

	@Override
	public IValue getState(int granule) throws ThinklabException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTotalGranularity() {
		return features == null ? 1 : features.size();
	}
	
	public Geometry getShape() {
		return shape;
	}

	public void setFeatures(FeatureCollection<?,?> features, String sourceURL) {
		this.features = features;
		this.featureURL = sourceURL;
	}

	/*
	 * a fast way to check if the features are different, assuming features are read-only
	 */
	public boolean hasDifferentFeatures(ShapeExtent otextent) {

		boolean ret = false;
		
		if (features == null && otextent.features == null) {

			if (shape == null && otextent.shape == null)
				ret = false;
			else if (shape == null || otextent.shape == null) 
				ret = true;
			else
				ret = shape.equals(otextent.shape);
			
		} else if (features == null || otextent.features == null) {
			ret = true;
		} else {
			ret = !(this.featureURL.equals(otextent.featureURL));
		}
		return ret;
	}


	@Override
	public Polylist conceptualize() throws ThinklabException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected IExtent createMergedExtent(
			ArealExtent orextent, ArealExtent otextent,
			CoordinateReferenceSystem ccr, Envelope common,
			 Envelope orenvnorm, Envelope otenvnorm) {

		// TODO everything needs thorough checking
		ArealExtent ret = null;
		
		// if any is a GridExtent, or if we have two different feature collections, we need
		// to move this to a GridExtent.
		if (otextent instanceof GridExtent) {

			// raster wins TODO should use the common envelope for the grid extent.
			GridExtent gext = new GridExtent((GridExtent)otextent);
			
			/*
			 * TODO rasterize our features onto the activation layer of the grid.
			 */
			
			
			ret = gext;
			
		} else if (orextent instanceof ShapeExtent && ((ShapeExtent)orextent).hasDifferentFeatures((ShapeExtent)otextent)) {
			
			// we can't really handle this as a vector operation yet. 
			// Will determine a polygonal overlay at some point. For now we just turn to raster, but
			// we need a smart guess for the resolution.
			// should check that they're exactly the same, or again rasterize

			
		} else {
			
			// should check that they're exactly the same, or again rasterize
			Geometry s =
				((ShapeExtent)orextent).getShape().intersection(((ShapeExtent)otextent).getShape());
			Envelope env = s.getEnvelopeInternal();	
			ret = new ShapeExtent(s, env, getCRS());
			
		}

		return ret;
		
	}

	@Override
	protected IExtent createConstrainedExtent(
			ArealExtent orextent, ArealExtent otextent,
			CoordinateReferenceSystem ccr, Envelope common,
			 Envelope orenvnorm, Envelope otenvnorm) {

		return createMergedExtent(orextent, otextent, ccr, common, orenvnorm, otenvnorm);
	}

	@Override
	public IDatasourceTransformation getDatasourceTransformation(
			IConcept mainObservable, IExtent extent) throws ThinklabException {
		
		IDatasourceTransformation ret = null;
		
		if (extent instanceof GridExtent) {
			ret = new Rasterize((GridExtent) extent);
		} else if (extent instanceof ShapeExtent) {
//			Geometry s =
//				((ShapeExtent)extent).getShape().intersection(this.getShape());
//			Envelope env = s.getEnvelopeInternal();	
//			ret = new ShapeExtent(s, env, getCRS());
			
			//throw new ThinklabValidationException("shape extent: cannot transform to extent " + extent);
		}
		return ret;
	}

	@Override
	public IExtent getExtent(int granule) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSignature() {
		try {
			return 
				Geospace.getCRSIdentifier(crs, false) +
				"," +
				new ShapeValue(shape,crs).getWKB();
		} catch (ThinklabException e) {
			throw new ThinklabRuntimeException(e);
		}
	}

	@Override
	public Collection<IValue> getEntities() throws ThinklabException {
		
		ArrayList<IValue> ret = new ArrayList<IValue>();
		if (features != null) {
		
			/*
			 * retrieve our shapes, cut appropriately for the bounding box. We need to use
			 * a lazy collection here, we may have thousands of these.
			 */
			return 
				new LazyShapeCollection(coverage.getFeatureIterator(envelope, (String[])null), crs);
			
		} else {
			ret.add(new ShapeValue(shape, crs));
		}
		return ret;
	}

	@Override
	public IExtent or(IExtent myExtent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IExtent getAggregatedExtent() {
		return new ShapeExtent(new ShapeValue(shape, getCRS()));
	}

//	@Override
//	public Polylist conceptualize() throws ThinklabException {
//
//		// TODO this is wrong syntax for an ArealLocation
//		return Polylist.list(
//				Geospace.AREAL_LOCATION,
//				Polylist.list(Geospace.LAT_LOWER_BOUND, "" + getSouth()),
//				Polylist.list(Geospace.LON_LOWER_BOUND, "" + getWest()),
//				Polylist.list(Geospace.LAT_UPPER_BOUND, "" + getNorth()),
//				Polylist.list(Geospace.LON_UPPER_BOUND, "" + getEast()),
//				Polylist.list(Geospace.CRS_CODE, 
//						Geospace.getCRSIdentifier(getCRS(), false)));
//		
//	}

}
