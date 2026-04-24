/**
 * @name Demo LX Source
 * @version 1.0.0
 * @author cloudmusic
 * @description LX兼容演示源，支持search与musicUrl
 * @homepage https://example.com
 */
const API_BASE = "https://example.com/music-api";
globalThis.lx.on(EVENT_NAMES.request, async ({ source, action, info }) => {
  if (action === "search") {
    const keyword = encodeURIComponent(info?.keyword || "");
    const data = await new Promise((resolve, reject) => {
      const cancel = lx.request(`${API_BASE}/search?source=${source}&keyword=${keyword}`, { method: "GET" }, (err, res) => {
        if (err) return reject(new Error(String(err)));
        try {
          resolve(JSON.parse(res.body || "{}"));
        } catch (e) {
          reject(e);
        }
      });
    });
    return {
      list: (data.list || []).map(item => ({
        id: String(item.id),
        name: item.name || "",
        artists: Array.isArray(item.artists) ? item.artists : [item.artist || ""],
        source: source,
      })),
    };
  }
  if (action === "musicUrl") {
    const id = encodeURIComponent(info?.id || "");
    const data = await new Promise((resolve, reject) => {
      lx.request(`${API_BASE}/play?source=${source}&id=${id}`, { method: "GET" }, (err, res) => {
        if (err) return reject(new Error(String(err)));
        try {
          resolve(JSON.parse(res.body || "{}"));
        } catch (e) {
          reject(e);
        }
      });
    });
    return {
      url: data.url || "",
      headers: data.headers || {},
    };
  }
  return { error: `unsupported action: ${action}` };
});

globalThis.lx.send(EVENT_NAMES.inited, {
  openDevTools: false,
  sources: {
    kw: { name: "酷我", actions: ["search", "musicUrl"], qualitys: ["standard", "exhigh", "lossless"] },
    kg: { name: "酷狗", actions: ["search", "musicUrl"], qualitys: ["standard", "exhigh"] },
    tx: { name: "QQ音乐", actions: ["search", "musicUrl"], qualitys: ["standard", "exhigh"] },
    wy: { name: "网易云", actions: ["search", "musicUrl"], qualitys: ["standard", "exhigh"] },
    mg: { name: "咪咕", actions: ["search", "musicUrl"], qualitys: ["standard", "exhigh"] },
  },
});
