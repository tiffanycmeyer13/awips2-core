/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.units;

import java.math.BigDecimal;
import java.math.BigInteger;

import tech.units.indriya.function.Calculus;
import tech.units.indriya.function.DefaultNumberSystem;

/**
 * Custom number system for the indriya units library to use. This tweaks the
 * default number system with a performance enhancement for narrowing
 * BigDecimals, as the default method relies on exception catching to determine
 * if BigDecimals can be converted to integers, which is slow. The slowness was
 * noticed when loading radial velocity as grid data.
 *
 * This change will be applied to the default number system under this indriya
 * issue, so this class can eventually go away:
 * https://github.com/unitsofmeasurement/indriya/issues/393
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 14, 2023 2031675    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class CustomNumberSystem extends DefaultNumberSystem {

    @Override
    public Number narrow(Number number) {
        if (number instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) number;
            decimal = decimal.stripTrailingZeros();
            if (decimal.scale() <= 0) {
                BigInteger integer = decimal.toBigInteger();
                return narrow(integer);
            }
            return number;
        }
        return super.narrow(number);
    }

    /**
     * Set an instance of this number system as the current units number system.
     *
     * @return null (has to be non-void to call from spring)
     */
    public static Object setAsCurrentNumberSystem() {
        Calculus.setCurrentNumberSystem(new CustomNumberSystem());
        return null;
    }
}
