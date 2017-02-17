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
package com.innerfunction.scffld.ui;

import android.app.Activity;
import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.view.View;

import com.innerfunction.scffld.Message;
import com.innerfunction.scffld.app.TitleBarStub;
import com.innerfunction.scffld.app.ViewController;

import static android.support.v4.view.GravityCompat.*;

/**
 * Attached by juliangoacher on 19/05/16.
 */
public class SlideViewController extends ViewController {

    private DrawerLayout drawerLayout;
    private int slidePosition = START;

    public SlideViewController(Context context) {
        super( context );
        setLayoutName("slide_view_layout");
    }

    @Override
    public View onCreateView(Activity activity) {
        drawerLayout = (DrawerLayout)super.onCreateView( activity );
        return drawerLayout;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Ensure that the slide view is put back into a paused state after a restart, if the
        // drawer is hidden.
        if( !drawerLayout.isDrawerOpen( slidePosition ) ) {
            getSlideView().changeState( State.Paused );
        }
    }

    public void setSlideView(ViewController slideView) {
        slideView.setRunnable( false );
        removeChildViewController( getSlideView() );
        addChildViewController( slideView );
        layoutManager.setViewComponent("slide", slideView );
        // Prevent the slide view from controlling the title bar.
        slideView.setTitleBar( new TitleBarStub() );
    }

    public ViewController getSlideView() {
        return (ViewController)layoutManager.getViewComponent("slide");
    }

    public void setMainView(ViewController mainView) {
        removeChildViewController( getMainView() );
        addChildViewController( mainView );
        layoutManager.setViewComponent("main", mainView );
    }

    public ViewController getMainView() {
        return (ViewController)layoutManager.getViewComponent("main");
    }

    public void setSlidePosition(String position) {
        slidePosition = "right".equals( position ) ? START : END;
    }

    public String getSlidePosition() {
        return slidePosition == START ? "right" : "left";
    }

    public void openDrawer() {
        // Pause the main view.
        ViewController mainView = getMainView();
        mainView.setRunnable( false );
        mainView.changeState( State.Paused );
        // Show slide view & update its state.
        ViewController slideView = getSlideView();
        slideView.setRunnable( true );
        slideView.changeState( getState() );
        drawerLayout.openDrawer( slidePosition );
    }

    public void closeDrawer() {
        // Hide and pause the slide view.
        drawerLayout.closeDrawers();
        ViewController slideView = getSlideView();
        slideView.setRunnable( false );
        slideView.changeState( State.Paused );
        // Restart the main view.
        ViewController mainView = getMainView();
        mainView.setRunnable( true );
        mainView.changeState( getState() );
    }

    public void toggleDrawer() {
        if( drawerLayout.isDrawerOpen( slidePosition ) ) {
            closeDrawer();
        }
        else {
            openDrawer();
        }
    }

    @Override
    public boolean onBackPressed() {
        // If drawer is open then close drawer.
        if( drawerLayout.isDrawerOpen( slidePosition ) ) {
            closeDrawer();
            return false;
        }
        // Else if main view then delegate to it.
        ViewController mainView = getMainView();
        if( mainView != null ) {
            return mainView.onBackPressed();
        }
        // Else don't process back button.
        return true;
    }

    @Override
    public boolean routeMessage(Message message, Object sender) {
        boolean routed = false;
        if( message.hasTarget("slideView") ) {
            message = message.popTargetHead();
            ViewController slideView = getSlideView();
            if( message.hasEmptyTarget() ) {
                routed = slideView.receiveMessage( message, sender );
            }
            else {
                routed = slideView.routeMessage( message, sender );
            }
        }
        else if( message.hasTarget("mainView") ) {
            message = message.popTargetHead();
            ViewController mainView = getMainView();
            if( message.hasEmptyTarget() ) {
                routed = mainView.receiveMessage( message, sender );
            }
            else {
                routed = mainView.routeMessage( message, sender );
            }
            closeDrawer();
        }
        return routed;
    }

    @Override
    public boolean receiveMessage(Message message, Object sender) {
        if( message.hasName("show") ) {
            // Replace main view.
            setMainView( (ViewController)message.getParameter("view") );
            return true;
        }
        if( message.hasName("show-in-slide") ) {
            // Replace the slide view.
            setSlideView( (ViewController)message.getParameter( "view" ) );
            return true;
        }
        if( message.hasName("show-slide") || message.hasName("open-slide") ) {
            // Open the slide view.
            openDrawer();
            return true;
        }
        if( message.hasName("hide-slide") ) {
            // Close the slide view.
            closeDrawer();
            return true;
        }
        if( message.hasName("toggle-slide") ) {
            toggleDrawer();
        }
        return false;
    }

}
