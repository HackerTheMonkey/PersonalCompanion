package com.opencloud.demo;

import android.view.View;
import android.view.View.OnClickListener;

public class EditCallMgmtCancelBtnOnClickListener implements OnClickListener
{
    @Override
    public void onClick(View view)
    {
        EditCallManagementRulesSubActivity.editCallManagementRulesSubActivity.finish();
    }

}