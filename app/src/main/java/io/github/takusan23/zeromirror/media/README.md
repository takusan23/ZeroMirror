# media

内部音声や画面録画を記録して、`H.264 / AAC / VP9 / Opus` にしてくれるクラスたちです。
`mp4 / WebM` にするのは `DashContainerWriter` / `WSContainerWriter` がやります。

## QtFastStart
`MediaMuxer`が作る`mp4`の`moovブロック`を先頭に移動するためのコードです。
以下の実装をお借りました、ありがとうございます！

https://github.com/ypresto/qtfaststart-java