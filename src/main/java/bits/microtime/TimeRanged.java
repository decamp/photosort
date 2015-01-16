/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.microtime;

import java.util.Comparator;

/**
* @author Philip DeCamp
*/
public interface TimeRanged {

    public static final Comparator<TimeRanged> START_TIME_ORDER = new Comparator<TimeRanged>() {
        public int compare(TimeRanged t1, TimeRanged t2) {
            long v1 = t1.getStartMicros();
            long v2 = t2.getStartMicros();

            if(v1 < v2)
                return -1;

            if(v1 > v2)
                return 1;

            return 0;
        }
    };

    public static final Comparator<TimeRanged> STOP_TIME_ORDER = new Comparator<TimeRanged>() {
        public int compare(TimeRanged t1, TimeRanged t2) {
            long v1 = t1.getStopMicros();
            long v2 = t2.getStopMicros();

            if(v1 < v2)
                return -1;

            if(v1 > v2)
                return 1;

            return 0;
        }
    };


    public long getStartMicros();
    public long getStopMicros();

}
