<p align="center">
    <img width="100" src="https://imgur.com/LtwpB75.png">
</p>
<p align="center">ぜろみらー</p>
<p align="center">Androidの画面をブラウザへミラーリング出来るアプリ</p>

<p align="center">
    <img height="300" src="https://imgur.com/ysPW4m3.png">
</p>

# ダウンロード
https://play.google.com/store/apps/details?id=io.github.takusan23.zeromirror

# 特徴

- ブラウザがあれば見れる
    - 同じLAN（同じ Wi-Fi）につながっていれば見れます
    - ブラウザがあれば見れます。**お手軽！！！！**
        - スマホでも見れる
- ほぼ設定なしで利用できる
    - Zero ( Configuration ) Mirroring → ぜろみらー
    - ビットレート、フレームレートの設定も可能です
- Android 10 以降は端末の内部音声も収録できます
- ブラウザ側は特に難しいことしていない
    - `MPEG-DASH`で配信されます
        - `VP9 / Opus / WebM`
        - `dash.js`を使ってます
    - `WebSocket`で`mp4ファイル`を細切れにして送り再生する機能もあったりします
        - `MEPG-DASH`が使えない場合に使えますが
        - 切り替え時にちらつくので、おすすめしません...

# スクリーンショット
`Jetpack Compose`を使っています。快適です。

<p align="center">
    <img width="200" src="https://imgur.com/9jP9IEr.png">
    <img width="200" src="https://imgur.com/tO6Rcnn.png">
</p>

# 開発者向け
あとでまた書くと思う...

## ビルド方法
多分最新版の`Android Studio`でビルドできると思います。  
このリポジトリをクローンして`Android Studio`で開いてください。

### Composeの部分が真っ赤になってる
Android Studio の Invalidate Caches を実行してみてください。

## ストリーミングしてる部分
`DashStreaming`、`WSStreaming` クラス参照

## 映像エンコーダー
`ScreenVideoEncoder`、`VideoEncoder` クラス参照

## ブログ記事

https://takusan.negitoro.dev/posts/android_standalone_webm_livestreaming/