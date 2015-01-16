/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

/**
 * MIT Media Lab
 * Cognitive Machines Group
 */

package bits.progress;

/** 
 * @author Philip DeCamp  
 */
public interface ProgressListener {
    public void setNote(String note);
    public void setMinimum(int min);
    public void setMaximum(int max);
    public void setProgress(int progress);
    public void setProgress(int progress, String message);
    public void addInfo(String text);
    public void exceptionOccurred(Exception ex);
    
    public void taskFinished(String note, String subnote);
    public void taskCancelled(String note, String subnote);
        
    public boolean isCancelled();
}
