/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.viz.core.grid.display;

import java.nio.FloatBuffer;

import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;

import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.geospatial.data.GeographicDataSource;
import com.raytheon.uf.common.numeric.source.DataSource;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.point.display.VectorGraphicsConfig;
import com.raytheon.uf.viz.core.point.display.VectorGraphicsRenderable;
import com.raytheon.uf.viz.core.rsc.DisplayType;

/**
 *
 * Performs same functions as the original GriddedVectorDisplay using wireframe
 * shapes instead of svg for much faster performance. This is still slightly
 * experimental but seems to work well. It should also have the drawing code
 * extracted to a class similar to PointWindDisplay so wireframe shape barbs and
 * arrows can be used elsewhere.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * Jun 22, 2010           bsteffen     Initial creation
 * Feb 07, 2011  7948     bkowal       added a public method to get the
 *                                     direction.
 * Aug 27, 2013  2287     randerso     Added VectorGraphicsRenderable Factory to
 *                                     allow application specific rendering of
 *                                     wind barbs and arrows. Added
 *                                     densityFactor to allow application
 *                                     specific adjustment of density. Added
 *                                     gridRelative flag to indicate whether
 *                                     direction data is relative to grid or
 *                                     true north
 * Sep 09, 2013  16257    MPorricelli  When setDestinationGeographicPoint fails
 *                                     (which can happen for global lat/lon grid
 *                                     winds displayed on Equidistant
 *                                     Cylindrical map) try again with different
 *                                     pixel location.
 * Sep 23, 2013  2363     bsteffen     Add more vector configuration options.
 * Jan 14, 2014  2661     bsteffen     Switch magnitude and direction from
 *                                     buffers to DataSource
 * May 14, 2015  4079     bsteffen     Move to core.grid
 * Oct 16, 2015  4849     bsteffen     Fix antimeridian direction.
 * Mar 26, 2018  6826     njensen      Use magnification 0.2 so tiny barbs
 *                                     appear for 0 magnification
 * Nov 06, 2020  8275     randerso     Added correction for latitude being
 *                                     slightly > 90 degrees due to round-off.
 *
 * </pre>
 *
 * @author bsteffen
 */
public class GriddedVectorDisplay extends AbstractGriddedDisplay<Coordinate> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GriddedVectorDisplay.class);

    private static final double MAX_ABS_LAT = 90.0;

    private static final double LAT_TOLERANCE = Math.ulp(MAX_ABS_LAT) * 2;

    private final DataSource magnitude;

    private final DataSource direction;

    private int lineWidth;

    private LineStyle lineStyle;

    private IExtent lastExtent;

    private VectorGraphicsConfig vectorConfig;

    private VectorGraphicsRenderable vectorRenderable;

    private boolean gridRelative;

    private DisplayType displayType;

    private GeodeticCalculator gc;

    /** used to avoid spamming the logs. */
    private boolean logProjectionProblems = true;

    /**
     *
     * @param magnitude
     *            a data source for the magnitude of vectors
     * @param direction
     *            a data source for the direction of vectors
     * @param descriptor
     *            the descriptor
     * @param gridGeometryOfGrid
     *            geometry of the data sources
     * @param densityFactor
     *            adjustment factor to make density match A1
     * @param gridRelative
     *            true if direction is grid relative, false if relative to true
     *            north
     * @param displayType
     *            how to render the vector
     * @param config
     *            custom rendering hints
     */
    public GriddedVectorDisplay(DataSource magnitude, DataSource direction,
            IMapDescriptor descriptor, GeneralGridGeometry gridGeometryOfGrid,
            double densityFactor, boolean gridRelative, DisplayType displayType,
            VectorGraphicsConfig config) {
        super(descriptor, gridGeometryOfGrid, config.getBaseSize(),
                densityFactor);
        this.magnitude = magnitude;
        this.direction = direction;
        this.gridRelative = gridRelative;
        this.displayType = displayType;
        this.gc = new GeodeticCalculator(descriptor.getCRS());
        this.vectorConfig = config;
    }

    /**
     * Construct using float buffers instead of data sources.
     *
     * @See {@link #GriddedVectorDisplay(DataSource, DataSource, IMapDescriptor, GeneralGridGeometry, double, boolean, DisplayType, VectorGraphicsConfig)}
     */
    public GriddedVectorDisplay(FloatBuffer magnitude, FloatBuffer direction,
            IMapDescriptor descriptor, GeneralGridGeometry gridGeometryOfGrid,
            double densityFactor, boolean gridRelative, DisplayType displayType,
            VectorGraphicsConfig config) {
        this(new GeographicDataSource(magnitude, gridGeometryOfGrid),
                new GeographicDataSource(direction, gridGeometryOfGrid),
                descriptor, gridGeometryOfGrid, densityFactor, gridRelative,
                displayType, config);
    }

    @Override
    public void paint(IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {
        if (lastExtent == null
                || !lastExtent.equals(paintProps.getView().getExtent())) {
            disposeResources();
            lastExtent = paintProps.getView().getExtent().clone();
        }
        if (vectorRenderable == null) {
            vectorRenderable = new VectorGraphicsRenderable(descriptor, target,
                    vectorConfig);
            super.paint(target, paintProps);
        }
        vectorRenderable.setColor(this.color);
        vectorRenderable.setLineWidth(lineWidth);
        vectorRenderable.setLineStyle(lineStyle);
        vectorRenderable.paint(target);
    }

    @Override
    protected void issueRefresh() {
        lastExtent = null;
        super.issueRefresh();
    }

    @Override
    protected void paint(Coordinate ijcoord, PaintProperties paintProps,
            Coordinate plotLoc, double adjSize) throws VizException {

        double spd = this.magnitude.getDataValue((int) ijcoord.x,
                (int) ijcoord.y);
        double dir = this.direction.getDataValue((int) ijcoord.x,
                (int) ijcoord.y);

        if (dir < -999_999 || dir > 9_999_999) {
            // perhaps this check should limit +/- 180
            return;
        }

        if (Double.isNaN(spd) || Double.isNaN(dir)) {
            return;
        }
        try {
            ReferencedCoordinate rCoord = new ReferencedCoordinate(
                    gridGeometryOfGrid, ijcoord);
            Coordinate latLon = rCoord.asLatLon();
            latLon.x = MapUtil.correctLon(latLon.x);

            /* fix lat for round-off making it slightly > 90 */
            double delta = Math.abs(latLon.y) - MAX_ABS_LAT;

            if (delta > 0 && delta < LAT_TOLERANCE) {
                latLon.y = MAX_ABS_LAT * Math.signum(latLon.y);
            }

            double[] stationLocation = { latLon.x, latLon.y };
            double[] stationPixelLocation = this.descriptor
                    .worldToPixel(stationLocation);

            if (stationPixelLocation != null) {
                stationPixelLocation[1]--;
                try {
                    double[] newWorldLocation = this.descriptor
                            .pixelToWorld(stationPixelLocation);
                    this.gc.setStartingGeographicPoint(stationLocation[0],
                            stationLocation[1]);
                    /*
                     * Near the antimeridian rounding errors in the reproject
                     * can lead to values that are just slightly out of range,
                     * the GeodeticCalculator is very strict about values being
                     * in the range ±180°.
                     */
                    newWorldLocation[0] = MapUtil
                            .correctLon(newWorldLocation[0]);
                    this.gc.setDestinationGeographicPoint(newWorldLocation[0],
                            newWorldLocation[1]);
                } catch (Exception e) {
                    /*
                     * Exceptions are logged rather than thrown because if the
                     * error is specific to a particular location it will still
                     * render other locations. Only log once to avoid spamming.
                     */
                    if (logProjectionProblems) {
                        statusHandler.error(
                                "Unable to calculate reprojected vector direction, some vectors will not be displayed.",
                                e);
                        logProjectionProblems = false;
                    }
                    return;
                }
            }

            if (gridRelative) {
                // rotate data from grid up to true north
                dir += (float) MapUtil.rotation(latLon,
                        GridGeometry2D.wrap(gridGeometryOfGrid));
            }

            // rotate dir from true north to display up
            dir -= this.gc.getAzimuth();

        } catch (Exception e) {
            throw new VizException(e);
        }

        dir = (float) Math.toRadians(dir);
        vectorConfig.setSizeScaler(adjSize / size);
        switch (displayType) {
        case ARROW:
            vectorRenderable.paintArrow(plotLoc, spd, dir);
            break;
        case BARB:
            vectorRenderable.paintBarb(plotLoc, spd, dir);
            break;
        case DUALARROW:
            vectorRenderable.paintDualArrow(plotLoc, spd, dir);
            break;
        default:
            throw new VizException("Unsupported display type: " + displayType);
        }
    }

    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * @param lineStyle
     */
    public void setLineStyle(LineStyle lineStyle) {
        this.lineStyle = lineStyle;
    }

    /**
     * @param density
     *            the density to set
     */
    @Override
    public boolean setDensity(double density) {
        if (super.setDensity(density)) {
            disposeResources();
            if (this.target != null) {
                this.target.setNeedsRefresh(true);
            }
            return true;
        }
        return false;
    }

    /**
     * @param magnification
     *            the magnification to set
     */
    @Override
    public boolean setMagnification(double magnification) {
        if (magnification == 0.0) {
            magnification = 0.2;
        }
        if (super.setMagnification(magnification)) {
            disposeResources();
            if (this.target != null) {
                this.target.setNeedsRefresh(true);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void disposeResources() {
        if (vectorRenderable != null) {
            vectorRenderable.dispose();
            vectorRenderable = null;
        }
    }

    @Override
    protected Coordinate createResource(Coordinate coord) throws VizException {
        return coord;
    }

    @Override
    protected Coordinate getResource(Coordinate coord) {
        return coord;
    }

}
