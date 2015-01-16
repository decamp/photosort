/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.photosort;

import java.io.*;
import java.nio.*;
import java.util.*;
import bits.progress.*;

public class PhotoSort implements ProgressTask {


    public static void main( String[] args ) {
        String inputPath = null;
        String outputPath = null;
        boolean move = false;
        String unsortedPath = null;
        String namingPattern = null;

        for( int i = 0; i < args.length; i++ ) {
            if( args[i].startsWith( "-" ) ) {
                if( args[i].startsWith( "-h" ) ) {
                    printUsage( true );

                } else if( args[i].startsWith( "-m" ) ) {
                    move = true;

                } else if( args[i].startsWith( "-u" ) ) {
                    if( i >= args.length - 1 ) {
                        printUsage( true );
                    }

                    unsortedPath = args[++i];
                } else if( args[i].startsWith( "-n" ) ) {
                    if( i >= args.length - 1 ) {
                        printUsage( true );
                    }

                    namingPattern = args[++i];
                }

            } else if( inputPath == null ) {
                inputPath = args[i];

            } else if( outputPath == null ) {
                outputPath = args[i];

            } else {
                printUsage( true );
            }
        }

        if( inputPath == null || outputPath == null ) {
            printUsage( true );
            return;
        }

        PhotoSort sorter = new PhotoSort();
        sorter.setSource( new File( inputPath ) );
        sorter.setTarget( new File( outputPath ) );
        sorter.enableMove( move );
        sorter.setNameFormatter( NameFormatter.compile( namingPattern ) );
        sorter.setUndatedNameFormatter( NameFormatter.compile( NameFormatter.DEFAULT_UNDATED_PATTERN ) );

        sorter.startTask( null );
    }
    
    public static void printUsage(boolean exit) {
        System.out.println("PhotoSort <input_path> <output_path> [-hm] [-u <unsorted folder>] [-n <name pattern>");
        System.out.println("\t-h == see this helpful message");
        System.out.println("\t-m == move files instead of copying them");
        System.out.println("\t-n == specify naming pattern (Default: " + NameFormatter.DEFAULT_FILE_PATTERN + ")");
        
        for(TokenFormatter tf: TokenFormatter.values())
            System.out.format("\t\t%s == %s\n", tf.token(), tf.description());
        
        System.out.println("\t-u == specify naming pattern for files without timestamps (Default: " + NameFormatter.DEFAULT_UNDATED_PATTERN + ")");
        
        if(exit) {
            System.exit( 0 );
        }
    }
    
    
    private boolean mMove = false;
    private String mOpGerund = "Copying ";
    private File mSource = null;
    private File mTarget = null;
    private NameFormatter mFormatter = null;
    private NameFormatter mUndatedFormatter = null;
    
    private Thread mThread = null;
    
    
    
    public void setSource(File source) {
        mSource = source;
    }
    
    public void setTarget(File target) {
        mTarget = target;
    }
    
    public void enableMove(boolean move) {
        mMove = move;
        mOpGerund = (mMove ? "Moving " : "Copying ");
    }
    
    public void setNameFormatter(NameFormatter formatter) {
        mFormatter = formatter;
    }
    
    public void setUndatedNameFormatter(NameFormatter formatter) {
        mUndatedFormatter = formatter;
    }
    
    
    public void startTask(ProgressListener monitor) {
        if(monitor == null)
            monitor = new ProgressAdapter();
        
        final ProgressListener mon = monitor;
        
        synchronized(this) {
            if(mThread != null)
                throw new IllegalMonitorStateException("Internal error: multiple sorting threads.");
                
            mThread = new Thread() {
                public void run() {
                    try{
                        executeSort(mon);
                    }finally{
                        synchronized(PhotoSort.this) {
                            mThread = null;
                        }
                    }
                }
            };
                
            mThread.start();
        }
    }
    
    public synchronized void cancelTask() {
        if(mThread != null) {
            mThread.interrupt();
        }
    }
    
    
    private void executeSort(final ProgressListener monitor) {

        if(mSource == null) {
            monitor.exceptionOccurred(new Exception("No source folder specified."));
            return;
        }
        
        if(mTarget == null) {
            monitor.exceptionOccurred(new Exception("No target folder specified."));
            return;
        }
        
        if(!mSource.exists()) {
            monitor.exceptionOccurred(new Exception("Source folder does not exist."));
            return;
        }
        
        monitor.setNote("Locating files...");
        SortStats stats = new SortStats();
        List<File> inList = null;
        
        try{
            inList = findInputFiles();
            if(Thread.interrupted())
                throw new InterruptedException();
        }catch(InterruptedException ex) {
            monitor.taskCancelled("Cancelled", stats.toString());
        }

        stats.mFiles = inList.size();
        monitor.setNote("Sorting...");
        monitor.setMinimum(0);
        monitor.setMaximum(inList.size());
        
        for(int i = 0; i < inList.size(); i++) {
            monitor.setProgress(i, mOpGerund + i + " of " + inList.size());
            
            try{
                sortFile(inList.get(i), monitor, stats);
                if(Thread.interrupted())
                    throw new InterruptedException();
            }catch(InterruptedException ex) {
                monitor.taskCancelled("Cancelled", "");
                monitor.addInfo(stats.toString());
                
                return;
            }
        }
        
        monitor.taskFinished("Complete", "");
        monitor.addInfo(stats.toString());
    }
    
    private List<File> findInputFiles() throws InterruptedException {
        List<File> ret = new ArrayList<File>();
        
        if(mSource.isFile()) {
            if(FileUtil.ALL_FILTER.accept(mSource))
                ret.add(mSource);
            
            return ret;
        }
        
        DirSpider spider = new DirSpider(mSource, FileUtil.ALL_FILTER);
        for(File f = spider.getNextFile(true); f != null; f = spider.getNextFile(true)) {
            ret.add(f);
        }
        
        return ret;
    }
    
    private void sortFile(File source, ProgressListener monitor, SortStats stats) throws InterruptedException {
        ByteBuffer in = null;
        ByteBuffer out = null;
        
        try{
            in = FileUtil.bufferFile(source);
            long micros = TimestampReader.readJpegTimestampMicros(in);
            NameFormatter format = null;
            
            if(micros != Long.MIN_VALUE) {
                if(mFormatter == null)
                    mFormatter = NameFormatter.compile(NameFormatter.DEFAULT_FILE_PATTERN);
                
                format = mFormatter;
            }else{
                if(mUndatedFormatter == null)
                    mUndatedFormatter = NameFormatter.compile(NameFormatter.DEFAULT_UNDATED_PATTERN);
                
                format = mUndatedFormatter;
            }
            
            File target = new File(mTarget, format.format(source, mTarget, micros));
            File targetDir = target.getParentFile();
            String[] parts = splitFile(target.getName());
            FileIter iter = new FileIter(targetDir, parts[0], parts[1]);
            
            while(true) {
                target = iter.next();
                
                if(!target.exists())
                    break;
                
                if(!FileUtil.diff(target, in)) {
                    stats.mDuplicates++;
                    return;
                }
            }
            
            transferFile(source, target, monitor, stats);
            if(micros == Long.MIN_VALUE)
                stats.mUndated++;
            
        }catch(InterruptedIOException ex) {
            throw new InterruptedException();
            
        }catch(IOException ex) {
            if(Thread.interrupted())
                throw new InterruptedException();
            
            stats.mFailed++;
            String msg = ex.getMessage();
            if(msg == null)
                msg = ex.getClass().getName();
            
            monitor.addInfo("Failed to sort \"" + source.getPath() + "\": " + msg + "\n");
        }
    }
    
    private void transferFile(File source, File target, ProgressListener monitor, SortStats stats) throws IOException {
        File targetDir = target.getParentFile();
        if(!targetDir.exists()) {
            if(!targetDir.mkdirs())
                throw new IOException("Failed to make output directory");
        }
        
        if(mMove) {
            FileUtil.moveFile(source, target);
            System.out.println(source.getPath() + " m> " + target.getPath());
            stats.mMoved++;
        }else{
            FileUtil.copyFile(source, target);
            System.out.println(source.getPath() + " c> " + target.getPath());
            stats.mCopied++;
        }        
    }
    
    private String[] splitFile(String name) {
        int idx = name.lastIndexOf('.');
        
        if(idx < 0) 
            return new String[]{name, ".jpg"};
        
        String ext = name.substring(idx).toLowerCase();
        if(ext.equals(".jpeg"))
            ext = ".jpg";
        
        return new String[]{name.substring(0, idx), ext};
    }
    
}
