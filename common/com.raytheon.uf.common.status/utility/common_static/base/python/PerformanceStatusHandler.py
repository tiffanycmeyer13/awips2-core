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

from com.raytheon.uf.common.status import PerformanceStatus

#
# Python performance logging mechanism to allow python to log to the
# *_perf.log file using the same format as Java code.
#
# Example usage:
#    perfLog = PerformanceStatusHandler("MyFunction")
#    t0 = time.perf_counter()
#
#    # perform action to be timed
#
#    t1 = time.perf_counter()
#    perfLog.logDuration("description of action", t1-t0)
#
# Sample performance log output:
#   INFO  2021-12-10 11:41:09,174 1318 [thread-id] PerformanceLogger: MyFunction: description of action took nnn ms
#
# SOFTWARE HISTORY
#
# Date          Ticket#  Engineer  Description
# ------------- -------- --------- --------------------------------------------
# Dec 10, 2021  8342     randerso  Initial Creation.
#
##


class PerformanceStatusHandler():

    def __init__(self, prefix):
        self._perfLog = PerformanceStatus.getHandler(prefix)

    def log(self, message):
        """
        Log a message to the performance log

        Args:
            message:    the message to be logged
        """
        self._perfLog.log(str(message))

    def logDuration(self, message, duration):
        """
        Log a message to the performance log with duration

        Args:
            message:    the message to be logged
            duration:   duration in seconds
        """
        self._perfLog.logDuration(str(message), int(duration * 1000))
