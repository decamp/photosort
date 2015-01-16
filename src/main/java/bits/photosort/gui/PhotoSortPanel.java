/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.photosort.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import bits.photosort.*;
import bits.progress.ProgressDialog;


/** 
 * @author Philip DeCamp  
 */
public class PhotoSortPanel extends JPanel {

    
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        PhotoSortPanel panel = new PhotoSortPanel( frame );
        frame.setContentPane( panel );
        frame.setSize( 650, 350 );
        frame.setLocationRelativeTo( null );
        frame.setVisible(true);
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    }

    private final FilePanel mSourceChooser;
    private final FilePanel mTargetChooser;
    private final JComboBox mOpBox;
    private final JTextField mPatternField;
    private final JTextField mUndatedField;
    
    private final JButton mGoButton;
    
    public PhotoSortPanel( Frame parent ) {
        int h = 32;
        setLayout(new WordLayout(4,4));
        add(WordLayout.createNoWrapMode());

        System.setProperty("apple.awt.fileDialogForDirectories", "true");
        FileDialog chooser = new FileDialog( parent );
        //JFileChooser chooser = new JFileChooser();
        //chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        mSourceChooser = new FilePanel("IN", "Select", chooser);
        mSourceChooser.setSize(100, h);
        add(mSourceChooser);
        add(WordLayout.createWallHook());
        add(WordLayout.createNewLine());
        
        mTargetChooser = new FilePanel("OUT", "Select", chooser);
        mTargetChooser.setSize(100, h);
        add(mTargetChooser);
        add(WordLayout.createWallHook());
        add(WordLayout.createNewLine());
        
        mOpBox = new JComboBox(new String[]{"COPY", "MOVE"});
        
        mOpBox.setSize(150, h);
        add(WordLayout.createCursorHorizontal(84));
        add(mOpBox);
        add(WordLayout.createNewLine());
        
        JLabel label;
        
        label = new JLabel("Normal:");
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setSize(80, h);
        add(label);
        
        mPatternField = new JTextField(NameFormatter.DEFAULT_FILE_PATTERN);
        mPatternField.setSize(100, h);
        add(mPatternField);
        add(WordLayout.createWallHook());
        add(WordLayout.createNewLine());
        
        label = new JLabel("Undated:");
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setSize(80, h);
        add(label);
        
        mUndatedField = new JTextField(NameFormatter.DEFAULT_UNDATED_PATTERN);
        mUndatedField.setSize(100, h);
        add(mUndatedField);
        add(WordLayout.createWallHook());
        add(WordLayout.createNewLine());

        add(WordLayout.createCursorVertical(20));
        
        mGoButton = new JButton("Go");
        mGoButton.setSize(100, 38);
        add(WordLayout.createCursorHorizontal(84));
        add(mGoButton);

        mGoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                process();
            }
        });
    }


    
    private void process() {
        NameFormatter f1 = null;
        NameFormatter f2 = null;
        
        try{
            f1 = NameFormatter.compile(mPatternField.getText());
        }catch(Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Name Pattern", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try{
            f2 = NameFormatter.compile(mUndatedField.getText());
        }catch(Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Undated Pattern", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try{
            PhotoSort sorter = new PhotoSort();
            sorter.setSource(mSourceChooser.getFile());
            sorter.setTarget(mTargetChooser.getFile());
            sorter.enableMove(mOpBox.getSelectedItem().equals("MOVE"));
            sorter.setNameFormatter(f1);
            sorter.setUndatedNameFormatter(f2);
            
            ProgressDialog d = new ProgressDialog(this, sorter, "Photo Sort Progress");
            d.startTask();
            
        }catch(Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    
}
