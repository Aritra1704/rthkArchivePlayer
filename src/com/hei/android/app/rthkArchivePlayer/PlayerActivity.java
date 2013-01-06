package com.hei.android.app.rthkArchivePlayer;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.hei.android.app.rthkArchivePlayer.model.AsxModel;
import com.hei.android.app.rthkArchivePlayer.model.AsxModel.AsxEntryModel;
import com.hei.android.app.rthkArchivePlayer.model.EpisodeModel;
import com.hei.android.app.rthkArchivePlayer.model.ProgrammeModel;
import com.hei.android.app.rthkArchivePlayer.player.AudioPlayer;
import com.hei.android.app.rthkArchivePlayer.player.asfPlayer.AsfPlayer;
import com.hei.android.app.rthkArchivePlayer.player.message.PlayerMessageHandler;
import com.hei.android.app.rthkArchivePlayer.player.mmsPlayer.MMSPlayer;
import com.hei.android.app.rthkArchivePlayer.repository.HistoryRepository;
import com.hei.android.app.widget.actionBar.ActionBarActivity;

public class PlayerActivity extends ActionBarActivity {
	private static final String TAG = "PlayerActivity";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

	private ImageButton _playButton;
	private ImageButton _stopButton;
	private ImageButton _previousButton;
	private ImageButton _nextButton;
	private TextView _titleText;
	private TextView _lengthText;
	private SeekBar _seekBar;

	private AudioPlayer _player;
	private PlayerMessageHandler _messagHandler;

	private AsxModel _asx;
	private int _playingItem;
	private String _length;
	private String _title;

	private boolean _isPlaying = false;
	private boolean _isPaused = false;
	private boolean _isSeeking = false;
	private boolean _next = false;
	private boolean _previous = false;

	private WifiLock _wifiLock;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);

		final Intent intent = getIntent();
		final Uri data = intent.getData();
		if(data == null) {
			Log.e(TAG, "No url of asx file");
			finish();
			return;
		}

		_messagHandler = createMessageHandler();

		_playButton = (ImageButton) findViewById(R.id.player_play_pause_button);
		_stopButton = (ImageButton) findViewById(R.id.player_stop_button);
		_nextButton = (ImageButton) findViewById(R.id.player_next_button);
		_previousButton = (ImageButton) findViewById(R.id.player_previous_button);
		_titleText = (TextView) findViewById(R.id.player_title_text);
		_lengthText = (TextView) findViewById(R.id.player_length_text);
		_seekBar = (SeekBar) findViewById(R.id.player_seek_bar);

		_previousButton.setEnabled(false);
		_nextButton.setEnabled(false);
		_stopButton.setEnabled(false);
		_seekBar.setEnabled(false);

		final Serializable serializable = intent.getSerializableExtra(getString(R.string.key_episode));
		if(serializable != null) {
			final EpisodeModel episode = (EpisodeModel) serializable;
			final ProgrammeModel programme = episode.getProgramme();
			final String programmeName = programme.getName();
			final Date date = episode.getDate();
			_title = programmeName + " " + DATE_FORMAT.format(date);
			_titleText.setText(_title);
			setTitle(_title);

			HistoryRepository.getInstance(this).addHistory(this, episode);
		}

		final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		_wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, PlayerActivity.class.getName());

		initListeners();

		final String url = data.toString();
		final LoadAsxModelTask loadAsxModelTask = new LoadAsxModelTask();
		loadAsxModelTask.execute(new String[]{url});
	}

	@Override
	protected void onDestroy() {
		stop();
		super.onDestroy();
	}

	private PlayerMessageHandler createMessageHandler() {
		return new PlayerMessageHandler() {

			@Override
			protected void playerStopped() {
				if(_next) {
					_next = false;
					_seekBar.setProgress(0);
					playingItemIncrement();
					start();
					return;
				}

				if(_previous) {
					_previous = false;
					_seekBar.setProgress(0);
					playingItemDecrement();
					start();
					return;
				}

				final List<AsxEntryModel> entries = _asx.getEntries();
				final int size = entries.size();
				if(_playingItem < size) {
					next();
					start();

				}
				else {
					_playButton.setImageResource(R.drawable.player_play);
					_lengthText.setText("");
					_isPlaying = false;
				}
			}

			@Override
			protected void playerStarted(double length) {
				_playButton.setImageResource(R.drawable.player_pause);

				_length = getPrettyTimeString(length);
				_seekBar.setMax((int) length); 

				_isPlaying = true;
			}

			@Override
			protected void playerBufferStateUpdate(final boolean isPlaying,
					final int audioBufferSizeMs, final int audioBufferCapacityMs) {
				if(isPlaying && audioBufferCapacityMs <= 0) {
					_lengthText.setText(R.string.player_status_buffering);
				}
			}

			@Override
			protected void playerException(final Throwable t) {
				// TODO Auto-generated method stub

			}

			@Override
			protected void playerCurrentPosUpdate(double pos) {
				if(!_isSeeking) {
					final String currentPos = getPrettyTimeString(pos);
					_lengthText.setText(currentPos + "/" + _length);
					_seekBar.setProgress((int) pos); 
				}
			}

			@Override
			protected void playerSought() {
				_isSeeking = false;
			}
		};
	}

	private void initListeners() {
		_playButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View arg0) {
				if (_isPaused) {
					resume();
				}
				else if (_isPlaying) {
					pause();
				}
				else {
					start();
				}
			}
		});

		_seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				final int time = seekBar.getProgress();
				_player.seek(time);
				_lengthText.setText(R.string.player_status_buffering);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				_isSeeking = true;
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					final String seekTime = getPrettyTimeString(progress);
					_lengthText.setText(seekTime + "/" + _length);
				}
			}
		});

		_stopButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				stop();
			}
		});

		_previousButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				previous();
			}
		});

		_nextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				next();
			}
		});
	}

	private void start() {
		final List<AsxEntryModel> entries = _asx.getEntries();
		final AsxEntryModel asxEntryModel = entries.get(_playingItem - 1);

		final String url = asxEntryModel.getRef();
		_lengthText.setText(R.string.player_status_buffering);
		_seekBar.setEnabled(true);

		try {
			_player = new AsfPlayer(this, _messagHandler, -1);
			_player.playAsync(url);

		}
		catch (Exception e) {
			_wifiLock.acquire();
			_player = new MMSPlayer(_messagHandler);
			_player.playAsync(url);
		}
	}


	private void pause() {
		_wifiLock.release();
		_playButton.setImageResource(R.drawable.player_play);
		_isPaused = true;
		_player.pause();
	}

	private void resume() {
		_wifiLock.acquire();
		_playButton.setImageResource(R.drawable.player_pause);
		_isPaused = false;
		_player.resume();
	}


	private void stop() {
		if(_wifiLock.isHeld()) {
			_wifiLock.release();
		}
		if (_player != null) {
			_player.stop();
			_player = null;
			_seekBar.setProgress(0);
			_seekBar.setEnabled(false);
		}
	}

	private void next() {
		final List<AsxEntryModel> entries = _asx.getEntries();
		final int max = entries.size();
		if(_playingItem >= max) {
			throw new RuntimeException("Invalid playing item = " + _playingItem);
		}
		_next = true;
		stop();
	}

	private void previous() {
		if(_playingItem <= 1) {
			throw new RuntimeException("Invalid playing item = " + _playingItem);
		}
		_previous = true;
		stop();
	}

	private String getPrettyTimeString(double sec) {
		final int min = (int) (sec / 60.0);
		final int remainSec = ((int) sec) % 60;
		return String.format("%1$02d:%2$02d", min, remainSec);
	}

	private void playingItemIncrement() {
		setPlayingItem(_playingItem + 1);
	}

	private void playingItemDecrement() {
		setPlayingItem(_playingItem - 1);
	}

	private void setPlayingItem(int playingItem) {
		final int max = _asx.getEntries().size();
		if(playingItem > max || playingItem < 1) {
			throw new RuntimeException("Invalid playint item = " + playingItem); 
		}

		_previousButton.setEnabled(playingItem > 1);
		_nextButton.setEnabled(playingItem < max);

		final String title = _title + " (" + playingItem + "/" + max + ")";
		setTitle(title);
		_titleText.setText(title);

		_playingItem = playingItem;
	}

	private class LoadAsxModelTask extends AsyncTask<String, Void, AsxModel> {
		@Override
		protected AsxModel doInBackground(String... urls) {
			if(urls.length < 1) {
				return null;
			}
			try {
				final AsxModel asxModel = AsxModel.createModelFromUrl(urls[0]);
				return asxModel;
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
				return null;
			}
		}

		@Override
		protected void onPostExecute(AsxModel model) {
			if(model == null) {
				new AlertDialog.Builder(PlayerActivity.this)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("無法下載播法清單")
				.setMessage("請檢查裝置是否連接到互聯網。")
				.setPositiveButton("確定", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface arg0, final int arg1) {
						PlayerActivity.this.finish();
					}

				})
				.create()
				.show();
			}
			else {
				Log.d(TAG, model.toString());

				if(_title == null) {
					_title = _asx.getTitle();
				}

				_asx = model;
				setPlayingItem(1);

				start();
			}
		}
	}
}
