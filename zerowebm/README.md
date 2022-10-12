# zerowebm

Androidの`MediaMuxer`を利用せずに、自前で`WebM`へ保存していくための実装があります。

## なぜ

`WebM`をストリーミングする際にファイルを細切れにする必要があるのですが、`MediaMuxer`にはファイルを分割したりする機能はないみたいです。  
なので`MediaMuxer`を使ってストリーミングする場合、`MediaMuxer`が書き込んだファイルから部分的にコピーして細切れに保存していく必要があり、ストレージの無駄遣いをしてしまいます。

これを解決するのが`zerowebm`です。

## できること

- WebMファイルへ書き込むバイト配列の生成

バイト配列を受け取るので、ファイルの保存処理を自由にできるようになりました。（好きなように分割できるようになった）

## しくみ
分かりづらいと思いますが

https://takusan.negitoro.dev/posts/video_webm_spec/

## これ単体で試したい！
あります

https://github.com/takusan23/ZeroWebM