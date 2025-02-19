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
package com.raytheon.uf.common.colormap.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.List;

import javax.measure.Unit;
import javax.measure.UnitConverter;

import com.raytheon.uf.common.colormap.IColorMap;
import com.raytheon.uf.common.colormap.LogConverter;
import com.raytheon.uf.common.colormap.image.ColorMapData.ColorMapDataType;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.units.UnitConv;

/**
 * Colormapper class, written to mimic colormapRaster.glsl in java. Any changes
 * to files mapping.glsl or colormapRaster.glsl will probably need to be
 * reflected here
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 13, 2010           mschenke  Initial creation
 * Feb 15, 2013  1638     mschenke  Moved IndexColorModel creation to
 *                                  common.colormap utility
 * Nov 04, 2013  2492     mschenke  Rewritten to model glsl equivalent
 * Apr 15, 2014  3016     randerso  Check in Max's fix for getColorByIndex
 * Apr 22, 2014  2996     dgilling  Rewrite colorMap() to output images with
 *                                  proper transparency for GFE data.
 * Apr 27, 2015  4425     nabowle   Handle ColorMapDataType.DOUBLE.
 * Nov 02, 2016  5957     bsteffen  Fix indexing to include last color more.
 * Feb 07, 2018  6816     randerso  Implemented getLinearValue and
 *                                  getLogFactorValue functions.
 * Apr 17, 2018  6972     bsteffen  Add getLogValue
 *
 * </pre>
 *
 * @author mschenke
 */
public class Colormapper {

    /*
     * These constants are public as they are used in the OGC and possibly other
     * plugins
     */
    public static final int COLOR_MODEL_NUMBER_BITS = 8;

    public static final float MAX_VALUE = 255.0f;

    private static final int TRANSPARENT = new Color(0, 0, 0, 0).getRGB();

    /**
     * This method will color map a Buffer to a RenderedImage given size and
     * parameters
     *
     * @param cmapData
     * @param parameters
     * @return the color mapped image
     */
    public static RenderedImage colorMap(ColorMapData cmapData,
            ColorMapParameters parameters) {
        int width = cmapData.getDimensions()[0];
        int height = cmapData.getDimensions()[1];
        Buffer buf = cmapData.getBuffer();
        int dataSize = buf.capacity();
        ColorMapDataType dataType = cmapData.getDataType();
        double noDataValue = parameters.getNoDataValue();
        Unit<?> dataUnit = cmapData.getDataUnit();
        if (dataUnit == null) {
            dataUnit = parameters.getDataUnit();
        }
        Unit<?> colorMapUnit = parameters.getColorMapUnit();
        UnitConverter converter = null;
        if (dataUnit != null && colorMapUnit != null
                && parameters.getDataMapping() == null
                && !dataUnit.equals(colorMapUnit)
                && dataUnit.isCompatible(colorMapUnit)) {
            converter = UnitConv.getConverterToUnchecked(dataUnit,
                    colorMapUnit);
        }

        int numColors = parameters.getColorMap().getSize();
        List<com.raytheon.uf.common.colormap.Color> colors = parameters
                .getColorMap().getColors();

        int[] indexedColors = new int[numColors];
        for (int i = 0; i < numColors; i++) {
            com.raytheon.uf.common.colormap.Color color = colors.get(i);
            indexedColors[i] = new Color(color.getRed(), color.getGreen(),
                    color.getBlue(), color.getAlpha()).getRGB();
        }

        int[] pixels = new int[dataSize];
        for (int i = 0; i < dataSize; ++i) {
            int rgbValue = TRANSPARENT;

            double dataValue = getDataValue(buf, i, dataType);
            if ((!Double.isNaN(dataValue)) && (dataValue != noDataValue)) {
                double cmapValue = dataValue;
                if (converter != null) {
                    cmapValue = converter.convert(dataValue);
                }

                double index = getColorMappingIndex(cmapValue, parameters);
                index = capIndex(index);
                int cmapIndex = (int) Math.min(index * numColors,
                        numColors - 1);
                rgbValue = indexedColors[cmapIndex];
            }

            pixels[i] = rgbValue;
        }

        BufferedImage bi = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        bi.setRGB(0, 0, width, height, pixels, 0, width);
        return bi;
    }

    /**
     * Builds a color model from a color map
     *
     * @param aColorMap
     * @return the color model
     */
    public static IndexColorModel buildColorModel(IColorMap aColorMap) {
        int size = aColorMap.getSize();
        byte[] red = new byte[size];
        byte[] green = new byte[size];
        byte[] blue = new byte[size];
        byte[] alpha = new byte[size];

        List<com.raytheon.uf.common.colormap.Color> colors = aColorMap
                .getColors();
        for (int i = 0; i < size; ++i) {
            com.raytheon.uf.common.colormap.Color color = colors.get(i);
            red[i] = (byte) (color.getRed() * MAX_VALUE);
            green[i] = (byte) (color.getGreen() * MAX_VALUE);
            blue[i] = (byte) (color.getBlue() * MAX_VALUE);
            alpha[i] = (byte) (color.getAlpha() * MAX_VALUE);
        }

        return new IndexColorModel(COLOR_MODEL_NUMBER_BITS, size, red, green,
                blue, alpha);
    }

    /**
     * Returns the double representation of the data value for the Buffer at the
     * given index
     *
     * @param buffer
     * @param idx
     * @param dataType
     * @return the double value
     */
    public static double getDataValue(Buffer buffer, int idx,
            ColorMapDataType dataType) {
        switch (dataType) {
        case BYTE: {
            return ((ByteBuffer) buffer).get(idx) & 0xFF;
        }
        case SIGNED_BYTE: {
            return ((ByteBuffer) buffer).get(idx);
        }
        case SHORT: {
            return ((ShortBuffer) buffer).get(idx);
        }
        case UNSIGNED_SHORT: {
            return ((ShortBuffer) buffer).get(idx) & 0xFFFF;
        }
        case INT: {
            return ((IntBuffer) buffer).get(idx);
        }
        case FLOAT: {
            return ((FloatBuffer) buffer).get(idx);
        }
        case DOUBLE: {
            return ((DoubleBuffer) buffer).get(idx);
        }
        }
        return 0.0;
    }

    /**
     * This function takes an index value and caps it to the range 0-1
     *
     * @param index
     * @return index capped to the range 0-1
     */
    public static double capIndex(double index) {
        if (index < 0.0) {
            index = 0.0;
        } else if (index > 1.0) {
            index = 1.0;
        }
        return index;
    }

    /**
     * Given a colorMap value linearly determine the index into cmapMin/cmapMax
     *
     * @param cmapValue
     * @param cmapMin
     * @param cmapMax
     * @return the index
     */
    public static double getLinearIndex(double cmapValue, double cmapMin,
            double cmapMax) {
        return (cmapValue - cmapMin) / (cmapMax - cmapMin);
    }

    /**
     * Given a linear color map index, compute the corresponding value.
     *
     * This is the inverse of getLinearIndex
     *
     * @param index
     * @param cmapMin
     * @param cmapMax
     * @return the value
     */
    public static double getLinearValue(double index, double cmapMin,
            double cmapMax) {
        return (index * (cmapMax - cmapMin)) + cmapMin;
    }

    /**
     * This function logarithmically finds the index for the cmapValue into
     * cmapMin/cmapMax.
     *
     * @param cmapValue
     * @param cmapMin
     * @param cmapMax
     * @param mirror
     * @return the index
     */
    public static double getLogIndex(double cmapValue, double cmapMin,
            double cmapMax, boolean mirror) {
        boolean inverted = false;
        double rangeMin = Math.abs(cmapMin);
        double rangeMax = Math.abs(cmapMax);
        double rangeValue = Math.abs(cmapValue);
        if (rangeMin > rangeMax) {
            // Inverted colormapping range (cmapMax is closest to 0)
            inverted = true;
            double tmp = rangeMin;
            rangeMin = rangeMax;
            rangeMax = tmp;
        }

        double index;
        // Flag if min/max values are on opposite sides of zero
        boolean minMaxOpposite = (cmapMin < 0 && cmapMax > 0)
                || (cmapMin > 0 && cmapMax < 0);

        if (mirror || minMaxOpposite) {
            if (cmapMax < 0) {
                // Invert colormapping if negative range was given
                cmapValue = -cmapValue;
            }
            // Log scaling is happening on both sides of zero, need to compute
            // our zero index value
            double zeroVal = rangeMin;
            if (minMaxOpposite) {
                // Min/Max are on opposite sides of zero, compute a zero value
                zeroVal = Math.max(rangeMin, rangeMax) * 0.0001;
            }

            double negCmapMax = rangeMin;
            double posCmapMax = rangeMax;
            if (mirror) {
                negCmapMax = posCmapMax = rangeMax;
            }

            // Compute log zero val and log neg/pos max vals
            double absLogZeroVal = Math.abs(Math.log(zeroVal));
            double logNegCmapMax = absLogZeroVal + Math.log(negCmapMax);
            double logPosCmapMax = absLogZeroVal + Math.log(posCmapMax);
            // Calculate index which zeroVal is at based on neg max and pos max
            double zeroValIndex = logNegCmapMax
                    / (logNegCmapMax + logPosCmapMax);
            if (cmapValue > 0) {
                index = LogConverter.valueToIndex(rangeValue, zeroVal,
                        posCmapMax);
                index = zeroValIndex + (1 - zeroValIndex) * index;
            } else {
                index = LogConverter.valueToIndex(rangeValue, zeroVal,
                        negCmapMax);
                index = zeroValIndex - zeroValIndex * index;
            }
            if (inverted) {
                index = 1.0 - index;
            }
        } else {
            // Simple case, just use log converter to get index
            index = LogConverter.valueToIndex(rangeValue, rangeMin, rangeMax);
            if (inverted) {
                index = 1.0 - index;
            }
            if (cmapMin > 0 && cmapValue < rangeMin
                    || (cmapMin < 0 && cmapValue > -rangeMin)) {
                index = -index;
            }
        }
        return index;
    }

    /**
     * This function finds the correct value for a given logarithmic index using
     * the cmapMin/cmapMax.
     *
     * @param index
     * @param cmapMin
     * @param cmapMax
     * @param mirror
     * @return the index
     */
    public static double getLogValue(double index, double cmapMin,
            double cmapMax, boolean mirror) {
        boolean inverted = false;
        double rangeMin = Math.abs(cmapMin);
        double rangeMax = Math.abs(cmapMax);
        if (rangeMin > rangeMax) {
            // Inverted colormapping range (cmapMax is closest to 0)
            inverted = true;
            double tmp = rangeMin;
            rangeMin = rangeMax;
            rangeMax = tmp;
        }

        double cmapValue;
        // Flag if min/max values are on opposite sides of zero
        boolean minMaxOpposite = (cmapMin < 0 && cmapMax > 0)
                || (cmapMin > 0 && cmapMax < 0);

        if (mirror || minMaxOpposite) {
            // Log scaling is happening on both sides of zero, need to compute
            // our zero index value
            double zeroVal = rangeMin;
            if (minMaxOpposite) {
                // Min/Max are on opposite sides of zero, compute a zero
                // valuezeroValIndex
                zeroVal = Math.max(rangeMin, rangeMax) * 0.0001;
            }

            double negCmapMax = rangeMin;
            double posCmapMax = rangeMax;
            if (mirror) {
                negCmapMax = posCmapMax = rangeMax;
            }

            // Compute log zero val and log neg/pos max vals
            double absLogZeroVal = Math.abs(Math.log(zeroVal));
            double logNegCmapMax = absLogZeroVal + Math.log(negCmapMax);
            double logPosCmapMax = absLogZeroVal + Math.log(posCmapMax);
            // Calculate index which zeroVal is at based on neg max and pos max
            double zeroValIndex = logNegCmapMax
                    / (logNegCmapMax + logPosCmapMax);

            if (inverted) {
                index = 1.0 - index;
            }
            if (index > zeroValIndex) {
                index = (index - zeroValIndex) / (1 - zeroValIndex);
                cmapValue = LogConverter.indexToValue(index, zeroVal,
                        posCmapMax);
            } else {
                index = (zeroValIndex - index) / zeroValIndex;

                cmapValue = -LogConverter.indexToValue(index, zeroVal,
                        negCmapMax);
            }
            if (cmapMax < 0) {
                // Invert colormapping if negative range was given
                cmapValue = -cmapValue;
            }
        } else {
            boolean belowZero = (index < 0);
            if (belowZero) {
                index = -index;
            }
            if (inverted) {
                index = 1.0 - index;
            }
            // Simple case, just use log converter to get index
            cmapValue = LogConverter.indexToValue(index, rangeMin, rangeMax);
            if (belowZero) {
                cmapValue = -cmapValue;
            }

        }
        return cmapValue;
    }

    /**
     * This function calculates a new index to use based on the logFactor and
     * passed in value
     *
     * @param cmapValue
     * @param cmapMin
     * @param cmapMax
     * @param logFactor
     * @return the index
     *
     */
    public static double getLogFactorIndex(double cmapValue, double cmapMin,
            double cmapMax, double logFactor) {
        double index = getLinearIndex(cmapValue, cmapMin, cmapMax);

        if (logFactor > 0.0) {
            double minLog = Math.log(logFactor);
            double maxLog = Math.log(logFactor + 1.0);

            double lg = Math.log(logFactor + index);

            index = capIndex((lg - minLog) / (maxLog - minLog));
        }
        return index;
    }

    /**
     * Given a log factor color map index, compute the corresponding value
     *
     * This is the inverse of getLogFactorIndex
     *
     * @param index
     * @param cmapMin
     * @param cmapMax
     * @param logFactor
     * @return the value
     */
    public static double getLogFactorValue(double index, double cmapMin,
            double cmapMax, double logFactor) {

        index = capIndex(index);
        if (logFactor > 0.0) {
            double minLog = Math.log(logFactor);
            double maxLog = Math.log(logFactor + 1.0);

            double lg = (index * (maxLog - minLog)) + minLog;

            index = Math.exp(lg) - logFactor;

        }
        return getLinearValue(index, cmapMin, cmapMax);
    }

    /**
     * Returns an index for the cmapValue based on the
     * {@link ColorMapParameters}
     *
     * @param cmapValue
     * @param colorMapParameters
     * @return color mapping index between 0.0 and 1.0
     */
    public static double getColorMappingIndex(double cmapValue,
            ColorMapParameters colorMapParameters) {
        double logFactor = colorMapParameters.getLogFactor();
        double cmapMin = colorMapParameters.getColorMapMin();
        double cmapMax = colorMapParameters.getColorMapMax();

        double index;
        if (colorMapParameters.isLogarithmic()) {
            index = getLogIndex(cmapValue, cmapMin, cmapMax,
                    colorMapParameters.isMirror());

        } else if (logFactor > 0.0) {
            // Apply logFactor if set
            index = getLogFactorIndex(cmapValue, cmapMin, cmapMax, logFactor);

        } else {
            index = getLinearIndex(cmapValue, cmapMin, cmapMax);
        }

        return index;
    }

    /**
     * Gets the {@link Color} from the color map in the parameters at the index
     * (capped 0-1) passed in
     *
     * @param index
     * @param colorMapParameters
     * @return the color
     */
    public static com.raytheon.uf.common.colormap.Color getColorByIndex(
            double index, ColorMapParameters colorMapParameters) {
        index = capIndex(index);
        IColorMap colorMap = colorMapParameters.getColorMap();
        int numColors = colorMap.getSize();
        if (colorMapParameters.isInterpolate()) {
            index = Math.min(index * numColors, numColors - 1);
            int lowIndex = (int) Math.floor(index);
            int highIndex = (int) Math.ceil(index);
            double lowWeight = highIndex - index;
            double highWeight = 1.0f - lowWeight;
            com.raytheon.uf.common.colormap.Color low = colorMap.getColors()
                    .get(lowIndex);
            com.raytheon.uf.common.colormap.Color high = colorMap.getColors()
                    .get(highIndex);
            float r = (float) (lowWeight * low.getRed()
                    + highWeight * high.getRed());
            float g = (float) (lowWeight * low.getGreen()
                    + highWeight * high.getGreen());
            float b = (float) (lowWeight * low.getBlue()
                    + highWeight * high.getBlue());
            float a = (float) (lowWeight * low.getAlpha()
                    + highWeight * high.getAlpha());
            return new com.raytheon.uf.common.colormap.Color(r, g, b, a);
        } else {
            int colorIndex = (int) Math.min(index * numColors, numColors - 1);
            return colorMap.getColors().get(colorIndex);
        }
    }

    /**
     * Returns a {@link Color} for the cmapValue based on the
     * {@link ColorMapParameters}
     *
     * @param cmapValue
     * @param colorMapParameters
     * @return the color
     */
    public static com.raytheon.uf.common.colormap.Color getColorByValue(
            double cmapValue, ColorMapParameters colorMapParameters) {
        return getColorByIndex(
                getColorMappingIndex(cmapValue, colorMapParameters),
                colorMapParameters);
    }
}
