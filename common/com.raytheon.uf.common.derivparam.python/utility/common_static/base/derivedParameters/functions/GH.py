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

#
# SOFTWARE HISTORY
#
# Date           Ticket#      Engineer      Description
# ------------   ----------   -----------   -----------
# Aug 05, 2015   4703         njensen       Removed unused imports
# Apt 26, 2018   6974         bsteffen      remove execute1.
#

import meteolib

import P

def execute2(pres):
    return meteolib.ptozsa(pres)*4

def execute3(prCloudStation,lowCldStation,midCldStation,hiCldStation):
    prCloudClg = P.execute6(prCloudStation,lowCldStation,midCldStation,hiCldStation)
    return meteolib.ptozsa(prCloudClg)
