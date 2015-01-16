/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

/**
 * MIT Media Lab
 * Cognitive Machines Group
 */

package bits.photosort;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.logging.*;

import bits.microtime.TimeBlock;


/**
 * Basic utility for traversing directory structures.  DirSpider provides
 * an iteration over each file in a directory tree.  DirSpider may be 
 * given a set of FileFilter and Comparator objects that specify which 
 * directories are crawled, which files are returned, and in which order.
 * files are returned and in what order.
 * <p>
 * DirSpider is not thread-safe.  However, DirSpider honors thread interrupts
 * on all potentially time-consuming operations.  If interrupted, DirSpider
 * will revert to its previous state as if the interrupted method had never
 * been called.
 * <p>
 * By default, the DirSpider will recurse into all non-hidden directories
 * in alphabetical order.  For each directory, the DirSpider will return
 * all non-hidden, non-directory files in alphabetical order.  If a directory
 * contains both files and child directories, the files will be returned before
 * traversing the child directories.  This ordering is reversed when performing
 * backwards iterations, as provided with <code>getPreviousFile</code>().
 * <p>
 * DirSpider maintains a pointer into a directory tree that always points to a 
 * space between two files.  That is, if DirSpider currently points to 
 * <code>file1</code>, then <code>getNextFile()</code> will return 
 * <code>file1</code> and increment the pointer, while 
 * <code>getPreviousFile()</code> will decrement the pointer and return the file
 * immediately before <code>file1</code>.
 * <p>
 * If calling the DirSpider constructor directly rather than using any of the
 * factory methods, between 1 and 5 arguments can be provided.  
 * <p>
 * 1) Mandatory: The root directory that the DirSpider should reside in.  No
 * files or directories outside of this directory tree will be returned, unless
 * symbolic links are used, which makes things really messy and shouldn't be 
 * done unless you're careful.
 * <p>
 * 2) Optional: A FileFilter that determines which files are returned by the
 * dir spider.  Default: all non-hidden, non-directory files.
 * <p>
 * 3) Optional: A Comparator of type "File" that determines how the files 
 * within a directory are sorted.  Default: alphabetical, case-insensitive.
 * <p>
 * 4) Optional: A FileFilter that determines which directories are searched.
 * Default: all non-hidden, directory files.
 * <p>
 * 5) Optional: A Comparator of type "File" that determines in which order
 * a set of peer directories are traversed.  Default: alphabetical, 
 * case-insensitive.
 * <p>
 * While DirSpider may be used in any file tree, DirSpider provides additional
 * functionality for the HSP directory structure, in which the directory 
 * hierarchy goes from root to year to month to day to hour to minute.  When
 * using such a structure, DirSpider may accurate provide a time range for any
 * file it retrieves, will support gotoTimeMillis(), and also allows for a 
 * time range to be specified such that only files within a certain time
 * range will be returned.
 * 
 * @author Philip DeCamp  
 */
public class DirSpider {
    
    protected static Logger sLog = Logger.getLogger(DirSpider.class.getName());
    
    
    public static final Comparator<File> ALPHA_COMPARATOR = new Comparator<File>() {
        public int compare(File f1, File f2) {
            return String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName());
        }
    };

    
    /**
     * Accepts only non-hidden directory files.
     */
    public static final FileFilter DIR_FILTER = new FileFilter() {
        public boolean accept(File file) {
            return file.isDirectory() && !file.isHidden();
        }
    };
    
    
    /**
     * Accepts only "indexed directories", meaning directories which begin
     * with a number.
     */
    public static final FileFilter INDEXED_DIR_FILTER = new FileFilter() {
        public boolean accept(File file) {
            if(!file.isDirectory() || file.isHidden())
                return false;
            
            Matcher m = DIR_INDEX_PATTERN.matcher(file.getName());
            return m.find();
        }
    };
    
    
    /**
     * Accepts only non-directory, non-hidden files.
     */
    public static final FileFilter FILE_FILTER = new FileFilter() {
        public boolean accept(File file) {
            return !(file.isDirectory() || file.isHidden());
        }
    };

    
    public static final Pattern DIR_INDEX_PATTERN = Pattern.compile("^(\\d++)");
    
    public static final Pattern CHANNEL_PATTERN = Pattern.compile("(?:^|-)(\\d++)");
    public static final Pattern SQUINT_PATTERN = Pattern.compile("(?:^|-)(\\d++).*?\\.squint$");
    public static final Pattern WINK_PATTERN = Pattern.compile("(?:^|-)(\\d++).*?\\.wink$");
    public static final Pattern BEPCM_PATTERN = Pattern.compile("(?:^|-)(\\d++).*?\\.bepcm$");
    
    
    /**
     * Accepts only .squint files.
     */
    public static final FileFilter SQUINT_FILTER = new FileFilter() {
        public boolean accept(File file) {
            return file.getName().endsWith(".squint");
        }
    };
    
    
    /**
     * Accepts only .wink files.
     */
    public static final FileFilter WINK_FILTER = new FileFilter() {
        public boolean accept(File file) {
            return file.getName().endsWith(".wink");
        }
    };
    

    /**
     * Accepts only .bepcm files.
     */
    public static final FileFilter BEPCM_FILTER = new FileFilter() {
        public boolean accept(File file) {
            return file.getName().endsWith(".bepcm");
        }
    };
    

    /**
     * Accepts files for a given channel.
     */
    public static FileFilter getChannelFilter(final int channel) {
        return new FileFilter() {
            public boolean accept(File file) {
                if(!file.isFile())
                    return false;
                
                Matcher m = CHANNEL_PATTERN.matcher(file.getName());
                return m.find() && Integer.parseInt(m.group(1)) == channel;
            }
        };
    }
        
    
    
    public static DirSpider getChannelSpider(File rootFile, int channel) {
        FileFilter filter = getChannelFilter(channel);
        return new DirSpider(rootFile, filter);
    }
    
    public static DirSpider getSquintSpider(File rootFile, int channel) {
        return new DirSpider(rootFile, getSquintFilter(channel));
    }
    
    public static DirSpider getSquintSpider(File rootFile, File startFile, int channel) {
        DirSpider spider = getSquintSpider(rootFile, channel);
        if(startFile != null)
            spider.gotoFile(startFile);
        return spider;
    }
    
    public static DirSpider getAllSquintSpider(File rootFile) {
        return new DirSpider(rootFile, SQUINT_FILTER);
    }
    
    public static DirSpider getAllSquintSpider(File rootFile, File startFile) {
        DirSpider spider = getAllSquintSpider(rootFile);
        if(startFile != null)
            spider.gotoFile(startFile);
        return spider;
    }
    
    public static DirSpider getFirstSquintSpider(File rootFile) {
        return getFirstSquintSpider(rootFile, null);
    }
    
    public static DirSpider getFirstSquintSpider(File rootFile, File startFile) {
        DirSpider spider = getAllSquintSpider(rootFile, startFile);
        
        for(File file = spider.getNextFile(); file != null; file = spider.getNextFile()) {
            Matcher m = SQUINT_PATTERN.matcher(file.getName());
            if(m.find()) {
                int channel = parseChannel(file);
                if(channel >= 0) {
                    spider = new DirSpider(rootFile, getSquintFilter(channel));
                    return spider;
                }
            }
        }
        
        return null;
    }
    
    public static DirSpider getBEPCMSpider(File rootFile, int channel) {
        return new DirSpider(rootFile, getBEPCMFilter(channel));
    }
    
    public static DirSpider getBEPCMSpider(File rootFile, File startFile, int channel) {
        DirSpider spider = getBEPCMSpider(rootFile, channel);
        if(startFile != null)
            spider.gotoFile(startFile);
        return spider;
    }
    
    public static DirSpider getAllBEPCMSpider(File rootFile) {
        return new DirSpider(rootFile, BEPCM_FILTER);
    }
    
    public static DirSpider getAllBEPCMSpider(File rootFile, File startFile) {
        DirSpider spider = getAllBEPCMSpider(rootFile);
        if(startFile != null)
            spider.gotoFile(startFile);
        return spider;
    }
    
    public static DirSpider getFirstBEPCMSpider(File rootFile) {
        return getFirstBEPCMSpider(rootFile, null);
    }
    
    public static DirSpider getFirstBEPCMSpider(File rootFile, File startFile) {
        DirSpider spider = getAllBEPCMSpider(rootFile, startFile);
        
        for(File file = spider.getNextFile(); file != null; file = spider.getNextFile()) {
            Matcher m = BEPCM_PATTERN.matcher(file.getName());
            if(m.find()) {
                int channel = parseChannel(file);
                if(channel >= 0) {
                    System.out.println("Reading through channel " + channel);
                    spider = new DirSpider(rootFile, getBEPCMFilter(channel));
                    return spider;
                }
            }
        }
        
        return null;
    }
    
    public static DirSpider getWinkSpider(File rootFile, int channel) {
        return new DirSpider(rootFile, getWinkFilter(channel));
    }
    
    public static DirSpider getWinkSpider(File rootFile, File startFile, int channel) {
        DirSpider spider = getWinkSpider(rootFile, channel);
        if(startFile != null)
            spider.gotoFile(startFile);
        return spider;
    }
    
    
    
    /**
     * Retrieves the channel number from a file name.  The channel number is 
     * assumed to be at the start of the file name.  For instance,
     * 21_kitchen_2005.squint has channel number 21.
     * 
     * @return the channel of a file, or -1 if not found. 
     */
    public static int parseChannel(File file) {
        Matcher m = CHANNEL_PATTERN.matcher(file.getName());
        if(m.find()){
            return Integer.parseInt(m.group(1));
        }
        
        return -1;
    }
    
    /**
     * Retrieves an index from a directory, where "index" just
     * means that the directory begins with a number.  A directory
     * named "03_feb" has index 3.
     * 
     * @return the index of a directory, -1 if no index is found.
     */
    public static int parseDirectoryIndex(File file) {
        Matcher m = DIR_INDEX_PATTERN.matcher(file.getName());
        if(m.find()){
            return Integer.parseInt(m.group(1));
        }
        
        return -1;
    }
    
    /**
     * Applies only to "indexed" directories that begin with numbers.  
     * When finding the "root" of the indexed directory tree, this
     * method will climb up the tree until it finds a directory with 
     * no index.  For example, if give the file
     * 
     * \data\avdata\2006\04_feb\21_mon
     * 
     * The "root" file returned will be
     * 
     * \data\avdata
     * 
     * @return nearest ancestor directory that does not have an index. 
     */
    public static File findRootFile(File file) {
        if(file.isFile())
            file = file.getParentFile();
        
        while(file != null && (DIR_INDEX_PATTERN.matcher(file.getName()).find())) {
            file = file.getParentFile();
        }
        
        return file;
    }
    
    /**
     * @param channel A blackeye channel ID.
     * @return a FileFilter that accepts only squint files on the provided channel.
     */
    public static FileFilter getSquintFilter(final int channel) {
        return new FileFilter() {
            public boolean accept(File file){
                Matcher m = SQUINT_PATTERN.matcher(file.getName());
                return (m.find() && (channel < 0 || Integer.parseInt(m.group(1)) == channel));
            }
        };
    }
    
    /**
     * @param channel A Blackeye channel ID.
     * @return a FileFilter that accepts only wink files on the provided channel.-
     */
    public static FileFilter getWinkFilter(final int channel) {
        return new FileFilter() {
            public boolean accept(File file) {
                Matcher m = WINK_PATTERN.matcher(file.getName());
                return (m.find() && (channel < 0 || Integer.parseInt(m.group(1)) == channel));
            }
        };
    }

    /**
     * @param channel A Blackeye channel ID.
     * @return a FileFilter that accepts only .bepcm files for the provided channel.
     */
    public static FileFilter getBEPCMFilter(final int channel) {
        return new FileFilter() {
            public boolean accept(File file) {
                Matcher m = BEPCM_PATTERN.matcher(file.getName());
                return (m.find() && (channel < 0 || Integer.parseInt(m.group(1)) == channel));
            }
        };
    }

    /**
     * Parses the directory structure to determine the block
     * in which a file resides.  This block may be a minute, hour,
     * day, month, year, or infinitely large depending on where in 
     * the data structure the file resides.
     */
    public static TimeBlock getTimeFromFile(File file) {
        //TODO: LOW: More efficient implementation.        
        File root = findRootFile(file);
        if(root == null)
            return null;
        
        Stack<Integer> indexStack = new Stack<Integer>();
        
        while(!file.equals(root)) {
            int index = parseDirectoryIndex(file);
            if(index >= 0){
                indexStack.push(index);
            }
            
            file = file.getParentFile();
        }
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0l);
        int level = TimeDir.TIME_LEVEL_YEAR;
        
        while(!indexStack.isEmpty()) {
            if(level > TimeDir.TIME_LEVEL_MINUTE)
                break;
            
            int value = TimeDir.convertDirectoryIndexToCalendarValue(level, indexStack.pop());
            cal.set(TimeDir.getCalendarLevel(level), value);
            level++;
        }
        
        if(--level == TimeDir.TIME_LEVEL_ALL)
            return null;
        
        long startMillis = cal.getTimeInMillis();
        cal.add(TimeDir.getCalendarLevel(level), 1);
        long stopMillis = cal.getTimeInMillis();
        
        return TimeBlock.fromMicros(startMillis * 1000L, stopMillis * 1000L);
    }
    
    
    
    private static final int STATE_RESET = 0;
    private static final int STATE_GOTO_END = 1;
    private static final int STATE_GOTO_CURRENT = 2;
    private static final int STATE_GOTO_FILE_START = 4;
    private static final int STATE_GOTO_FILE_END = 5;
    private static final int STATE_GOTO_TIME = 6;
    private static final int STATE_READY = 7;
    
    private final File mRootFile;
    private final FileFilter mDirFilter;
    private final FileFilter mFileFilter;
    private final Comparator<File> mDirComp;
    private final Comparator<File> mFileComp;

    private final Calendar mCalendar = Calendar.getInstance();
    private final Calendar mGotoCalendar = Calendar.getInstance();
    
    private DirCache[] mStack;
    private int mStackSize = 0;
    private TimeBlock mRange = null;
    
    private int mState = STATE_RESET;
    
    private File mCurrentFile = null;
    private long mCurrentStartMicros = 0;
    private long mCurrentStopMicros = 0;
    private boolean mCurrentIsForward = true;
    
    private File mGotoFile = null;
    private long mGotoMicros = 0;
    
    
    public DirSpider(File rootFile) {
        this(rootFile, FILE_FILTER, ALPHA_COMPARATOR, DIR_FILTER, ALPHA_COMPARATOR);
    }
    
    public DirSpider(File rootFile, FileFilter fileFilter) {
        this(rootFile, fileFilter, ALPHA_COMPARATOR, DIR_FILTER, ALPHA_COMPARATOR);
    }
    
    public DirSpider(File rootFile, FileFilter fileFilter, Comparator<File> fileComparator) {
        this(rootFile, fileFilter, fileComparator, DIR_FILTER, ALPHA_COMPARATOR);
    }
    
    public DirSpider(File rootFile, FileFilter fileFilter, Comparator<File> fileComparator, FileFilter dirFilter) {
        this(rootFile, fileFilter, fileComparator, dirFilter, ALPHA_COMPARATOR);
    }
    
    public DirSpider(File rootFile, 
                     FileFilter fileFilter, 
                     Comparator<File> fileComparator,
                     FileFilter dirFilter, 
                     Comparator<File> dirComparator)
    {
        if(rootFile == null)
            throw new NullPointerException("RootFile cannot be null.");
        
        if(!rootFile.isDirectory())
            throw new IllegalArgumentException("RootFile must be a directory: " + rootFile);
        
        mRootFile = rootFile;
        mDirFilter = dirFilter;
        mFileFilter = fileFilter;
        mDirComp = dirComparator;
        mFileComp = fileComparator;
        
        mStack = new DirCache[10];
        TimeDir.zeroCalendar(mCalendar);
    }
   
    
    
    /**
     * @return the root file being used by this DirSpider.
     */
    public File getRootDir() {
        return mRootFile;
    }
    
    
    /**
     * Retrieves the next file in the directory tree without being 
     * interrupted.
     * 
     * @return the next file in the tree
     */
    public File getNextFile() {
        try{
            return getNextFile(false);
        }catch(InterruptedException ex){
            ex.printStackTrace(System.err);
            System.err.println("This should never have happened.");
        }
        
        return null;
    }
    
    
    /**
     * Returns the next file in the tree.  If the interruptable flag is set to
     * TRUE, then the search will stop if the calling thread is interrupted and 
     * will throw an InterruptedException.  The DirSpider is still useable
     * after an InterruptedException and the search order is not affected.
     * A subsequent call to getNextFile() will return the same file that would
     * have been returned if the interruption had not occurred.  However,
     * getPrevFile() may return the file that had already been returned.
     * </p><p>
     * If the DirSpider has been given a non-null time range, files that fall
     * outside that TimeRange will not be returned.
     * </p> 
     * 
     * @param interruptable - Specifies whether search operations will cease if interrupted.
     * @return the next file in the tree
     * @throws InterruptedException if interruptable parameter == true and thread is interrupted. 
     */
    public File getNextFile(boolean interruptable) throws InterruptedException {
        
        if(mState != STATE_READY) {
            getReady(interruptable);
            
            if(mStackSize == 0)
                return null;
        
        }else if(mStackSize == 0) {
            return null;

        }else{
            //In case anything goes wrong, go back to the last returned file.
            mState = STATE_GOTO_CURRENT;
        }
        
FORWARD_SEARCH_LOOP:
        while( !interruptable || !Thread.interrupted() ) {
            DirCache cache = mStack[mStackSize-1];
            
            //Check if the current directory is within the time range.
            if(mRange != null) {
             
                //Check if we're too far.
                if(mRange.getStopMicros() <= cache.mStartMicros) {
                    return null;
                }
                
                //Check if we're too early.
                if(mRange.getStartMicros() >= cache.mStopMicros) {
                    if(!pop())
                        return null;
                    
                    continue;
                }
            }
            
            //Check if there are any files.
            if(cache.mFileIndex < cache.mFiles.length) {
                mCurrentFile = cache.mFiles[cache.mFileIndex];
                mCurrentStartMicros = cache.mStartMicros;
                mCurrentStopMicros = cache.mStopMicros;
                mCurrentIsForward = true;
                mState = STATE_READY;
                cache.mFileIndex++;
                return mCurrentFile;
            }
            
            cache.mFileIndex = cache.mFiles.length;
            
            //No more files in this directory.  
            //Check if there are any directories.
            
            //Can't trust the user-provided filter to provide only
            //directories, so we need to loop through the directory
            //array.
            for(cache.mDirIndex++; cache.mDirIndex < cache.mDirs.length; cache.mDirIndex++) {
                if(cache.mDirs[cache.mDirIndex].isDirectory()) {
                    //Found directory.  Go to next level.
                    pushNewDirectory(cache.mDirs[cache.mDirIndex]);
                    continue FORWARD_SEARCH_LOOP;
                }
            }
            
            //If we failed to find a new directory, go up a level.
            //If we can't go up, return null.
            cache.mDirIndex = cache.mDirs.length;
            if(!pop())
                return null;
        }

        throw new InterruptedException();
    }
    
    
    /**
     * Returns previous file in stack 
     */
    public File getPreviousFile() {
        try{
            return getPreviousFile(false);
        }catch(InterruptedException ex){
            ex.printStackTrace(System.err);
            System.err.println("This should never have happened.");
        }
        
        return null;
    }

    
    /**
     * Returns the previous file in the tree.  If the interruptable flag is set to 
     * TRUE, then the search will stop if the calling thread is interrupted and 
     * will throw an InterruptedException.  The DirSpider is still useable 
     * after an InterruptedException and the search order is not affected.  A
     * subsequent call to getPrevFile() will return the same file that would have
     * been returned if the interruption had not occurred.  However, getNextFile()
     * may return a file that had already been returned.  
     * <p>
     * If the DirSpider has been given a non-null time range, files that fall
     * outside that TimeRange will not be returned.
     * 
     * @param interruptable - Specifies whether the search operation will cease if thread is interrupted.
     * @return the previous file in the tree
     * @throws InterruptedException if interruptable parameter == true and thread is interrupted.
     */
    public File getPreviousFile(boolean interruptable) throws InterruptedException {

        if(mState != STATE_READY) {
            getReady(interruptable);
            
            if(mStackSize == 0)
                return null;
        
        }else if(mStackSize == 0) {
            return null;

        }else{
            //In case anything goes wrong, go back to the last returned file.
            mState = STATE_GOTO_CURRENT;
        }
        
BACKWARD_SEARCH_LOOP:
        while(!interruptable || !Thread.interrupted()) {
            DirCache cache = mStack[mStackSize-1];
            
            //Check if the current directory is within the time range.
            if(mRange != null) {
             
                //Check if we're too early.
                if(mRange.getStopMicros() <= cache.mStartMicros) {
                    if(!pop())
                        return null;
                    
                    continue;
                }
                
                //Check if we're too far.
                if(mRange.getStartMicros() >= cache.mStopMicros) {
                    return null;
                }
            }
            
            //Check if there are any directories.
            for(cache.mDirIndex--; cache.mDirIndex >= 0; cache.mDirIndex--) {
                //The user-provided filter may provide non-directory files,
                //making it necessary to loop through the directory array.
                
                if(cache.mDirs[cache.mDirIndex].isDirectory()) {
                    pushNewDirectoryReversed(cache.mDirs[cache.mDirIndex]);
                    continue BACKWARD_SEARCH_LOOP;
                }
            }
            
            cache.mDirIndex = -1;
                        
            //Check if there are any files.
            if(--cache.mFileIndex >= 0) {
                mCurrentFile = cache.mFiles[cache.mFileIndex];
                mCurrentStartMicros = cache.mStartMicros;
                mCurrentStopMicros = cache.mStopMicros;
                mCurrentIsForward = false;
                mState = STATE_READY;
                return cache.mFiles[cache.mFileIndex];
            }
            
            cache.mFileIndex = 0;
            
            //If there are no files, we go up the directory tree.
            if(!pop())
                return null;
        }
    
        throw new InterruptedException();
    }
    

    /**
     * Returns the last non-null file returned by DirSpider without changing
     * any state.  The last returned file is set to <code>null</code> upon 
     * instantiation and after calls to <code>reset()</code>, <code>gotoTime*()</code>, 
     * or <code>gotoFile()</code>.
     * <p>
     * The specified range of the DirSpider does not affect getCurrentFile().
     * 
     * @return the current file that DirSpider points to.
     */
    public File getLastReturnedFile() {
        return mCurrentFile;
    }
    
    
    /**
     * <p>Returns the directory containing the file returned by getLastReturnedFile(), 
     * or the root file if getCurrentFile() == null.
     * 
     * @return the parent directory of the current file 
     */
    public File getLastReturnedDirectory() {
        File ret = getLastReturnedFile();
        if(ret == null)
            return mRootFile;
        
        return ret.getParentFile();
    }
    
    
    /**
     * Returns the crawl direction from the last non-null file that DirSpider
     * returned.  If the last non-null file was returned from 
     * <code>getNextFile()</code>, then this method will return
     * <code>Direction.FORWARD</code>.  If from <code>getPreviousFile()</code>,
     * then <code>Direction.BACKWARD</code> will be returned.  If the DirSpider
     * has been reset (ie, if <code>getLastReturnedFile()</code> would return
     * <code>null</code>), then Direction.NONE will be returned.
     * <p>
     * This is useful for users that want to crawl directories in both 
     * directions, but don't want to retrieve a file twice when changing 
     * directions.
     * 
     * @return most recent crawl direction
     */
    public Direction getLastDirection() {
        if(mCurrentFile == null)
            return Direction.NONE;
        
        if(mCurrentIsForward)
            return Direction.FORWARD;
        
        return Direction.BACKWARD;
    }
    
    
    /**
     * Resets the DirSpider to it's initial state such that the DirSpider
     * points to the beginning of the file tree.  reset() does not affect
     * the time range of the DirSpider, which must be cleared separately.
     */
    public void reset() {
        mCurrentFile = null;
        mState = STATE_RESET;
    }
    
    
    /**
     * Similar to reset(), except that the DirSpider will subsequently point to
     * the end of the file tree instead of the beginning.  Does not affect
     * the time range of the DirSpider.
     */
    public void gotoEnd() {
        mCurrentFile = null;
        mState = STATE_GOTO_END;
    }

    
    /**
     * Causes the DirSpider to go to a particular time within a timestamped
     * directory tree.  Provides minute-level accuracy or less.
     * 
     * @param millis Microsecond timestamp to go to.
     */
    public void gotoTimeMillis(long millis) {
        mState = STATE_GOTO_TIME;
        mGotoMicros = millis * 1000L;
        mCurrentFile = null;
    }
    
    
    /**
     * Causes the DirSpider to go to a particular time within a timestamped
     * directory tree.  Provides minute-level accuracy or less.
     * 
     * @param micros Microsecond timestamp to go to.
     */
    public void gotoTimeMicros(long micros) {
        mState = STATE_GOTO_TIME;
        mGotoMicros = micros;
        mCurrentFile = null;
    }


    /**
     * Causes the DirSpider to go to a particular file.  If the File does not 
     * belong to the rootdirectory, the DirSpider stop returning files until
     * it receives a valid initialization.
     * <p>
     * If the target file would normally be traversed by DirSpider, it will
     * be the next file returned by getNextFile().
     * 
     * @param target DirSpider will subsequently point to the start of this file.
     */
    public void gotoFile(File target) {
        mGotoFile = target;
        mState = STATE_GOTO_FILE_START;
        mCurrentFile = null;
    }

    
    /**
     * Like <code>gotoFile()</code>, but causes DirSpider to go to the end of
     * a particular file rather than the beginning.
     * <p>
     * If the target file would normally be traversed by DirSpider, it will
     * be the next file returned by getPreviousFile().
     * 
     * @param target DirSpider will subsequently point to the end of this file. 
     */
    public void gotoFileEnd(File target) {
        mGotoFile = target;
        mState = STATE_GOTO_FILE_END;
        mCurrentFile = null;
    }
        
    
    /**
     * Sets a time range in which this DirSpider will operate.  The DirSpider
     * will only return files whose directory times intersect with this range.
     * <p>
     * This call does not affect the state of the DirSpider instance until
     * the next file retrieval operation.
     * 
     *  @param range A range of time in which to limit DirSpider.  May be null if no time range is desired.
     */
    public void setTimeRange(TimeBlock range) {
        mRange = range;
    }

    
    /**
     * Sets a time range in which this DirSpider will operate.  The DirSpider
     * will only return files whose directory times intersect with this range.
     * <p>
     * This call does not affect the state of the DirSpider instance until
     * the next file retrieval operation.
     * 
     *  @param startMicros Start of range
     *  @param stopMicros Stop of range
     */
    public void setTimeRange(long startMicros, long stopMicros) {
        mRange = TimeBlock.fromMicros(startMicros, stopMicros);
    }

    
    /**
     * Clears the time range for DirSpider, allowing the DirSpider to return
     * all files within the file tree regardless of time.
     * <p>
     * Equivalent to calling 
     * <code>
     * setTimeRange(null)
     * </code>
     */
    public void clearTimeRange() {
        mRange = null;
    }
    
    
    /**
     * @return the current time range, or null if it has not been set.
     */ 
    public TimeBlock getTimeRange() {
        return mRange;
    }
    

    /**
     * Retrieves the TimeBlock for the last file retrieved.  This TimeBlock is based on 
     * the directory indices leading to the last file retrieved.  For example, if the
     * root file is 
     * <p>
     * <code>/tmp/root</code>
     * <p>
     * and the last file returned was
     * <p>
     * <code>/tmp/root/2005/01_jan/something/file.txt</code>
     * <p>
     * Then the DataBlock for that file will cover the month of January 2005.  However,
     * if the last file returned was
     * <p>
     * <code>/tmp/root/something/2005/01_jan/file.txt</code>
     * <p>
     * Then the DataBlock for that file will cover the entire span of time representable
     * by a TimeBlock because the first directory searched, "something", does not have an
     * index and thus the DirSpider will be unable to determine the containing TimeBlock.
     * In such cases, you may try using the getTimeFromFile() method, which makes a best
     * guess as to the root of the indexed directory tree and operates from there.  In
     * contrast, this method assumes that the rootFile provided by the user is the root
     * of the indexed directory tree. 
     * 
     * @return TimeBlock associated with the last file returned by this method.  If the
     *               last retrieval operation was aborted, the block returned is
     *               undefined.
     */
    public TimeBlock getDirectoryTime() {
        if(mCurrentFile == null)
            return null;
        
        return TimeBlock.fromMicros(mCurrentStartMicros, mCurrentStopMicros);
    }
    
    
    
    private File[] getFiles(File dir) {
        File[] files = dir.listFiles(mFileFilter);
        if(files == null)
            files = new File[0];
        
        Arrays.sort(files, mFileComp);
        return files;
    }
    
    private File[] getDirs(File dir) {
        File[] dirs = dir.listFiles(mDirFilter);
        if(dirs == null)
            dirs = new File[0];
        Arrays.sort(dirs, mDirComp);
        return dirs;
    }
    
    private DirCache pushNewDirectory(File dir) {
        DirCache cache = pushEmpty();
        
        cache.mFiles = getFiles(dir);
        cache.mFileIndex = 0;
        cache.mDirs = getDirs(dir);
        cache.mDirIndex = -1;
        
        if(mStackSize > 1) {
            DirCache prev = mStack[mStackSize-2];
            int index = parseDirectoryIndex(dir);
            
            if(prev.mTimeLevel > TimeDir.TIME_LEVEL_MINUTE || index < 0) {
                cache.mTimeLevel = TimeDir.TIME_LEVEL_NONE;
                cache.mStartMicros = prev.mStartMicros;
                cache.mStopMicros = prev.mStopMicros;
            
            }else{
                cache.mTimeLevel = prev.mTimeLevel + 1;
                int calLevel = TimeDir.getCalendarLevel(cache.mTimeLevel);
                int calValue = TimeDir.convertDirectoryIndexToCalendarValue(cache.mTimeLevel, index);
                mCalendar.set(calLevel, calValue);
                cache.mStartMicros = mCalendar.getTimeInMillis() * 1000l;
                mCalendar.add(calLevel, 1);
                cache.mStopMicros = mCalendar.getTimeInMillis() * 1000l;
                mCalendar.add(calLevel, -1);
            }

        }else{
            cache.mTimeLevel = TimeDir.TIME_LEVEL_ALL;
            cache.mStartMicros = 0l;
            cache.mStopMicros = Long.MAX_VALUE;
        }
        
        return cache;
    }
    
    private DirCache pushNewDirectoryReversed(File dir) {
        DirCache cache = pushNewDirectory(dir);
        cache.mFileIndex = cache.mFiles.length;
        cache.mDirIndex = cache.mDirs.length;
        return cache;
    }
    
    private DirCache pushEmpty() {
        if(mStackSize == mStack.length) {
            DirCache[] stack = new DirCache[mStack.length * 2];
            System.arraycopy(mStack, 0, stack, 0, mStack.length);
            mStack = stack;
        }
        
        if(mStack[mStackSize] == null) {
            mStack[mStackSize] = new DirCache();
        }
        
        return mStack[mStackSize++];
    }
       
    private boolean pop() {
        if(mStackSize <= 1)
            return false;

        DirCache cache = mStack[--mStackSize];
        
        if(cache.mTimeLevel >= TimeDir.TIME_LEVEL_YEAR && cache.mTimeLevel <= TimeDir.TIME_LEVEL_MINUTE) {
            mCalendar.set( TimeDir.getCalendarLevel(cache.mTimeLevel),
                           TimeDir.getMinimumCalendarValue(cache.mTimeLevel));
            
        }
        
        return true;
    }

    private void getReady(boolean interruptable) throws InterruptedException {
        switch(mState) {
        
        case STATE_GOTO_CURRENT:
            if(mCurrentFile != null) {
                doGotoFile(mCurrentFile, interruptable, mCurrentIsForward);
                break;
            }
            
            doReset();
            break;
            
        case STATE_GOTO_FILE_START:
            if(mGotoFile != null) {
                doGotoFile(mGotoFile, interruptable, false);
                break;
            }
            
            doReset();
            break;
            
        case STATE_GOTO_FILE_END:
            if(mGotoFile != null) {
                doGotoFile(mGotoFile, interruptable, true);
                break;
            }
            
            doReset();
            break;
            
        case STATE_RESET:
            doReset();
            break;
            
        case STATE_GOTO_END:
            doGotoEnd();
            break;
            
        case STATE_GOTO_TIME:
            doGotoMicros(mGotoMicros, interruptable);
            break;
        
        default:
        }
    }
    
    private void doClear() {
        mStackSize = 0;
        TimeDir.zeroCalendar(mCalendar);
        mCurrentFile = null;
    }
    
    private void doReset() {
        doClear();
        pushNewDirectory(mRootFile);
    }
    
    private void doGotoEnd() {
        doClear();
        pushNewDirectoryReversed(mRootFile);
    }
    
    private void doGotoMicros(long micros, boolean interruptable) throws InterruptedException {
        
        //Go backward through stack until containing directory is found.
        for(int i = mStackSize - 1; i >= 0; i--) {
            if(mStack[i].mStartMicros <= micros && mStack[i].mStopMicros > micros) {

                //We need to stop at the minute level.
                while(mStackSize > 0 && mStack[mStackSize-1].mTimeLevel > TimeDir.TIME_LEVEL_MINUTE) {
                    pop();
                }

                break;
            }

            pop();
        }
        
        if(mStackSize == 0) {
            pushNewDirectory(mRootFile);

        }else{
            DirCache cache = mStack[mStackSize - 1];
            cache.mFileIndex = 0;
            cache.mDirIndex = -1;
            
            if(cache.mTimeLevel == TimeDir.TIME_LEVEL_MINUTE)
                return;
        }        
        
        //Use a calendar to determine which directory to go to.
        mGotoCalendar.setTimeInMillis(micros / 1000L);
        
GOTO_LEVEL_LOOP:
        for( int level = mStack[mStackSize-1].mTimeLevel + 1; 
                 level <= TimeDir.TIME_LEVEL_MINUTE; 
                 level++ )
        {
            
            if(interruptable && Thread.interrupted())
                throw new InterruptedException();
            
            int targetIndex = TimeDir.convertCalendarValueToDirectoryIndex(
                                            level,
                                            mGotoCalendar.get(TimeDir.getCalendarLevel(level)));
            
            DirCache cache = mStack[mStackSize-1];
            File[] files = cache.mFiles;
            File[] dirs = cache.mDirs;
            
            //We only descend into the next directory if we find the exact targetIndex.
            for(int i = 0; i < dirs.length; i++) {
                Matcher m = DIR_INDEX_PATTERN.matcher(dirs[i].getName());
            
                if(m.find()) {
                    int dirIndex = Integer.parseInt(m.group(1));
                    
                    if(dirIndex == targetIndex) {
                        //Found the exact index we were looking for.
                        //Go to the next level.
                        cache.mFileIndex = files.length;
                        cache.mDirIndex = i;
                        pushNewDirectory(dirs[i]);
                        continue GOTO_LEVEL_LOOP;
                        
                    }else if(dirIndex > targetIndex) {
                        //Passed our target time.
                        //Set pointer to beginning of directory and quit loop.
                        cache.mFileIndex = cache.mFiles.length;
                        cache.mDirIndex = i - 1;
                        break GOTO_LEVEL_LOOP;
                    }
                }
            }
            
            //If we reach the end of the directory without reaching the target,
            //we set the DirSpider state to point to the end of the directory
            //and return.
            cache.mFileIndex = files.length;
            cache.mDirIndex = dirs.length;
            break;
        }
    }
    
    private void doGotoFile(File target, boolean interruptable, boolean passTarget) throws InterruptedException {
        doClear();
        
        if(target == null || !target.exists())
            return;
        
        Stack<File> stack = new Stack<File>();
        
        //Roll back file until we get to the DirSpider root directory.
        while(!target.equals(mRootFile)) {
            stack.push(target);
            
            target = target.getParentFile();
            if(target == null)
                return;
        }
        
        if(stack.isEmpty()) {
            pushNewDirectory(target);
            return;
        }
        
        File nextTarget = stack.pop();
        
        while(true) {
            if(interruptable && Thread.interrupted())
                throw new InterruptedException();
            
            DirCache cache = pushNewDirectory(target);
        
            if(!stack.isEmpty()) {
                //Go past all the files.
                cache.mFileIndex = cache.mFiles.length;

                //Find correct index for directory, or go to last dir index.
                for(cache.mDirIndex++; cache.mDirIndex < cache.mDirs.length; cache.mDirIndex++) {
                    if(cache.mDirs[cache.mDirIndex].equals(nextTarget))
                        break;
                }
                
                target = nextTarget;
                nextTarget = stack.pop();
                                
            }else{
                //Find target file.
                while(cache.mFileIndex < cache.mFiles.length) {
                    if(cache.mFiles[cache.mFileIndex].equals(nextTarget)) {
                        if(passTarget)
                            cache.mFileIndex++;
                        return;
                    }
                    
                    cache.mFileIndex++;
                }
                
                //Or target directory, if all else fails.
                for(cache.mDirIndex++; cache.mDirIndex < cache.mDirs.length; cache.mDirIndex++) {
                    if(cache.mDirs[cache.mDirIndex].equals(nextTarget)) {
                        if(!passTarget)
                            cache.mDirIndex--;
                        
                        return;
                    }
                }
                
                //Or just go to the start of the directory and call it good enough.        
                cache.mFileIndex = 0;
                cache.mDirIndex = -1;
                break;
            }
        }
    }
    
    
    
    private static class DirCache {
        public File[] mDirs = null;
        public File[] mFiles = null;
        public int mDirIndex = 0;
        public int mFileIndex = 0;
        
        public int mTimeLevel = TimeDir.TIME_LEVEL_NONE;        
        public long mStartMicros = -1l;
        public long mStopMicros = -1l;
    }
    
}
