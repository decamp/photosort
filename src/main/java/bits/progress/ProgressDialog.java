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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/** 
 * @author Philip DeCamp  
 */
public class ProgressDialog extends JFrame {

    private final ProgressTask mTask;
    private final ProgressListener mProgressListener;
    private final Component mParent;
    private final boolean mExitOnClose;
    
    private boolean mCancelled = false;
    private boolean mCompleted = false;

    private JLabel mNote;
    private JLabel mSubnote;
    private JProgressBar mBar;
    private JTextPane mInfo;
    private JButton mButton;
    
    
    public ProgressDialog(Component parent, ProgressTask task, String title) {
        this(parent, task, title, false);
    }
        
    public ProgressDialog(Component parent, ProgressTask task, String title, boolean exitOnClose) {
        super(title);
        mParent = parent;
        mTask = task;
        mExitOnClose = exitOnClose;
        mProgressListener = new MyProgressListener();
        initLayout();
    }
    
    
    private void initLayout() {
        final JPanel panel = new JPanel();
        setContentPane(panel);
        
        mNote = new JLabel();
        panel.add(mNote);
        
        mSubnote = new JLabel();
        panel.add(mSubnote);
        
        mBar = new JProgressBar();
        panel.add(mBar);
        
        mInfo = new JTextPane();
        mInfo.setEditable(false);
        mInfo.setDocument(new DefaultStyledDocument());
        final JScrollPane infoScroll = new JScrollPane(mInfo);
        panel.add(infoScroll);
        
        mButton = new JButton("Cancel");
        mButton.setSize(100, 35);
        panel.add(mButton);
        
        mButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                new Thread(){
                    public void run(){
                        buttonPressed();
                    }
                }.start();
            }
        });
        
        
        panel.setLayout(new LayoutAdapter(){
            public void layoutContainer(Container cont){
                int width = panel.getWidth();
                int height = panel.getHeight();
                
                int y = 4;
                int x = 10;
                mNote.setBounds(x, y, width - x * 2, 25);
                
                y += 30;
                mSubnote.setBounds(x, y, width - x * 2, 25);
                
                y += 30;
                mBar.setBounds(x, y, width - x * 2, 25);
                
                y += 30;
                infoScroll.setBounds(x, y, width - x * 2, height - y - 50);
                
                y += infoScroll.getHeight() + 5;
                mButton.setBounds(x, y, 100, 35);
            }
        });
        
        
        if(mParent == null) {
            setBounds(200, 200, 600, 600);
            
        }else{
            setBounds( mParent.getX() + mParent.getWidth() / 2 - 200,
                       mParent.getY() + mParent.getHeight() / 2 - 150,
                       600,
                       600 );
        }
        
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                ProgressDialog.this.windowClosing();
            }
        });
    }

    public void startTask() {
        setVisible(true);
        mTask.startTask(mProgressListener);
    }
    
    public ProgressListener getProgressListener() {
        return mProgressListener;
    }
        
    private void buttonPressed() {
        synchronized(this){
            if(mCompleted || mCancelled) {
                if(mExitOnClose)
                    System.exit(0);
                
                setVisible(false);
                return;
            }
            
            mCancelled = true;
        }
        
        mNote.setText("CANCELLING");
        mButton.setText("OK");
        mTask.cancelTask();
    }

    private void windowClosing() {
        synchronized(this){
            if(mCompleted || mCancelled) {
                if(mExitOnClose)
                    System.exit(0);
                
                setVisible(false);
                return;
            }
        }
        
        if(JOptionPane.showConfirmDialog( this, 
                                          "Are you sure you want to cancel?", 
                                          "Terminating Task", 
                                          JOptionPane.YES_NO_OPTION) 
                                              == JOptionPane.YES_OPTION)
        {
            buttonPressed();
            
            if(mExitOnClose)
                System.exit(0);
            
            setVisible(false);
        }
    }
    
    
    
    private class MyProgressListener implements ProgressListener { 
        
        public void exceptionOccurred(Exception ex) {
            taskCancelled(mNote.getText() + " - CANCELLED!!!", null);
            JOptionPane.showMessageDialog(getParent(), ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        public boolean isCancelled() {
            return mCancelled;
        }
        
        public void setMaximum(int max) {
            mBar.setMaximum(max);
        }
        
        public void setMinimum(int min) {
            mBar.setMinimum(min);
        }
        
        public void setProgress(int progress) {
            mBar.setValue(progress);
            mSubnote.setText((progress - mBar.getMinimum()) + " out of " + (mBar.getMaximum() - mBar.getMinimum()));
        }

        public void setProgress(int progress, String message) {
            mBar.setValue(progress);
            mSubnote.setText(message);
        }

        public void addInfo(String info) {
            try{
                int length = mInfo.getDocument().getLength();
                
                if(length > 10000){
                    length /= 2;
                    mInfo.getDocument().remove(0, length);
                }
                
                mInfo.getDocument().insertString(length, info, null);
            }catch(BadLocationException ex) {
                ex.printStackTrace();
                mInfo.setDocument(new DefaultStyledDocument());
            }
        }
        
        public void setNote(String text) {
            mNote.setText(text);
        }
    

        public void taskFinished(String note, String subnote) {
            synchronized(ProgressDialog.this){
                mCompleted = true;
                mButton.setText("OK");
            }
            
            if(note != null)
                mNote.setText(note);
            
            if(subnote != null)
                mSubnote.setText(subnote);
        }

        public void taskCancelled(String note, String subnote) {
            taskFinished(note, subnote);
        }
    }

}