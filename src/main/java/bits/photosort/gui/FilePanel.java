/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.photosort.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;

import bits.progress.LayoutAdapter;


/**
 * @author Philip DeCamp
 */
public class FilePanel extends JPanel {

    private JButton    mButton;
    private JTextField mField;
    private FileDialog mChooser;

    public FilePanel( Frame parent, String label, String chooserText ) {
        this( label, chooserText, new FileDialog( parent ) );
    }

    public FilePanel( String label, String chooserText, FileDialog chooser ) {
        mChooser = chooser;
        mField = new JTextField();
        mButton = new JButton( label );

        String fileString = mChooser.getFile();
        if( fileString != null ) {
            mField.setText( fileString );
        }

        add( mButton );
        add( mField );

        mButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                showChooser();
            }
        } );

        setLayout( new LayoutAdapter() {
            public void layoutContainer( Container cont ) {
                int w = cont.getWidth();
                int h = cont.getHeight();

                mButton.setBounds( 0, 0, 80, h );
                mField.setBounds( 84, 0, w - 84, h );
            }
        } );
    }


    public File getFile() {
        return new File( mField.getText() );
    }

    public void setFile( File file ) {
        if( file != null ) {
            mField.setText( file.getPath() );
        } else {
            mField.setText( "" );
        }
    }


    private void showChooser() {
        File file = new File( mField.getText() );
        if( file.exists() ) {
            File dir = file;
            if( dir.isFile() ) {
                dir = dir.getParentFile();
            }

            mChooser.setDirectory( dir.getAbsolutePath() );
        }

        mChooser.setVisible( true );
        String filePath = mChooser.getFile();
        if( filePath == null ) {
            return;
        }

        mField.setText( filePath );
    }

}
