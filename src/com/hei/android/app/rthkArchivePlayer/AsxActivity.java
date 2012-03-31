package com.hei.android.app.rthkArchivePlayer;

import greendroid.app.ActionBarActivity;
import greendroid.app.GDListActivity;
import greendroid.widget.ActionBar;
import greendroid.widget.ActionBar.OnActionBarListener;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.Item;
import greendroid.widget.item.SubtextItem;

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
import android.widget.ListView;

import com.hei.android.app.rthkArchivePlayer.model.AsxModel;
import com.hei.android.app.rthkArchivePlayer.model.AsxModel.AsxEntryModel;

public class AsxActivity extends GDListActivity{
	public AsxActivity() {
		super(ActionBar.Type.Empty);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final String actionBarTitle = intent.getStringExtra(ActionBarActivity.GD_ACTION_BAR_TITLE);
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

		final ItemAdapter adapter = new ItemAdapter(this);
		final List<AsxEntryModel> entries = model.getEntries();
		final int entrySize = entries.size();
		final List<String> urls = new ArrayList<String>(entrySize);
		
		for (final AsxEntryModel entry : entries) {
			final String title = entry.getTitle();
			final String abs = entry.getAbstract();

			final SubtextItem entryItem = new SubtextItem(title, abs);
			entryItem.setTag(entry);
			entryItem.enabled = true;
			adapter.add(entryItem);
			
			final String url = entry.getRef();
			urls.add(url);
		}

		setListAdapter(adapter);
		

		addActionBarItem(ActionBarItem.Type.Star);
		getActionBar().setOnActionBarListener(new OnActionBarListener() {
			
			@Override
			public void onActionBarItemClicked(int position) {
				final Intent intent = new Intent(AsxActivity.this, DownloadActivity.class);
				intent.putExtra(getString(R.string.key_playlist), urls.toArray(new String[entrySize]));
				startActivity(intent);
			}
		});
	}

	@Override
	protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final Item item = (Item) l.getAdapter().getItem(position);
		final Object tag = item.getTag();
		if (tag instanceof AsxEntryModel) {
			final AsxEntryModel model = (AsxEntryModel) tag;
			final String url = model.getRef();
			//			final Uri uri = Uri.parse(url);
			//			final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			final Intent intent = new Intent(this, PlayerActivity.class);
			intent.putExtra(getString(R.string.key_url), url);
			startActivity(intent);
		}
	}
	
}