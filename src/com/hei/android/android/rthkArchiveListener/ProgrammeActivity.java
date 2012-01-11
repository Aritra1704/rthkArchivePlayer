package com.hei.android.android.rthkArchiveListener;

import greendroid.app.ActionBarActivity;
import greendroid.app.GDListActivity;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.Item;
import greendroid.widget.item.SeparatorItem;
import greendroid.widget.item.TextItem;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.hei.android.android.rthkArchiveListener.model.BasicProgrammeModel;
import com.hei.android.android.rthkArchiveListener.model.EpisodeModel;

public class ProgrammeActivity extends GDListActivity {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy-MM-dd");

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final BasicProgrammeModel programme = (BasicProgrammeModel) intent.getSerializableExtra(getString(R.string.key_programme));

		final ItemAdapter adapter = new ItemAdapter(this);

		final String pageUrl = programme.getPageUrl();
		final Connection conn = Jsoup.connect(pageUrl);
		try {
			final Document document = conn.get();
			final Elements items = document.select("div.title a");
			for (final Element element : items) {
				final String href = element.attr("href");
				final String text = element.text();

				final int indexOfSpace = text.indexOf(' ');
				if(indexOfSpace == -1) {
					continue;
				}

				final String dateString = text.substring(0, indexOfSpace);
				final String title = text.substring(indexOfSpace + 1);

				final Date date;
				try {
					date = DATE_FORMAT.parse(dateString);
				}catch (final ParseException e) {
					e.printStackTrace();
					continue;
				}

				final EpisodeModel episode = new EpisodeModel(title, "http://programme.rthk.org.hk/channel/radio/" + href, date);

				final SeparatorItem separatorItem = new SeparatorItem(dateString);
				adapter.add(separatorItem);

				final TextItem item = new TextItem(title);
				item.setTag(episode);
				item.enabled = true;

				adapter.add(item);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}

		setListAdapter(adapter);
	}


	@Override
	protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final Item item = (Item) l.getAdapter().getItem(position);
		final Object tag = item.getTag();

		if (tag instanceof EpisodeModel) {
			final EpisodeModel episode = (EpisodeModel) tag;
			final Intent intent = new Intent(this, AsxActivity.class);
			final String asxUrl = episode.getAsxUrl();

			if(asxUrl == null) {
				new AlertDialog.Builder(this)
				.setMessage("網上直播完畢稍後提供節目重溫。")
				.setPositiveButton("確定", new OnClickListener() {

					@Override
					public void onClick(final DialogInterface arg0, final int arg1) {

					}

				})
				.show();
				return;
			}

			final Uri uri = Uri.parse(asxUrl);
			intent.setData(uri);

			final String name = episode.getName();
			intent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE, name);

			startActivity(intent);
		}

	}
}
