package com.hei.android.app.rthkArchivePlayer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hei.android.app.rthkArchivePlayer.R;

public class EpisodeItemView extends LinearLayout {
	private TextView _textView;
	private ImageView _downloadBtn;
	
	public EpisodeItemView(Context context) {
		super(context);
        initWidgets();
	}
	
    public EpisodeItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWidgets();
    }

    public EpisodeItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initWidgets();
    }
    
    private void initWidgets() {
        _textView = (TextView) findViewById(R.id.programme_item_text);
        _downloadBtn = (ImageView) findViewById(R.id.episode_item_download);
    }
    
    public void setText(CharSequence text) {
    	_textView.setText(text);
    }
    
    public void setDownloadButtonOnClickListener(OnClickListener l) {
    	_downloadBtn.setOnClickListener(l);
    }
}
