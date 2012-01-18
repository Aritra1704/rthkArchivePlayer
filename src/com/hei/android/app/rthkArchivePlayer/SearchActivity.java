package com.hei.android.app.rthkArchivePlayer;

import greendroid.app.ActionBarActivity;
import greendroid.app.GDListActivity;
import greendroid.widget.ActionBar;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.Item;
import greendroid.widget.item.TextItem;

import java.io.IOException;
import java.net.URLEncoder;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import com.hei.android.app.rthkArchivePlayer.model.BasicProgrammeModel;

public class SearchActivity extends GDListActivity {
	private static final int SEARCH_BTN_ID = 0;
	private static final String SEARCH_URL = "http://search.rthk.org.hk/search/search_archive_2010.php?archivetype=all&keyword=";

	private AlertDialog _searchDialog;

	public SearchActivity() {
		super(ActionBar.Type.Empty);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle("RTHK 節目重溫");
		addActionBarItem(ActionBarItem.Type.Search);

		final LayoutInflater factory = LayoutInflater.from(this);
		final View dialogView = factory.inflate(R.layout.searh_dialog, null);
		_searchDialog = new AlertDialog.Builder(this)
		.setTitle("搜尋")
		.setView(dialogView)
		.setPositiveButton("搜尋", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int whichButton) {
				final EditText editText = (EditText) dialogView.findViewById(R.id.search_field);
				final String keywords = editText.getText().toString();

				if(keywords.equals("")) {
					return;
				}

				final String query = URLEncoder.encode(keywords);
				final ItemAdapter adapter = new ItemAdapter(SearchActivity.this);
				final Connection connection = Jsoup.connect(SEARCH_URL + query);
				try {
					final Document document = connection.get();
					final Elements links = document.select("a");
					for (final Element link : links) {
						final String href = link.attr("href");
						final String text = link.text();

						final BasicProgrammeModel programme = new BasicProgrammeModel(text, href);
						final TextItem item = new TextItem(text);
						item.setTag(programme);
						item.enabled = true;

						adapter.add(item);
					}
				} catch (final IOException e) {
					e.printStackTrace();

					new AlertDialog.Builder(SearchActivity.this)
					.setIcon(R.drawable.alert_dialog_icon)
					.setTitle("搜尋失敗")
					.setMessage("請檢查裝置是否連接到互聯網。")
					.setPositiveButton("確定", new OnClickListener() {

						@Override
						public void onClick(final DialogInterface arg0, final int arg1) {

						}

					})
					.create()
					.show();
				}

				setListAdapter(adapter);
			}

		})
		.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int whichButton) {

				/* User clicked cancel so do some stuff */
			}
		})
		.create();

		_searchDialog.show();
	}

	@Override
	protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final Item item = (Item) l.getAdapter().getItem(position);
		final Object tag = item.getTag();

		if (tag instanceof BasicProgrammeModel) {
			final BasicProgrammeModel programme = (BasicProgrammeModel) tag;
			final Intent intent = new Intent(this, ProgrammeActivity.class);
			intent.putExtra(getString(R.string.key_programme), programme);
			intent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE, programme.getName());
			startActivity(intent);
		}
	}

	@Override
	public boolean onHandleActionBarItemClick(final ActionBarItem item, final int position) {
		final int itemId = item.getItemId();
		if(itemId == SEARCH_BTN_ID) {
			_searchDialog.show();
		}

		return false;
	}
}
