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
package com.raytheon.uf.viz.core.rsc.capabilities;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.raytheon.uf.viz.core.drawables.ext.IImagingExtension.ImageProvider;

/**
 * Capability for imaging features
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 12, 2009            njensen     Initial creation
 * Apr 05, 2018 6889       njensen     Changed brightness from float to Float
 *                                     to distinguish missing brightness
 * 
 * </pre>
 * 
 * @author njensen
 */

@XmlAccessorType(XmlAccessType.NONE)
public class ImagingCapability extends AbstractCapability {

    private static final float BRIGHTNESS_DEFAULT = 1.0f;

    private ImageProvider provider;

    @XmlAttribute
    private float contrast = 1.0f;

    @XmlAttribute
    private Float brightness = null;

    @XmlAttribute
    private boolean interpolationState = false;

    @XmlAttribute
    protected float alpha = 1.0f;

    /**
     * @return the contrast
     */
    public float getContrast() {
        return contrast;
    }

    /**
     * @param contrast
     *            the contrast to set
     */
    public void setContrast(float contrast) {
        setContrast(contrast, true);
    }

    public void setContrast(float contrast, boolean notify) {
        if (this.contrast != contrast) {
            this.contrast = contrast;
            if (notify) {
                this.capabilityChanged();
            }
        }
    }
    
    public boolean isBrightnessSet() {
        return brightness != null;
    }

    /**
     * @return the brightness
     */
    public float getBrightness() {
        if (brightness == null) {
            brightness = BRIGHTNESS_DEFAULT;
        }
        return brightness;
    }

    /**
     * @param brightness
     *            the brightness to set
     */
    public void setBrightness(Float brightness) {
        setBrightness(brightness, true);
    }

    public void setBrightness(Float brightness, boolean notify) {
        if (!brightness.equals(this.brightness)) {
            this.brightness = brightness;
            if (notify) {
                this.capabilityChanged();
            }
        }
    }

    /**
     * @return the interpolationState
     */
    public boolean isInterpolationState() {
        return interpolationState;
    }

    /**
     * @param interpolationState
     *            the interpolationState to set
     */
    public void setInterpolationState(boolean interpolationState) {
        if (this.interpolationState != interpolationState) {
            this.interpolationState = interpolationState;
            capabilityChanged();
        }
    }

    /**
     * @return the alpha
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * @param alpha
     *            the alpha to set
     */
    public void setAlpha(float alpha) {
        setAlpha(alpha, true);
    }

    public void setAlpha(float alpha, boolean notify) {
        if (this.alpha != alpha) {
            this.alpha = alpha;
            if (notify) {
                this.capabilityChanged();
            }
        }
    }

    /**
     * @return the provider
     */
    public ImageProvider getProvider() {
        return provider;
    }

    /**
     * @param provider
     *            the provider to set
     */
    public void setProvider(ImageProvider provider) {
        this.provider = provider;
    }

    @Override
    public AbstractCapability clone() {
        ImagingCapability ic = new ImagingCapability();
        ic.provider = provider;
        ic.contrast = contrast;
        ic.brightness = brightness;
        ic.interpolationState = interpolationState;
        ic.alpha = alpha;
        return ic;
    }
}
