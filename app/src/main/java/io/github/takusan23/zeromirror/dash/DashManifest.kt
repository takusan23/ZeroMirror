package io.github.takusan23.zeromirror.dash

import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

/**
 * MPEG-DASHのマニフェストファイルを作る
 *
 * [MPD検証ツール](https://conformance.dashif.org/)
 */
object DashManifestTool {

    /**
     * ISO 8601 で映像データの利用可能時間を指定する必要があるため
     * MPEG-DASHの場合は指定時間になるまで再生を開始しない機能があるらしい。
     */
    private val isoDateFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.JAPAN)
    } else {
        // 本当は "yyyy-MM-dd'T'HH:mm:ssXXX" が正解だが、X が 7以降 のため、Z を使っている。
        // JavaScriptでパース出来ているので問題ないはず
        // 機種変してください；；
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.JAPAN)
    }

    /**
     * マニフェストを作成する
     *
     * @param fileIntervalSec 動画ファイルの生成間隔
     * @param hasAudio 音声を動画に含めている場合はtrue
     * @param isVP8 VP9じゃなくてVP8を使う場合はtrue
     * @return XML
     */
    fun createManifest(
        fileIntervalSec: Int = 1,
        hasAudio: Boolean = false,
        isVP8: Boolean = false,
    ): String {
        // なんか dynamic のときは必要？
        val publishTime = isoDateFormat.format(System.currentTimeMillis())
        // コンテンツが利用可能になる時間（ISO-8601）
        // この値があることで、途中から再生した場合でも途中のセグメントから取得するようになる
        val availabilityStartTime = isoDateFormat.format(System.currentTimeMillis())
        val videoCodec = if (isVP8) "vp8" else "vp9"
        // minimumUpdatePeriod="P60S" みたいな感じに指定すると、マニフェストファイルを指定した時間の間隔で更新してくれるみたい
        return if (hasAudio) {
            """
            <?xml version="1.0" encoding="utf-8"?>
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" publishTime="$publishTime" availabilityStartTime="$availabilityStartTime" maxSegmentDuration="PT${fileIntervalSec}S" minBufferTime="PT${fileIntervalSec}S" type="dynamic" profiles="urn:mpeg:dash:profile:isoff-live:2011,http://dashif.org/guidelines/dash-if-simple">
              <BaseURL>/</BaseURL>
              <Period start="PT0S" id="live">
              
                <AdaptationSet mimeType="video/webm" contentType="video">
                  <Role schemeIdUri="urn:mpeg:dash:role:2011" value="main" />
                  <!-- duration が更新頻度っぽい -->
                  <SegmentTemplate duration="$fileIntervalSec" initialization="/video_init.webm" media="/video${"$"}Number${'$'}.webm" startNumber="0"/>
                  <Representation id="video_track" codecs="$videoCodec"/>
                </AdaptationSet>
                
                <AdaptationSet mimeType="audio/webm" contentType="audio">
                  <Role schemeIdUri="urn:mpeg:dash:role:2011" value="main" />
                  <!-- duration が更新頻度っぽい -->
                  <SegmentTemplate duration="$fileIntervalSec" initialization="/audio_init.webm" media="/audio${"$"}Number${'$'}.webm" startNumber="0"/>
                  <Representation id="audio_track" codecs="opus"/>
                </AdaptationSet>
              </Period>
            </MPD>
        """.trimIndent()
        } else {
            """
            <?xml version="1.0" encoding="utf-8"?>
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" publishTime="$publishTime" availabilityStartTime="$availabilityStartTime" maxSegmentDuration="PT${fileIntervalSec}S" minBufferTime="PT${fileIntervalSec}S" type="dynamic" profiles="urn:mpeg:dash:profile:isoff-live:2011,http://dashif.org/guidelines/dash-if-simple">
              <BaseURL>/</BaseURL>
              <Period start="PT0S" id="live">              
                <AdaptationSet mimeType="video/webm" contentType="video">
                  <Role schemeIdUri="urn:mpeg:dash:role:2011" value="main" />
                  <!-- duration が更新頻度っぽい -->
                  <SegmentTemplate duration="$fileIntervalSec" initialization="/video_init.webm" media="/video${"$"}Number${'$'}.webm" startNumber="0"/>
                  <Representation id="video_track" codecs="$videoCodec"/>
                </AdaptationSet>
              </Period>
            </MPD>
        """.trimIndent()
        }
    }

}