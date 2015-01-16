/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.photosort;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/** 
 * @author Philip DeCamp  
 */
enum TokenFormatter {
    
    FILE_NAME    ("%n", false, "Name of source file (without extension)") {
        String format(File source, String pattern, File targetDir, long micros) {
            String name = source.getName();
            int idx = name.lastIndexOf('.');
            if(idx < 0)
                return name;
            
            return name.substring(0, idx);
        }
    },
    
    FILE_EXT     ("%e", false, "Extension of source file (eg. jpg, png)") {
        String format(File source, String pattern, File targetDir, long micros) {
            String name = source.getName();
            int idx = name.lastIndexOf('.');
            if(idx < 0)
                return "";
            
            return name.substring(idx + 1);
        }
    },
    
    DATE         ("%d", true, "Date pattern a la SimpleDateFormat.  (eg. %d(YYYY-mm-dd_HHmmss))") {
        String format(File source, String pattern, File targetDir, long micros) {
            DateFormat f = new SimpleDateFormat(pattern);
            return f.format(new Date(micros / 1000L));
        }
    },
    
    PARENT       ("%p", false, "Parent directory of source file") {
        String format(File source, String pattern, File targetDir, long micros) {
            File parent = source.getParentFile();
            if(parent == null)
                return "";
            
            return parent.getName();
        }
    },
    
    ESCAPE       ("%%", false, "Percent sign") {
        String format(File source, String pattern, File targetDir, long micros) {
            return "%";
        }
    };
    
    
    private final String mToken;
    private final String mDescription;
    private final boolean mNeedsArg;
    
    TokenFormatter(String token, boolean needsArg, String description) {
        mToken = token;
        mNeedsArg = needsArg;
        mDescription = description;
    }
    
    
    public String token() {
        return mToken;
    }
    
    public String description() {
        return mDescription;
    }   
    
    public boolean needsArgument() {
        return mNeedsArg;
    }
    
    abstract String format(File input, String pattern, File targetDir, long micros);
    
}