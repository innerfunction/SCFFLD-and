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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.innerfunction.scffld.Configuration;
import com.innerfunction.scffld.Container;
import com.innerfunction.scffld.IOCConfigurationAware;
import com.innerfunction.scffld.Message;
import com.innerfunction.scffld.app.AppContainer;
import com.innerfunction.scffld.app.ViewController;
import com.innerfunction.uri.Resource;

import com.nakardo.atableview.foundation.NSIndexPath;
import com.nakardo.atableview.protocol.ATableViewDataSource;
import com.nakardo.atableview.protocol.ATableViewDelegate;
import com.nakardo.atableview.view.ATableView.ATableViewStyle;
import com.nakardo.atableview.view.ATableViewCell;

/**
 * A configurable table view component.
 * Attached by juliangoacher on 20/05/16.
 */
public class TableViewController extends ViewController implements IOCConfigurationAware {

    static final String Tag = TableViewController.class.getSimpleName();

    /** The table view. */
    protected ATableView tableView;
    /** The data displayed by the table. */
    protected TableData tableData;
    /** The default factory for producing table cells (rows). */
    private TableViewCellFactory defaultFactory;
    /** A map of table cell factories, keyed by table display mode name. */
    protected Map<String,TableViewCellFactory> cellFactoriesByDisplayMode = new HashMap<>();
    /** The table style. Values are "Plain" or "Grouped". */
    private String tableStyle = "Plain";
    /** The section title text colour, for grouped tables. */
    protected int sectionTitleColor;
    /** The section title background colour, for grouped tables. */
    protected int sectionTitleBackgroundColor;
    /** The ID of the selected row. */
    private String selectedID;
    /** The index of the selected row. */
    private NSIndexPath selectedIndexPath;
    /** A flag indicating whether the table has a search bar. */
    private boolean hasSearchBar;
    /** The text of a message to display when filtering by favorites. Currently obsolete. */
    private String filterByFavouritesMessage;
    /** The text of a message to display when the search filter is cleared. */
    private String clearFilterMessage;
    /** The table's content. The table data is derived from this. */
    private Object content;
    /** The name of a filter applied to the table data. */
    private String filterName;

    public TableViewController(Context context) {
        super( context );
        setHideTitleBar( false );
        this.tableData = new TableData( context );
    }

    @Override
    public void beforeIOCConfigure(Configuration configuration) {}

    @Override
    public void afterIOCConfigure(Configuration configuration) {
        defaultFactory = cellFactoriesByDisplayMode.get("default");
        if( defaultFactory == null ) {
            defaultFactory = new TableViewCellFactory();
            iocContainer.configureObject( defaultFactory, configuration, "TableViewFragment.defaultFactory");
            cellFactoriesByDisplayMode.put("default", defaultFactory );
        }
        // Pass a reference to the table data to each cell factory.
        for( String mode : cellFactoriesByDisplayMode.keySet() ) {
            TableViewCellFactory factory = cellFactoriesByDisplayMode.get( mode );
            factory.setParent( this );
            factory.setTableData( tableData );
        }
    }

    @Override
    public View onCreateView(Activity activity) {
        super.onCreateView( activity );
        ATableViewStyle style = "Grouped".equals( tableStyle ) ? ATableViewStyle.Grouped : ATableViewStyle.Plain;
        tableView = new ATableView( style, activity );
        tableView.setDataSource( makeDataSource() );
        tableView.setDelegate( makeDelegate() );
        int backgroundColor = getBackgroundColor();
        if( backgroundColor != 0 ) {
            tableView.setBackgroundColor( getBackgroundColor() );
        }
        //tableView.setSeparatorStyle( ATableViewCell.ATableViewCellSeparatorStyle.None );
        addView( tableView );
        return tableView;
    }

    protected ATableViewDataSource makeDataSource() {
        // The table view data source is a class i.e. not an interface, so has to be subclassed.
        // This implementation just forwards requests to the same method on the view controller
        // instance.
        return new ATableViewDataSource() {
            @Override
            public int numberOfSectionsInTableView(com.nakardo.atableview.view.ATableView tableView) {
                return TableViewController.this.numberOfSectionsInTableView( tableView );
            }
            @Override
            public int numberOfRowsInSection(com.nakardo.atableview.view.ATableView tableView, int section) {
                return TableViewController.this.numberOfRowsInSection( tableView, section );
            }
            @Override
            public String titleForHeaderInSection(com.nakardo.atableview.view.ATableView tableView, int section) {
                return TableViewController.this.titleForHeaderInSection( tableView, section );
            }
            @Override
            public ATableViewCell cellForRowAtIndexPath(com.nakardo.atableview.view.ATableView tableView, NSIndexPath indexPath) {
                return TableViewController.this.cellForRowAtIndexPath( tableView, indexPath );
            }
        };
    }

    public int numberOfSectionsInTableView(com.nakardo.atableview.view.ATableView tableView) {
        return tableData.getSectionCount();
    }

    public int numberOfRowsInSection(com.nakardo.atableview.view.ATableView tableView, int section) {
        return tableData.getSectionSize( section );
    }

    public String titleForHeaderInSection(com.nakardo.atableview.view.ATableView tableView, int section) {
        return tableData.getSectionTitle( section );
    }

    public ATableViewCell cellForRowAtIndexPath(com.nakardo.atableview.view.ATableView tableView, NSIndexPath indexPath) {
        TableViewCellFactory factory = getCellFactoryForIndexPath( indexPath );
        return factory.resolveCellForTable( tableView, indexPath, tableView.getDataSource() );
    }

    protected ATableViewDelegate makeDelegate() {
        // The table view delegate is a class i.e. not an interface, so has to be subclassed.
        // This implementation just forwards requests to the same method on the view controller
        // instance.
        return new ATableViewDelegate() {
            @Override
            public final void didSelectRowAtIndexPath(final com.nakardo.atableview.view.ATableView tableView, final NSIndexPath indexPath) {
                TableViewController.this.didSelectRowAtIndexPath( tableView, indexPath );
            }
            @Override
            public final int heightForRowAtIndexPath(final com.nakardo.atableview.view.ATableView tableView, final NSIndexPath indexPath) {
                return TableViewController.this.heightForRowAtIndexPath( tableView, indexPath );
            }
        };
    }

    public void didSelectRowAtIndexPath(com.nakardo.atableview.view.ATableView tableView, NSIndexPath indexPath) {
        String action = actionForRowAtIndexPath( indexPath );
        if( action != null ) {
            AppContainer appContainer = AppContainer.findAppContainer( iocContainer );
            appContainer.postMessage( action, TableViewController.this );
            tableData.clearFilter();
        }
    }

    public int heightForRowAtIndexPath(com.nakardo.atableview.view.ATableView tableView, NSIndexPath indexPath) {
        TableViewCellFactory factory = getCellFactoryForIndexPath( indexPath );
        return factory.heightForRowAtIndexPath( indexPath ).intValue();
    }

    @Override
    public void onStart() {
        super.onStart();
        loadContent();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Make sure the current filter is applied to the data. (Note that the filter is cleared
        // after dispatching a table row action, so it needs to be reapplied if navigating back
        // after an action).
        applyFilterName( filterName );
        if( selectedIndexPath == null && selectedID != null ) {
            selectedIndexPath = tableData.getIndexPathForFirstRowWithFieldValue( selectedID, "id" );
        }
        if( selectedIndexPath != null ) {
            // NOTE This method is defined in the ATableView subclass belonging to this package,
            // and selects the row as well as scrolling to it.
            tableView.scrollToRowWithIndexPath( selectedIndexPath );
        }
    }

    public ATableView getTableView() {
        return tableView;
    }

    public void setTableStyle(String style) {
        this.tableStyle = style;
    }

    public void setSectionTitleColor(int color) {
        this.sectionTitleColor = color;
    }

    public void setSectionTitleBackgroundColor(int color) {
        this.sectionTitleBackgroundColor = color;
    }

    public void setCellFactoriesByDisplayMode(Map<String,TableViewCellFactory> factories) {
        this.cellFactoriesByDisplayMode = factories;
    }

    public Map<String,TableViewCellFactory> getCellFactoriesByDisplayMode() {
        return cellFactoriesByDisplayMode;
    }

    public void setSelectedID(String selectedID) {
        this.selectedID = selectedID;
        this.selectedIndexPath = null;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndexPath = NSIndexPath.indexPathForRowInSection( index, 0 );
        this.selectedID = null;
    }

    public void setHasSearchBar(boolean hasSearchBar) {
        this.hasSearchBar = hasSearchBar;
    }

    public void setFilterByFavouritesMessage(String message) {
        this.filterByFavouritesMessage = message;
    }

    public void setClearFilterMessage(String message) {
        this.clearFilterMessage = message;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public void setFilterName(String name) {
        this.filterName = name;
    }

    public void refreshTableView() {
        if( tableView != null ) {
            tableView.post( new Runnable() {
                @Override
                public void run() {
                    tableView.reloadData();
                }
            } );
        }
    }

    public void loadContent() {
        if( content == null ) {
            Log.w( Tag, "No content specified");
        }
        else if( content instanceof Resource ) {
            setRows( ((Resource)content).asConfiguration() );
        }
        else if( content instanceof Configuration ) {
            setRows( (Configuration)content );
        }
        else if( content instanceof List ) {
            setRowsArray( (List)content );
        }
    }

    /**
     * Set the table's data.
     * Use this to configure the table's rows data.
     * @param rows A configuration object containing row data.
     */
    public void setRows(Configuration rows) {
        Object sourceData = rows.getSourceData();
        if( sourceData instanceof List ) {
            rows.setData( formatData( (List)sourceData ) );
            tableData.setRowsConfiguration( rows );
            applyFilterName( filterName );
        }
    }

    /**
     * Set the table's data.
     * Use this when passing row data programmatically.
     * @param rows An array of row data.
     */
    public void setRowsArray(List rows) {
        if( rows != null ) {
            rows = formatData( rows );
            tableData.setRowsData( rows );
            applyFilterName( filterName );
        }
    }

    public void applyFilterName(String name) {
        this.filterName = name;
        if( name != null ) {
            TableData.FilterPredicate predicate = getFilterPredicateForName( name );
            if( predicate != null ) {
                tableData.filterBy( predicate );
            }
            else {
                tableData.clearFilter();
            }
        }
        else {
            tableData.clearFilter();
        }
        refreshTableView();
    }

    /**
     * Return a filter predicate for a specified name.
     * Filter predicates are used to filter table data by a search term. The default implementation
     * of this method does nothing; subclasses should override the method and return useful filters.
     * @param name
     * @return Returns null.
     */
    public TableData.FilterPredicate getFilterPredicateForName(String name) {
        return null;
    }

    /**
     * Apply additional formatting to the table's data.
     * @param data
     * @return
     */
    public List formatData(List data) {
        return data;
    }

    /**
     * Get the action for the row at the specified index path.
     * Called when a table row is selected. Default implementation reads the "action"
     * property of the row's data item.
     */
    public String actionForRowAtIndexPath(NSIndexPath indexPath) {
        Configuration rowData = tableData.getRowDataForIndexPath( indexPath );
        return rowData.getValueAsString("action");
    }

    public String displayModeForIndexPath(NSIndexPath indexPath) {
        return "default";
    }

    public NSIndexPath indexPathForFirstRowWithDisplayMode(String displayMode) {
        for( int s = 0; s < tableData.getSectionCount(); s++ ) {
            for( int r = 0; r < tableData.getSectionSize( s ); r++ ) {
                NSIndexPath path = NSIndexPath.indexPathForRowInSection( r, s );
                String mode = displayModeForIndexPath( path );
                if( mode.equals( displayMode ) ) {
                    return path;
                }
            }
        }
        return null;
    }

    public TableViewCellFactory getCellFactoryForIndexPath(NSIndexPath indexPath){
        String displayMode = displayModeForIndexPath( indexPath );
        TableViewCellFactory factory = cellFactoriesByDisplayMode.get( displayMode );
        if( factory == null ) {
            factory = defaultFactory;
        }
        return factory;
    }

    public void filterByFavourites(final boolean showFavourites) {
        tableData.filterBy(new TableData.FilterPredicate() {
            @SuppressWarnings("rawtypes")
            @Override
            public boolean testRow(Map row) {
                Object favourite = row.get("favourite");
                if( favourite instanceof Boolean ) {
                    return ((Boolean)favourite).equals( showFavourites );
                }
                if( favourite instanceof Number ) {
                    return ((Number)favourite).intValue() == (showFavourites ? 1 : 0);
                }
                return !showFavourites;
            }
        });
        refreshTableView();
        if( filterByFavouritesMessage != null ) {
            Toast.makeText( getActivity(), filterByFavouritesMessage, Toast.LENGTH_SHORT ).show();
        }
    }

    @Override
    public boolean receiveMessage(Message message, Object sender) {
        if( message.hasName("load") ) {
            setContent( message.getParameter("content") );
            return true;
        }
        if( message.hasName("filter") ) {
            applyFilterName( (String)message.getParameter("name") );
            return true;
        }
        if( message.hasName("clear-filter") ) {
            tableData.clearFilter();
            refreshTableView();
            return true;
        }
        return super.receiveMessage( message, sender );
    }

}