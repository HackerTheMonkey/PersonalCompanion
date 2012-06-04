package com.opencloud.demo;

import java.text.AttributedCharacterIterator.Attribute;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CustomListAdapter extends BaseAdapter
{    
    private Context context = null;
    private String[] dataSet = null;
    private String[] dataSetDescriptions = null;
    private String[] dataSetIconPaths = null;
    /**
     * Padding Settings
     */
    private final int ITEM_NAME_TEXTVIEW_PADDING_TOP = 28;
    private final int ITEM_NAME_TEXTVIEW_PADDING_BOTTOM = 3;
    private final int ITEM_NAME_TEXTVIEW_PADDING_RIGHT = 0;
    private final int ITEM_NAME_TEXTVIEW_PADDING_LEFT = 0;
    
    private final int ITEM_DESC_TEXTVIEW_PADDING_TOP = 75;
    private final int ITEM_DESC_TEXTVIEW_PADDING_BOTTOM = 5;
    private final int ITEM_DESC_TEXTVIEW_PADDING_RIGHT = 0;
    private final int ITEM_DESC_TEXTVIEW_PADDING_LEFT = 0;
    

    
    public CustomListAdapter(Context context, String[] dataSet, String[] dataSetDescriptions, String[] dataSetIconPaths)
    {
        this.context = context;
        this.dataSet = dataSet;
        this.dataSetDescriptions = dataSetDescriptions;
        this.dataSetIconPaths = dataSetIconPaths;
    }
    
    @Override
    public int getCount()
    {        
        return dataSet.length;        
    }

    @Override
    public Object getItem(int itemIndex)
    { 
        return dataSet[itemIndex];
    }

    @Override
    public long getItemId(int itemIndex)
    {
        long itemId = Long.parseLong(String.valueOf(itemIndex));
        return itemId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parentViewGroup)
    {
        /**
         * Inflate the main_list_item layout, convert it into a view, modify the view content as per
         * the position of the item and then return it back to be used for the display of the list menu item.
         */                
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);                
        ViewGroup inflatedViewGroup = (ViewGroup)layoutInflater.inflate(R.layout.main_list_item, null);
        /**
         * Insert TextView separators at the required indices (via using the CommonViewFactory to generate a new TextView separator)
         */
        SeparatorPositions separatorPositions = SeparatorPositions.getSeparatorAt(position);
        if(separatorPositions != null)
        {                     
            /**
             * Obtain an instance of the separator TextView.
             */
            TextView sepTextView = CommonViewFactory.generatedSeparatorTextView(context);            
            /**
             * Set the text property of the sepTextView to an appropriate value, the value that is associated
             * with the matching enum separator position
             */            
            sepTextView.setText(separatorPositions.text);
            /**
             * Add the generated TextView to the inflatedLayout ViewGroup as the first child
             */
            inflatedViewGroup.addView(sepTextView, 0);
            /**
             * Adjust the padding of the title and desc TextViews(s) child elements of the
             * inflatedViewGroup to accommodate for the addition of the separator TextView
             */
            TextView itemNameTextView = (TextView) inflatedViewGroup.findViewById(R.id.itemNameTextView);
            itemNameTextView.setPadding(ITEM_NAME_TEXTVIEW_PADDING_LEFT, ITEM_NAME_TEXTVIEW_PADDING_TOP, ITEM_NAME_TEXTVIEW_PADDING_RIGHT, ITEM_NAME_TEXTVIEW_PADDING_BOTTOM);
            
            TextView itemDescTextView = (TextView) inflatedViewGroup.findViewById(R.id.itemDescTextView);
            itemDescTextView.setPadding(ITEM_DESC_TEXTVIEW_PADDING_LEFT, ITEM_DESC_TEXTVIEW_PADDING_TOP, ITEM_DESC_TEXTVIEW_PADDING_RIGHT, ITEM_DESC_TEXTVIEW_PADDING_BOTTOM);
            /**
             * Add a LayoutParams rule to the listItemIconImageView to position it right
             * below the added separatorTextView.
             * Note: The ID of the generated TextView is set by the CommonViewFactory to the value of generatedSepTextView which is
             * a TextView created in the separator_text_view.xml file for this purpose only.
             */
            ImageView listItemIconImageView = (ImageView)inflatedViewGroup.findViewById(R.id.listItemIconImageView);
            ((RelativeLayout.LayoutParams)listItemIconImageView.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.generatedSepTextView);
        }
        /**
         * Change the icon of the current ImageView
         */
        ImageView imageView = (ImageView)inflatedViewGroup.findViewById(R.id.listItemIconImageView);
        int iconResourceId = context.getResources().getIdentifier(context.getPackageName() + ":drawable/" + dataSetIconPaths[position], null, null);        
        imageView.setImageResource(iconResourceId);                     
        /**
         * Modify the text property of the menu item title TextView
         */
        TextView titleTextView = (TextView)inflatedViewGroup.findViewById(R.id.itemNameTextView);
        titleTextView.setText(dataSet[position]);
        /**
         * Modify the text property of the description TextView.
         */
        TextView descTextView = (TextView)inflatedViewGroup.findViewById(R.id.itemDescTextView);
        descTextView.setText(dataSetDescriptions[position]);
        /**
         * Return back the modified view
         */
        return inflatedViewGroup;
    }
    
    private enum SeparatorPositions
    {        
        /**
         * Defining the enum constants here that corresponds to
         * the category of the main menu items
         */
        CALL_MGMT ("Call Management", 0),
        SOCIAL_NETWORK_MGMT("Social Network Management", 2),
        ACCOUNT_MGMT("Account Management", 6),
        OTHERS("Others", 8);
        /**
         * Declaring the text property
         */
        private String text = null;
        /**
         * This is the position where the given separator need to be
         * injected.
         */
        private int position = 0;                        
        /**
         * This is the constructor that is implictly called by the compiler
         * when creating the enum constants          
         */
        private SeparatorPositions(String separatorText, int position)
        {
            this.text = separatorText;
            this.position = position;
        }
        /**
         * This method iterates over all of the defined enums, and return an enum object whos
         * position property is matching with the passed position parameter. If nothing is found,
         * null is returned instead.
         */
        public static SeparatorPositions getSeparatorAt(int position)
        {                        
            /**
             * Iterate over all of the defined enum constants
             */
            for(SeparatorPositions sp : values())
            {
                if(sp.position == position)
                {
                    return sp;
                }
            }
            return null;
        }
    }

}
