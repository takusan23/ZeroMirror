package io.github.takusan23.zeromirror.dash

import java.text.SimpleDateFormat
import java.util.*

/**
 * MPEG-DASHのマニフェストファイルを作る
 */
object DashManifestTool {

    /**
     * ISO 8601 で映像データの利用可能時間を指定する必要があるため
     * MPEG-DASHの場合は指定時間になるまで再生を開始しない機能があるらしい。
     */
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.JAPAN)

    /**
     * マニフェストを作成する
     *
     * @param fileIntervalSec 動画ファイルの生成間隔
     * @return XML
     */
    fun createManifest(fileIntervalSec: Int = 3): String {
        // コンテンツが利用可能になる時間（ISO-8601）
        // この値があることで、途中から再生した場合でも途中のセグメントから取得するようになる
        val formattedAvailabilityStartTime = isoDateFormat.format(System.currentTimeMillis())
        // minimumUpdatePeriod="P60S" みたいな感じに指定すると、マニフェストファイルを指定した時間の間隔で更新してくれるみたい
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" availabilityStartTime="$formattedAvailabilityStartTime" maxSegmentDuration="PT${fileIntervalSec}S" minBufferTime="PT${fileIntervalSec}S" type="dynamic" profiles="urn:mpeg:dash:profile:isoff-live:2011,http://dashif.org/guidelines/dash-if-simple">
              <BaseURL>/</BaseURL>
              <Period start="PT0S">
                <AdaptationSet mimeType="video/webm">
                  <Role schemeIdUri="urn:mpeg:dash:role:2011" value="main" />
                  <!-- duration が更新頻度っぽい -->
                  <SegmentTemplate duration="$fileIntervalSec" initialization="/init.webm" media="/file_${"$"}Number${'$'}.webm" startNumber="0"/>
                  <Representation id="default" codecs="vp9,opus"/>
                </AdaptationSet>
              </Period>
            </MPD>
        """.trimIndent()
    }

}