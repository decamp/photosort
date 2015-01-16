/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.photosort;

import java.io.File;

/** 
 * @author Philip DeCamp  
 */
class FileIter {
    
    private final File mDir;
    private final String mStart;
    private final String mStop;
    private int mNumber = 0;
    
    
    public FileIter(File dir, String start, String stop) {
        mDir = dir;
        mStart = start;
        mStop = stop;
    }
    
    
    public File next() {
        if(mNumber == 0) {
            mNumber++;
            return new File(mDir, mStart + mStop);
        }
        
        File ret = new File(mDir, mStart + "-" + mNumber + mStop);
        mNumber++;
        return ret;
    }
    
}