// Basado en el codelab TinyPrice de epanchanaf. Ver THIRD_PARTY_NOTICES.md.
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

type Dependencies = {
  getApiKey: () => string | undefined;
  fetch?: typeof fetch;
  now?: () => number;
  timeoutMs?: number;
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

function offerFromResult(result: Record<string, unknown>, position: number, query: string) {
  let sourceUrl = "";
  let store = typeof result.site_name === "string" ? result.site_name.trim() : "";
  try {
    const url = new URL(typeof result.url === "string" ? result.url : "");
    if (["https:", "http:"].includes(url.protocol) && !url.username && !url.password) {
      sourceUrl = url.toString();
      store ||= url.hostname.replace(/^www\./, "");
    }
  } catch { /* Las fuentes no válidas no se abren desde Android. */ }

  return {
    id: `${position}-${sourceUrl || query}`,
    store: store || "Fuente web",
    productName: typeof result.title === "string" && result.title.trim() ? result.title : query,
    price: "Ver en fuente",
    originalPrice: null,
    shipping: "Consulta en tienda",
    delivery: "Disponibilidad por confirmar",
    rating: "—",
    sourceUrl,
    summary: typeof result.snippet === "string" ? result.snippet : "Resultado encontrado en la web.",
    tags: ["Resultado web"],
    isBestValue: false,
  };
}

export function createSearchProducts(dependencies: Dependencies) {
  const fetcher = dependencies.fetch ?? fetch;
  const now = dependencies.now ?? Date.now;
  const requestTimes = new Map<string, number[]>();

  return async (request: Request): Promise<Response> => {
    if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
    if (request.method !== "POST") return json({ error: "Usa una solicitud POST para buscar." }, 405);

    let query = "";
    try {
      const body = await request.json();
      query = typeof body?.query === "string" ? body.query.trim() : "";
    } catch {
      return json({ error: "El cuerpo de la solicitud debe ser JSON válido." }, 400);
    }
    if (query.length < 2 || query.length > 200) {
      return json({ error: "La búsqueda debe tener entre 2 y 200 caracteres." }, 400);
    }

    // Límite didáctico por instancia: no es un contador distribuido.
    const time = now();
    for (const [key, times] of requestTimes) {
      if (!times.some(timestamp => timestamp > time - 60_000)) requestTimes.delete(key);
    }
    const client = request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || "local-client";
    const recent = (requestTimes.get(client) ?? []).filter(timestamp => timestamp > time - 60_000);
    if (recent.length >= 20) {
      return json({ error: "Demasiadas búsquedas. Inténtalo en un minuto." }, 429);
    }
    if (!requestTimes.has(client) && requestTimes.size >= 5000) {
      return json({ error: "Servicio ocupado. Inténtalo en un minuto." }, 429);
    }
    recent.push(time);
    requestTimes.set(client, recent);

    const apiKey = dependencies.getApiKey();
    if (!apiKey) {
      return json({ code: "TINYFISH_NOT_CONFIGURED", error: "La búsqueda en vivo aún no está configurada." }, 503);
    }
    const searchUrl = new URL("https://api.search.tinyfish.ai");
    searchUrl.searchParams.set("query", query);
    searchUrl.searchParams.set("purpose", "Encontrar páginas de compra y ofertas relevantes para comparar productos.");
    searchUrl.searchParams.set("location", "EC");
    searchUrl.searchParams.set("language", "es");

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), dependencies.timeoutMs ?? 15_000);
    try {
      const response = await fetcher(searchUrl, {
        headers: { "X-API-Key": apiKey, Accept: "application/json" },
        signal: controller.signal,
      });
      if (!response.ok) {
        console.error("TinyFish search failed", { status: response.status });
        return json({ error: "No fue posible consultar las fuentes web ahora." }, 502);
      }
      const payload = await response.json();
      if (!payload || !Array.isArray(payload.results)) {
        return json({ error: "La fuente web devolvió una respuesta no válida." }, 502);
      }
      const results = payload.results.filter((value: unknown) =>
        value !== null && typeof value === "object" && !Array.isArray(value));
      return json({
        query,
        offers: results.slice(0, 10).map((result: Record<string, unknown>, index: number) =>
          offerFromResult(result, index + 1, query)),
        searchedAt: new Date(now()).toISOString(),
      });
    } catch {
      // Evita registrar consultas, cabeceras o excepciones que puedan incluir la clave.
      console.error("TinyFish request could not be completed");
      return json({ error: "La búsqueda web no está disponible en este momento." }, 502);
    } finally {
      clearTimeout(timer);
    }
  };
}
