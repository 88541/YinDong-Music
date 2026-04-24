/*!
 * @name 全豆要
 * @description 聚合 星海/Huibq/聆川/溯音/念心/长青/歌一刀专属汽水音乐，多链路自动回退
 * @version v3.0.0
 * @author Toskysun
 */
const CACHE_TTL = 300000,
  MAX_CACHE_SIZE = 500,
  SAFE_URL_RE = /^https?:\/\//i,
  MAIN_API_URL =
    "https://music-api.gdstudio.xyz/api.php?use_xbridge3=true&loader_name=forest&need_sec_link=1&sec_link_scene=im&theme=light",
  BACKUP_API_URL = "https://music-dl.sayqz.com/api/",
  HUIBQ_API_URL = "https://lxmusicapi.onrender.com",
  HUIBQ_API_KEY = "share-v3",
  LINGCHUAN_API_URL = "https://lc.guoyue2010.top/api/music",
  SUYIN_QQ_API_URL = "https://oiapi.net/api/QQ_Music",
  SUYIN_QQ_API_KEY = "oiapi-ef6133b7-ac2f-dc7d-878c-d3e207a82575",
  SUYIN_WY_API_URL = "https://oiapi.net/api/Music_163",
  SUYIN_KW_API_URL = "https://oiapi.net/api/Kuwo",
  SUYIN_MG_API_URL = "https://api.xcvts.cn/api/music/migu",
  CHANGQING_VIP_URL_MAP = {
    tx: "http://175.27.166.236/kgqq/qq.php?type=mp3&id={id}&level={level}",
    wy: "http://175.27.166.236/wy/wy.php?type=mp3&id={id}&level={level}",
    kw: "https://musicapi.haitangw.net/music/kw.php?type=mp3&id={id}&level={level}",
    kg: "https://music.haitangw.cc/kgqq/kg.php?type=mp3&id={id}&level={level}",
    mg: "https://music.haitangw.cc/musicapi/mg.php?type=mp3&id={id}&level={level}",
  },
  NIANXIN_VIP_URL_MAP = {
    tx: "https://music.nxinxz.com/kgqq/tx.php?id={id}&level={level}&type=mp3",
    wy: "http://music.nxinxz.com/wy.php?id={id}&level={level}&type=mp3",
    kw: "http://music.nxinxz.com/kw.php?id={id}&level={level}&type=mp3",
    kg: "https://music.nxinxz.com/kgqq/kg.php?id={id}&level={level}&type=mp3",
    mg: "http://music.nxinxz.com/mg.php?id={id}&level={level}&type=mp3",
  },
  QSVIP_SOURCE_ID = "qsvip",
  QSVIP_SOURCE_NAME = "汽水VIP",
  QSVIP_API_URL = "https://api.vsaa.cn/api/music.qishui.vip",
  QSVIP_API_FALLBACK_URL = "http://api.vsaa.cn/api/music.qishui.vip",
  QSVIP_PROXY_URL = "https://proxy.qishui.vsaa.cn/qishui/proxy",
  MUSIC_QUALITY = {
    wy: ["128k", "192k", "320k", "flac", "flac24bit"],
    tx: ["128k", "192k", "320k", "flac", "flac24bit"],
    kw: ["128k", "192k", "320k", "flac", "flac24bit"],
    kg: ["128k", "192k", "320k", "flac", "flac24bit"],
    mg: ["128k", "192k", "320k", "flac"],
  },
  MAIN_API_SOURCE_MAP = {
    wy: "netease",
    tx: "tencent",
    kw: "kuwo",
    kg: "kugou",
    mg: "migu",
  },
  MAIN_API_QUALITY_MAP = {
    "128k": "128",
    "192k": "192",
    "320k": "320",
    flac: "740",
    flac24bit: "999",
  },
  BACKUP_API_SOURCE_MAP = {
    wy: "netease",
    tx: "qq",
    kw: "kuwo",
  },
  SUYIN_QQ_BR_MAP = {
    "128k": 7,
    "320k": 5,
    flac: 4,
    hires: 3,
    atmos: 2,
    master: 1,
  },
  SUYIN_KW_QUALITY_MAP = {
    flac: 1,
    "320k": 5,
    "128k": 7,
  },
  HIGH_QUALITY_SET = new Set(["flac", "flac24bit", "hires", "master", "atmos"]),
  LOSSLESS_QUALITY_SET = new Set([
    "flac",
    "flac24bit",
    "hires",
    "master",
    "atmos",
  ]),
  cache = new Map(),
  { EVENT_NAMES, request, on, send, env, version } = globalThis.lx;

function log(...args) {
  console.log("[全豆要]", ...args);
}

function httpFetch(url, options = { method: "GET" }) {
  return new Promise((resolve, reject) => {
    request(
      url,
      {
        timeout: 6000,
        ...options,
      },
      (err, res) => {
        if (err) return reject(new Error("请求失败: " + err.message));
        let body = res?.body;
        if (typeof body === "string") {
          const trimmed = body.trim();
          if (
            trimmed.startsWith("{") ||
            trimmed.startsWith("[") ||
            trimmed.startsWith('"')
          )
            try {
              body = JSON.parse(trimmed);
            } catch (_e) {}
        }
        resolve({
          statusCode: res?.statusCode ?? 0,
          headers: res?.headers || {},
          body: body,
        });
      },
    );
  });
}

async function sendRequest(url, params = {}) {
  const queryStr = Object.keys(params)
      .filter((key) => params[key] !== undefined && params[key] !== null)
      .map(
        (key) =>
          encodeURIComponent(key) + "=" + encodeURIComponent(params[key]),
      )
      .join("&"),
    separator = url.includes("?") ? "&" : "?",
    fullUrl = "" + url + (queryStr ? separator + queryStr : ""),
    response = await httpFetch(fullUrl, {
      method: "GET",
      timeout: 5000,
    });
  if (response.statusCode >= 400)
    throw new Error("HTTP " + response.statusCode);
  return response.body;
}

function qsVipBuildQuery(params = {}) {
  const pairs = Object.keys(params)
    .filter((key) => params[key] !== undefined && params[key] !== null)
    .map(
      (key) =>
        encodeURIComponent(String(key)) +
        "=" +
        encodeURIComponent(String(params[key])),
    );
  return pairs.length ? "?" + pairs.join("&") : "";
}

async function qsVipGet(url, params = {}, timeout = 15000) {
  const urls =
    url === QSVIP_API_URL ? [QSVIP_API_URL, QSVIP_API_FALLBACK_URL] : [url];
  let lastErr = null;
  for (const endpoint of urls) {
    try {
      const fullUrl = "" + endpoint + qsVipBuildQuery(params),
        response = await httpFetch(fullUrl, {
          method: "GET",
          timeout: timeout,
        });
      if (response.statusCode >= 400)
        throw new Error("HTTP " + response.statusCode);
      return response.body;
    } catch (e) {
      lastErr = e;
    }
  }
  throw lastErr || new Error("汽水VIP请求失败");
}

async function qsVipPost(url, body = {}, timeout = 60000) {
  const response = await httpFetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: body,
    timeout: timeout,
  });
  if (response.statusCode >= 400)
    throw new Error("HTTP " + response.statusCode);
  return response.body;
}

function qsVipPickId(info) {
  return (
    info?.id ||
    info?.songmid ||
    info?.songId ||
    info?.hash ||
    info?.rid ||
    info?.mid ||
    info?.strMediaMid ||
    info?.mediaId ||
    ""
  ).toString();
}

function qsVipMapQuality(quality) {
  switch (String(quality || "").toLowerCase()) {
    case "128k":
      return "low";
    case "320k":
      return "standard";
    case "flac":
      return "lossless";
    case "flac24bit":
      return "hi_res";
    default:
      return "standard";
  }
}

function qsVipNormalizeSongItem(item) {
  const songId = item?.id || item?.vid ? String(item.id || item.vid) : "";
  return {
    id: songId,
    songmid: songId,
    hash: songId,
    name: item?.name ? String(item.name) : "未知歌曲",
    singer: item?.artists ? String(item.artists) : "未知歌手",
    albumName: item?.album ? String(item.album) : "",
    duration: item?.duration ? Math.floor(Number(item.duration) / 1000) : 0,
    pic: item?.cover || item?.pic ? String(item.cover || item.pic) : "",
    _raw: item || {},
  };
}

function qsVipExtractSong(result) {
  const data = result?.data;
  if (Array.isArray(data)) return data[0] || null;
  if (data && typeof data === "object" && data[0]) return data[0];
  return null;
}

async function qsVipSearch(keyword, page = 1, pageSize = 30) {
  if (!keyword)
    return {
      isEnd: true,
      list: [],
    };
  const result = await qsVipGet(
      QSVIP_API_URL,
      {
        act: "search",
        keywords: keyword,
        page: page,
        pagesize: pageSize,
        type: "music",
      },
      15000,
    ),
    list = Array.isArray(result?.data?.lists) ? result.data.lists : [],
    total = result?.data?.total ? Number(result.data.total) : list.length;
  return {
    isEnd: list.length < pageSize,
    list: list.map(qsVipNormalizeSongItem),
    total: total,
  };
}

async function qsVipResolveMusicUrl(musicInfo, quality) {
  const songId = qsVipPickId(musicInfo);
  if (!songId) throw new Error("汽水VIP缺少歌曲ID");
  const result = await qsVipGet(
      QSVIP_API_URL,
      {
        act: "song",
        id: songId,
        quality: qsVipMapQuality(quality),
      },
      20000,
    ),
    song = qsVipExtractSong(result);
  if (!song?.url) throw new Error("汽水VIP未返回可用URL");
  if (song.ekey) {
    const proxyResult = await qsVipPost(
      QSVIP_PROXY_URL,
      {
        url: song.url,
        key: song.ekey,
        filename: song.name || "KMusic",
        ext: song.codec_type ? String(song.codec_type) : "aac",
      },
      60000,
    );
    if (Number(proxyResult?.code) === 200 && proxyResult?.url)
      return String(proxyResult.url);
    throw new Error("汽水VIP代理解密失败");
  }
  return String(song.url);
}

async function qsVipGetLyric(musicInfo) {
  const songId = qsVipPickId(musicInfo);
  if (!songId)
    return {
      lyric: "",
    };
  const result = await qsVipGet(
      QSVIP_API_URL,
      {
        act: "song",
        id: songId,
      },
      15000,
    ),
    song = qsVipExtractSong(result);
  return {
    lyric: song?.lyric ? String(song.lyric) : "",
  };
}

async function handleQsVipRequest(action, info = {}) {
  if (action === "musicSearch" || action === "search") {
    const keyword = info?.keyword ? String(info.keyword) : "",
      page = info?.page ? Number(info.page) : 1,
      limit = info?.limit ? Number(info.limit) : 30;
    return qsVipSearch(keyword, page, limit);
  }
  if (action === "musicUrl") {
    if (!info?.musicInfo) throw new Error("请求参数不完整");
    const url = await qsVipResolveMusicUrl(info.musicInfo, info.type);
    return ensureSafeUrl(url, "汽水VIP");
  }
  if (action === "lyric") return qsVipGetLyric(info?.musicInfo || {});
  throw new Error("action not support");
}

function mapQuality(quality, supported) {
  const list = Array.isArray(supported) ? supported : ["128k"],
    q = String(quality || "128k").toLowerCase();
  if (list.includes(q)) return q;
  const hierarchy = ["flac24bit", "flac", "320k", "192k", "128k"];
  let idx = hierarchy.indexOf(q);
  if (idx < 0) idx = hierarchy.length - 1;
  for (let i = idx; i < hierarchy.length; i++) {
    if (list.includes(hierarchy[i])) return hierarchy[i];
  }
  for (let i = hierarchy.length - 1; i >= 0; i--) {
    if (list.includes(hierarchy[i])) return hierarchy[i];
  }
  return list[0] || "128k";
}

function cleanText(text) {
  if (!text) return "";
  return String(text)
    .replace(/\(\s*Live\s*\)/gi, "")
    .replace(/\([^)]*\)/g, "")
    .replace(/\s+/g, "")
    .replace(/[^\w\u4e00-\u9fa5]/g, "")
    .trim()
    .toLowerCase();
}

function getSearchPriority(musicInfo) {
  const priorities = [],
    name = musicInfo?.name || "",
    album = musicInfo?.albumName || musicInfo?.album || "",
    singer = musicInfo?.singer || "";
  if (name && album) {
    const cleaned = cleanText(name + album);
    if (cleaned)
      priorities.push({
        keyword: cleaned,
        strict: true,
      });
  }
  if (name && singer) {
    const cleaned = cleanText(name + singer);
    if (cleaned)
      priorities.push({
        keyword: cleaned,
        strict: true,
      });
  }
  if (name) {
    const cleaned = cleanText(name);
    if (cleaned)
      priorities.push({
        keyword: cleaned,
        strict: false,
      });
  }
  return priorities;
}

function textLike(a, b) {
  const cleanA = cleanText(a),
    cleanB = cleanText(b);
  if (!cleanA || !cleanB) return true;
  return cleanA.includes(cleanB) || cleanB.includes(cleanA);
}

function checkKwMatch(data, musicInfo) {
  const song = data?.song || data?.data?.song || "",
    singer = data?.singer || data?.data?.singer || "",
    album = data?.album || data?.data?.album || "";
  if (!textLike(song, musicInfo?.name || "")) return false;
  if (musicInfo?.singer && singer && !textLike(singer, musicInfo.singer))
    return false;
  if (
    (musicInfo?.albumName || musicInfo?.album) &&
    album &&
    !textLike(album, musicInfo.albumName || musicInfo.album)
  )
    return false;
  return true;
}

function checkMgMatch(data, musicInfo) {
  if (!textLike(data?.title || "", musicInfo?.name || "")) return false;
  if (
    musicInfo?.singer &&
    data?.artist &&
    !textLike(data.artist, musicInfo.singer)
  )
    return false;
  if (
    (musicInfo?.albumName || musicInfo?.album) &&
    data?.album &&
    !textLike(data.album, musicInfo.albumName || musicInfo.album)
  )
    return false;
  return true;
}

function parseKwFromMessage(message) {
  if (!message) return null;
  const result = {},
    lines = String(message).split("\n");
  for (const line of lines) {
    if (line.includes("歌名："))
      result.song = line.replace("歌名：", "").trim();
    if (line.includes("歌手："))
      result.singer = line.replace("歌手：", "").trim();
    if (line.includes("专辑："))
      result.album = line.replace("专辑：", "").trim();
  }
  return result.song ? result : null;
}

function getSongId(musicInfo) {
  return musicInfo?.hash ?? musicInfo?.songmid ?? musicInfo?.id ?? null;
}

function getQqSongId(musicInfo) {
  const mid =
    musicInfo?.meta?.qq?.mid ||
    musicInfo?.meta?.mid ||
    musicInfo?.songmid ||
    (typeof musicInfo?.id === "string" && !/^\d+$/.test(musicInfo.id)
      ? musicInfo.id
      : null);
  if (mid)
    return {
      type: "mid",
      value: mid,
    };
  const songid =
    musicInfo?.meta?.qq?.songid ||
    musicInfo?.meta?.songid ||
    (typeof musicInfo?.id === "number"
      ? musicInfo.id
      : typeof musicInfo?.id === "string" && /^\d+$/.test(musicInfo.id)
        ? Number(musicInfo.id)
        : null);
  if (songid)
    return {
      type: "songid",
      value: songid,
    };
  return null;
}

function mapLegacyVipQualityLevel(quality) {
  const q = String(quality || "128k").toLowerCase();
  if (
    q === "flac" ||
    q === "flac24bit" ||
    q === "hires" ||
    q === "master" ||
    q === "atmos"
  )
    return "lossless";
  if (q === "320k" || q === "192k") return "exhigh";
  return "standard";
}

function getLegacyVipSongId(source, musicInfo) {
  if (source === "kg") {
    return (
      musicInfo?.hash ||
      musicInfo?.songmid ||
      musicInfo?.id ||
      musicInfo?.songId ||
      musicInfo?.rid ||
      null
    );
  }
  if (source === "tx") {
    const qqId = getQqSongId(musicInfo);
    if (qqId?.value) return qqId.value;
  }
  return (
    musicInfo?.songmid ||
    musicInfo?.id ||
    musicInfo?.songId ||
    musicInfo?.rid ||
    musicInfo?.hash ||
    null
  );
}

function buildLegacyVipUrl(source, quality, musicInfo, urlMap, label) {
  const template = urlMap[source];
  if (!template) throw new Error(label + "不支持该平台");
  const songId = getLegacyVipSongId(source, musicInfo);
  if (!songId) throw new Error(label + "缺少songId");
  const level = mapLegacyVipQualityLevel(quality);
  return template
    .replace("{id}", encodeURIComponent(String(songId)))
    .replace("{level}", encodeURIComponent(level));
}

function getCacheKey(source, musicInfo, quality = "") {
  const name = musicInfo?.name || "",
    singer = musicInfo?.singer || "",
    album = musicInfo?.albumName || musicInfo?.album || "";
  return source + "_" + name + "_" + singer + "_" + album + "_" + quality;
}

function getCachedUrl(key) {
  const entry = cache.get(key);
  if (!entry) return null;
  if (Date.now() - entry.timestamp >= CACHE_TTL)
    return (cache.delete(key), null);
  return entry.url;
}

function setCachedUrl(key, url) {
  cache.set(key, {
    url: url,
    timestamp: Date.now(),
  });
  if (cache.size > MAX_CACHE_SIZE) {
    const oldest = cache.keys().next().value;
    if (oldest !== undefined) cache.delete(oldest);
  }
}

function ensureSafeUrl(url, label) {
  if (!url || typeof url !== "string") throw new Error(label + " 返回空URL");
  if (!SAFE_URL_RE.test(url.trim())) throw new Error(label + " 返回不安全URL");
  return url;
}

function isObviouslyLossyUrl(url) {
  if (!url) return false;
  const lower = String(url).toLowerCase();
  if (/\.flac(?:$|[?#])/.test(lower) || /\bquflac\b/.test(lower)) return false;
  if (/\.mp3(?:$|[?#])/.test(lower)) return true;
  if (/\bqu(?:128|192|320)\b/.test(lower)) return true;
  return false;
}

function isObviouslyBelow320Url(url) {
  if (!url) return false;
  const lower = String(url).toLowerCase();
  if (/\.flac(?:$|[?#])/.test(lower) || /\bquflac\b/.test(lower)) return false;
  if (
    /\bqu320\b/.test(lower) ||
    /(?:[?&]|\b)br=320(?:[&#]|$)/.test(lower) ||
    /(?:[?&]|\b)level=exhigh(?:[&#]|$)/.test(lower)
  )
    return false;
  if (/\bqu(?:128|192)\b/.test(lower)) return true;
  if (/(?:[?&]|\b)br=(?:96|128|192)(?:[&#]|$)/.test(lower)) return true;
  if (/(?:[?&]|\b)level=standard(?:[&#]|$)/.test(lower)) return true;
  return false;
}

function buildUserAgent() {
  return "lx-music-" + (env || "desktop") + "/" + (version || "unknown");
}

async function resolveXinghai(source, songId, quality, musicInfo) {
  const apiSource = MAIN_API_SOURCE_MAP[source];
  if (!apiSource) throw new Error("星海主API不支持该平台");
  const id = songId ?? getSongId(musicInfo);
  if (!id) throw new Error("缺少songId");
  const mappedQuality = mapQuality(quality, [
      "128k",
      "192k",
      "320k",
      "flac",
      "flac24bit",
    ]),
    apiQuality = MAIN_API_QUALITY_MAP[mappedQuality];
  if (!apiQuality) throw new Error("星海主API音质映射失败");
  const url =
      MAIN_API_URL +
      "&types=url&source=" +
      apiSource +
      "&id=" +
      encodeURIComponent(id) +
      "&br=" +
      apiQuality,
    response = await httpFetch(url, {
      method: "GET",
      headers: {
        "User-Agent": "LX-Music-Mobile",
        Accept: "application/json",
      },
    }),
    body = response.body;
  if (!body || typeof body !== "object" || !body.url)
    throw new Error(body?.msg || "星海主API未返回可用URL");
  return body.url;
}

async function resolveXinghaiBackup(source, songId, quality, musicInfo) {
  const apiSource = BACKUP_API_SOURCE_MAP[source];
  if (!apiSource) throw new Error("星海备API不支持该平台");
  const id = songId ?? getSongId(musicInfo);
  if (!id) throw new Error("缺少songId");
  const mappedQuality = mapQuality(quality, [
    "128k",
    "192k",
    "320k",
    "flac",
    "flac24bit",
  ]);
  return (
    BACKUP_API_URL +
    "?source=" +
    encodeURIComponent(apiSource) +
    "&id=" +
    encodeURIComponent(id) +
    "&type=url&br=" +
    encodeURIComponent(mappedQuality)
  );
}

async function resolveHuibq(source, songId, quality, musicInfo) {
  const hash = musicInfo?.hash ?? musicInfo?.songmid;
  if (!hash) throw new Error("Huibq缺少hash/songmid");
  const mappedQuality = mapQuality(quality, ["320k", "128k"]),
    url =
      HUIBQ_API_URL +
      "/url/" +
      source +
      "/" +
      encodeURIComponent(hash) +
      "/" +
      encodeURIComponent(mappedQuality),
    response = await httpFetch(url, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        "User-Agent": buildUserAgent(),
        "X-Request-Key": HUIBQ_API_KEY,
      },
    }),
    body = response.body;
  if (!body || typeof body !== "object" || Number.isNaN(Number(body.code)))
    throw new Error("Huibq返回无效");
  switch (Number(body.code)) {
    case 0:
      if (!body.url) throw new Error("Huibq返回空URL");
      return body.url;
    case 1:
      throw new Error("Huibq block ip");
    case 2:
      throw new Error("Huibq get music url failed");
    case 4:
      throw new Error("Huibq internal server error");
    case 5:
      throw new Error("Huibq too many requests");
    case 6:
      throw new Error("Huibq param error");
    default:
      throw new Error(body.msg || "Huibq unknown error");
  }
}

async function resolveLingchuan(source, songId, quality, musicInfo) {
  const hash = musicInfo?.hash ?? musicInfo?.songmid;
  if (!hash) throw new Error("聆川缺少hash/songmid");
  const mappedQuality = mapQuality(quality, ["320k", "128k"]),
    url =
      LINGCHUAN_API_URL +
      "/url?source=" +
      encodeURIComponent(source) +
      "&songId=" +
      encodeURIComponent(hash) +
      "&quality=" +
      encodeURIComponent(mappedQuality),
    response = await httpFetch(url, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        "User-Agent": buildUserAgent(),
      },
      follow_max: 5,
    }),
    body = response.body;
  if (!body || typeof body !== "object" || Number.isNaN(Number(body.code)))
    throw new Error("聆川返回无效");
  switch (Number(body.code)) {
    case 200:
      if (!body.url) throw new Error("聆川返回空URL");
      return body.url;
    case 403:
      throw new Error("聆川403 forbidden");
    case 429:
      throw new Error("聆川429 rate limit");
    case 500:
      throw new Error("聆川500 " + (body.message || "server error"));
    default:
      throw new Error(body.message || "聆川未知错误");
  }
}

function extractSuyinQqUrl(data) {
  if (data?.music) return data.music;
  if (data?.url) return data.url;
  if (data?.message) {
    const match = String(data.message).match(/音频链接[：:](.+?)(?:\n|$)/);
    if (match && match[1]) return match[1].trim();
  }
  throw new Error("溯音QQ未找到音频链接");
}

function mapSuyinQqQuality(quality) {
  const q = String(quality || "128k").toLowerCase();
  if (q === "flac24bit") return "hires";
  if (q === "192k") return "320k";
  if (SUYIN_QQ_BR_MAP[q]) return q;
  return "128k";
}

async function resolveSuyinQQ(musicInfo, quality) {
  const qqId = getQqSongId(musicInfo);
  if (!qqId) throw new Error("溯音QQ缺少songmid/id");
  const mappedQuality = mapSuyinQqQuality(quality),
    br = SUYIN_QQ_BR_MAP[mappedQuality] || SUYIN_QQ_BR_MAP["128k"],
    brList = [br, 4, 5, 7]
      .filter((val, idx, arr) => arr.indexOf(val) === idx && val >= br)
      .sort((a, b) => a - b);
  let lastErr = null;
  for (const currentBr of brList) {
    try {
      const params = {
        key: SUYIN_QQ_API_KEY,
        type: "json",
        br: currentBr,
        n: 1,
      };
      if (qqId.type === "mid") params.mid = qqId.value;
      else params.songid = qqId.value;
      const result = await sendRequest(SUYIN_QQ_API_URL, params);
      return extractSuyinQqUrl(result);
    } catch (e) {
      lastErr = e;
    }
  }
  throw new Error("溯音QQ全部音质尝试失败: " + (lastErr?.message || "unknown"));
}

async function resolveSuyinWY(musicInfo) {
  const songId = musicInfo?.songmid || musicInfo?.id;
  if (!songId) throw new Error("溯音163缺少songmid/id");
  const result = await sendRequest(SUYIN_WY_API_URL, {
    id: songId,
  });
  if (result?.code === 0 && result?.data) {
    const data = Array.isArray(result.data) ? result.data[0] : result.data;
    if (data?.url) return data.url;
  }
  throw new Error("溯音163获取失败");
}

async function fetchKwAudio(keyword, br, musicInfo = null) {
  const result = await sendRequest(SUYIN_KW_API_URL, {
    msg: keyword,
    n: 1,
    br: br,
  });
  if (result?.data?.url) {
    if (musicInfo && !checkKwMatch(result, musicInfo))
      throw new Error("溯音酷我歌曲信息不匹配");
    return result.data.url;
  }
  if (result?.message) {
    const match = String(result.message).match(/音乐链接[：:](\S+)/);
    if (match && match[1]) {
      if (musicInfo) {
        const parsed = parseKwFromMessage(result.message);
        if (parsed && !checkKwMatch(parsed, musicInfo))
          throw new Error("溯音酷我歌曲信息不匹配");
      }
      return match[1];
    }
  }
  throw new Error("溯音酷我未找到链接");
}

async function resolveSuyinKW(musicInfo, quality) {
  if (!musicInfo?.name) throw new Error("溯音酷我需要歌曲名");
  const cacheKey = getCacheKey("kw", musicInfo, quality),
    cached = getCachedUrl(cacheKey);
  if (cached) return cached;
  const mappedQuality = mapQuality(quality, ["flac", "320k", "128k"]),
    kwBr = SUYIN_KW_QUALITY_MAP[mappedQuality] || 1,
    priorities = getSearchPriority(musicInfo);
  let lastErr = null;
  for (const entry of priorities) {
    try {
      const audioUrl = await fetchKwAudio(
        entry.keyword,
        kwBr,
        entry.strict ? musicInfo : null,
      );
      if (audioUrl) {
        setCachedUrl(cacheKey, audioUrl);
        return audioUrl;
      }
    } catch (e) {
      lastErr = e;
    }
  }
  throw new Error("溯音酷我失败: " + (lastErr?.message || "unknown"));
}

async function resolveSuyinMG(musicInfo) {
  if (!musicInfo?.name) throw new Error("溯音咪咕需要歌曲名");
  const cacheKey = getCacheKey("mg", musicInfo),
    cached = getCachedUrl(cacheKey);
  if (cached) return cached;
  const priorities = getSearchPriority(musicInfo);
  let lastErr = null;
  for (const entry of priorities) {
    try {
      const result = await sendRequest(SUYIN_MG_API_URL, {
        gm: entry.keyword,
        n: 1,
        num: 1,
        type: "json",
      });
      if (result?.code === 200 && result?.music_url) {
        if (entry.strict && !checkMgMatch(result, musicInfo))
          throw new Error("溯音咪咕歌曲信息不匹配");
        setCachedUrl(cacheKey, result.music_url);
        return result.music_url;
      }
    } catch (e) {
      lastErr = e;
    }
  }
  throw new Error("溯音咪咕失败: " + (lastErr?.message || "unknown"));
}

async function resolveSuyin(source, songId, quality, musicInfo) {
  switch (source) {
    case "tx":
      return resolveSuyinQQ(musicInfo, quality);
    case "wy":
      return resolveSuyinWY(musicInfo);
    case "kw":
      return resolveSuyinKW(musicInfo, quality);
    case "mg":
      return resolveSuyinMG(musicInfo);
    default:
      throw new Error("溯音不支持该平台");
  }
}

async function resolveChangqingVip(source, songId, quality, musicInfo) {
  return buildLegacyVipUrl(
    source,
    quality,
    musicInfo,
    CHANGQING_VIP_URL_MAP,
    "长青SVIP",
  );
}

async function resolveNianxinVip(source, songId, quality, musicInfo) {
  return buildLegacyVipUrl(
    source,
    quality,
    musicInfo,
    NIANXIN_VIP_URL_MAP,
    "念心SVIP",
  );
}

const PROVIDERS = {
  xinghai: {
    name: "星海主",
    fn: resolveXinghai,
  },
  xinghaiBackup: {
    name: "星海备",
    fn: resolveXinghaiBackup,
  },
  huibq: {
    name: "Huibq",
    fn: resolveHuibq,
  },
  lingchuan: {
    name: "聆川",
    fn: resolveLingchuan,
  },
  suyinQQ: {
    name: "溯音QQ",
    fn: (_source, _songId, quality, musicInfo) =>
      resolveSuyin("tx", _songId, quality, musicInfo),
  },
  suyin163: {
    name: "溯音163",
    fn: (_source, _songId, quality, musicInfo) =>
      resolveSuyin("wy", _songId, quality, musicInfo),
  },
  suyinSearch: {
    name: "溯音搜索",
    fn: (_source, _songId, quality, musicInfo) =>
      resolveSuyin("kw", _songId, quality, musicInfo),
  },
  suyinMigu: {
    name: "溯音咪咕",
    fn: (_source, _songId, quality, musicInfo) =>
      resolveSuyin("mg", _songId, quality, musicInfo),
  },
  changqingVip: {
    name: "长青SVIP",
    fn: resolveChangqingVip,
  },
  nianxinVip: {
    name: "念心SVIP",
    fn: resolveNianxinVip,
  },
};

function getFallbackChain(source, isHighQuality, quality = "128k") {
  const highQualityChains = {
      wy: [
        "xinghai",
        "huibq",
        "lingchuan",
        "nianxinVip",
        "changqingVip",
        "xinghaiBackup",
        "suyin163",
      ],
      tx: [
        "xinghai",
        "huibq",
        "lingchuan",
        "nianxinVip",
        "changqingVip",
        "xinghaiBackup",
        "suyinQQ",
      ],
      kw: [
        "xinghai",
        "huibq",
        "lingchuan",
        "nianxinVip",
        "changqingVip",
        "xinghaiBackup",
        "suyinSearch",
      ],
      kg: ["xinghai", "huibq", "lingchuan", "nianxinVip", "changqingVip"],
      mg: [
        "xinghai",
        "huibq",
        "lingchuan",
        "suyinMigu",
        "nianxinVip",
        "changqingVip",
      ],
    },
    losslessChains = {
      wy: [
        "xinghai",
        "huibq",
        "lingchuan",
        "nianxinVip",
        "changqingVip",
        "suyin163",
        "xinghaiBackup",
      ],
      tx: [
        "xinghai",
        "huibq",
        "lingchuan",
        "nianxinVip",
        "changqingVip",
        "suyinQQ",
        "xinghaiBackup",
      ],
      kw: [
        "xinghai",
        "huibq",
        "lingchuan",
        "nianxinVip",
        "changqingVip",
        "xinghaiBackup",
      ],
      kg: ["nianxinVip", "changqingVip", "xinghai", "huibq", "lingchuan"],
      mg: ["xinghai", "huibq", "lingchuan", "nianxinVip", "changqingVip"],
    },
    standardChains = {
      wy: [
        "nianxinVip",
        "changqingVip",
        "xinghai",
        "huibq",
        "lingchuan",
        "suyin163",
        "xinghaiBackup",
      ],
      tx: [
        "nianxinVip",
        "changqingVip",
        "xinghai",
        "huibq",
        "lingchuan",
        "suyinQQ",
        "xinghaiBackup",
      ],
      kw: [
        "nianxinVip",
        "changqingVip",
        "xinghai",
        "huibq",
        "lingchuan",
        "suyinSearch",
        "xinghaiBackup",
      ],
      kg: ["nianxinVip", "changqingVip", "xinghai", "huibq", "lingchuan"],
      mg: [
        "nianxinVip",
        "changqingVip",
        "xinghai",
        "huibq",
        "lingchuan",
        "suyinMigu",
      ],
    };
  if (quality === "320k") {
    const chain = standardChains[source] || [];
    return chain.map((key) => PROVIDERS[key]).filter(Boolean);
  }
  const chain =
    (isHighQuality ? losslessChains : highQualityChains)[source] || [];
  return chain.map((key) => PROVIDERS[key]).filter(Boolean);
}

async function getMusicUrl(source, musicInfo, quality) {
  if (!source || typeof source !== "string" || !MUSIC_QUALITY[source])
    throw new Error("不支持的平台");
  if (!musicInfo || typeof musicInfo !== "object")
    throw new Error("缺少歌曲信息");
  const effectiveQuality = quality || "128k",
    mappedQuality = mapQuality(effectiveQuality, MUSIC_QUALITY[source]),
    songId = getSongId(musicInfo),
    isHighQuality = HIGH_QUALITY_SET.has(effectiveQuality.toLowerCase()),
    fallbackChain = getFallbackChain(source, isHighQuality, mappedQuality);
  if (!fallbackChain.length) throw new Error("未找到可用fallback链");
  const errors = [];
  for (const provider of fallbackChain) {
    try {
      log(
        "尝试 " +
          provider.name +
          " source=" +
          source +
          " quality=" +
          mappedQuality,
      );
      const rawUrl = await provider.fn(
          source,
          songId,
          mappedQuality,
          musicInfo,
        ),
        safeUrl = ensureSafeUrl(rawUrl, provider.name);
      if (mappedQuality === "320k" && isObviouslyBelow320Url(safeUrl))
        throw new Error(provider.name + " 返回疑似低于320流，继续尝试");
      if (
        LOSSLESS_QUALITY_SET.has(mappedQuality) &&
        isObviouslyLossyUrl(safeUrl)
      )
        throw new Error(provider.name + " 返回有损流，继续尝试");
      return safeUrl;
    } catch (e) {
      errors.push(provider.name + ": " + e.message);
    }
  }
  throw new Error("所有源均失败: " + errors.join("; "));
}

const musicSources = {},
  SOURCE_NAMES = {
    wy: "网易云音乐",
    tx: "QQ音乐",
    kw: "酷我音乐",
    kg: "酷狗音乐",
    mg: "咪咕音乐",
  };

Object.keys(MUSIC_QUALITY).forEach((key) => {
  musicSources[key] = {
    name: SOURCE_NAMES[key],
    type: "music",
    actions: ["musicUrl"],
    qualitys: MUSIC_QUALITY[key],
  };
});

musicSources[QSVIP_SOURCE_ID] = {
  name: QSVIP_SOURCE_NAME,
  type: "music",
  actions: ["musicSearch", "musicUrl", "lyric"],
  qualitys: ["128k", "320k", "flac", "flac24bit"],
};

on(EVENT_NAMES.request, ({ action, source, info }) => {
  if (source === QSVIP_SOURCE_ID) return handleQsVipRequest(action, info);
  if (action !== "musicUrl")
    return Promise.reject(new Error("action not support"));
  if (!info?.musicInfo) return Promise.reject(new Error("请求参数不完整"));
  return getMusicUrl(source, info.musicInfo, info.type || "128k")
    .then((url) => Promise.resolve(url))
    .catch((err) => Promise.reject(err));
});

send(EVENT_NAMES.inited, {
  openDevTools: false,
  sources: musicSources,
});

log("初始化完成，聚合音源已就绪");
