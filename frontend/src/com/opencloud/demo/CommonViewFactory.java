package com.opencloud.demo;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CommonViewFactory
{
    public static final int VIEW_ID = 123456;
    
    public static TextView generatedSeparatorTextView(Context context)
    {
        /**
         * Create a new empty TextView
         */
        TextView textView = new TextView(context);
        /**
         * Set the ID of the text view to a mock TextView that is created only
         * for the sole purpose of making an ID available to be used for this particular
         * TextView
         */
        textView.setId(R.id.generatedSepTextView);
        /**
         * Create a new RelativeLayout.LayoutParams and set it to the
         * created TextView
         */
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        /**
         * Align the view to the top of its parent
         */
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        /**
         * Set the LayoutParams to the textView
         */
        textView.setLayoutParams(layoutParams);
        /**
         * Set the text style and the background color of the separator TextView
         */
        textView.setTextAppearance(context, R.style.SeparatorTextViewStyle);
        /**
         * Set the background color of the textView
         */
        textView.setBackgroundColor(0x85B8D1B2);
        return textView;
    }
}
