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

# File auto-generated against equivalent DynamicSerialize Java class

class GroupsRequest(object):

    def __init__(self):
        self.groups = None
        self.request = None
        self.filename = None

    def getGroups(self):
        return self.groups

    def setGroups(self, groups):
        self.groups = groups

    def getRequest(self):
        return self.request

    def setRequest(self, request):
        self.request = request

    def getFilename(self):
        return self.filename

    def setFilename(self, filename):
        self.filename = filename

