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
public interface ProgressTask {
    
    /**
     * Asks the task to start.  Task is responsible for creating own thread.
     */
    public void startTask(ProgressListener monitor);
    
    /**
     * Requests that the task be cancelled mid-operation.
     */
    public void cancelTask();

}
