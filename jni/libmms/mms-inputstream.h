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
/* Header for class com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream */

#ifndef _Included_com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream
#define _Included_com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream
 * Method:    nativeConnect
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream_nativeConnect
  (JNIEnv *, jobject, jstring);

/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream
 * Method:    nativeRead
 * Signature: (I[BII)I
 */
JNIEXPORT jint JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream_nativeRead
  (JNIEnv *, jobject, jint, jbyteArray, jint, jint);

/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream
 * Method:    nativeClose
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream_nativeClose
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream
 * Method:    nativeGetLength
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream_nativeGetLength
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream
 * Method:    nativeSeek
 * Signature: (ID)Z
 */
JNIEXPORT jboolean JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream_nativeSeek
  (JNIEnv *, jobject, jint, jdouble);

/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream
 * Method:    nativeGetHeader
 * Signature: (I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream_nativeGetHeader
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream
 * Method:    nativeGetSize
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream_nativeGetSize
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream
 * Method:    nativeSeekByte
 * Signature: (IJ)J
 */
JNIEXPORT jlong JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream_nativeSeekByte
  (JNIEnv *, jobject, jint, jlong);


/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream
 * Method:    nativeGetCurrentPos
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_mmsPlayer_MMSInputStream_nativeGetCurrentPos
  (JNIEnv *, jobject, jint);

#ifdef __cplusplus
}
#endif
#endif

