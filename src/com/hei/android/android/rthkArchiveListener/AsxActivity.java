package com.hei.android.android.rthkArchiveListener;

import greendroid.app.ActionBarActivity;
import greendroid.app.GDListActivity;
import greendroid.widget.ActionBar;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.Item;
import greendroid.widget.item.SubtextItem;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.hei.android.android.rthkArchiveListener.model.AsxModel;
import com.hei.android.android.rthkArchiveListener.model.AsxModel.AsxEntryModel;

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
			model = AsxModel.createModelFromUrl(url);
		}
		Log.d("RTHK", model.toString());

		final ItemAdapter adapter = new ItemAdapter(this);
		final List<AsxEntryModel> entries = model.getEntries();
		for (final AsxEntryModel entry : entries) {
			final String title = entry.getTitle();
			final String abs = entry.getAbstract();

			final SubtextItem entryItem = new SubtextItem(title, abs);
			entryItem.setTag(entry);
			entryItem.enabled = true;
			adapter.add(entryItem);
		}

		setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final Item item = (Item) l.getAdapter().getItem(position);
		final Object tag = item.getTag();
		if (tag instanceof AsxEntryModel) {
			final AsxEntryModel model = (AsxEntryModel) tag;
			final String url = model.getRef();
			final Uri uri = Uri.parse(url);
			final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		}

	}
}