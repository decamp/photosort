/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.microtime;

import java.util.Date;

/**
* @author Philip DeCamp
*/
public class TimeBlock implements TimeRanged {

    public static TimeBlock fromMicros( long startMicros, long stopMicros ) {
        return new TimeBlock( startMicros, stopMicros );
    }


    private final long mStartMicros;
    private final long mStopMicros;


    private TimeBlock( long startMicros, long stopMicros ) {
        mStartMicros = startMicros;
        mStopMicros = stopMicros;
    }


    public long getStartMicros() {
        return mStartMicros;
    }

    public long getStopMicros() {
        return mStopMicros;
    }

    public long getSpanMicros() {
        return mStopMicros - mStartMicros;
    }

    public boolean containsMicro( long micro ) {
        return micro >= mStartMicros && micro < mStopMicros;
    }

    /**
     * Creates a TimeBlock that contains the same micros, but ensures that
     * the stopMicros >= startMicros.
     *
     * @return TimeBlock with the same range and with a non-negative span.
     */
    public TimeBlock normalize() {
        if( mStopMicros >= mStartMicros ) {
            return this;
        }

        return new TimeBlock( mStopMicros + 1, mStartMicros + 1 );
    }


    public int hashcode() {
        return (int)(mStartMicros ^ mStopMicros);
    }

    public boolean equals( Object o ) {
        if( o == null || !(o.getClass() == TimeBlock.class) ) {
            return false;
        }

        TimeBlock t = (TimeBlock)o;
        return mStartMicros == t.mStartMicros && mStopMicros == t.mStopMicros;
    }

    public String toString() {
        return String.format( "TimeBlock [%s] to [%s]",
                              new Date( mStartMicros / 1000L ).toString(),
                              new Date( mStopMicros / 1000L ).toString() );
    }

}
