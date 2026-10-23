import { createSearchProducts } from "./handler.ts";

Deno.serve(createSearchProducts({
  getApiKey: () => Deno.env.get("TINYFISH_API_KEY"),
}));
