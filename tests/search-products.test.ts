import test from "node:test";
import assert from "node:assert/strict";
import { createSearchProducts } from "../supabase/functions/search-products/handler.ts";

const request = (body: unknown = { query: "Switch OLED" }) => new Request("https://example.test/search-products", {
  method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
});
const fakeKey = "fake-key-only-for-tests";
const result = { title: "Nintendo Switch OLED", url: "https://www.example.com/switch", snippet: "Oferta de tienda" };
const setup = (payload: unknown = { results: [result] }) => createSearchProducts({
  getApiKey: () => fakeKey, fetch: async () => Response.json(payload),
});

test("OPTIONS permite preflight CORS", async () => {
  const response = await setup()(new Request("https://example.test", { method: "OPTIONS" }));
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("Access-Control-Allow-Methods"), "POST, OPTIONS");
});

test("GET devuelve 405", async () => {
  assert.equal((await setup()(new Request("https://example.test"))).status, 405);
});

test("JSON mal formado devuelve 400", async () => {
  const response = await setup()(new Request("https://example.test", { method: "POST", body: "{" }));
  assert.equal(response.status, 400);
});

test("valida tipo y longitud sin llamar al proveedor", async () => {
  let calls = 0;
  const handler = createSearchProducts({ getApiKey: () => fakeKey, fetch: async () => {
    calls++; return Response.json({ results: [] });
  } });
  for (const body of [{}, null, { query: 4 }, { query: "x" }, { query: "   " }, { query: "x".repeat(201) }]) {
    assert.equal((await handler(request(body))).status, 400);
  }
  assert.equal(calls, 0);
});

test("admite consultas de exactamente 2 y 200 caracteres", async () => {
  const handler = setup();
  assert.equal((await handler(request({ query: "ab" }))).status, 200);
  assert.equal((await handler(request({ query: "x".repeat(200) }))).status, 200);
});

test("falta de secreto devuelve 503", async () => {
  const response = await createSearchProducts({ getApiKey: () => undefined })(request());
  assert.equal(response.status, 503);
  assert.equal((await response.json()).code, "TINYFISH_NOT_CONFIGURED");
});

test("envía query y clave al proveedor, y adapta sin inventar precios", async () => {
  const handler = createSearchProducts({ getApiKey: () => fakeKey, now: () => 100000, fetch: async (input, init) => {
    const url = new URL(String(input));
    assert.equal(url.origin, "https://api.search.tinyfish.ai");
    assert.equal(url.searchParams.get("query"), "Switch OLED");
    assert.equal(url.searchParams.get("location"), "EC");
    assert.equal(new Headers(init?.headers).get("X-API-Key"), fakeKey);
    return Response.json({ results: [result] });
  } });
  const response = await handler(request({ query: "  Switch OLED  " }));
  const body = await response.json();
  assert.equal(response.status, 200);
  assert.equal(body.query, "Switch OLED");
  assert.equal(body.offers[0].store, "example.com");
  assert.equal(body.offers[0].price, "Ver en fuente");
  assert.equal(body.offers[0].sourceUrl, result.url);
  assert.ok(!JSON.stringify(body).includes(fakeKey));
});

test("limita a diez resultados y descarta entradas no estructuradas", async () => {
  const response = await setup({ results: [null, 42, ...Array.from({ length: 15 }, () => result)] })(request());
  assert.equal((await response.json()).offers.length, 10);
});

test("no expone URLs peligrosas o con credenciales", async () => {
  const response = await setup({ results: ["javascript:alert(1)", "file:///secret", "https://user:password@example.com", "no-url"]
    .map(url => ({ url })) })(request());
  const body = await response.json();
  assert.ok(body.offers.every((offer: { sourceUrl: string }) => offer.sourceUrl === ""));
});

test("cero resultados produce lista vacía con 200", async () => {
  const response = await setup({ results: [] })(request());
  assert.equal(response.status, 200);
  assert.deepEqual((await response.json()).offers, []);
});

test("contrato incompatible del proveedor produce 502", async () => {
  assert.equal((await setup({ unexpected: true })(request())).status, 502);
});

test("fallo del proveedor no revela su cuerpo ni el secreto", async () => {
  const handler = createSearchProducts({ getApiKey: () => fakeKey, fetch: async () => new Response(fakeKey, { status: 401 }) });
  const response = await handler(request());
  assert.equal(response.status, 502);
  assert.ok(!(await response.text()).includes(fakeKey));
});

test("error de red y timeout producen 502", async () => {
  const failed = createSearchProducts({ getApiKey: () => fakeKey, fetch: async () => { throw new Error(fakeKey); } });
  assert.equal((await failed(request())).status, 502);
  const slow = createSearchProducts({ getApiKey: () => fakeKey, timeoutMs: 10, fetch: async (_input, init) =>
    new Promise((_resolve, reject) => init?.signal?.addEventListener("abort", () => reject(new Error("timeout")), { once: true })) });
  assert.equal((await slow(request())).status, 502);
});

test("solicitud 21 devuelve 429 y el límite se recupera", async () => {
  let time = 100000;
  const handler = createSearchProducts({ getApiKey: () => fakeKey, now: () => time, fetch: async () => Response.json({ results: [] }) });
  for (let i = 0; i < 20; i++) assert.equal((await handler(request())).status, 200);
  assert.equal((await handler(request())).status, 429);
  time += 60001;
  assert.equal((await handler(request())).status, 200);
});
