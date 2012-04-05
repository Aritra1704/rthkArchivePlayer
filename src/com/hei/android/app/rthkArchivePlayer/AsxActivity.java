package com.hei.android.app.rthkArchivePlayer;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hei.android.app.rthkArchivePlayer.model.AsxModel;
import com.hei.android.app.rthkArchivePlayer.model.AsxModel.AsxEntryModel;
import com.hei.android.app.widget.actionBar.ActionBarActivity;

public class AsxActivity extends ActionBarActivity{

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final String actionBarTitle = intent.getStringExtra(getString(R.string.key_action_bar_title));
		if (actionBarTitle != null) {
			setTitle(actionBarTitle);
		}
		else {
			setTitle("RTHK 節目重溫");
		}

		final Uri data = intent.getData();
		final AsxModel model;
		if(data == null) {
			//model = AsxModel.createModelFromUrl("http://www.rthk.hk/asx/rthk/radio1/Free_as_the_wind/20120105.asx");
			finish();
			return;
		}
		else {
			final String url = data.toString();
			try {
				model = AsxModel.createModelFromUrl(url);
			} catch (final RuntimeException e) {

				new AlertDialog.Builder(AsxActivity.this)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("無法下載播法清單")
				.setMessage("請檢查裝置是否連接到互聯網。")
				.setPositiveButton("確定", new OnClickListener() {

					@Override
					public void onClick(final DialogInterface arg0, final int arg1) {
						AsxActivity.this.finish();
					}

				})
				.create()
				.show();
				return;
			}
		}
		Log.d("RTHK", model.toString());

		final LinearLayout layout = new LinearLayout(this);
		final List<AsxEntryModel> entries = model.getEntries();
		final int entrySize = entries.size();
		final List<String> urls = new ArrayList<String>(entrySize);
		
		for (final AsxEntryModel entry : entries) {
			final String title = entry.getTitle();
			final String abs = entry.getAbstract();
			
			final String url = entry.getRef();
			urls.add(url);

			final TextView textView = new TextView(this);
			textView.setText(title);
			layout.addView(textView);
			textView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					final Intent intent = new Intent(AsxActivity.this, PlayerActivity.class);
					intent.putExtra(getString(R.string.key_url), url);
					startActivity(intent);
					
				}
			});
			
		}

		setContentView(layout);
		

		/*addActionBarItem(ActionBarItem.Type.Star);
		getActionBar().setOnActionBarListener(new OnActionBarListener() {
			
			@Override
			public void onActionBarItemClicked(int position) {
				final Intent intent = new Intent(AsxActivity.this, DownloadActivity.class);
				intent.putExtra(getString(R.string.key_playlist), urls.toArray(new String[entrySize]));
				startActivity(intent);
			}
		});*/
	}

	
}