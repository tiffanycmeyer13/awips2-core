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
package com.raytheon.uf.viz.core.drawables.ext.colormap;

import java.awt.image.RenderedImage;

import javax.measure.Unit;

import com.raytheon.uf.common.colormap.image.ColorMapData;
import com.raytheon.uf.common.colormap.image.Colormapper;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.colormap.prefs.IColorMapParametersListener;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.data.IColorMapDataRetrievalCallback;
import com.raytheon.uf.viz.core.data.IRenderedImageCallback;
import com.raytheon.uf.viz.core.drawables.IColormappedImage;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.ext.IImagingExtension;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * General colormapped image, regenerates image if parameters change
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ---------------------------------------
 * Dec 16, 2011           mschenke  Initial creation
 * Feb 27, 2013  1532     bsteffen  Delete uf.common.colormap.image
 * Nov 11, 2013  2492     mschenke  Added getDataUnti to IColormappedImage
 * Apr 15, 2014  3016     randerso  Fix null pointer during construction
 * Aug 11, 2021  8635     randerso  Fix listener leak
 *
 * </pre>
 *
 * @author mschenke
 */

public class ColormappedImage implements IColormappedImage,
        IRenderedImageCallback, IColorMapParametersListener {

    private IImage image;

    private IColorMapDataRetrievalCallback callback;

    private ColorMapParameters parameters;

    private Unit<?> dataUnit;

    public ColormappedImage(IGraphicsTarget target,
            IColorMapDataRetrievalCallback callback,
            ColorMapParameters parameters) {
        this.callback = callback;
        setColorMapParameters(parameters);
        image = target.initializeRaster(this);
    }

    /**
     * Get the wrapped image for the colormapped image
     *
     * @return
     */
    public IImage getWrappedImage() {
        return image;
    }

    @Override
    public Status getStatus() {
        return image.getStatus();
    }

    @Override
    public void setInterpolated(boolean isInterpolated) {
        image.setInterpolated(isInterpolated);
    }

    private void disposeImage() {
        if (image != null) {
            image.dispose();
        }
    }

    @Override
    public void dispose() {
        disposeImage();

        if (parameters != null) {
            parameters.removeListener(this);
        }
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public void setBrightness(float brightness) {
        image.setBrightness(brightness);
    }

    @Override
    public void setContrast(float contrast) {
        image.setContrast(contrast);
    }

    @Override
    public Class<? extends IImagingExtension> getExtensionClass() {
        return GeneralColormappedImageExtension.class;
    }

    @Override
    public ColorMapParameters getColorMapParameters() {
        return parameters;
    }

    @Override
    public void setColorMapParameters(ColorMapParameters params) {
        if (params != this.parameters) {
            if (this.parameters != null) {
                this.parameters.removeListener(this);
            }

            this.parameters = params;
            if (this.parameters != null) {
                this.parameters.addListener(this);
            }

            disposeImage();
        }
    }

    @Override
    public double getValue(int x, int y) {
        return Double.NaN;
    }

    @Override
    public RenderedImage getImage() throws VizException {
        RenderedImage image = null;
        if (parameters != null && parameters.getColorMap() != null) {
            ColorMapData colorMapData = callback.getColorMapData();
            if (colorMapData != null) {
                this.dataUnit = colorMapData.getDataUnit();
                image = Colormapper.colorMap(colorMapData, parameters);
            }
        }
        return image;
    }

    @Override
    public void colorMapChanged() {
        disposeImage();
    }

    @Override
    public void stage() throws VizException {
        image.stage();
    }

    @Override
    public Unit<?> getDataUnit() {
        return dataUnit == null ? getColorMapParameters().getDataUnit()
                : dataUnit;
    }

}
