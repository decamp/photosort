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
 * @author Philip DeCamp  
 */
public class NameFormatter {

    public static final String DEFAULT_FILE_PATTERN = "%d(yyyy)/%d(yyyy_MM_dd)/%d(yyyy_MM_dd-HHmmss).%e".replace("/", File.separator);
    public static final String DEFAULT_UNDATED_PATTERN = "undated/%n.%e".replace("/", File.separator);
    
    public static NameFormatter compile(String pattern) throws IllegalArgumentException {
        Pattern argPat = Pattern.compile("^\\(([^\\)]*+)\\)"); 
        Matcher m = Pattern.compile("[^\\%]+|\\%").matcher(pattern);
        
        List<String> tokens = new ArrayList<String>();
        List<TokenFormatter> formatters = new ArrayList<TokenFormatter>();
        TokenFormatter[] arr = TokenFormatter.values();
        
        while(m.find()) {
            if(m.group(0).startsWith("%")) {
                int off = m.end();
                int len = pattern.length();
                
                if(len - off < 1)
                    throw new IllegalArgumentException("Invalid pattern: ends with %");
                
                String token = "%" + pattern.charAt(off++);
                
                for(TokenFormatter ff: arr) {
                    if(!ff.token().equals(token))
                        continue;
                    
                    if(ff.needsArgument()) {
                        if(len - off < 2)
                            throw new IllegalArgumentException("Invalid pattern: missing argument: " + ff.token() + ")");
                        
                        Matcher mm = argPat.matcher(pattern.substring(off));
                        if(!mm.find())
                            throw new IllegalArgumentException("Invalid pattern: missing argument: " + ff.token() + ")");
                
                        tokens.add(mm.group(1));
                        formatters.add(ff);
                        off += mm.end();
                        
                    }else{
                        tokens.add(null);
                        formatters.add(ff);
                    }
                    
                    break;
                }
                
                m.region(off, len);
                
            }else{
                tokens.add(m.group(0));
                formatters.add(null);
            }
        }
        
        return new NameFormatter(tokens, formatters);
    }
    
    
    private final List<String> mTokens;
    private final List<TokenFormatter> mFormatters;
        
    
    public NameFormatter(List<String> tokens, List<TokenFormatter> formatters) {
        mTokens = tokens;
        mFormatters = formatters;
    }
    
    
    public String format(File source, File targetDir, long micros) {
        StringBuilder sb = new StringBuilder();
        
        for(int i = 0; i < mTokens.size(); i++) {
            TokenFormatter formatter = mFormatters.get(i);
            String token = mTokens.get(i);
            
            if(formatter == null) {
                sb.append(token);
            }else{
                sb.append(formatter.format(source, token, targetDir, micros));
            }
        }
        
        
        return sb.toString();
    }
    
}
