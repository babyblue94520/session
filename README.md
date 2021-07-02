# Session Manager

## 需求

* 高併發時，高效能
* 降低內網頻寬消耗
* 高效能在線列表
* 踢人機制


## 設計

__Session__ 的內容，從建立到失效大概 __99%__ 不會頻繁異動主要內容，除了 __lastAccessedTime__

![](https://i.imgur.com/KlmQUeB.png)

* __Session__ 存放在 __RDBS(MySQL)__，解決 __在線列表__ 需求
* __Session attributes__ 使用 __Json__ 字串格式儲存，移除 __Java Serializable__ 語言限制，提高使用彈性
* 使用 __Redis Pub/Sub__ 通知清除舊的 __Session__
* 本地端緩存 __Session__ 排程批次的更新 __last accessed time__，降低對 __RDBS__ 的壓力
* 內建 __Ping__ 的狀態，實現前端檢查 __Session timeout__

## 效能

### 情境

在登入的情況下，呼叫返回本地緩存物件的 __API__

    {"data":[{"value":"1","name":"admin"}]}

__Spring Redis Session__

![](https://i.imgur.com/DgolX78.png)


* 單一服務在每秒 __3__ 千多請求下，讀取 __Session__ 佔用 __4__ 百多 __Mbps__ 頻寬

* 每次請求必須先從 __Redis__ 取回 __Session__ 字串，然後反序列成 __Java__ 物件

自製 __Session Manager__

![](https://i.imgur.com/rHWvzmO.png)
