/**
 * VectorCoverage.java
 * ----------------------------------------------------------------------------------
 * 
 * Copyright (C) 2008 www.integratedmodelling.org
 * Created: Feb 26, 2008
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
 * @date      Feb 26, 2008
 * @license   http://www.gnu.org/licenses/gpl.txt GNU General Public License v3
 * @link      http://www.integratedmodelling.org
 **/
package org.integratedmodelling.geospace.coverage;

import java.awt.Color;
import java.io.File;
import java.net.URL;

import javax.swing.JFrame;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.gui.swing.JMapPane;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.DefaultMapLayer;
import org.geotools.map.MapContext;
import org.geotools.map.MapLayer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.shape.ShapefileRenderer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.integratedmodelling.corescience.interfaces.cmodel.IConceptualModel;
import org.integratedmodelling.geospace.extents.ArealExtent;
import org.integratedmodelling.geospace.extents.GridExtent;
import org.integratedmodelling.geospace.feature.AttributeTable;
import org.integratedmodelling.geospace.gis.ThinklabRasterizer;
import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.exception.ThinklabIOException;
import org.integratedmodelling.thinklab.exception.ThinklabUnimplementedFeatureException;
import org.integratedmodelling.thinklab.exception.ThinklabValidationException;
import org.integratedmodelling.thinklab.interfaces.literals.IValue;
import org.integratedmodelling.utils.MiscUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class VectorCoverage implements ICoverage {

	private FeatureCollection<SimpleFeatureType, SimpleFeature> features = null;
	CoordinateReferenceSystem crs = null;
	private BoundingBox boundingBox = null;
	private String layerName = null;
	private String valueField = null;
	private String sourceUrl = null;
	
	/*
	 * if this is > -1, we have an independent attribute table associated with the feature collection. 
	 * If so, valueField is the feature attribute we use to link to it, and the handle points to a 
	 * translation table for the attribute.
	 */
	int attributeHandle = -1;

	
	public VectorCoverage(URL url, String valueField, boolean validate) throws ThinklabException {
		
		sourceUrl  = url.toString();
		
		ShapefileDataStore sds;
		try {
			sds = new ShapefileDataStore(url);
			layerName = MiscUtilities.getNameFromURL(url.toString());
			features = sds.getFeatureSource(sds.getTypeNames()[0]).getFeatures();
			boundingBox = sds.getFeatureSource(sds.getTypeNames()[0]).getBounds();
			
			// cross fingers
			crs = features.getSchema().getCoordinateReferenceSystem();
			
			if (validate)
				validateFeatures();

		} catch (Exception e) {
			throw new ThinklabIOException(e);
		}
	}

	/**
	 * Check all features and make sure they're OK for a vector coverage. Means that they should
	 * be all polygons and have the identified value field.
	 */
	private void validateFeatures() {
		// TODO
	}

	public VectorCoverage(
			FeatureCollection<SimpleFeatureType, SimpleFeature> features,
			CoordinateReferenceSystem crs, 
			String valueField, 
			boolean validate) {
		
		this.features = features;
		this.crs = crs;
		this.valueField = valueField;

		if (validate)
			validateFeatures();
		
		computeEnvelope();
	}
	
	/**
	 * This one takes the features from one source and the values from another, typically a CVS file or
	 * database query, and links the features to the values through the value of an attribute of the feature.
	 * 
	 * E.g., connect a country shapefile with a spreadsheet of country population statistics through the country
	 * ISO code.
	 * 
	 * If created through the CoverageFactory, we ensure that there is no duplication of the coverages and tables
	 * as long as the corresponding URLs stay the same.
	 * 
	 * @param features
	 * @param crs
	 * @param valueField
	 * @param attributes
	 * @param validate
	 * @throws ThinklabException 
	 */
	public VectorCoverage(
			FeatureCollection<SimpleFeatureType, SimpleFeature> features,
			CoordinateReferenceSystem crs, 
			AttributeTable attributes,
			String linkField,
			String linkTargetField,
			String valueField,
			boolean validate) throws ThinklabException {
		
		this.features = features;
		this.crs = crs;
		this.valueField = linkField;
		this.attributeHandle = attributes.index(linkTargetField, valueField);
		
		if (validate)
			validateFeatures();
		
		computeEnvelope();
	}
	
	private void computeEnvelope() {
		
		// determine common envelope for all features.
		for (FeatureIterator<SimpleFeature> f = features.features(); f.hasNext() ; ) {
			
			SimpleFeature ff = f.next();
			
			BoundingBox env = ff.getBounds();
			
			if (boundingBox == null) {
				boundingBox = env;
			} else {
				boundingBox.include(env);
			}
		}
			
	}

	public String getCoordinateReferenceSystemCode()
			throws ThinklabValidationException {
		try {
			return CRS.lookupIdentifier(crs, true);
		} catch (FactoryException e) {
			// FIXME when this thing works, just throw the exception
			return crs.getIdentifiers().iterator().next().toString();
			// throw new ThinklabValidationException(e);
		}
	}

	public BoundingBox getBoundingBox() {
		return boundingBox;
	}
	
	public double getLatLowerBound() {
		return boundingBox.getMinY();
	}

	public double getLatUpperBound() {
		return boundingBox.getMaxY();
	}

	public double getLonLowerBound() {
		return boundingBox.getMinX();
	}

	public double getLonUpperBound() {
		return boundingBox.getMaxX();
	}
	
	public String getLayerName() {
		return layerName == null ? "no name" : layerName;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public IValue getSubdivisionValue(int subdivisionOrder,
			IConceptualModel conceptualModel, ArealExtent extent)
			throws ThinklabValidationException {
		// TODO Auto-generated method stub
		return null;
	}

	public void loadData() {
		// nothing to do
	}

	public VectorCoverage requireCRS(CoordinateReferenceSystem crs)
			throws ThinklabException {
		// TODO Auto-generated method stub
		return null;
	}

	public ICoverage requireMatch(ArealExtent arealExtent,
			boolean allowClassChange) throws ThinklabException {

		ICoverage ret = null;
		
		if (arealExtent instanceof GridExtent && allowClassChange) {
			
				/* 
				 * WOW - we want a raster - rasterize it 
				 * TODO implement some sort of caching mechanism
				 */
				ret = convertToRaster((GridExtent) arealExtent);

		}  else {
		
			/*
			 * TODO reproject and subset if the passed extent is different from ours
			 */
			ret = this;
		}
		
		return ret;
	}

	/**
	 * Create a new raster coverage for the passed extent and return it. It even sounds 
	 * easy.
	 * 
	 * @param arealExtent
	 * @return
	 * @throws ThinklabException
	 */
	public ICoverage convertToRaster(GridExtent arealExtent) throws ThinklabException {
		return ThinklabRasterizer.rasterize(this, valueField, -9999.0f, arealExtent);
	}

	public void show() {

		JFrame frame = new JFrame(getLayerName());
		
        // create the style
        StyleBuilder sb = new StyleBuilder();
        Style simple = sb.createStyle(sb.createPolygonSymbolizer(
                    Color.LIGHT_GRAY, Color.BLACK, 1));
        
        JMapPane mapPane = new JMapPane();
        mapPane.setRenderer(new ShapefileRenderer());
        
        MapLayer layer = new DefaultMapLayer(features, simple);
        layer.setTitle(getLayerName());
 
        MapContext context = new DefaultMapContext(crs == null ? DefaultGeographicCRS.WGS84 : crs);
        context.addLayer(layer);
        
        mapPane.setContext(context);
        mapPane.setMapArea(layer.getBounds());
        
        frame.setContentPane(mapPane);
        frame.setLocationRelativeTo(null);
        frame.setSize(640,480);
        frame.setVisible(true);
	}

	public void writeImage(File outfile, String format)
			throws ThinklabIOException {
		// TODO Auto-generated method stub
		
	}
	
	public CoordinateReferenceSystem getCoordinateReferenceSystem() {
		return crs;
	}

	public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatures() {
		return features;
	}

	public void write(File f) throws ThinklabException {
		if ( ! (f.toString().endsWith(".shp"))) {
			throw new ThinklabUnimplementedFeatureException(
					"vector coverage: writing: only shapefile format is supported for now");
		}
		
		
//		DefaultTransaction transaction = new DefaultTransaction("Example1");
//		FeatureStore<SimpleFeatureType, SimpleFeature> store = 
//			(FeatureStore<SimpleFeatureType, SimpleFeature>) dataStore.getFeatureSource( typeName );
//		store.setTransaction( transaction );
//
//		SimpleFeatureType featureType = store.getFeatureType();
//
//		try {
//		    store.addFeatures( features );
//		    transaction.commit(); // actually writes out the features in one go
//		}
//		catch( Exception eek){
//		    transaction.rollback();
//		}
		
	}



}
