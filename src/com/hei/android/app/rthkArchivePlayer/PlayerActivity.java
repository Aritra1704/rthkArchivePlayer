package com.hei.android.app.rthkArchivePlayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.hei.android.app.rthkArchivePlayer.player.AsfPlayer;
import com.hei.android.app.rthkArchivePlayer.player.message.PlayerMessageHandler;

public class PlayerActivity extends Activity {

	private Button _playButton;
	private boolean _isPlaying;
	private AsfPlayer _player;
	private String _url;
	private PlayerMessageHandler _messagHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);

		final Intent intent = getIntent();
		_url = intent.getStringExtra(getString(R.string.key_url));

		_messagHandler = createMessageHandler();
		
		_playButton = (Button) findViewById(R.id.playButton);
		_playButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (_isPlaying) {
					stop();
				}
				else {
					start();
				}
			}
		});
	}

	private PlayerMessageHandler createMessageHandler() {
		return new PlayerMessageHandler() {
			
			@Override
			protected void playerStopped() {
				_playButton.setEnabled(true);
				_playButton.setText("Play");
				_isPlaying = false;
			}
			
			@Override
			protected void playerStarted() {
				_playButton.setEnabled(true);
				_playButton.setText("Stop");
				_isPlaying = true;
			}
			
			@Override
			protected void playerPCMFeedBuffer(boolean isPlaying,
					int audioBufferSizeMs, int audioBufferCapacityMs) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			protected void playerException(Throwable t) {
				// TODO Auto-generated method stub
				
			}
		};
	}

	private void start() {
		_playButton.setEnabled(false);
		_player = new AsfPlayer(_messagHandler);
		_player.playAsync(_url);
	}


	private void stop() {
		_playButton.setEnabled(false);
		
		if (_player != null) { 
			_player.stop(); 
			_player = null; 
		}
	}
}
