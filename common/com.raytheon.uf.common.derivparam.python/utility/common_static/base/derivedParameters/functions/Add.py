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
##

import numpy

import Vector
from functools import reduce


def execute(*args):
    """Perform scalar or vector addition"""
    if len(args) == 1 and isinstance(args[0], list):
        return execute(*args[0])
    elif isinstance(args[0], tuple):
        return vectorAddition(args)
    else:
        return scalarAddition(args)

def scalarAddition(args):
    return reduce(numpy.add, args)


def vectorAddition(args):
    uResult = numpy.zeros_like(args[0][0])
    vResult = numpy.zeros_like(args[0][0])

    for u, v in args:
        uResult += u
        vResult += v

    return Vector.componentsTo(uResult, vResult)
