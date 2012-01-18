/*
** AACPlayer - Freeware Advanced Audio (AAC) Player for Android
** Copyright (C) 2011 Spolecne s.r.o., http://www.spoledge.com
**  
** This program is free software; you can redistribute it and/or modify
** it under the terms of the GNU General Public License as published by
** the Free Software Foundation; either version 3 of the License, or
** (at your option) any later version.
** 
** This program is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
** GNU General Public License for more details.
** 
** You should have received a copy of the GNU General Public License
** along with this program. If not, see <http://www.gnu.org/licenses/>.
**/

#include <jni.h>
/* Header for class com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder */

#ifndef _Included_com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder
#define _Included_com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder
 * Method:    nativeStart
 * Signature: (Lcom/hei/android/app/rthkArchivePlayer/player/ArrayBufferReader;Lcom/hei/android/app/rthkArchivePlayer/player/decoder/Decoder/Info;)I
 */
JNIEXPORT jint JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder_nativeStart
  (JNIEnv *, jobject, jobject, jobject);

/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder
 * Method:    nativeDecode
 * Signature: (I[SI)I
 */
JNIEXPORT jint JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder_nativeDecode
  (JNIEnv *, jobject, jint, jshortArray, jint);

/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder
 * Method:    nativeStop
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder_nativeStop
  (JNIEnv *, jobject, jint);

#ifdef __cplusplus
}
#endif
#endif
