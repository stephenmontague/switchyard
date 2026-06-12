import type { CatalogEntryDto } from "@/lib/types";

// The reference Warehouse catalog, mirroring com.proxyapp.profile.WarehouseProfile. Used by the
// Catalog page's "Import warehouse profile" action to seed a fresh (or pre-Part-3) install. It's
// only a starting point — operators edit freely afterward, and can define entirely different
// types for any domain. Keep in sync with WarehouseProfile.catalog() so an import matches what
// the proxy boots with.
export const WAREHOUSE_CATALOG: CatalogEntryDto[] = [
  { type: "CONTAINER_PUTAWAY", direction: "CLOUD_TO_EDGE", codec: "json", cloudEndpoint: null, businessIdField: "containerId" },
  { type: "PUTAWAY_CONFIRM", direction: "EDGE_TO_CLOUD", codec: "json", cloudEndpoint: "/api/putaway-confirm", businessIdField: "containerId" },
  { type: "WAVE_RELEASE", direction: "CLOUD_TO_EDGE", codec: "json", cloudEndpoint: null, businessIdField: "orderId" },
  { type: "PICK_CONFIRM", direction: "EDGE_TO_CLOUD", codec: "json", cloudEndpoint: "/api/pick-confirm", businessIdField: "orderId" },
  { type: "CYCLE_COUNT_REQ", direction: "CLOUD_TO_EDGE", codec: "json", cloudEndpoint: null, businessIdField: "countId" },
  { type: "CYCLE_COUNT_CONFIRM", direction: "EDGE_TO_CLOUD", codec: "json", cloudEndpoint: "/api/cycle-count-confirm", businessIdField: "countId" },
];
