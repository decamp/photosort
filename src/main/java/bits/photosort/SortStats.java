/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.photosort;

/** 
 * @author Philip DeCamp  
 */
class SortStats {

    int mFiles = 0;
    int mCopied = 0;
    int mMoved = 0;
    int mFailed = 0;
    int mDuplicates = 0;
    int mUndated = 0;
    
    public String toString() {
        StringBuilder s = new StringBuilder();
        
        s.append(String.format("%-6d  files found\n", mFiles));
        s.append("\n");
        
        if(mCopied > 0 || mMoved == 0)
            s.append(String.format("%-6d  files copied\n", mCopied));
        
        if(mMoved > 0)
            s.append(String.format("%-6d  files moved\n", mMoved));
        
        s.append(String.format("%-6d  duplicates found\n", mDuplicates));
        s.append(String.format("%-6d  missing timestamps\n", mUndated));
        s.append(String.format("%-6d  failures\n", mFailed));
            
        return s.toString();
    }
    
}
