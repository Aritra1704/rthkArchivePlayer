package com.hei.android.app.rthkArchivePlayer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hei.android.app.rthkArchivePlayer.R;

public class ProgrammeItemView extends LinearLayout{
	private TextView _textView;
	private ImageView _starBtn;
	private boolean _starred;
	
	public ProgrammeItemView(Context context) {
		super(context);
        initWidgets(context);
	}
	
    public ProgrammeItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWidgets(context);
    }

    public ProgrammeItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initWidgets(context);
    }
    
    private void initWidgets(Context context) {
    	final LayoutInflater layoutInflater = LayoutInflater.from(context);
    	layoutInflater.inflate(R.layout.programme_item_view,this);
    	
        _textView = (TextView) findViewById(R.id.programme_item_text);
        _starBtn = (ImageView) findViewById(R.id.programme_item_star);
    }
    
    public void setText(CharSequence text) {
    	_textView.setText(text);
    }
    
    public void setStarred(boolean starred) {
    	_starred = starred;
    	if(_starred) {
            _starBtn.setImageResource(R.drawable.programme_item_star_filled);
    	}
    	else {
            _starBtn.setImageResource(R.drawable.programme_item_star_empty);
    	}
    }
    
    public void toggleStarred() {
    	setStarred(!_starred);
    }
    
    public boolean isStarred() {
    	return _starred;
    }
    
    public void setStarButtonOnClickListener(OnClickListener l) {
    	_starBtn.setOnClickListener(new StarButtonOnClickListnerWrapper(l, this));
    }
    
    private static class StarButtonOnClickListnerWrapper implements OnClickListener{
    	private final OnClickListener _underlyingListener;
    	private final ProgrammeItemView _itemView;
    	
    	StarButtonOnClickListnerWrapper(OnClickListener l, ProgrammeItemView itemView) {
    		_underlyingListener = l;
    		_itemView = itemView;
    	}
    	
		@Override
		public void onClick(View v) {
			_itemView.toggleStarred();
			_underlyingListener.onClick(v);
		}
    	
    }
}
