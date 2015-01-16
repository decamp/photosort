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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


/** 
 * @author Philip DeCamp  
 */
public class FileUtil {

    public static final FileFilter JPEG_FILTER = new FileFilter() {
        public boolean accept(File file) {
            if(file.isHidden())
                return false;
            
            String name = file.getName().toLowerCase();
            return name.endsWith(".jpg") || name.endsWith(".jpeg");
        }
    };
    
    public static final FileFilter ALL_FILTER = new FileFilter() {
        public boolean accept(File file) {
            return file.isFile() && !file.isHidden();
        }
    };
    
    private static final String COPY_COMMAND;
    private static final String MOVE_COMMAND;
    
    static {
        String osName = System.getProperty("os.name");
        String copyCommand;
        String moveCommand;
        
        if(osName == null || osName.toLowerCase().contains("win")){
            copyCommand = "copy";
            moveCommand = "move";
        }else{
            copyCommand = "cp";
            moveCommand = "mv";
        }
        
        COPY_COMMAND = copyCommand;
        MOVE_COMMAND = moveCommand;
    }

    
    /**
     * Puts the whole stream into the string. 
     * Don't do this if the stream is too big, obviously.
     * @throws IOException
     */
    public static String readStream(Reader r) throws IOException {
        StringBuffer buffer = new StringBuffer();
        BufferedReader br = new BufferedReader(r);
        String line;
        while ((line = br.readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();
    }

    public static ByteBuffer bufferFile(File file) throws IOException {
        long size = file.length();
        
        ByteBuffer buf = ByteBuffer.allocate((int)(size & 0x7FFFFFFF));
        FileChannel chan = new FileInputStream(file).getChannel();
        while(buf.remaining() > 0) {
            int n = chan.read(buf);
            if(n <= 0)
                throw new IOException("Read operation failed.");
        }
        
        chan.close();
        buf.flip();
        return buf;
    }
    
    
    /**
     * Copies source file to target file.
     * 
     * @param source - Source file.
     * @param target - Target file.
     * @throws InterruptedIOException if thread is interrupted.  Attempts to delete partial file if this occurs.
     * @throws IOException if writing fails. 
     */
    public static void copyFile(File source, File target) throws IOException {
        Process p = Runtime.getRuntime().exec(new String[]{
                COPY_COMMAND,
                source.getAbsolutePath(),
                target.getAbsolutePath()
        });
        
        try{
            int ret = p.waitFor();
        
            if(ret != 0)
                throw new IOException("Failed to copy " + source.getAbsolutePath() + " to " + target.getAbsolutePath());
        
        }catch(InterruptedException ex) {
            p.destroy();
            target.delete();
            throw new InterruptedIOException(ex.getMessage());
        }
    }
    
    public static void moveFile(File source, File target) throws IOException {
        Process p = Runtime.getRuntime().exec(new String[]{
                MOVE_COMMAND,
                source.getAbsolutePath(),
                target.getAbsolutePath()
        });
        
        try{
            int ret = p.waitFor();
            
            if(ret != 0)
                throw new IOException("Failed to move " + source.getAbsolutePath() + " to " + target.getAbsolutePath());
            
        }catch(InterruptedException ex) {
            p.destroy();
            throw new InterruptedIOException(ex.getMessage());
        }
        
    }

    
    
    
    public static boolean diff(File f1, File f2) throws IOException {
        if(f1 != null && !f1.exists())
            f1 = null;
        
        if(f2 != null && !f2.exists())
            f2 = null;
        
        if(f1 == f2 || f1 != null && f1.equals(f2))
            return false;
        
        if(f1 == null)
            return f2 != null && f2.length() != 0;
        
        if(f2 == null)
            return f1 != null && f1.length() != 0;
        
        long len = f1.length();
        
        if(len != f2.length())
            return true;

        DataInputStream s1 = new DataInputStream(new BufferedInputStream(new FileInputStream(f1)));
        DataInputStream s2 = new DataInputStream(new BufferedInputStream(new FileInputStream(f2)));
        
        long p = 0;
        
        for(; p < len - 7; p += 8) {
            if(s1.readLong() != s2.readLong())
                return true;
        }
        
        for(; p < len; p++) {
            if(s1.read() != s2.read())
                return true;
        }

        s1.close();
        s2.close();
        return false;
    }
    
    public static boolean diff(File f1, ByteBuffer b2) throws IOException {
        if(f1 != null && !f1.exists())
            f1 = null;

        if(b2 != null)
            b2 = b2.slice();
        
        if(f1 == null)
            return b2 != null && b2.remaining() != 0;
        
        if(b2 == null)
            return f1 != null && f1.length() != 0;
        
        if(f1.length() != b2.remaining())
            return true;
        
        DataInputStream s1 = new DataInputStream(new BufferedInputStream(new FileInputStream(f1)));
        final int len = b2.capacity();
        int p = 0;
        
        for(; p < len - 7; p += 8) {
            if(s1.readLong() != b2.getLong())
                return true;
        }
        
        for(; p < len; p++) {
            if(s1.read() != (b2.get() & 0xFF))
                return true;
        }
        
        s1.close();
        return false;
    }
    
    public static boolean diff(ByteBuffer b1, ByteBuffer b2) throws IOException {
        if(b1 == b2 || b1 != null && b1.equals(b2))
            return false;
        
        if(b1 == null)
            return b2 != null && b2.remaining() != 0;
        
        if(b2 == null)
            return b1 != null && b1.remaining() != 0;

        b1 = b1.slice();
        b2 = b2.slice();
        
        int len = b1.capacity();
        if(len != b2.capacity())
            return true;
        
        int p = 0;
        
        for(; p < len - 7; p += 8)
            if(b1.getLong() != b2.getLong())
                return true;
        
        for(; p < len; p++)
            if(b1.get() != b2.get())
                return true;
        
        return false;
    }
    
}
