/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.photosort;

import java.io.*;
import java.util.*;
import java.util.regex.*;


/**
 * Specification and utility for the data directory hierarchies.  Data 
 * directories use a tree structure that goes from year to month to day to
 * hour to minute.
 * <p>
 * This class is not an Enum in order to make it easier to compare and loop 
 * through different time levels.
 */
public final class TimeDir {
    
    public final static int TIME_LEVEL_ALL = 0;
    public final static int TIME_LEVEL_YEAR = 1;
    public final static int TIME_LEVEL_MONTH = 2;
    public final static int TIME_LEVEL_DAY = 3;
    public final static int TIME_LEVEL_HOUR = 4;
    public final static int TIME_LEVEL_MINUTE = 5;
    public final static int TIME_LEVEL_FILE = 6;
    public final static int TIME_LEVEL_NONE = 7;
    
    private static final Pattern DIR_INDEX_PATTERN = Pattern.compile("^(\\d++)");
    
    private static final String[] DAY_NAMES = new String[] {
        "sun", "mon", "tue", "wed", "thu", "fri", "sat"
    };
    
    private static final String[] NUMBERED_MONTH_NAMES = {
        "01_jan", "02_feb", "03_mar", 
        "04_apr", "05_may", "06_jun",
        "07_jul", "08_aug", "09_sep",
        "10_oct", "11_nov", "12_dec"
    };
    
    private static final int[] CALENDAR_LEVEL = new int[]{
        Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY, Calendar.MINUTE
    };
    
    private static final String[] LEVEL_LABEL = new String[]{
        "All", "Year", "Month", "Day", "Hour", "Minute", "File", "None"
    };
    
    private static final int[] MIN_CALENDAR_VALUE = new int[]{1970, 0, 1, 0, 0};

    
    
    /**
     * Returns a label corresponding to the directory level.  For example, if the TIME_LEVEL_HOUR
     * label is passed to getLevelLabel, it will return "Hour".
     * 
     * @param	level	Level for which a label is desired.
     * @return	The name of the provided label, or null if level is out of range. 
     */
    public static String getLevelLabel(int level) {
        if(level < 0 || level > TIME_LEVEL_NONE)
            return null;

        return LEVEL_LABEL[level];
    }

    
    /**
     * Returns the corresponding calendar time for a given time level that may be used
     * to access a java.util.Calendar field.
     * 
     * @param	timeLevel	Time level to convert to a calendar level.
     * @return	The equivalent calendar level, or -1 if out of bounds.
     */
    public static int getCalendarLevel(int timeLevel) {
        if(timeLevel < TIME_LEVEL_YEAR || timeLevel > TIME_LEVEL_MINUTE)
            return -1;

        return CALENDAR_LEVEL[timeLevel - 1];
    }

    
    /**
     * @param cal - Calendar object set to desired time.
     * @param timeLevel - A TIME_LEVEL constant.
     * @returns the directory index for the provided calendar at the provided timeLevel.
     */
    public static int getDirectoryIndex(Calendar cal, int timeLevel) {
        if(timeLevel < TIME_LEVEL_YEAR || timeLevel > TIME_LEVEL_MINUTE)
            return -1;
        
        int calValue = cal.get(CALENDAR_LEVEL[timeLevel - 1]);
        
        if(timeLevel == TIME_LEVEL_MONTH)
            return calValue + 1;
        
        return calValue;
    }
        
    
    /**
     * @param timeLevel
     * @return the smallest valid calendar level for the given time level.
     */
    public static int getMinimumCalendarValue(int timeLevel) {
        if(timeLevel < 1 || timeLevel > 5)
            return 0;
        
        return MIN_CALENDAR_VALUE[timeLevel - 1];
    }
    
    
    /**
     * Sets most valid fields in a calendar to the lowest possible level.  These
     * fields include year, month, day of month, hour of day, minute, second and 
     * millisecond.  These fields do not include day of week.
     * 
     * @param cal    A calendar object to zero.
     */
    public static void zeroCalendar(Calendar cal) {
        for(int i = TIME_LEVEL_YEAR; i <= TIME_LEVEL_MINUTE; i++)
            cal.set(CALENDAR_LEVEL[i-1], MIN_CALENDAR_VALUE[i-1]);
        
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }
    
    
    /**
     * <p>Converts a single java.util.Calendar value to a directory index.
     * </p><p>
     * In a data directory tree, "02_feb" may be a directory representing a 
     * month of data for february.  "02" is the directory index.  Directory
     * indices are prepended to directory names in order to make them easy to 
     * parse and sort.
     * </p><p>
     * The current directory index system nearly, but not exactly follows the
     * values for a java.util.Calendar, hence the need for a method to convert
     * between the two.
     * </p>  
     * 
     * @param timeLevel     A level of time.
     * @param calValue      A calendar value for the provided timeLevel.
     * @return The corresponding directory index for the provided timeLevel and calendar value. 
     */
    public static int convertCalendarValueToDirectoryIndex(int timeLevel, int calValue) {
        if(timeLevel == TIME_LEVEL_MONTH)
            return calValue + 1;
        return calValue;
    }
    
    
    /**
     * <p>Converts a single directory index into a java.util.Calendar value.
     * </p><p>
     * In a data directory tree, "02_feb" may be a directory representing a 
     * month of data for february.  "02" is the directory index.  Directory
     * indices are prepended to directory names in order to make them easy to 
     * parse and sort.
     * </p><p>
     * The current directory index system nearly, but not exactly follows the
     * values for a java.util.Calendar, hence the need for a method to convert
     * between the two.
     * </p>
     * 
     * @param timeLevel     A level of time.
     * @param index         A directory index for the provided timeLevel.
     * @return The corresponding Calendar value for the provided timeLevel and directory index.
     * 
     */
    public static int convertDirectoryIndexToCalendarValue(int timeLevel, int index) {
        if(timeLevel == TIME_LEVEL_MONTH)
            return index - 1;
        return index;
    }
    
    
    /**
     * @param cal - Calendar object containing the desired time.
     * @return a Timestamp string that can be added to a filename.
     */
    public static String getFilenameTimestamp(Calendar cal) {
        String dst = (cal.get(Calendar.DST_OFFSET) == 0)? "": "d";
        String s = String.format( "%04d_%02d_%02d-%02d%02d%s",
                                  cal.get(Calendar.YEAR),
                                  cal.get(Calendar.MONTH) + 1,
                                  cal.get(Calendar.DAY_OF_MONTH),
                                  cal.get(Calendar.HOUR_OF_DAY),
                                  cal.get(Calendar.MINUTE),
                                  dst);
        
        return s;
    }

    
    /**
     * @param cal - Calendar object set to desired time.
     * @param timeLevel - Time level of directory.
     * @return a timed directory name.
     */
    public static String getTimedDirName(Calendar cal, int timeLevel) {
        int index = TimeDir.getDirectoryIndex(cal, timeLevel);
        
        switch(timeLevel) {
        
        case TimeDir.TIME_LEVEL_YEAR:
            return String.format("%04d", index);
        
        case TimeDir.TIME_LEVEL_MONTH:
            return NUMBERED_MONTH_NAMES[index - 1];
        
        case TimeDir.TIME_LEVEL_DAY:
            return String.format("%02d_%s", index, DAY_NAMES[cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY]);
            
        case TimeDir.TIME_LEVEL_HOUR:
        case TimeDir.TIME_LEVEL_MINUTE:
            return String.format("%02d", index);

        default:
            return "";
        }
    }
    
    
    /**
     * Produces a path for an HSP style data directory.  This command does not
     * check for any existing files or directories. 
     * 
     *  @param micros - Timestamp for directory.
     *  @return a minute-level path for the provided micros.
     */
    public static String getDataPath(long micros) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(micros / 1000);
        StringBuilder builder = new StringBuilder();
        
        for(int level = TimeDir.TIME_LEVEL_YEAR; level < TimeDir.TIME_LEVEL_FILE; level++) {
            builder.append(getTimedDirName(cal, level));
            builder.append(File.separator);
        }
        
        return builder.toString();
    }


    /**
     * Creates an HSP style data directory.  This directory is only created if it
     * does not already exist.  Data is created recursively down to minute level.
     * 
     * Note that there may be some inconsistency in directory naming.  The index
     * number that appears before the directory is the only necessary component.
     * For example, this method will create a january directory as "01_jan".  
     * However, if the directory "01" already exists, this method will use that
     * existing directory.
     * 
     *  @param root - Root data directory in which to construct new directory.
     *  @param micros - Timestamp for directory.
     *  @return a minute-level directory for the provided micros, possibly new.
     *  @throws IOException if directory creation fails.
     */
    public static File createDataDirectory(File root, long micros) throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(micros / 1000);
        
        for(int level = TimeDir.TIME_LEVEL_YEAR; level < TimeDir.TIME_LEVEL_FILE; level++) {

            int index = TimeDir.getDirectoryIndex(cal, level);
            String dirName = getTimedDirName(cal, level);
            File[] subDirs = root.listFiles(new IndexFilter(index));
            File nextDir = null;
            
            if(subDirs == null || subDirs.length == 0) {
                nextDir = new File(root, dirName);
                
            }else if(subDirs.length == 1) {
                nextDir = subDirs[0];
                
            }else{
                File[] exactDirs = root.listFiles(new ExactFilter(dirName));
                
                if(exactDirs != null && exactDirs.length == 1) {
                    nextDir = exactDirs[0];
                }else{
                    nextDir = subDirs[0];
                }
            }
            
            if(!nextDir.exists() && !nextDir.mkdir())
                throw new IOException("Failed to make directory: " + nextDir);
            
            root = nextDir;
        }
        
        return root;
    }
   
    
    /**
     * This is a safe version of createDataDirectory that may be used for
     * debugging.  It does not actually create a directory.  It only prints
     * out all of the directories it would make.  The file returned by this
     * method may not actually exist.
     */
    public static File printCreateDataDirectory(File root, long micros) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(micros / 1000);
        
        for(int level = TimeDir.TIME_LEVEL_YEAR; level < TimeDir.TIME_LEVEL_FILE; level++) {

            int index = TimeDir.getDirectoryIndex(cal, level);
            String dirName = getTimedDirName(cal, level);
            File[] subDirs = root.listFiles(new IndexFilter(index));
            File nextDir = null;
            
            if(subDirs == null || subDirs.length == 0) {
                nextDir = new File(root, dirName);
                
            }else if(subDirs.length == 1) {
                nextDir = subDirs[0];
                
            }else{
                File[] exactDirs = root.listFiles(new ExactFilter(dirName));
                
                if(exactDirs != null && exactDirs.length == 1) {
                    nextDir = exactDirs[0];
                }else{
                    nextDir = subDirs[0];
                }
            }
            
            if(!nextDir.exists()) {
                System.out.println("MAKEDIR: " + nextDir.getPath());
            }
            
            root = nextDir;
        }
        
        return root;
    }


    /**
     * Produces an output directory (using createDataDirectory()), and then
     * returns a new, non-existant file within that directory for a given 
     * file.
     * 
     * @param root - Root directory in which to place file.
     * @param micros - Time of file.
     * @param channel - Channel of file.
     * @param extension - The file extension to use.
     * @return Unused file with appropriate name and parent directory.
     * @throws IOException if directory creation fails.
     */
    public static File newDataFile( File root, 
                                    long micros,
                                    int channelId,
                                    String channelName,
                                    String extension) 
                                    throws IOException 
    {
        root = createDataDirectory(root, micros);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(micros / 1000);
        String timestamp = getFilenameTimestamp(cal);
        
        String path = String.format("%02d_%s_%s%s",
                channelId,
                channelName, 
                timestamp, 
                extension);
        
        File file = new File(root, path);
        int attempt = 0;
        
        while(file.exists()) {
            path = String.format("%02d_%s_%s%s-%d",
                    channelId,
                    channelName, 
                    timestamp, 
                    extension,
                    ++attempt);
            
            file = new File(root, path);
        }
        
        return file;                                                                    
    }
    
    
    
    
    private static class IndexFilter implements FileFilter {
        private final int mIndex;
        
        public IndexFilter(int index) {
            mIndex = index;
        }
        
        public boolean accept(File file) {
            if(!file.isDirectory())
                return false;
            
            Matcher m = DIR_INDEX_PATTERN.matcher(file.getName());
            
            if(!m.find())
                return false;
            
            return mIndex == Integer.parseInt(m.group(1));
        }
    }


    private static class ExactFilter implements FileFilter {
        private final String mName;
        
        public ExactFilter(String name) {
            mName = name;
        }
        
        public boolean accept(File file) {
            return file.isDirectory() && file.getName().equals(mName);
        }
    }

}
