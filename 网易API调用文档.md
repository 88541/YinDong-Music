# 歌曲搜索 API 调用文档

## 接口地址

```
GET http://156.225.18.78:3000/search
```

## 重要说明

> **本 API 已内置登录 Cookie**，支持 VIP 音乐完整播放，无需额外传递 Cookie 参数。

## 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keywords | string | 是 | 搜索关键词（歌曲名、歌手名等） |
| type | number | 否 | 搜索类型，默认为 1 |
| limit | number | 否 | 返回数量，默认为 30 |
| offset | number | 否 | 偏移量，用于分页，默认为 0 |
| cookie | string | 否 | 自定义 Cookie（可选，默认使用内置 VIP Cookie）|

### 搜索类型 (type)

| 值 | 说明 |
|----|------|
| 1 | 单曲 |
| 10 | 专辑 |
| 100 | 歌手 |
| 1000 | 歌单 |
| 1002 | 用户 |
| 1004 | MV |
| 1006 | 歌词 |
| 1009 | 电台 |
| 1014 | 视频 |
| 1018 | 综合 |

## 请求示例

### 基础搜索
```
http://156.225.18.78:3000/search?keywords=周杰伦晴天
```

### 指定返回数量
```
http://156.225.18.78:3000/search?keywords=周杰伦&limit=10
```

### 分页搜索
```
http://156.225.18.78:3000/search?keywords=周杰伦&limit=30&offset=30
```

### 搜索歌手
```
http://156.225.18.78:3000/search?keywords=周杰伦&type=100
```

### 搜索歌单
```
http://156.225.18.78:3000/search?keywords=华语经典&type=1000
```

---

## 热门搜索

### 1. 获取默认搜索关键词

当搜索框为空时显示的默认关键词。

#### 接口地址
```
GET http://156.225.18.78:3000/search/default
```

#### 请求示例
```
http://156.225.18.78:3000/search/default
```

#### 响应示例
```json
{
  "code": 200,
  "data": {
    "showKeyword": "周杰伦",
    "realkeyword": "周杰伦",
    "searchType": 1018,
    "action": 1740916854339
  }
}
```

### 2. 获取热门搜索列表

#### 接口地址
```
GET http://156.225.18.78:3000/search/hot
```

#### 请求示例
```
http://156.225.18.78:3000/search/hot
```

#### 响应示例
```json
{
  "code": 200,
  "result": {
    "hots": [
      {
        "first": "周杰伦",
        "second": 1,
        "iconType": 0
      },
      {
        "first": "晴天",
        "second": 2,
        "iconType": 0
      },
      {
        "first": "薛之谦",
        "second": 3,
        "iconType": 0
      }
    ]
  }
}
```

### 3. 获取热搜列表（详细）

#### 接口地址
```
GET http://156.225.18.78:3000/search/hot/detail
```

#### 请求示例
```
http://156.225.18.78:3000/search/hot/detail
```

#### 响应示例
```json
{
  "code": 200,
  "data": [
    {
      "searchWord": "周杰伦",
      "score": 1234567,
      "content": "周杰伦新专辑",
      "source": 0,
      "iconType": 0,
      "iconUrl": null,
      "url": null,
      "alg": "alg_search_hot",
      "type": 1
    }
  ]
}
```

### 4. 获取搜索建议

输入关键词时自动提示相关搜索。

#### 接口地址
```
GET http://156.225.18.78:3000/search/suggest
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keywords | string | 是 | 搜索关键词 |
| type | string | 否 | 返回类型，mobile 为移动端 |

#### 请求示例
```
http://156.225.18.78:3000/search/suggest?keywords=周&type=mobile
```

#### 响应示例
```json
{
  "code": 200,
  "result": {
    "allMatch": [
      {
        "keyword": "周杰伦",
        "type": 1,
        "alg": "alg_search",
        "lastKeyword": "周"
      },
      {
        "keyword": "周深",
        "type": 1,
        "alg": "alg_search",
        "lastKeyword": "周"
      }
    ]
  }
}
```

---

## 响应示例

```json
{
  "result": {
    "songs": [
      {
        "id": 447926067,
        "name": "晴天",
        "artists": [
          {
            "id": 6452,
            "name": "周杰伦",
            "picUrl": null,
            "alias": [],
            "albumSize": 0,
            "picId": 0,
            "fansGroup": null,
            "img1v1Url": "https://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg",
            "img1v1": 0,
            "trans": null
          }
        ],
        "album": {
          "id": 34880,
          "name": "叶惠美",
          "artist": {
            "id": 0,
            "name": "",
            "picUrl": null,
            "alias": [],
            "albumSize": 0,
            "picId": 0,
            "fansGroup": null,
            "img1v1Url": "https://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg",
            "img1v1": 0,
            "trans": null
          },
          "publishTime": 1059580800000,
          "size": 11,
          "copyrightId": 1007,
          "status": 1,
          "picId": 109951163200249252,
          "mark": 0
        },
        "duration": 269000,
        "copyrightId": 1007,
        "status": 0,
        "alias": [],
        "rtype": 0,
        "ftype": 0,
        "mvid": 0,
        "fee": 1,
        "rUrl": null,
        "mark": 8192
      }
    ],
    "hasMore": true,
    "songCount": 600
  },
  "code": 200
}
```

## 响应字段说明

### 歌曲信息 (songs)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 歌曲 ID（获取播放链接需要） |
| name | string | 歌曲名称 |
| artists | array | 歌手信息数组 |
| album | object | 专辑信息 |
| duration | number | 歌曲时长（毫秒） |
| fee | number | 付费类型：0=免费，1=VIP，4=付费专辑 |
| mvid | number | MV ID，0 表示无 MV |

### 歌手信息 (artists)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 歌手 ID |
| name | string | 歌手名称 |

### 专辑信息 (album)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 专辑 ID |
| name | string | 专辑名称 |
| publishTime | number | 发布时间（时间戳） |

## 获取歌曲播放链接

### 接口地址

```
GET http://156.225.18.78:3000/song/url/v1
```

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 歌曲 ID |
| level | string | 否 | 音质等级，默认为 `exhigh` |

### 音质等级 (level)

| 等级 | 说明 | 码率 | 适用场景 | 可用性 |
|------|------|------|----------|--------|
| `standard` | 标准音质 | 128kbps MP3 | 省流量播放 | ✅ 全部歌曲可用 |
| `exhigh` | 极高音质 | 320kbps MP3 | 在线播放推荐 | ✅ 全部歌曲可用 |
| `lossless` | 无损音质 | FLAC | 高品质下载 | ✅ VIP歌曲可用 |
| `hires` | Hi-Res 音质 | 96kHz/24bit | 发烧友首选 | ⚠️ 部分歌曲可用 |
| `jymaster` | **超清母带** | 192kHz/24bit | 录音室母带品质 | ⚠️ 部分歌曲支持 |
| `sky` | 沉浸环绕声 | - | 环绕音效 | ⚠️ 部分歌曲支持 |
| `jyeffect` | 高清环绕声 | - | 环绕音效 | ⚠️ 部分歌曲支持 |

> **注意**：超清母带 (`jymaster`) 和部分高级音质需要歌曲本身支持，如果获取失败，请降级使用 `hires` 或 `lossless` 音质。

### 播放链接示例

```
# 标准音质（在线播放 - 省流量）
http://156.225.18.78:3000/song/url/v1?id=447926067&level=standard

# 极高音质（在线播放推荐）
http://156.225.18.78:3000/song/url/v1?id=447926067&level=exhigh

# 无损音质（下载推荐）
http://156.225.18.78:3000/song/url/v1?id=447926067&level=lossless

# Hi-Res 音质（高品质下载）
http://156.225.18.78:3000/song/url/v1?id=447926067&level=hires

# 超清母带（最高品质 - 录音室母带）
http://156.225.18.78:3000/song/url/v1?id=447926067&level=jymaster

# 沉浸环绕声
http://156.225.18.78:3000/song/url/v1?id=447926067&level=sky
```

### 播放链接响应示例

```json
{
  "data": [
    {
      "id": 447926067,
      "url": "http://m802.music.126.net/.../song.mp3",
      "br": 320000,
      "size": 10823456,
      "md5": "a1b2c3d4e5f6...",
      "code": 200,
      "expi": 1200,
      "type": "MP3",
      "gain": 0,
      "peak": 0,
      "fee": 1,
      "uf": null,
      "payed": 0,
      "flag": 0,
      "canExtend": false,
      "freeTrialInfo": null,
      "level": "exhigh",
      "encodeType": "mp3"
    }
  ],
  "code": 200
}
```

### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| url | string | 歌曲播放/下载链接 |
| br | number | 码率（bps） |
| size | number | 文件大小（字节） |
| type | string | 音频格式（MP3/FLAC等） |
| level | string | 音质等级 |

## 完整调用流程示例

### 1. 搜索歌曲
```
GET http://156.225.18.78:3000/search?keywords=晴天
```

### 2. 从结果中获取歌曲 ID
从返回数据中找到 `result.songs[0].id`，例如：`447926067`

### 3. 获取歌曲播放链接（在线播放 - 极高音质）
```
GET http://156.225.18.78:3000/song/url/v1?id=447926067&level=exhigh
```

### 4. 获取歌曲下载链接（下载 - 无损音质）
```
GET http://156.225.18.78:3000/song/url/v1?id=447926067&level=lossless
```

### 5. 获取歌词
```
GET http://156.225.18.78:3000/lyric?id=447926067
```

## 获取歌词

### 接口地址

```
GET http://156.225.18.78:3000/lyric
```

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 歌曲 ID |

### 请求示例

```
http://156.225.18.78:3000/lyric?id=447926067
```

### 响应示例

```json
{
  "sgc": true,
  "sfy": false,
  "qfy": false,
  "lrc": {
    "version": 1,
    "lyric": "[00:00.000] 作词 : 周杰伦\n[00:01.000] 作曲 : 周杰伦\n[00:02.000] 编曲 : 周杰伦\n[00:20.000]故事的小黄花\n[00:23.000]从出生那年就飘着\n[00:26.000]童年的荡秋千\n[00:29.000]随记忆一直晃到现在\n..."
  },
  "klyric": {
    "version": 0,
    "lyric": ""
  },
  "tlyric": {
    "version": 1,
    "lyric": "[00:00.000] Lyricist: Jay Chou\n[00:01.000] Composer: Jay Chou\n..."
  },
  "code": 200
}
```

### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| lrc.lyric | string | 原歌词（带时间戳） |
| tlyric.lyric | string | 翻译歌词（带时间戳） |
| klyric.lyric | string | 卡拉 OK 歌词（逐字歌词） |

### 歌词格式说明

歌词采用 LRC 格式，每行格式为：
```
[mm:ss.ms] 歌词内容
```

例如：
```
[00:20.000]故事的小黄花
[00:23.000]从出生那年就飘着
```

### 完整调用流程（含歌词）

```
1. 搜索歌曲
   GET http://156.225.18.78:3000/search?keywords=晴天

2. 获取歌曲 ID
   从返回数据中获取 result.songs[0].id

3. 获取播放链接
   GET http://156.225.18.78:3000/song/url/v1?id=447926067&level=exhigh

4. 获取歌词
   GET http://156.225.18.78:3000/lyric?id=447926067

5. 获取歌曲评论
   GET http://156.225.18.78:3000/comment/music?id=447926067

6. 获取热门评论
   GET http://156.225.18.78:3000/comment/hot?id=447926067&type=0
```

## 获取歌曲评论

### 接口地址

```
GET http://156.225.18.78:3000/comment/music
```

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 歌曲 ID |
| limit | number | 否 | 返回数量，默认为 20 |
| offset | number | 否 | 偏移量，用于分页，默认为 0 |
| before | number | 否 | 分页参数，取上一页最后一项的 time 值 |

### 请求示例

```
# 获取歌曲最新评论（第一页）
http://156.225.18.78:3000/comment/music?id=447926067&limit=20&offset=0

# 获取歌曲最新评论（第二页）
http://156.225.18.78:3000/comment/music?id=447926067&limit=20&offset=20
```

### 响应示例

```json
{
  "code": 200,
  "total": 123456,
  "comments": [
    {
      "user": {
        "nickname": "音乐爱好者",
        "avatarUrl": "https://p1.music.126.net/...",
        "userId": 123456789
      },
      "content": "这首歌太好听了！",
      "time": 1609459200000,
      "likedCount": 888,
      "commentId": 987654321
    }
  ]
}
```

### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| total | number | 评论总数 |
| comments | array | 评论列表 |
| comments[].user.nickname | string | 用户昵称 |
| comments[].user.avatarUrl | string | 用户头像 |
| comments[].content | string | 评论内容 |
| comments[].time | number | 评论时间戳 |
| comments[].likedCount | number | 点赞数 |
| comments[].commentId | number | 评论 ID |

## 获取热门评论

### 接口地址

```
GET http://156.225.18.78:3000/comment/hot
```

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 资源 ID（歌曲/歌单/MV等） |
| type | number | 否 | 资源类型：0=歌曲，2=歌单，3=MV，默认为 0 |
| limit | number | 否 | 返回数量，默认为 20 |
| offset | number | 否 | 偏移量，默认为 0 |

### 资源类型对照

| type 值 | 说明 |
|---------|------|
| 0 | 歌曲 |
| 1 | MV |
| 2 | 歌单 |
| 3 | 专辑 |
| 4 | 电台 |
| 5 | 视频 |
| 6 | 动态 |

### 请求示例

```
# 获取歌曲热门评论
http://156.225.18.78:3000/comment/hot?id=447926067&type=0&limit=15

# 获取歌单热门评论
http://156.225.18.78:3000/comment/hot?id=3778678&type=2&limit=15
```

### 响应示例

```json
{
  "code": 200,
  "hotComments": [
    {
      "user": {
        "nickname": "热评用户",
        "avatarUrl": "https://p1.music.126.net/...",
        "userId": 123456789
      },
      "content": "这是热门评论内容",
      "time": 1609459200000,
      "likedCount": 9999,
      "commentId": 987654321
    }
  ],
  "total": 100
}
```

## 歌单功能

### 1. 获取歌单详情

#### 接口地址
```
GET http://156.225.18.78:3000/playlist/detail
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 歌单 ID |

#### 请求示例
```
http://156.225.18.78:3000/playlist/detail?id=3778678
```

#### 响应示例
```json
{
  "code": 200,
  "playlist": {
    "id": 3778678,
    "name": "云音乐飙升榜",
    "description": "云音乐中每天热度上升最快的100首单曲",
    "coverImgUrl": "https://p1.music.126.net/...",
    "playCount": 123456789,
    "trackCount": 100,
    "creator": {
      "nickname": "网易云音乐",
      "userId": 1
    },
    "tracks": [
      {
        "id": 447926067,
        "name": "晴天",
        "artists": [{"name": "周杰伦"}],
        "album": {"name": "叶惠美"}
      }
    ]
  }
}
```

### 2. 获取歌单所有歌曲

#### 接口地址
```
GET http://156.225.18.78:3000/playlist/track/all
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 歌单 ID |
| limit | number | 否 | 返回数量，默认为 20 |
| offset | number | 否 | 偏移量，默认为 0 |

#### 请求示例
```
http://156.225.18.78:3000/playlist/track/all?id=3778678&limit=50&offset=0
```

### 3. 获取精品歌单

#### 接口地址
```
GET http://156.225.18.78:3000/top/playlist/highquality
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| cat | string | 否 | 歌单分类，如 "华语"、"流行"、"摇滚" |
| limit | number | 否 | 返回数量，默认为 20 |
| before | number | 否 | 分页参数 |

#### 请求示例
```
http://156.225.18.78:3000/top/playlist/highquality?cat=华语&limit=10
```

---

## 排行榜功能

### 1. 获取所有排行榜

#### 接口地址
```
GET http://156.225.18.78:3000/toplist
```

#### 请求示例
```
http://156.225.18.78:3000/toplist
```

#### 响应示例
```json
{
  "code": 200,
  "list": [
    {
      "id": 3778678,
      "name": "飙升榜",
      "coverImgUrl": "https://p1.music.126.net/...",
      "playCount": 123456789,
      "tracks": [
        {"first": "歌曲名1", "second": "歌手名1"},
        {"first": "歌曲名2", "second": "歌手名2"}
      ]
    }
  ]
}
```

### 2. 获取排行榜详情

#### 接口地址
```
GET http://156.225.18.78:3000/toplist/detail
```

#### 请求示例
```
http://156.225.18.78:3000/toplist/detail
```

### 3. 获取歌手榜

#### 接口地址
```
GET http://156.225.18.78:3000/top/artists
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| offset | number | 否 | 偏移量，默认为 0 |
| limit | number | 否 | 返回数量，默认为 30 |

#### 请求示例
```
http://156.225.18.78:3000/top/artists?offset=0&limit=10
```

### 4. 常用排行榜ID

| 排行榜 | ID |
|--------|-----|
| 飙升榜 | 3778678 |
| 新歌榜 | 3779629 |
| 热歌榜 | 3778678 |
| 原创榜 | 2884035 |
| 云音乐说唱榜 | 19723756 |
| 云音乐古典榜 | 71384707 |
| 云音乐电音榜 | 1978921795 |
| 黑胶VIP爱听榜 | 5453912201 |

---

## 每日推荐功能

### 1. 获取每日推荐歌曲

#### 接口地址
```
GET http://156.225.18.78:3000/recommend/songs
```

#### 请求示例
```
http://156.225.18.78:3000/recommend/songs
```

#### 响应示例
```json
{
  "code": 200,
  "data": {
    "dailySongs": [
      {
        "id": 447926067,
        "name": "晴天",
        "artists": [{"name": "周杰伦"}],
        "album": {"name": "叶惠美"},
        "reason": "根据你喜欢的歌曲推荐"
      }
    ],
    "recommendReasons": [
      {
        "songId": 447926067,
        "reason": "根据你喜欢的歌曲推荐"
      }
    ]
  }
}
```

### 2. 获取私人 FM

#### 接口地址
```
GET http://156.225.18.78:3000/personal_fm
```

#### 请求示例
```
http://156.225.18.78:3000/personal_fm
```

#### 响应示例
```json
{
  "code": 200,
  "data": [
    {
      "id": 447926067,
      "name": "晴天",
      "artists": [{"name": "周杰伦"}],
      "album": {"name": "叶惠美"}
    }
  ]
}
```

### 3. 获取推荐歌单

#### 接口地址
```
GET http://156.225.18.78:3000/recommend/resource
```

#### 请求示例
```
http://156.225.18.78:3000/recommend/resource
```

---

## MV 播放功能

### 1. 获取 MV 详情

#### 接口地址
```
GET http://156.225.18.78:3000/mv/detail
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| mvid | number | 是 | MV ID |

#### 请求示例
```
http://156.225.18.78:3000/mv/detail?mvid=5436712
```

#### 响应示例
```json
{
  "code": 200,
  "data": {
    "id": 5436712,
    "name": "晴天 MV",
    "artistName": "周杰伦",
    "cover": "https://p1.music.126.net/...",
    "playCount": 12345678,
    "publishTime": "2003-07-31",
    "desc": "MV描述..."
  }
}
```

### 2. 获取 MV 播放链接

#### 接口地址
```
GET http://156.225.18.78:3000/mv/url
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | MV ID |
| r | number | 否 | 分辨率，如 1080、720、480 |

#### 请求示例
```
http://156.225.18.78:3000/mv/url?id=5436712&r=1080
```

#### 响应示例
```json
{
  "code": 200,
  "data": {
    "id": 5436712,
    "url": "http://vodkgeyttp8.vod.126.net/.../xxx.mp4",
    "r": 1080
  }
}
```

### 3. 获取全部 MV

#### 接口地址
```
GET http://156.225.18.78:3000/mv/all
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| area | string | 否 | 地区：内地、港台、欧美、日本、韩国 |
| type | string | 否 | 类型：官方版、原生、现场版、网易出品 |
| order | string | 否 | 排序：上升最快、最热、最新 |
| limit | number | 否 | 返回数量，默认为 30 |
| offset | number | 否 | 偏移量，默认为 0 |

#### 请求示例
```
http://156.225.18.78:3000/mv/all?area=内地&order=最热&limit=10
```

### 4. 获取歌手 MV

#### 接口地址
```
GET http://156.225.18.78:3000/artist/mv
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 歌手 ID |
| limit | number | 否 | 返回数量，默认为 30 |
| offset | number | 否 | 偏移量，默认为 0 |

#### 请求示例
```
http://156.225.18.78:3000/artist/mv?id=6452&limit=10
```

### 5. 获取 MV 评论

#### 接口地址
```
GET http://156.225.18.78:3000/comment/mv
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | MV ID |
| limit | number | 否 | 返回数量，默认为 20 |
| offset | number | 否 | 偏移量，默认为 0 |

#### 请求示例
```
http://156.225.18.78:3000/comment/mv?id=5436712&limit=20
```

---

## 获取歌曲封面

### 1. 从搜索结果获取封面

搜索结果中直接包含专辑封面：

```json
{
  "result": {
    "songs": [
      {
        "id": 447926067,
        "name": "晴天",
        "album": {
          "id": 34880,
          "name": "叶惠美",
          "picUrl": "https://p1.music.126.net/...jpg",
          "pic": 109951163200249252
        }
      }
    ]
  }
}
```

**封面字段**：`album.picUrl`

### 2. 获取歌曲动态封面

#### 接口地址
```
GET http://156.225.18.78:3000/song/dynamic/cover
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 歌曲 ID |

#### 请求示例
```
http://156.225.18.78:3000/song/dynamic/cover?id=447926067
```

#### 响应示例
```json
{
  "code": 200,
  "data": {
    "songId": 447926067,
    "coverUrl": "https://p1.music.126.net/...gif",
    "coverType": "dynamic"
  }
}
```

### 3. 获取歌曲详情（含封面）

#### 接口地址
```
GET http://156.225.18.78:3000/song/detail
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ids | string | 是 | 歌曲 ID，多个用逗号分隔 |

#### 请求示例
```
http://156.225.18.78:3000/song/detail?ids=447926067,186016
```

#### 响应示例
```json
{
  "code": 200,
  "songs": [
    {
      "id": 447926067,
      "name": "晴天",
      "al": {
        "id": 34880,
        "name": "叶惠美",
        "picUrl": "https://p1.music.126.net/...jpg",
        "pic_str": "109951163200249252"
      },
      "ar": [
        {
          "id": 6452,
          "name": "周杰伦"
        }
      ]
    }
  ]
}
```

**封面字段**：`al.picUrl`（专辑封面）

### 封面尺寸参数

获取不同尺寸的封面，可以在 URL 后添加参数：

| 参数 | 尺寸 |
|------|------|
| `?param=50y50` | 50x50 |
| `?param=100y100` | 100x100 |
| `?param=200y200` | 200x200 |
| `?param=400y400` | 400x400 |
| `?param=800y800` | 800x800 |

**示例**：
```
https://p1.music.126.net/xxx.jpg?param=400y400
```

---

## JavaScript 调用示例

```javascript
// 搜索歌曲并获取封面
fetch('http://156.225.18.78:3000/search?keywords=周杰伦晴天')
  .then(res => res.json())
  .then(data => {
    console.log('搜索结果:', data);
    const song = data.result.songs[0];
    const songId = song.id;
    const coverUrl = song.album.picUrl;  // 获取封面
    console.log('歌曲ID:', songId);
    console.log('封面URL:', coverUrl);
    console.log('400x400封面:', coverUrl + '?param=400y400');
    
    // 获取播放链接（极高音质）
    return fetch(`http://156.225.18.78:3000/song/url/v1?id=${songId}&level=exhigh`);
  })
  .then(res => res.json())
  .then(data => {
    console.log('播放链接:', data.data[0].url);
    console.log('音质:', data.data[0].level);
    console.log('码率:', data.data[0].br);
  });

// 下载歌曲（无损音质）
async function downloadSong(songId, songName) {
  const response = await fetch(`http://156.225.18.78:3000/song/url/v1?id=${songId}&level=lossless`);
  const data = await response.json();
  
  if (data.data && data.data[0].url) {
    const downloadUrl = data.data[0].url;
    const a = document.createElement('a');
    a.href = downloadUrl;
    a.download = `${songName}.mp3`;
    a.click();
  }
}
```

## Python 调用示例

```python
import requests

# 搜索歌曲
response = requests.get('http://156.225.18.78:3000/search?keywords=周杰伦晴天')
data = response.json()

# 获取歌曲 ID
song_id = data['result']['songs'][0]['id']
print(f"歌曲ID: {song_id}")

# 获取播放链接（极高音质）
response = requests.get(f'http://156.225.18.78:3000/song/url/v1?id={song_id}&level=exhigh')
data = response.json()
print(f"播放链接: {data['data'][0]['url']}")
print(f"音质: {data['data'][0]['level']}")
print(f"码率: {data['data'][0]['br']} bps")

# 下载歌曲（无损音质）
def download_song(song_id, song_name):
    response = requests.get(f'http://156.225.18.78:3000/song/url/v1?id={song_id}&level=lossless')
    data = response.json()
    
    if data['data'] and data['data'][0]['url']:
        download_url = data['data'][0]['url']
        audio_response = requests.get(download_url)
        
        # 根据音质确定文件扩展名
        level = data['data'][0]['level']
        ext = 'flac' if level == 'lossless' else 'mp3'
        
        with open(f"{song_name}.{ext}", "wb") as f:
            f.write(audio_response.content)
        print(f"下载完成: {song_name}.{ext}")

# 调用下载函数
download_song(song_id, "晴天")
```

## 音质选择建议

| 使用场景 | 推荐音质 | 说明 |
|----------|----------|------|
| 在线试听 | `exhigh` | 320kbps，音质好且加载快 |
| 手机播放 | `standard` | 128kbps，省流量 |
| 收藏下载 | `lossless` | FLAC无损，音质最佳 |
| 发烧友 | `hires` | Hi-Res，超高解析度 |
| **专业用户** | **`jymaster`** | **超清母带，录音室原始品质** |

### 超清母带音质说明

**超清母带（jymaster）**是最高级别的音频品质：

- **采样率**：192kHz / 24bit
- **音质特点**：录音室原始母带，未经压缩
- **文件大小**：约为普通 MP3 的 10-20 倍
- **适用人群**：专业音乐制作人、发烧友、音频工程师
- **参数值**：`jymaster`（不是 `master`）
- **可用性**：⚠️ **部分歌曲支持**，需要歌曲本身提供母带音质
- **注意事项**：
  - 需要专业设备才能体现优势，普通耳机可能听不出差别
  - 如果获取超清母带失败，建议降级使用 `hires` 或 `lossless`
  - 部分歌曲可能提供 `sky`（沉浸环绕声）或 `jyeffect`（高清环绕声）

### 环绕声音质

| 参数 | 说明 | 特点 |
|------|------|------|
| `sky` | 沉浸环绕声 | 空间音频效果，支持头部追踪 |
| `jyeffect` | 高清环绕声 | 增强的立体声场效果 |

> 使用 `sky` 音质时，API 会自动添加 `immerseType=c51` 参数以启用空间音频。

## 注意事项

1. **歌曲 ID 是核心**：所有与歌曲相关的接口都需要歌曲 ID
2. **VIP 歌曲**：本 API 已内置 VIP Cookie，支持 VIP 歌曲完整播放和下载
3. **音质等级**：根据需求选择合适的音质，`exhigh` 是在线播放的最佳选择
4. **下载文件**：下载链接直接返回音频文件，可直接保存
5. **频率限制**：建议合理控制请求频率，避免触发限制

## 相关接口

| 接口 | 说明 |
|------|------|
| `/song/url/v1?id={id}&level={level}` | 获取歌曲播放/下载链接 |
| `/song/detail?ids={id}` | 获取歌曲详情 |
| `/lyric?id={id}` | 获取歌词 |
| `/playlist/detail?id={id}` | 获取歌单详情 |
| `/artist/songs?id={id}` | 获取歌手热门歌曲 |
| `/song/download/url?id={id}` | 获取下载链接（旧版）|
