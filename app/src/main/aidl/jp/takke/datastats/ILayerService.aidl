// ILayerService.aidl
package jp.takke.datastats;

oneway interface ILayerService {

    void restart();

    void stop();

    void startSnapshot(long previewBytes);

    // 補間モード中でも即座に再描画させる。previewBytes は表示値。
    // 直接 static 変数を書き換える裏口的な結合を避けるため AIDL 経由で伝達する。
    void forceRedraw(long previewBytes);
}
