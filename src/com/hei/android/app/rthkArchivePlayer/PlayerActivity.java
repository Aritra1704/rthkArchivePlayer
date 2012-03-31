package com.hei.android.app.rthkArchivePlayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.hei.android.app.rthkArchivePlayer.player.AudioPlayer;
import com.hei.android.app.rthkArchivePlayer.player.asfPlayer.AsfPlayer;
import com.hei.android.app.rthkArchivePlayer.player.message.PlayerMessageHandler;
import com.hei.android.app.rthkArchivePlayer.player.mmsPlayer.MMSPlayer;

public class PlayerActivity extends Activity {
	private static final String STATUS_BUFFERING = "Buffering...";

	private Button _playButton;
	private Button _skipButton;
	private TextView _statusText;
	private SeekBar _seekBar;
	
	private AudioPlayer _player;
	private PlayerMessageHandler _messagHandler;
	
	private String _url;
	private String _length;
	private double _pos;
	
	private boolean _isPlaying = false;
	private boolean _isPaused = false;
	private boolean _isSeeking = false;

	private WifiLock _wifiLock;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);

		final Intent intent = getIntent();
		_url = intent.getStringExtra(getString(R.string.key_url));

		_messagHandler = createMessageHandler();

		_playButton = (Button) findViewById(R.id.playButton);
		_statusText = (TextView) findViewById(R.id.playerLengthText);
		_seekBar = (SeekBar) findViewById(R.id.playerSeekBar);
		_skipButton = (Button) findViewById(R.id.playeSkipButton);
		
		_statusText.setText(STATUS_BUFFERING);
		
		final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		_wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, PlayerActivity.class.getName());
		
		initListeners();
		start();
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
				_playButton.setEnabled(true);
				_playButton.setText("Play");
				_statusText.setText("");
				_isPlaying = false;
			}

			@Override
			protected void playerStarted(double length) {
				_playButton.setEnabled(true);
				_playButton.setText("Pause");

				_length = getPrettyTimeString(length);
				_seekBar.setMax((int) length); 

				_isPlaying = true;
			}

			@Override
			protected void playerBufferStateUpdate(final boolean isPlaying,
					final int audioBufferSizeMs, final int audioBufferCapacityMs) {
				if(isPlaying && audioBufferCapacityMs <= 0) {
					_statusText.setText(STATUS_BUFFERING);
				}

			}

			@Override
			protected void playerException(final Throwable t) {
				// TODO Auto-generated method stub

			}

			@Override
			protected void playerCurrentPosUpdate(double pos) {
				_pos = pos;
				if(!_isSeeking) {
					final String currentPos = getPrettyTimeString(pos);
					_statusText.setText(currentPos + "/" + _length);
					_seekBar.setProgress((int) pos); 
				}
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
				_isSeeking = false;
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
					_statusText.setText(seekTime + "/" + _length);
				}
			}
		});
		
		_skipButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_player.seek(_pos + 300);
			}
		});
	}

	private void start() {
		_wifiLock.acquire();
		_playButton.setEnabled(false);
		
		try {
			_player = new AsfPlayer(this, _messagHandler, -1);
			_player.playAsync(_url);
			
		}
		catch (Exception e) {
			_player = new MMSPlayer(_messagHandler);
			_player.playAsync(_url);
		}
	}
	
	
	private void pause() {
		_wifiLock.release();
		_playButton.setText("Resume");
		_isPaused = true;
		_player.pause();
	}
	
	private void resume() {
		_wifiLock.acquire();
		_playButton.setText("Pause");
		_isPaused = false;
		_player.resume();
	}


	private void stop() {
		_wifiLock.release();
		_playButton.setEnabled(false);

		if (_player != null) {
			_player.stop();
			_player = null;
		}
	}

	private String getPrettyTimeString(double sec) {
		final int min = (int) (sec / 60.0);
		final int remainSec = ((int) sec) % 60;
		return String.format("%1$02d:%2$02d", min, remainSec);
	}
}
