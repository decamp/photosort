/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.photosort;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.text.*;


public class TimestampReader {

    
    public static void main(String[] args) {
        try{
            test1();
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    
    private static void test1() throws Exception {
        //String path = "/Volumes/DATA2/hsp_data/forgeorge/living_pics/039.jpg";
        String path = "/Users/decamp/Photos/FILE0001.JPG";
        ByteBuffer buf = bufferFile(new File(path));
        long timestamp = readJpegTimestampMicros(buf);
        
        System.out.println(timestamp);
        
        if(timestamp != Long.MIN_VALUE) {
            System.out.println(new java.util.Date(timestamp / 1000L));
        }
    }
    
    
    private static ByteBuffer bufferFile(File file) throws IOException {
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
     * 
     * Once we have the ExifMarker data, we need to delve into the where the timestamp
     * should be kept.  This is a fairly grueling process because we have to go three layers
     * deep.
     * 
     * JPEG Marker 
     *   |
     *   -- TIFF Header
     *       |
     *       -- IFD0
     *           |
     *           -- SubIFD
     *               |
     *               -- MakerNote IFD
     *                   |
     *                   -- Timestamp
     */
    public static long readJpegTimestampMicros(ByteBuffer buf) throws IOException {
        buf = readExifSegment(buf);
        if(buf == null)
            return Long.MIN_VALUE;
        
        buf = readTiffHeader(buf);
        if(buf == null)
            return Long.MIN_VALUE;
        
        int offset = findIfdTagOffset(buf, 0x8769);
        if(offset < 0)
            return Long.MIN_VALUE;
        
        int off2 = findIfdTagOffset(buf, 0x9003, offset);
        if(off2 >= 0) {
            if(buf.position() + off2 + 20 > buf.limit())
                return -1;
            
            DateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
            buf.position(buf.position() + off2);
            byte[] bytes = new byte[20];
            buf.get(bytes);
            String dateString = new String(bytes);
            
            try{
                return format.parse(dateString).getTime() * 1000L;
            }catch(ParseException ex) {
                return Long.MIN_VALUE;
            }
        }
        
        offset = findIfdTagOffset(buf, 0x927C, offset);
        if(offset < 0)
            return Long.MIN_VALUE;
        
        offset = findIfdTagOffset(buf, 0xFDE8, offset);
        if(offset < 0)
            return Long.MIN_VALUE;
        
        if(offset + 8 > buf.remaining())
            return Long.MIN_VALUE;
        
        buf.position(buf.position() + offset);
        int sec = buf.getInt();
        int usec = buf.getInt();
        
        return sec * 1000000L + usec;
    }
    
    /**
     * @param buf Buffer containing JPEG.
     * @return buffer containing EXIF segment.
     */
    private static ByteBuffer readExifSegment(ByteBuffer buf) {
        buf = buf.duplicate();
        
        //Loop through each byte in JPEG and find markers.
        while(buf.remaining() >= 4) {
            int b = (buf.get() & 0xFF);
            
            //Markers always start with 0xFF.  However, they may start with multiple 0xFFs.
            while(b == 0xFF) {
                if(buf.remaining() < 3)
                    return null;
                
                b = (buf.get() & 0xFF);
                
                //Another 0xFF means the marker is still starting.
                //0x00 means the previous 0xFF was not a marker.
                //0xD8 and 0xD9 indicate SOE and EOF (StartOfImage or EndOfImage), which are not segments.
                if(b == 0xFF || b == 0x00 || b == 0xD8 || b == 0xD9)
                    continue;
                
                //After the marker (0xFF + one-byte ID), the next two bytes indicate the segment length.
                //The segment length includes itself (two bytes), but not the two-byte marker.
                int length = (buf.getShort() & 0xFFFF) - 2;
                
                //Check if we have all the data.
                if(length > buf.remaining())
                    return null;
                
                //Check if this is the exif segment.
                if(b == 0xE1) {
                    buf.limit(buf.position() + length);
                    return buf;
                }
                
                buf.position(buf.position() + length);
            }
        }
        
        return null;
    }

    /**
     * @param buf Buffer containing EXIF segment.
     * @return ByteBuffer containing IFD0.
     */
    private static ByteBuffer readTiffHeader(ByteBuffer buf) {
        if(buf.remaining() < 6)
            return null;
        
        buf = buf.duplicate();
        
        if(buf.get() == 'E' && buf.get() == 'x' && buf.get() == 'i' && buf.get() == 'f' && 
           buf.get() == 0 && buf.get() == 0)
        {     
            return buf;
        }
        
        return null;
    }
     
    /**
     * @param buf Buffer containing IFD0
     * @return timestamp micros, or Long.MIN_VALUE if not found.
     */
    private static long findTimestampMicros(ByteBuffer buf) {
        int offset = findIfdTagOffset(buf, 0x8769);
        if(offset < 0)
            return Long.MIN_VALUE;
        
        offset = findIfdTagOffset(buf, 0x927C, offset);
        if(offset < 0)
            return Long.MIN_VALUE;
        
        offset = findIfdTagOffset(buf, 0xFDE8, offset);
        if(offset < 0)
            return Long.MIN_VALUE;
        
        if(offset + 8 > buf.remaining())
            return Long.MIN_VALUE;
        
        buf = buf.duplicate();
        buf.position(buf.position() + offset);
        int sec = buf.getInt();
        int usec = buf.getInt();
        
        return sec * 1000000L + usec;
    }
    
    /**
     * @param buf Buffer containing IFD0
     * @param tag Tag of entry to locate.
     * @return offset of entry, or -1 if not found.
     */
    private static int findIfdTagOffset(ByteBuffer buf, int tag) {
        return findIfdTagOffset(buf, tag, -1);
    }
    
    /**
     * @param buf Buffer containing IFD0
     * @param tag Tag of entry to locate.
     * @param offset Offset of index to use, or -1 if first index should be used.
     * @return offset to entry, or -1 if not found.
     */
    private static int findIfdTagOffset(ByteBuffer buf, int tag, int offset) {
        if(buf.remaining() < 8)
            return -1;
        
        buf = buf.duplicate();
        int start = buf.position();
        int length = buf.remaining();
        
        //2-byte indicator of byte alignment indicator.  
        //MM means Motoral (Big-Endian).  
        //II means Intel (Little-Endian).
        {
            byte order = buf.get();
            if(order != buf.get())
                return -1;
            
            if(order == (byte)0x4D) {
                buf.order(ByteOrder.BIG_ENDIAN);
            }else if(order == (byte)0x49) {
                buf.order(ByteOrder.LITTLE_ENDIAN);
            }else{
                return -1;
            }
        }
        
        //2-byte constant.
        if((buf.getShort() & 0xFFFF) != 0x002A)
            return -1;
        
        //If no offset is given, use 4-byte offset to SubIFD tag.
        if(offset < 0) 
            offset = buf.getInt();
        
        //Check offset validity.
        if(offset < 0 || length < offset + 2)
            return -1;
        
        //Get number of entries.
        buf.position(start + offset);
        int entryCount = (buf.getShort() & 0xFFFF);
        
        //Check length validity.
        if(length < offset + 2 + 12 * entryCount)
            return -1;
        
        //Find the entry.
        for(int i = 0; i < entryCount; i++) {
            buf.position(start + offset + 2 + i * 12);
            if((buf.getShort() & 0xFFFF) == tag) {
                buf.position(start + offset + 2 + i * 12 + 8);
                return buf.getInt();
            }
        }
        
        return -1;
    }
    
    
}