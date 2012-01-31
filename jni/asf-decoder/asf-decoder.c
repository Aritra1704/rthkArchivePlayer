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

#define AACD_MODULE "AsfDecoder[native]"

#include "asf-decoder.h"
#include "asf-common.h"
#include "asf-array-common.h"

#include <string.h>

extern AACDDecoder aacd_ffmpeg_wma_decoder;

/****************************************************************************************************
 * FUNCTIONS
 ****************************************************************************************************/


/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder
 * Method:    nativeStart
 * Signature: (Lcom/hei/android/app/rthkArchivePlayer/player/ArrayBufferReader;Lcom/hei/android/app/rthkArchivePlayer/player/decoder/Decoder/Info;)I
 */
JNIEXPORT jint JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder_nativeStart
  (JNIEnv *env, jobject thiz, jobject jreader, jobject aacInfo)
{
    AACD_TRACE( "start() start" );
    AACDDecoder *dec = &aacd_ffmpeg_wma_decoder;

    if (!dec)
    {
        AACD_ERROR( "start() decoder not found");
        return 0;
    }

    AACDArrayInfo *ainfo = aacda_start( env, dec, jreader, aacInfo );

    if (!ainfo)
    {
        AACD_ERROR( "start() cannot initialize decoder - out-of-memory error ?" );
        return 0;
    }

    ainfo->env = env;

    AACD_TRACE( "start() calling read_buffer" );

    unsigned char* buffer = aacda_read_buffer( ainfo );
    unsigned long buffer_size = ainfo->cinfo.bytesleft;

    AACD_TRACE( "start() got %d bytes from read_buffer", buffer_size );

    int pos = ainfo->decoder->sync( buffer, buffer_size );
    AACD_TRACE( "start() sync returned %d", pos );

    if (pos < 0)
    {
        AACD_ERROR( "start() failed - ADTS sync word not found" );
        aacda_stop( ainfo );

        return 0;
    }

    buffer += pos;
    buffer_size -= pos;

    AACD_TRACE( "start() calling decoder->start()" );
    long err = ainfo->decoder->start( &ainfo->cinfo, ainfo->ext, buffer, buffer_size );

    if (err < 0)
    {
        AACD_ERROR( "start() failed err=%d", err );
        aacda_stop( ainfo );

        return 0;
    }

    // remember pointers for first decode round:
    if (!ainfo->cinfo.input_ctrl)
    {
        ainfo->cinfo.buffer = buffer + err;
        ainfo->cinfo.bytesleft = buffer_size - err;
    }

    AACD_DEBUG( "start() bytesleft=%d", ainfo->cinfo.bytesleft );

    aacd_start_info2java( env, &ainfo->cinfo, aacInfo );

    ainfo->env = NULL;

    AACD_TRACE( "nativeStart() stop" );

    return (jint) ainfo;
}


/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder
 * Method:    nativeDecode
 * Signature: (I[SI)I
 */
JNIEXPORT jint JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder_nativeDecode
  (JNIEnv *env, jobject thiz, jint jinfo, jshortArray outBuf, jint outLen)
{
    AACDArrayInfo *ainfo = (AACDArrayInfo*) jinfo;
    ainfo->env = env;

    // prepare internal output buffer :
    jshort *jsamples = aacda_prepare_samples( ainfo, outLen );

    aacda_decode( ainfo, jsamples, outLen );

    // copy samples back to Java heap:
    (*env)->SetShortArrayRegion( env, outBuf, 0, ainfo->cinfo.round_samples, jsamples );

    aacd_decode_info2java( env, &ainfo->cinfo, ainfo->aacInfo );

    ainfo->env = NULL;

    return (jint) ainfo->cinfo.round_samples;
}


/*
 * Class:     com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder
 * Method:    nativeStop
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_hei_android_app_rthkArchivePlayer_player_decoder_AsfDecoder_nativeStop
  (JNIEnv *env, jobject thiz, jint jinfo)
{
    AACDArrayInfo *ainfo = (AACDArrayInfo*) jinfo;
    ainfo->env = env;
    aacda_stop( ainfo );
}

