/**
 * CellExtent.java
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

import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.integratedmodelling.corescience.CoreScience;
import org.integratedmodelling.corescience.CoreScience.PhysicalNature;
import org.integratedmodelling.corescience.interfaces.IExtent;
import org.integratedmodelling.corescience.interfaces.internal.IDatasourceTransformation;
import org.integratedmodelling.corescience.interfaces.lineage.ILineageTraceable;
import org.integratedmodelling.corescience.units.Unit;
import org.integratedmodelling.geospace.Geospace;
import org.integratedmodelling.geospace.coverage.RasterActivationLayer;
import org.integratedmodelling.geospace.gis.ThinklabRasterizer;
import org.integratedmodelling.geospace.implementations.observations.RasterGrid;
import org.integratedmodelling.geospace.interfaces.IGridMask;
import org.integratedmodelling.geospace.literals.ShapeValue;
import org.integratedmodelling.geospace.transformations.Resample;
import org.integratedmodelling.thinklab.constraint.Restriction;
import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.exception.ThinklabRuntimeException;
import org.integratedmodelling.thinklab.exception.ThinklabUnimplementedFeatureException;
import org.integratedmodelling.thinklab.exception.ThinklabValidationException;
import org.integratedmodelling.thinklab.interfaces.knowledge.IConcept;
import org.integratedmodelling.thinklab.interfaces.literals.IOperator;
import org.integratedmodelling.thinklab.interfaces.literals.IValue;
import org.integratedmodelling.utils.Pair;
import org.integratedmodelling.utils.Polylist;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Cell extents are the extents generated by the Raster conceptual models. Vector
 * CMs should be prepared to deal with them too.
 * 
 * A GridExtent may have been defined from a ShapeExtent by rasterizing objects. For this
 * reason, we implement ILineageTraceable so that the original objects can be reconstructed.
 * This must be transferred to the conceptualized grid in some (preferably elegant) way (TODO).
 * 
 * @author Ferdinando
 *
 */
public class GridExtent extends ArealExtent implements ILineageTraceable {

	GeometryFactory gFactory = null;
	int xDivs = 0;
	int yDivs = 0;
	double cellLength = 0.0;
	double cellHeight = 0.0;
	double xOrigin = 0.0;
	double yOrigin = 0.0;
	IGridMask activationLayer = null;
	
	ShapeExtent ancestor = null;
	
	private double cellHeightMeters;
	private double cellWidthMeters;
	private double cellAreaMeters = -1.0;
	Geometry boundary = null;
	public ShapeValue shape;
	
	public GridExtent(
				CoordinateReferenceSystem crs, 
				double x1, // lonLowerBound
				double y1, // latLowerBound
				double x2, // lonUpperBound
				double y2, // latUpperBound
				int xDivs, 
				int yDivs) {
		
		super(crs, x1, y1, x2, y2);
		this.xOrigin = x1;
		this.yOrigin = y1;

		setResolution(xDivs, yDivs);
	}

	public GridExtent(GridExtent gridExtent) {
		super(gridExtent.getCRS(), gridExtent.getWest(), gridExtent.getSouth(), gridExtent.getEast(), gridExtent.getNorth());
		this.xOrigin = gridExtent.getEast();
		this.yOrigin = gridExtent.getSouth();
		this.ancestor = gridExtent.ancestor;
		this.setResolution(gridExtent.getXCells(), gridExtent.getYCells());
	}

	/**
	 * Create a grid from a shape in the CRS of the shape and using the given
	 * resolution for the larger extent.
	 * 
	 * @param shape
	 * @param linearResolution
	 * @throws ThinklabException
	 */
	public GridExtent(ShapeValue shape, int linearResolution) throws ThinklabException {
		
		super(shape);
		Pair<Integer, Integer> xy = RasterGrid.getRasterBoxDimensions(shape, linearResolution);
		this.setResolution(xy.getFirst(), xy.getSecond());
		this.shape = shape;
		activationLayer = ThinklabRasterizer.createMask(shape, this);
	}
	
	/**
	 * Create a grid extent with a rasterized shape in the given grid. Will not
	 * clip the shape or anything: use ONLY when you already know the precise aspect factor for the extent 
	 * resulting from the shape.
	 * 
	 * @param shape
	 * @param x
	 * @param y
	 * @throws ThinklabException
	 */
	public GridExtent(ShapeValue shape, int x, int y) throws ThinklabException {
		super(shape);
		this.setResolution(x, y);
		this.ancestor = new ShapeExtent(shape);
		this.ancestor.shape = shape;
		activationLayer = ThinklabRasterizer.createMask(shape, this);
	}
	
	/**
	 * Used to set the activation layer from a shape. This also create a ShapeExtent and
	 * sets the lineage to it, so that our "object" can be reconstructed.
	 * 
	 * @param extent
	 * @param shape
	 * @throws ThinklabException 
	 */
	public GridExtent(GridExtent extent, ShapeValue shape) throws ThinklabException {
		this(extent);
		this.ancestor = new ShapeExtent(extent.envelope, crs);
		this.ancestor.shape = shape;
		activationLayer = ThinklabRasterizer.createMask(shape, this);
	}
	
	public void setResolution(int xDivs, int yDivs) {

		this.xDivs = xDivs;
		this.yDivs = yDivs;
		cellLength = getNormalizedEnvelope().getWidth() / xDivs;
		cellHeight = getNormalizedEnvelope().getHeight() / yDivs;
	}

	public int[] getXYCoordinates(int index) {
		int xx = index % getXCells();
		int yy = getYCells() - (index / getXCells()) - 1;
		return new int[]{xx, yy};
	}

	public static int[] getXYCoordinates(int index, int width, int height) {
		int xx = index % width;
		int yy = height - (index / width) - 1;
		return new int[]{xx, yy};
	}
	
	public int getIndex(int x, int y) {
		return (y * getXCells()) + x;
	}
	
	/**
	 * Activate the cells that correspond to the passed shape. Note that this
	 * doesn't change the multiplicity.
	 * 
	 * @param roi
	 * @throws ThinklabException
	 */
	public void createActivationLayer(ShapeValue roi) throws ThinklabException {
		this.activationLayer = ThinklabRasterizer.createMask(roi, this);
		this.shape = roi;
	}
	
	@Override
	public boolean equals(Object obj) {

		if (obj instanceof GridExtent) {
			
			GridExtent ge = (GridExtent) obj;
			
			/*
			 * TODO compare activation layers if we ever get to implement them.
			 */
			
			return 
				xDivs == ge.xDivs &&
				yDivs == ge.yDivs &&
				envelope.equals(ge.envelope);
		} 
		
		return false;
	}
	
	private void checkMeters() {

		if (cellAreaMeters < -0.1) {
			
			try {
				MathTransform transf = CRS.findMathTransform(crs, Geospace.get().getMetersCRS());
			
				Coordinate p1 = JTS.transform(
					new Coordinate(getWest(), getSouth()), null, transf);
				Coordinate p2 = JTS.transform(
						new Coordinate(getEast(), getNorth()), null, transf);

				this.cellWidthMeters  = Math.abs((p2.x - p1.x) / getXCells());
				this.cellHeightMeters = Math.abs((p2.y - p1.y) / getYCells());
				this.cellAreaMeters   = this.cellWidthMeters * this.cellHeightMeters;

			} catch (Exception e) {
				throw new ThinklabRuntimeException(e);
			}
			
		}
	}

	public double getCellWidthMeters() {
		checkMeters();
		return this.cellWidthMeters;
	}
	
	public double getCellHeightMeters() {
		checkMeters();
		return this.cellHeightMeters;
	}

	public double getCellAreaMeters() {
		checkMeters();
		return this.cellAreaMeters ;
	}

	public double getTotalAreaSquareMeters() {
		
		double ret = getCellAreaMeters();
		if (activationLayer != null) {
			ret *= activationLayer.totalActiveCells();
		} else {
			ret *= (xDivs*yDivs);
		}
		
		return ret;
	}

	
	/**
	 * Return a box in standard AWT coordinates. Useful for some graphical ops and rasterization.
	 * 
	 * @return
	 */
	public java.awt.geom.Rectangle2D.Double getDefaultBox() {
		
		ReferencedEnvelope env = getDefaultEnvelope();
		return new java.awt.geom.Rectangle2D.Double(
				env.getMinX(),
				env.getMinY(), 
				env.getWidth(), 
				env.getHeight());
	}
	
	public ShapeValue getShape() {
		return (ShapeValue) (shape == null ? getFullExtentValue() : shape);
	}
	
	public java.awt.geom.Rectangle2D.Double getNormalizedBox() {
		
		ReferencedEnvelope env = getNormalizedEnvelope();
		return new java.awt.geom.Rectangle2D.Double(
				env.getMinX(),
				env.getMinY(), 
				env.getWidth(), 
				env.getHeight());
	}
	
	/**
	 * return the envelope of the cell at x,y, irrespective of the activation layer
	 */
	public ReferencedEnvelope getCellEnvelope(int x, int y) {
		
		double x1 = xOrigin + (cellLength * x);
		double y1 = yOrigin + (cellHeight * y);
		double x2 = x1 + cellLength;
		double y2 = y1 + cellHeight;
		return new ReferencedEnvelope(x1,x2,y1,y2, getCRS());
	}
	
	/* return the area of each cell, in whatever coordinates we have. This is a regular grid, so no parameters passed. */
	public double getCellArea() {
		
		Envelope env = getCellEnvelope(0,0);
		return env.getHeight() * env.getWidth();
	}
	
	@Override
	public String toString() {
		return "grid(" + xDivs + "," + yDivs + ": " + 
		envelope.getMinX() + " : " + envelope.getMaxX() + ", " + 
		envelope.getMinY() + " : " + envelope.getMaxY() +
		" " + getCRS().getName() + ")"; 
	}
	
	@Override
	public IValue getFullExtentValue() {
		return new ShapeValue(getBoundary(), crs);
	}

	@Override
	public Object getValue(int granule) {

		/* 
		 * determine coordinates of granule. This should not get called if the activation layer
		 * is null, so no check for now. 
		 */
		Pair<Integer, Integer> xy = requireActivationLayer(true).getCell(granule);
		
		/*
		 * TODO reimplement to use nextCell on activation layer. Must enforce sequential access
		 * to use effectively. 
		 */
		
		double x1 = xOrigin + (cellLength * xy.getFirst());
		double y1 = yOrigin + (cellHeight * xy.getSecond());
		double x2 = x1 + cellLength;
		double y2 = y1 + cellHeight;
		
		return new ShapeValue(x1, y1, x2, y2);
	}

	@Override
	public int getValueCount() {

		return xDivs * yDivs;

// TODO this logic is consistent with only computing states along the active cells, but this is a grid, not
// a tessellated polygon, so we should run states even on the no-data.
//		
//		return 
//			activationLayer == null ?
//				(xDivs * yDivs) :
//				activationLayer.totalActiveCells();
	}

	public Geometry getBoundary() {
		
		ReferencedEnvelope env = getNormalizedEnvelope(); 
		return ShapeValue.makeCell(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY());
	}

	
	
	public int getXCells() {
		return xDivs;
	}

	public int getYCells() {
		return yDivs;
	}

	public int getXMaxCell() {
		return xDivs;
	}

	public int getXMinCell() {
		return 0;
	}

	public int getYMinCell() {
		return 0;
	}

	public int getYMaxCell() {
		return yDivs;
	}

	public GridEnvelope getGridRange() {
		
		return new GeneralGridEnvelope( new int[] {0,0}, new int[] {xDivs, yDivs}, false);
	}

	public IGridMask getActivationLayer() throws ThinklabValidationException {
		
		if (activationLayer == null) {
			throw new ThinklabValidationException("no activation layer in grid extent");
		}
		
		return activationLayer;
	}
	
	public IGridMask requireActivationLayer(boolean active) {
		
		if (activationLayer == null) {
			activationLayer = new RasterActivationLayer(xDivs, yDivs, active, this);
		}
		
		return activationLayer;
	}

	public double getNSResolution() {
		return cellHeight;
	}

	public double getEWResolution() {
		return cellLength;
	}

	@Override
	public Polylist conceptualize() throws ThinklabException {
		
		Polylist ret = Polylist.list(
				Geospace.RASTER_GRID,
				// communicate the extent so we don't lose lineage info in contextualized obs
				Polylist.list(":extent", this),
				Polylist.list(":shape", this.shape),
				Polylist.list(":mask", this.activationLayer),
				Polylist.list(Geospace.X_RANGE_MAX,     Integer.toString(getXMaxCell())),
				Polylist.list(Geospace.Y_RANGE_MAX,     Integer.toString(getYMaxCell())),
				Polylist.list(Geospace.LAT_LOWER_BOUND, getSouth()),
				Polylist.list(Geospace.LON_LOWER_BOUND, getWest()),
				Polylist.list(Geospace.LAT_UPPER_BOUND, getNorth()),
				Polylist.list(Geospace.LON_UPPER_BOUND, getEast()),
				Polylist.list(Geospace.CRS_CODE, 
						Geospace.getCRSIdentifier(getCRS(), false)));
		
		return ret;
	}

	
	/*
	 * TODO check: this should be creating the largest extent that preserves
	 * the topology of the original extent and covers the other extent. Cell size should
	 * be the same as in the original extent, and the envelope should be the same or 
	 * smaller.
	 * 
	 * @see org.integratedmodelling.geospace.extents.ArealExtent#createMergedExtent(org.integratedmodelling.geospace.extents.ArealExtent, org.integratedmodelling.geospace.extents.ArealExtent, org.opengis.referencing.crs.CoordinateReferenceSystem, com.vividsolutions.jts.geom.Envelope, com.vividsolutions.jts.geom.Envelope, com.vividsolutions.jts.geom.Envelope)
	 */
	@Override
	protected IExtent createMergedExtent(
					ArealExtent orextent, ArealExtent otextent,
					CoordinateReferenceSystem ccr, Envelope common,
					Envelope orenvnorm, Envelope otenvnorm) throws ThinklabException {

		/*
		 * for now, raster always wins
		 */
		if (otextent instanceof GridExtent && !(orextent instanceof GridExtent)) {
			return makeRasterExtent((GridExtent)otextent, orextent, ccr, common);
		}

		if (orextent instanceof GridExtent && !(otextent instanceof GridExtent)) {
			return makeRasterExtent((GridExtent)orextent, otextent, ccr, common);
		}

		/*
		 * if we get here, we must be merging two rasters
		 */
		if ( !(orextent instanceof GridExtent && otextent instanceof GridExtent)) {
			throw new ThinklabUnimplementedFeatureException("RasterModel: cannot yet merge extents of different types");
		}

		GridExtent orext = (GridExtent)orextent;
		GridExtent otext = (GridExtent)otextent;
		
		/* we'll fix the resolution later  */
		GridExtent nwext = 
				new GridExtent(ccr, 
						common.getMinX(), common.getMinY(), common.getMaxX(), common.getMaxY(), 
						1, 1);

		// this is the constrained resolution; we recompute it below if we are free to choose
		int xc = otext.getXCells();
		int yc = otext.getYCells();
		
		// phase errors in both directions due to choosing resolutions that do not
		// match exactly. This is computed but not used right now. We could set what to 
		// do with it as a property: e.g., log a warning or even raise an exception if nonzero.
		double errx = 0.0;
		double erry = 0.0;
		
		/* choose the smallest of the reprojected cells unless we're constrained to accept otextent */
		Envelope cor = null;
		Envelope cot = null; 
		
		try {
			
			cor = orext.getCellEnvelope(0, 0).transform(ccr, true, 10);
			cot = otext.getCellEnvelope(0, 0).transform(ccr, true, 10);
				
		} catch (Exception e) {
			throw new ThinklabValidationException(e);
		}
		
		double aor = cor.getHeight() * cor.getWidth();
		double aot = cot.getHeight() * cot.getWidth();
			
		/*
		 * We take the finest res
		 */
		Envelope cell = aor < aot ? cor : cot;
	
		// System.out.println("cells are " + cor + " and " + cot + "; chosen " + cell + " because areas are " + aor + " and " + aot);
		
		/* recompute the number of cells in the new extent */
		xc = (int)Math.round(nwext.getNormalizedEnvelope().getWidth()/cell.getWidth());
		yc = (int)Math.round(nwext.getNormalizedEnvelope().getHeight()/cell.getHeight());
		
		errx = nwext.getNormalizedEnvelope().getWidth() - (cell.getWidth() * xc);
		erry = nwext.getNormalizedEnvelope().getHeight() - (cell.getHeight() * yc);
		
		// System.out.println("new cell size is " + xc + "," + yc);
		
		// TODO use the error, make sure it's not larger than too much
		// System.out.println("errors are " + errx + "," + erry);
		
		nwext.setResolution(xc, yc);
		
		System.out.println("extent is now " + nwext);
		
		
		return nwext;
	}

	@Override
	public IDatasourceTransformation getDatasourceTransformation(
			IConcept mainObservable, IExtent extent) throws ThinklabValidationException {

		if (extent instanceof GridExtent) {
			return new Resample((GridExtent) extent);
		} 
		
		throw new ThinklabValidationException(
				"don't know how to transform a gridded observation of " + 
				mainObservable + 
				" to match extent: " + extent);
	}

	@Override
	public IExtent getExtent(int granule) {
		// TODO Auto-generated method stub
		return null;
	}

	private ArealExtent makeRasterExtent(GridExtent grid, ArealExtent otextent, CoordinateReferenceSystem ccr,
			Envelope env) throws ThinklabException {

		/*
		 *  TODO adjust the extent as necessary to make the grid extent reflect the coordinate system and area
		 */
		GridExtent ret = 
			new GridExtent(ccr, 
					env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY(), 
					grid.getXCells(), grid.getYCells());
		
		return ret;
	}

	@Override
	protected IExtent createConstrainedExtent(ArealExtent orextent,
			ArealExtent otextent, CoordinateReferenceSystem ccr, Envelope common,
			Envelope orenvnorm, Envelope otenvnorm)
			throws ThinklabException {

		/*
		 * for now, raster always wins
		 */
		if (otextent instanceof GridExtent && !(orextent instanceof GridExtent)) {
			return makeRasterExtent((GridExtent)otextent, orextent, ccr, common);
		}

		if (orextent instanceof GridExtent && !(otextent instanceof GridExtent)) {
			return makeRasterExtent((GridExtent)orextent, otextent, ccr, common);
		}

		/*
		 * if we get here, we must be merging two rasters
		 */
		if ( !(orextent instanceof GridExtent && otextent instanceof GridExtent)) {
			throw new ThinklabUnimplementedFeatureException("RasterModel: cannot yet merge extents of different types");
		}

		GridExtent otext = (GridExtent)otextent;
		
		double xRatio = common.getWidth()/otenvnorm.getWidth();
		double yRatio = common.getHeight()/otenvnorm.getHeight();

		/* recompute the number of cells in the new extent */
		int xc = (int)Math.round(otext.getXCells()*xRatio);
		int yc = (int)Math.round(otext.getYCells()*yRatio);
		
		/* we'll fix the resolution later  */
		GridExtent nwext = 
				new GridExtent(ccr, 
						common.getMinX(), common.getMinY(), common.getMaxX(), common.getMaxY(), 
						xc, yc);
				
		System.out.println("constrained extent is now " + nwext);
		
		return nwext;

	}

	@Override
	public String getSignature() {
		// TODO Auto-generated method stub
		try {
			return "grid," + getWest() + "," + getSouth() + "," + getEast() + "," + getNorth() + "," +
				   getXCells() + "," + getYCells() + Geospace.getCRSIdentifier(getCRS(), false);
		} catch (ThinklabException e) {
			throw new ThinklabRuntimeException(e);
		}
	}

	@Override
	public Collection<Object> getAncestors() {

		ArrayList<Object> ret = new ArrayList<Object>();
		if (ancestor != null) {
			ret.add(ancestor);
		}
		return ret;
	}
	
	public void setAncestor(ShapeExtent extent) {
		this.ancestor = extent;
	}

	@Override
	public IExtent or(IExtent myExtent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IExtent getAggregatedExtent() {
		return new ShapeExtent((ShapeValue) getFullExtentValue());
	}

	@Override
	public Collection<Pair<String, Integer>> getStateLocators(int index) {
		
		Pair<Integer, Integer> xy = requireActivationLayer(true).getCell(index);

		int brow = 0, bcol = 0, trow = getYCells() -1, tcol = getXCells() - 1;
		int col = xy.getFirst(), row = xy.getSecond();
		int n = 0;
		
		ArrayList<Pair<String,Integer>> ret = new ArrayList<Pair<String,Integer>>();
		
		// n
		if (row < trow) {
			n = getIndex(col, row + 1);
			ret.add(new Pair<String, Integer>("n", n));
		}
		
		// s
		if (row > brow) {
			n = getIndex(col, row - 1);
			ret.add(new Pair<String, Integer>("s", n));
		}
		
		// e
		if (col > bcol) {
			n = getIndex(col - 1, row);
			ret.add(new Pair<String, Integer>("e", n));
		}
		
		// w
		if (col < tcol) {
			n = getIndex(col + 1, row);
			ret.add(new Pair<String, Integer>("w", n));
		}
		
		// nw
		if (row < trow && col < tcol) {
			n = getIndex(col + 1, row + 1);
			ret.add(new Pair<String, Integer>("nw", n));
		}

		// ne
		if (row < trow && col > bcol) {
			n = getIndex(col - 1, row + 1);
			ret.add(new Pair<String, Integer>("ne", n));
		}
		
		// se
		if (row > brow && col > bcol) {
			n = getIndex(col - 1, row - 1);
			ret.add(new Pair<String, Integer>("se", n));
		}
		
		// sw
		if (row > brow && col < tcol) {
			n = getIndex(col + 1, row - 1);
			ret.add(new Pair<String, Integer>("sw", n));
		}
		
		return ret;
	}

	public void setActivationLayer(IGridMask mask) {
		this.activationLayer = mask;
	}

	@Override
	public boolean checkDomainDiscontinuity() throws ThinklabException {
		return false;
	}

	
	@Override
	public IExtent intersection(IExtent extent) throws ThinklabException {
		
		/*
		 * compute intersection of envelopes in our CSR. Ignore axis swap finally.
		 */
		ReferencedEnvelope ourenv = this.getNormalizedEnvelope();
		ReferencedEnvelope itsenv = null;
		try {
			itsenv = ((ArealExtent)extent).getNormalizedEnvelope().transform(crs, true, 10);
		} catch (Exception e) {
			throw new ThinklabValidationException(e);
		}
		
		Envelope comenv = ourenv.intersection(itsenv);
		if (comenv == null)
			return null;
		
		/*
		 * adjust intersection to be in phase with our cell size, ensuring it is fully contained.
		 */
		double startx = comenv.getMinX();
		if (startx > ourenv.getMinX()) {
			double delta = startx - ourenv.getMinX();
			double nc = Math.floor(delta / cellLength);
			startx = ourenv.getMinX() + (cellLength * nc);
		}
		double endx = comenv.getMaxX();
		if (endx < ourenv.getMaxX()) {
			double delta = ourenv.getMaxX() - endx;
			double nc = Math.floor(delta / cellLength);
			endx = ourenv.getMaxX() - (cellLength * nc);
		}
		double starty = comenv.getMinY();
		if (starty > ourenv.getMinY()) {
			double delta = starty - ourenv.getMinY();
			double nc = Math.floor(delta / cellHeight);
			starty = ourenv.getMinY() + (cellHeight * nc);
		}
		double endy = comenv.getMaxY();
		if (endy < ourenv.getMaxY()) {
			double delta = ourenv.getMaxY() - endy;
			double nc = Math.floor(delta / cellHeight);
			endx = ourenv.getMaxY() - (cellHeight * nc);
		}
		
		/*
		 * compute new grid extent resolution for intersection.
		 */
		int xc = (int) ((endx - startx)/cellLength);
		int yc = (int) ((endy - starty)/cellHeight);
		
		/*
		 * create and return new extent.
		 */
		return new GridExtent(crs, startx, starty, endx, endy, xc, yc);
			
	}

	@Override
	public IExtent force(IExtent extent) throws ThinklabException {
		return extent;
	}

	@Override
	public IExtent union(IExtent extent) throws ThinklabException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IConcept getValueType() {
		// TODO for now. We should give it a geometry/shape concept.
		return null;
	}

	@Override
	public Restriction getConstraint(String operator) throws ThinklabException {
		return new Restriction("boundingbox", operator, getFullExtentValue().toString());
	}

	
	public Collection<Integer> getNeumannNeighbors(int xcell, int ycell) {
		ArrayList<Integer> ret = new ArrayList<Integer>();
		
		if (inRange(xcell-1, ycell))
			ret.add(getIndex(xcell-1, ycell));
		if (inRange(xcell+1, ycell))
			ret.add(getIndex(xcell+1, ycell));
		if (inRange(xcell, ycell-1))
			ret.add(getIndex(xcell, ycell-1));
		if (inRange(xcell, ycell+1))
			ret.add(getIndex(xcell, ycell+1));

		return ret;
	}
	
	public Collection<Integer> getMooreNeighbors(int xcell, int ycell) {
		
		ArrayList<Integer> ret = (ArrayList<Integer>) getNeumannNeighbors(xcell, ycell);
		
		if (inRange(xcell-1, ycell-1))
			ret.add(getIndex(xcell-1, ycell-1));
		if (inRange(xcell+1, ycell))
			ret.add(getIndex(xcell+1, ycell+1));
		if (inRange(xcell, ycell-1))
			ret.add(getIndex(xcell+1, ycell-1));
		if (inRange(xcell, ycell+1))
			ret.add(getIndex(xcell-1, ycell+1));

		return ret;
	}
	
	/**
	 * Get all grid cell indexes that are within n meters of the given point.
	 * @param i
	 * @param j
	 * @return
	 */
	public Collection<Integer> getCoordinatesWithinM(int xcell, int ycell, double meters) {
		
		ArrayList<Integer> ret = new ArrayList<Integer>();
		
		int xspan = (int)(meters/getCellWidthMeters());
		int yspan = (int)(meters/getCellHeightMeters());
		
		for (int x = 0; x < xspan; x++) {
			for (int y = 0; y < yspan; y++) {
				
				if (inRange(xcell-x, ycell-y))
					ret.add(getIndex(xcell-x, ycell-y));
				if (inRange(xcell+x, ycell-y))
					ret.add(getIndex(xcell+x, ycell-y));
				if (inRange(xcell-x, ycell+y))
					ret.add(getIndex(xcell-x, ycell+y));
				if (inRange(xcell+x, ycell+y))
					ret.add(getIndex(xcell+x, ycell+y));				
			}
		}
		
		return ret;
	}

	public boolean inRange(int x, int y) {

		return 
			x >= 0 && 
			x < getXCells() &&
			y >= 0 &&
			y < getYCells();
	}

	@Override
	public AggregationParameters getAggregationParameters(IConcept observable,
			Unit unit) throws ThinklabException {
		
		AggregationParameters ret = new AggregationParameters(observable, unit);
		ret.aggregationOperator = IOperator.AVG;
		ret.aggregatedUnit = unit;
		ret.aggregatedNature = PhysicalNature.INTENSIVE;
		
		if (CoreScience.isExtensive(observable)) {
			
			ret.aggregatedNature = PhysicalNature.EXTENSIVE;
			ret.aggregationOperator = IOperator.SUM;

			if (unit.isArealDensity()) {
				
				/*
				 * determine cell area and conversion factor to 
				 * turn density into quantity
				 */
				Unit sd = unit.getArealExtentUnit();
				Unit rf = new Unit("m^2");
				double um2 = rf.convert(1.0, sd);
				final double cnv = getCellAreaMeters()/um2;
				
				ret.aggregator = new Aggregator() {
					@Override
					public double getAggregationFactor(int granule) {
						return cnv;
					}
				};
				
				/*
				 * eliminate the temporal term from the aggregated unit
				 */
				ret.aggregatedUnit = 
					new Unit(unit.getUnit().times(sd.getUnit()));
			}				
		}

		return ret;

	}
	
	@Override
	public boolean isCovered(int granule) {

		if (activationLayer == null)
			return true;
		
		return activationLayer.isActive(granule);
	}

}
