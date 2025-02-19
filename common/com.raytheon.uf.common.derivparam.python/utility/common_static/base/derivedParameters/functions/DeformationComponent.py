##
# This software was developed and / or modified by Raytheon Company,
# pursuant to Contract DG133W-05-CQ-1067 with the US Government.
#
# U.S. EXPORT CONTROLLED TECHNICAL DATA
# This software product contains export-restricted data whose
# export/transfer/disclosure is restricted by U.S. law. Dissemination
# to non-U.S. persons whether in the United States or abroad requires
# an export license or other authorization.
#
# Contractor Name:        Raytheon Company
# Contractor Address:     6825 Pine Street, Suite 340
#                         Mail Stop B8
#                         Omaha, NE 68106
#                         402.291.0100
#
# See the AWIPS II Master Rights File ("Master Rights File.pdf") for
# further licensing information.
###

from numpy import sqrt
from numpy import shape
from numpy import empty
from numpy import where
from numpy import NaN
import Vector as Vector
from WorldWrapUtil import HandleWorldWrapX

##
# Calculate the deformation components of Vector.
#
# @param Vector: A tuple(U,V) of numpy arrays. U and V must be the same size,
#                at least 2d, and at least 3x3 in the first two dimensions.
# @param dx: The spacing between data points in the X direction (array or scalar)
# @param dy: The spacing between data points in the Y direction (array or scalar)
# @return: The deformation array of Vector.
@HandleWorldWrapX
def execute(U, V, dx, dy):

    # create an empty array the same dimensions as U
    resultx = empty(U.shape, U.dtype)
    resulty = empty(U.shape, U.dtype)


    # flag edges as invalid
    resultx[0,:] = NaN
    resultx[-1,:] = NaN
    resultx[1:-1,0] = NaN
    resultx[1:-1,-1] = NaN
    resulty[0,:] = NaN
    resulty[-1,:] = NaN
    resulty[1:-1,0] = NaN
    resulty[1:-1,-1] = NaN

    # strip off edges of dx if it's not a scalar
    shapedx = shape(dx)
    if len(shapedx) < sum(shapedx):
        dx = dx[1:-1,1:-1]

    # strip off edges of dy if it's not a scalar
    shapedy = shape(dy)
    if len(shapedy) < sum(shapedy):
        dy = dy[1:-1,1:-1]

    # Calculate deformation vector components.
    qqq = 0.5/dx
    www = 0.5/dy
    dst = (U[1:-1,0:-2] - U[1:-1,2:]) * qqq
    dst += (V[0:-2,1:-1] - V[2:,1:-1]) * www
    dsh = (U[0:-2,1:-1] - U[2:,1:-1]) * qqq
    dsh += (V[1:-1,2:] - V[1:-1,0:-2]) * www

    qqq = dst*dst + dsh*dsh
    www = sqrt(qqq)
    ans = sqrt((qqq+www*dst)/2)

    Q = where(ans != 0.0, www*dsh/(2*ans), www)

    # put ans in the valid block of result
    resultx[1:-1,1:-1] = Q
    resulty[1:-1,1:-1] = ans

    return Vector.execute(resultx,resulty)
