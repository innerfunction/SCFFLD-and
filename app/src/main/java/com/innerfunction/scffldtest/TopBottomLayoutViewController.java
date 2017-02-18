// Copyright 2017 InnerFunction Ltd.
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
package com.innerfunction.scffldtest;

import android.content.Context;

import com.innerfunction.scffld.app.TitleBarStub;
import com.innerfunction.scffld.app.ViewController;

/**
 * Created by juliangoacher on 18/02/17.
 */
public class TopBottomLayoutViewController extends ViewController {


    public TopBottomLayoutViewController(Context context) {
        super( context );
        setLayoutName("top_bottom_layout");
    }

    public void setTopView(ViewController topView) {
        addViewComponent("topView", topView );
    }

    public void setBottomView(ViewController bottomView) {
        addViewComponent("bottomView", bottomView );
        // Prevent the bottom view from controlling the title bar.
        bottomView.setTitleBar( new TitleBarStub() );
    }

}
