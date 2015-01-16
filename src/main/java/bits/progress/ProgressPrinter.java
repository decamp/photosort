/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.progress;

import java.io.*;

/** 
 * @author Philip DeCamp  
 */
public class ProgressPrinter extends ProgressAdapter {

    private final PrintStream mOut;
    private final PrintStream mErr;
    
    private int mMin = 0;
    private int mMax = 100;
    
    private boolean mCancelled = false;
    
    public ProgressPrinter() {
        mOut = System.out;
        mErr = System.err;
    }
    
    
    public void setNote(String note) {
        mOut.println(note);
    }
    
    public void setMinimum(int min) {
        mMin = min;
    }
    
    public void setMaximum(int max) {
        mMax = max;
    }
    
    public void setProgress(int prog) {
        float per = (float)(prog - mMin) / (float)(mMax - mMin) * 100.0f;
        mOut.println(per + "%");
    }
    
    public void setProgress(int prog, String message) {
        float per = (float)(prog - mMin) / (float)(mMax - mMin) * 100.0f;
        mOut.println(per + "%  " + message);
    }
    
    public void addInfo(String text) {
        mOut.println(text);
    }
    
    public void exceptionOccurred(Exception ex) {
        ex.printStackTrace(mErr);
    }
    
    public void taskFinished(String note, String subnote) {
        if(note != null)
            mOut.println(note);
        
        if(subnote != null)
            mOut.println(subnote);
    }
    
    public void taskCancelled(String note, String subnote) {
        mCancelled = true;
        
        if(note != null)
            mOut.println(note);
        
        if(subnote != null)
            mOut.println(subnote);
    }
    
    public boolean isCancelled() {
        return mCancelled;
    }
    
}
