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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;
import android.util.LruCache;

import com.innerfunction.scffld.Configuration;
import com.innerfunction.uri.Resource;
import com.innerfunction.util.Images;

import com.innerfunction.util.Null;
import com.nakardo.atableview.foundation.NSIndexPath;

/**
 * Data source for table views.
 * Attached by juliangoacher on 05/05/16.
 */
@SuppressLint("DefaultLocale")
@SuppressWarnings("rawtypes")
public class TableData {

    static final String Tag = TableData.class.getSimpleName();

    /** The image cache size, in MB. */
    static final int CacheSize = 2 * 1024 * 1024;
    /**
     * A shared cache of table row images.
     * Note on thread safety: All image operations which access the cache will be performed from the
     * UI thread (i.e. as a table row is being created); this is effectively single-threaded
     * operation, so no synchronization is necessary before calling cache methods.
     */
    static final LruCache<Object,Drawable> ImageCache = new LruCache<Object,Drawable>( CacheSize ) {
        @Override
        protected int sizeOf(Object key, Drawable image) {
            // NOTE Drawable doesn't provide any method for getting the image size, so instead
            // estimate the size assuming a 32 bit (4 byte) pixel depth.
            return image.getIntrinsicWidth() * image.getIntrinsicHeight() * 4;
        }
    };

    /**
     * An object used to represent misses in the image cache.
     */
    static final Drawable NullImage = new ShapeDrawable();

    /** Interface for wrapping table data filter predicates. */
    public interface FilterPredicate {
        boolean testRow(Map row);
    }

    /** The table data. May represent either grouped or non-grouped data. */
    private List data;
    /** The visible table data, i.e. after a filter has been applied. */
    private List visibleData;
    /** A list of the section tables, for grouped data. */
    private List<String> sectionTitles;
    /** A flag indicating whether the data is grouped or not. */
    private boolean grouped;
    /** A list of data fields to check when filtering by a search term. */
    private List<String> searchFieldNames;
    /** An empty configuration object. Used as the rows config when loading data from a list. */
    private Configuration emptyConfiguration;
    /**
     * A configuration object used to return row data.
     * This is used to allow any URI references within data to resolve correctly.
     */
    private Configuration currentRowData;
    /** A delegate object for modifying resolved data values. */
    private TableDataDelegate delegate;

    public TableData(Context context) {
        data = new ArrayList();
        visibleData = data;
        sectionTitles = new ArrayList<>();
        grouped = false;
        searchFieldNames = Arrays.asList("title", "description");
        emptyConfiguration = new Configuration( context );
    }

    public void reset() {
        data = Collections.emptyList();
        clearFilter();
    }

    public void setTableDataDelegate(TableDataDelegate tableDataDelegate) {
        delegate = tableDataDelegate;
    }

    public void setRowsConfiguration(Configuration rowsConfiguration) {
        this.currentRowData = new Configuration( Collections.EMPTY_MAP, rowsConfiguration );
        List rowsData;
        Object sourceData = rowsConfiguration.getSourceData();
        if( sourceData instanceof List ) {
            rowsData = (List)sourceData;
        }
        else {
            rowsData = Collections.EMPTY_LIST;
        }
        _setRowsData( rowsData );
    }

    public void setRowsData(List rowsData) {
        this.currentRowData = new Configuration( Collections.EMPTY_MAP, emptyConfiguration );
        _setRowsData( rowsData );
    }

    /**
     * Set the table row data.
     *
     * @param rowsData
     */
    @SuppressWarnings("unchecked")
    private void _setRowsData(List rowsData) {
        // Test whether the data is grouped or non-grouped. If grouped, then extract section header
        // titles from the data.
        // This method allows grouped data to be presented in one of two ways, and assumes that the
        // data is grouped
        // consistently throughout.
        // * The first grouping format is as an array of arrays. The section header title is
        // extracted as the first character
        // of the title of the first item in each group.
        // * The second grouping format is as an array of dictionarys. Each dictionary represents a
        // section object with
        // 'sectionTitle' and 'sectionData' properties.
        // Data can also be presented as an array of strings, in which case each string is used as a
        // row title.
        int dataSize = rowsData.size();
        Object firstItem = dataSize > 0 ? rowsData.get(0) : null;

        // Allow a table section to be represented using a List or String array.
        if( firstItem instanceof List || firstItem instanceof String[] ) {
            grouped = true;
            List<String> titles = new ArrayList<>( dataSize );
            for( Object section : rowsData ) {
                Map row = (Map)((List)section).get( 0 );
                if (row != null) {
                    titles.add( (String)row.get("title") );
                }
                else {
                    titles.add("");
                }
            }
            data = rowsData;
            sectionTitles = titles;
        }
        else if( firstItem instanceof Map ) {
            Map<String,Object> sectionDef = (Map<String,Object>)firstItem;
            if( sectionDef.containsKey("sectionTitle") || sectionDef.containsKey("sectionData") ) {
                grouped = true;
                List<String> titles = new ArrayList<>( dataSize );
                List sections = new ArrayList( dataSize );
                for( Object section : rowsData ) {
                    Map sectionMap = (Map)section;
                    String sectionTitle = null;
                    if( delegate != null ) {
                        sectionTitle = delegate.resolveTableDateSectionTitle( sectionMap, this );
                    }
                    if( sectionTitle == null ) {
                        sectionTitle = (String)sectionMap.get("sectionTitle");
                    }
                    titles.add( sectionTitle != null ? sectionTitle : "" );
                    List sectionData = (List)sectionMap.get("sectionData");
                    sections.add( sectionData != null ? sectionData : new ArrayList() );
                }
                data = sections;
                sectionTitles = titles;
            }
            else {
                grouped = false;
                data = rowsData;
            }

        }
        else if( firstItem instanceof String ) {
            grouped = false;
            List<Map<String,String>> rows = new ArrayList<>( dataSize );
            for( Object title : rowsData ) {
                Map<String,String> row = new HashMap<>();
                row.put("title", (String)title );
                rows.add( row );
            }
            data = rows;
        }
        else {
            grouped = false;
            data = new ArrayList();
        }
        visibleData = data;
    }

    /**
     * Get row data for an index path.
     */
    public Configuration getRowDataForIndexPath(NSIndexPath indexPath) {
        Map rowData = null;
        int section = indexPath.getSection();
        int row = indexPath.getRow();
        if( visibleData.size() > 0 ) {
            if( grouped ) {
                if( visibleData.size() > section ) {
                    List sectionData = (List)visibleData.get( section );
                    if( sectionData.size() > row ) {
                        rowData = (Map)sectionData.get( row );
                    }
                }
            }
            else if( visibleData.size() > row ) {
                rowData = (Map)visibleData.get( row );
            }
        }
        if( rowData == null ) {
            rowData = Collections.EMPTY_MAP;
        }
        currentRowData.setConfigData( rowData );
        return currentRowData;
    }

    /**
     * Find the index path of the first row with the specified value in the specified field.
     * @param value     The value being looked for.
     * @param fieldName The name of the field containing the required value.
     * @return The index path of the first matching row, or null if no row is found.
     */
    public NSIndexPath getIndexPathForFirstRowWithFieldValue(Object value, String fieldName) {
        if( grouped ) {
            for( int s = 0; s < visibleData.size(); s++ ) {
                List sectionData = (List)visibleData.get( s );
                for( int r = 0; r < sectionData.size(); r++ ) {
                    Map rowData = (Map)sectionData.get( r );
                    Object fieldValue = rowData.get( fieldName );
                    if( fieldValue != null && fieldValue.equals( value ) ) {
                        return NSIndexPath.indexPathForRowInSection( r, s );
                    }
                }
            }
        }
        else for( int r = 0; r < visibleData.size(); r++ ) {
            Map rowData = (Map)visibleData.get( r );
            Object fieldValue = rowData.get( fieldName );
            if( fieldValue != null && fieldValue.equals( value ) ) {
                return NSIndexPath.indexPathForRowInSection( r, 1 );
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return data.size() == 0;
    }

    /**
     * Return the number of sections in the table data.
     *
     * @return
     */
    public int getSectionCount() {
        if( visibleData.size() > 0 ) {
            return grouped ? visibleData.size() : 1;
        }
        return 0;
    }

    /**
     * Return the number of rows in the specified section.
     *
     * @param section
     * @return
     */
    public int getSectionSize(int section) {
        int size = 0;
        if( visibleData.size() > 0 ) {
            if( grouped ) {
                if( visibleData.size() > section ) {
                    List sectionData = (List)visibleData.get( section );
                    size = sectionData.size();
                }
                else {
                    size = 0;
                }
            }
            else if( section == 0 ) {
                size = visibleData.size();
            }
        }
        return size;
    }

    @SuppressWarnings("unchecked")
    public void filterBy(FilterPredicate predicate) {
        List result = new ArrayList();
        if( grouped ) {
            for( Object section : data ) {
                List<Map> filteredSection = new ArrayList<>();
                for( Map row : (List<Map>)section ) {
                    if( predicate.testRow( row ) ) {
                        filteredSection.add( row );
                    }
                }
                result.add( filteredSection );
            }
        }
        else for( Object rowObj : data ) {
            Map row = (Map)rowObj;
            if( predicate.testRow( row ) ) {
                result.add( row );
            }
        }
        this.visibleData = result;
    }

    public void filterBy(String searchTerm) {
        filterBy( searchTerm, null );
    }

    public void filterBy(String searchTerm, String scope) {
        // Convert search term to lower case (for a case insensitive match).
        final String lcSearchTerm = searchTerm.toLowerCase();
        // The list of data fields to include in the search.
        final List<String> searchNames = (scope != null) ? Arrays.asList( scope ) : searchFieldNames;
        // The filter predicate.
        FilterPredicate predicate = new FilterPredicate() {
            @Override
            public boolean testRow(Map row) {
                for( String name : searchNames ) {
                    Object value = row.get( name );
                    if( value instanceof String && ((String)value).toLowerCase().contains( lcSearchTerm ) ) {
                        return true;
                    }
                }
                return false;
            }
        };
        // Perform the search.
        filterBy( predicate );
    }

    public void clearFilter() {
        visibleData = data;
    }

    public void setSearchFieldNames(List<String> searchFieldNames) {
        this.searchFieldNames = searchFieldNames;
    }

    public void setSearchFieldNames(String... searchFieldNames) {
        this.searchFieldNames = Arrays.asList( searchFieldNames );
    }

    public String getSectionTitle(int section) {
        if( section < sectionTitles.size() ) {
            return sectionTitles.get( section );
        }
        return null;
    }

    // Image handlers.

    public Drawable loadImageWithRowData(Configuration rowData, String dataName) {
        return loadImageWithRowData( rowData, dataName, null, 0.0f );
    }
    public Drawable loadImageWithRowData(Configuration rowData, String dataName, Drawable defaultImage) {
        return loadImageWithRowData( rowData, dataName, defaultImage, 0.0f );
    }

    public Drawable loadImageWithRowData(Configuration rowData, String dataName, Drawable defaultImage, float radius) {
        Drawable result = null;
        Object imageRef = rowData.getUnmodifiedValue( dataName );
        if( imageRef != null ) {
            if( radius > 0.0f ) {
                imageRef = String.format( "%s.radius:%f", imageRef, radius );
            }
            result = ImageCache.get( imageRef );
            if( result == null ) {
                result = rowData.getValueAsImage( dataName );
                if( result == null ) {
                    result = defaultImage;
                }
                if( result == null ) {
                    result = NullImage;
                }
                else if( radius > 0.0f && result instanceof BitmapDrawable ) {
                    result = Images.toRoundedCorner( (BitmapDrawable)result, radius );
                }
            }
            ImageCache.put( imageRef, result );
        }
        return result == NullImage ? null : result;
    }

}