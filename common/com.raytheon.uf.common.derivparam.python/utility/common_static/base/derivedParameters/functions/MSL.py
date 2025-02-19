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

# ----------------------------------------------------------------
#Returns the sum of elevation and the 1st skyLayerBase
#
# ----------------------------------------------------------------
#
# SOFTWARE HISTORY
#
# Date           Ticket#      Engineer      Description
# ------------   ----------   -----------   -----------
#                             ????          Initial creation
# Aug 05, 2015   4703         njensen       Optimized
# Apr 30, 2020   7880         tgurney       Python 3 index fix
#


##
# Returns the sum of elevation and the 1st skyLayerBase
#
# @param skyLayerBase, : skyLayerBase,
# @param elevation : elevation
# @return:
# @rtype:
#
def execute(skyLayerBase, elevation, index):
    #convert elevation (m) to elevation (ft)
    elevation_ft = (elevation * 3.2808399)
    return skyLayerBase[:,int(index)] + elevation_ft
