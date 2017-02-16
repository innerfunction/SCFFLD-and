// Copyright 2016 InnerFunction Ltd.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License
package com.innerfunction.scffld.ui.table;

import android.annotation.SuppressLint;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;

import com.innerfunction.scffld.Configuration;
import com.innerfunction.scffld.IOCContextAware;

import com.nakardo.atableview.foundation.NSIndexPath;
import com.nakardo.atableview.internal.ATableViewCellContainerView;
import com.nakardo.atableview.internal.ATableViewCellAccessoryView.ATableViewCellAccessoryType;
import com.nakardo.atableview.protocol.ATableViewDataSource;
import com.nakardo.atableview.view.ATableView;
import com.nakardo.atableview.view.ATableViewCell;
import com.nakardo.atableview.view.ATableViewCell.ATableViewCellStyle;

/**
 * A factory class for generating table view cell instances.
 *
 * Attached by juliangoacher on 05/05/16.
 */
public class TableViewCellFactory implements IOCContextAware {

    static final String Tag = TableViewCellFactory.class.getSimpleName();

    static final int DefaultRowHeight = 58;
    static final int DefaultImageHeight = 50;
    static final int DefaultImageWidth = 50;
    static final String BackgroundImageViewTag = "BackgroundImageView";
    static final String Black = "#000000";
    static final String White = "#FFFFFF";

    public interface CellDecorator {
        ATableViewCell decorateCell(ATableViewCell cell, Configuration data, TableViewCellFactory factory);
    }

    private TableViewController parent;
    private TableData tableData;

    private CellDecorator decorator;

    /** The cell display style. */
    private String style = "Style1";
    /** The default main text colour. */
    private String textColor = Black;
    /** The default text colour for a selected cell. */
    private String selectedTextColor = Black;
    /** The default detail text colour. */
    private String detailTextColor = Black;
    /** The default detail text colour for a selected cell. */
    private String selectedDetailTextColor = Black;
    /** The cell's default background colour. */
    private String backgroundColor = White;
    /** The default background colour for a selected cell. */
    @SuppressWarnings("unused")
    private String selectedBackgroundColor = White;
    /** The default cell height. */
    private Number height = DefaultRowHeight;
    /** The default width of the cell's image. */
    private Number imageHeight = DefaultImageHeight;
    /** The default height of the cell's image. */
    private Number imageWidth = DefaultImageWidth;
    /** The cell's default accessory style. */
    private String accessory;
    /** The cell's default image. */
    private Drawable image;
    /** A background image for the cell. */
    private Drawable backgroundImage;
    @SuppressWarnings("unused")
    /** A background image for a selected cell. */
    private Drawable selectedBackgroundImage;
    /**
     * The device pixel ratio.
     * For converting from logical to real pixels.
     */
    private float pixelRatio = 1;

    /** IOCContextAware interface. */
    @Override
    public void setAndroidContext(Context context) {
        this.pixelRatio = context.getResources().getDisplayMetrics().density;
    }

    public void setParent(TableViewController parent) {
        this.parent = parent;
    }

    public void setTableData(TableData tableData) {
        this.tableData = tableData;
    }

    public void setDecorator(CellDecorator decorator) {
        this.decorator = decorator;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public void setTextColor(String color) {
        this.textColor = color;
    }

    public void setSelectedTextColor(String color) {
        this.selectedTextColor = color;
    }

    public void setDetailTextColor(String color) {
        this.detailTextColor = color;
    }

    public void setSelectedDetailTextColor(String color) {
        this.selectedDetailTextColor = color;
    }

    public void setBackgroundColor(String color) {
        this.backgroundColor = color;
    }

    public void setSelectedBackgroundColor(String color) {
        this.selectedBackgroundColor = color;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setImageHeight(int height) {
        this.imageHeight = height;
    }

    public void setImageWidth(int width) {
        this.imageWidth = width;
    }

    public void setAccessory(String accessory) {
        this.accessory = accessory;
    }

    public void setImage(Drawable image) {
        this.image = image;
    }

    public void setBackgroundImage(Drawable image) {
        this.backgroundImage = image;
    }

    public void setSelectedBackgroundImage(Drawable image) {
        this.selectedBackgroundImage = image;
    }

    // A cell in iOS represents a table row in Android
    @SuppressLint("DefaultLocale")
    public ATableViewCell resolveCellForTable(ATableView tableView, NSIndexPath indexPath, ATableViewDataSource dataSource) {

        Configuration rowData = tableData.getRowDataForIndexPath( indexPath );

        String style = rowData.getValueAsString("style", this.style );
        ATableViewCell cell = dataSource.dequeueReusableCellWithIdentifier( style );

        // Process title and description
        String title = rowData.getValueAsString("title");
        String description = rowData.getValueAsString("description");

        if( cell == null ) {
            ATableViewCellStyle cellStyle = ATableViewCellStyle.Default;
            if( style.equals("Style1") ) {
                cellStyle = ATableViewCellStyle.Value1;
            }
            else if( style.equals("Style2") ) {
                cellStyle = ATableViewCellStyle.Value2;
            }
            else if( style.equals("Subtitle") ) {
                cellStyle = ATableViewCellStyle.Subtitle;
            }
            else if( description != null ) {
                // No style explicitly defined, but a description is provided so display it.
                cellStyle = ATableViewCellStyle.Subtitle;
            }
            cell = new ATableViewCell( cellStyle, style, parent.getActivity() );
        }

        TextView textLabel = cell.getTextLabel();
        textLabel.setText( title );
        textLabel.setTextColor( rowData.getValueAsColor("textColor", textColor ) );
        // TODO: android TextView only has setHighlightColor, not highlightedTextColor
        textLabel.setHighlightColor( rowData.getValueAsColor("selectedTextColor", selectedTextColor ) );
        textLabel.setBackgroundColor( Color.TRANSPARENT );

        TextView detailTextLabel = cell.getDetailTextLabel();
        if( description != null && detailTextLabel != null ) {
            detailTextLabel.setText( description );
            detailTextLabel.setTextColor( rowData.getValueAsColor("detailTextColor", detailTextColor ) );
            detailTextLabel.setHighlightColor( rowData.getValueAsColor("detailSelectedTextColor", selectedDetailTextColor ) );
            detailTextLabel.setBackgroundColor( Color.TRANSPARENT );
        }

        cell.setBackgroundColor( rowData.getValueAsColor( "backgroundColor", backgroundColor ) );

        Number imageHeight = rowData.getValueAsNumber( "imageHeight", this.imageHeight );
        if( imageHeight == null ) {
            imageHeight = rowData.getValueAsNumber( "height", height );
        }
        if( imageHeight.intValue() == 0 ) {
            imageHeight = DefaultImageHeight;
        }
        Number imageWidth = rowData.getValueAsNumber( "imageWidth", this.imageWidth );
        if( imageWidth.intValue() == 0 ) {
            imageHeight = DefaultImageWidth;
        }

        float radius = imageHeight.floatValue() * pixelRatio * 4;
        Drawable image = tableData.loadImageWithRowData( rowData, "image", null, radius );
        ImageView imageView = cell.getImageView();
        if( image != null ) {
            imageWidth = convertToRealPixels( imageWidth );
            imageHeight = convertToRealPixels( imageHeight );
            LayoutParams params = new ATableViewCellContainerView.LayoutParams( imageWidth.intValue(), imageHeight.intValue() );
            // Set margins and crop the image to fit the available space
            params.setMargins( 20, 0, 0, 0 );
            imageView.setLayoutParams( params );
            imageView.setScaleType( ScaleType.CENTER_CROP );
            imageView.setAdjustViewBounds( true );
        }
        imageView.setImageDrawable( image );

        // Accessory
        String accessory = rowData.getValueAsString("accessory", this.accessory );
        if("None".equals( accessory ) ) {
            cell.setAccessoryType( ATableViewCellAccessoryType.None );
        }
        else if("DisclosureIndicator".equals( accessory ) ) {
            cell.setAccessoryType( ATableViewCellAccessoryType.DisclosureIndicator );
        }
        else if("DetailButton".equals( accessory ) ) {
            cell.setAccessoryType( ATableViewCellAccessoryType.DisclosureButton );
        }
        else if("Checkmark".equals( accessory ) ) {
            cell.setAccessoryType( ATableViewCellAccessoryType.Checkmark );
        }

        // Background image
        Drawable backgroundImage = tableData.loadImageWithRowData( rowData, "backgroundImage", this.backgroundImage );
        Drawable selectedImage = tableData.loadImageWithRowData( rowData, "selectedBackgroundImage", this.selectedBackgroundImage );

        StateListDrawable stateListDrawable = new StateListDrawable();
        if( backgroundImage != null ) {
            stateListDrawable.addState(new int[] { android.R.attr.state_enabled }, backgroundImage );
        }
        if( selectedImage != null ) {
            stateListDrawable.addState(new int[] { android.R.attr.state_selected }, selectedImage );
        }
        // NOTE: The following is a hack to get cell background images working correctly. This
        // implementation won't display the cell's selected state background image (or maybe it
        // does, but it hasn't been tested). Also, use of images bigger than the cell size may
        // cause problems. A proper implementation may require a subclass of ATableViewCell to
        // properly encapsulate the following logic and include state change (normal/selected)
        // logic.
        if( backgroundImage != null || selectedImage != null ) {
            ImageView backgroundView = (ImageView)cell.findViewWithTag( BackgroundImageViewTag );
            if( backgroundView == null ) {
                backgroundView = new ImageView( cell.getContext() );
                backgroundView.setTag( BackgroundImageViewTag );
                cell.addView( backgroundView );
                // Set top/bottom padding to avoid overwriting table cell separators.
                backgroundView.setPadding( 0, 2, 0, 2 );
            }
            backgroundView.setImageDrawable( stateListDrawable );
        }

        // Decorate the cell if a decorator is specified.
        if( decorator != null ) {
            decorator.decorateCell( cell, rowData, this );
        }

        return cell;
    }

    public Number heightForRowAtIndexPath(NSIndexPath indexPath) {
        Configuration rowData = tableData.getRowDataForIndexPath( indexPath );
        return rowData.getValueAsNumber("height", height );
    }

    /**
     * Convert from logical pixels to real pixels
     */
    public float convertToRealPixels(Number logicalPixels) {
        return logicalPixels.intValue() * pixelRatio;
    }

}
