package com.hei.android.app.rthkArchivePlayer;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;

public class StartActivityOnClickListener implements OnClickListener {
	private final Class<?> _activiy;
	
	public StartActivityOnClickListener(Class<?> activity) {
		_activiy = activity;
	}
	
	@Override
	public void onClick(View v) {
		final Context context = v.getContext();
		final Intent intent = new Intent(context, _activiy);
		context.startActivity(intent);
	}

}
